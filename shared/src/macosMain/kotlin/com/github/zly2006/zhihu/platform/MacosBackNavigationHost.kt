/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

@file:OptIn(
    androidx.compose.ui.InternalComposeUiApi::class,
    kotlinx.cinterop.ExperimentalForeignApi::class,
)

package com.github.zly2006.zhihu.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.backhandler.LocalCompatNavigationEventDispatcherOwner
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventInput
import platform.AppKit.NSEvent
import platform.AppKit.NSResponder
import platform.AppKit.NSWindow

private const val MACOS_ESCAPE_KEY_CODE = 53

private class MacosEscapeNavigationInput : NavigationEventInput() {
    fun dispatchBack() = dispatchOnBackCompleted()
}

private class MacosEscapeNavigationResponder(
    private val input: MacosEscapeNavigationInput,
) : NSResponder() {
    override fun keyDown(event: NSEvent) {
        if (event.keyCode.toInt() == MACOS_ESCAPE_KEY_CODE && !event.isARepeat()) {
            input.dispatchBack()
        } else {
            super.keyDown(event)
        }
    }
}

/**
 * 为 Compose Kotlin/Native macOS 窗口补齐 ESC 到返回事件的最后一段桥接。
 *
 * 这段代码看起来只是拦截一个按键，但它必须放在窗口宿主层，并且必须复用 Compose 窗口已有的
 * [NavigationEventDispatcher]。下面完整记录设计背景和取舍，避免以后因为看到实现较长，就把它重新
 * 简化成一个根节点 `onPreviewKeyEvent`，或者再次创建一套独立的 dispatcher。
 *
 * ## 用户需要的行为
 *
 * 桌面 JVM 和 Kotlin/Native macOS 上按一次 ESC，应当等价于 Android 上按一次系统返回。这里的
 * “返回”不是固定调用某一个 NavController，也不应根据当前是否显示详情页来猜测应该返回哪一栏。
 * 应用里同时存在对话框、弹出菜单、BottomSheet、页面局部状态、Navigation 返回栈和主分页状态；
 * 它们已经分别通过 Compose `BackHandler` 注册自己的返回行为。正确做法是只产生一次标准返回事件，
 * 由 Compose NavigationEvent 的优先级和后注册优先规则决定当前最上层的处理器，而不是在 ESC
 * 输入层复制一套业务导航判断。
 *
 * 期望的事件顺序是：
 *
 * 1. 当前获得焦点的 Compose 控件先收到 ESC。例如某个编辑器、菜单或未来新增的控件可以自行消费它；
 * 2. Compose 控件没有消费时，ESC 才转换成一次非预测式返回事件；
 * 3. Dialog、Popup、BottomSheet 等 overlay 的 BackHandler 优先关闭最上层内容；
 * 4. 没有 overlay 时，页面内部的返回处理器可以先收起局部状态；
 * 5. 再没有更内层的处理器时，应用的 NavController 弹出一层；
 * 6. 已经回到主导航但不在首页时，现有主分页 BackHandler 将分页切回首页；
 * 7. 首页且没有任何可关闭内容时不做业务导航，也不应连续退出多层。
 *
 * ## 曾经失败的实现及其根因
 *
 * 第一版实现从应用内容根部新建了 `NavigationEventDispatcher` 和 owner，再用
 * `LocalCompatNavigationEventDispatcherOwner` 覆盖 Compose 窗口提供的 owner；同时给根 `Box`
 * 增加 `focusable`、`FocusRequester` 和 `onPreviewKeyEvent`，在 ESC KeyUp 时向新 dispatcher
 * 派发返回。这个方案有两个根本问题。
 *
 * 第一，物理输入和返回处理器被拆到了两套 dispatcher。JVM Compose 窗口自己的
 * `BackNavigationEventInput` 始终注册在窗口原有 dispatcher 上，而应用内 `BackHandler` 因为读取了
 * 被覆盖的 CompositionLocal，注册到了应用新建的 dispatcher 上。物理 ESC 到达旧 dispatcher 时看不到
 * 页面处理器；Dialog 和 Popup 又会优先查找窗口内部 owner，因此 overlay 与普通页面甚至可能分别落在
 * 两套返回注册表中。表面症状可以从“部分页面不能返回”恶化成“所有页面都没有反应”，但根因不是某个
 * `enabled` 条件或者某个详情 controller，而是输入源和 handler 根本没有连接到同一条事件链。
 *
 * 第二，根节点的 `onPreviewKeyEvent` 在焦点子节点之前执行。即使它偶尔能收到 ESC，也会先于文本输入、
 * 菜单或其他焦点控件消费事件，破坏桌面平台应有的“焦点控件优先、返回作为 fallback”语义。为了让根
 * 节点收到按键而强行请求焦点，还可能改变正常焦点归属。对话框和 popup 在 Compose Native 中可能位于
 * scene 的独立 layer，依赖根节点焦点链覆盖所有 layer 也不可靠。
 *
 * 离屏 Compose UI 调试曾让这种实现看似可用，因为测试协议把按键或返回直接注入 Compose 测试场景，
 * 不经过真实 AppKit `NSWindow`、第一响应者和 `NSView.keyDown`。它可以验证某个 handler 收到人工事件，
 * 却不能证明真实 `.app` 中物理 ESC 的 AppKit 输入源与那个 handler 使用同一个 dispatcher。因此本功能
 * 的最终验收必须包含打包后的真实 macOS App 和物理/系统键盘事件，离屏测试只能补充验证导航结果，
 * 不能再作为窗口输入链正确的唯一证据。
 *
 * ## JVM 与 Kotlin/Native macOS 的差异
 *
 * JVM Desktop 的 Compose 窗口已经完整实现了所需链路。它先把 KeyEvent 交给 window preview、当前
 * Compose scene 和 window 普通 key handler；只有这些入口都没有消费事件时，窗口持有的
 * `BackNavigationEventInput` 才把 ESC KeyDown 转换成返回，并派发到同一个窗口 dispatcher。项目在 JVM
 * source set 中只需使用官方 `ui-backhandler-desktop` 的 `BackHandler`，不能再添加 owner 或 ESC 输入源。
 *
 * Compose 1.11.1 的 Kotlin/Native macOS `ComposeWindow` 也已经创建
 * `DefaultArchitectureComponentsOwner`，并通过平台 CompositionLocal 向 Dialog、Popup 和应用内容提供
 * 同一个 dispatcher；缺少的只有窗口按键处理的最后一步。它的原生 `NSView.keyDown` 会先调用
 * `scene.sendKeyEvent`，若 Compose scene 返回未消费，才调用 `super.keyDown`。但与 JVM 不同，它没有在
 * 这个 fallback 位置调用 `BackNavigationEventInput`，于是未消费的 ESC 最终沿 AppKit responder chain
 * 到达系统并产生提示音，而不是成为返回事件。
 *
 * ## 为什么使用 AppKit responder chain
 *
 * AppKit 本来就把未处理的键盘事件从 first responder 沿视图层级向上传递到 content view、window 和
 * application。Compose Native 的 content view 只有在 `scene.sendKeyEvent` 返回 false 时才调用
 * `super.keyDown`，因此把本 responder 插入 content view 与其原 next responder 之间，恰好得到和 JVM
 * 相同的时机：Compose 当前焦点控件和所有 scene layer 已经先处理过，只有未消费事件才会到达这里。
 * 这比全局 NSEvent monitor 更合适；monitor 在窗口分发前执行，会重新产生 Preview 抢占焦点控件的问题，
 * 而且更难限定生命周期和目标窗口。
 *
 * 本应用还在 Compose content view 上叠加了原生 AppKit source-list 侧栏。真实 App 验证发现，点击侧栏后
 * `NSOutlineView` 成为 first responder；AppKit 的默认 outline 实现会截住 ESC，却既不改变选择也不继续
 * 调用 next responder，于是事件到不了 Compose content view 和本 responder。这不意味着应该改用窗口级
 * monitor 抢在所有控件前面。侧栏本身没有“ESC 取消选择”的产品语义，因此它的自定义 subclass 只对
 * ESC 显式调用 `nextResponder.keyDown`，其余按键仍交给 `NSOutlineView`，保留方向键、选择和系统 source
 * list 行为。这样原生控件真正处理的按键仍由控件消费，没有实际处理的 ESC 才继续进入统一返回链。
 * 如果以后增加其他会成为 first responder 的原生控件，也应先判断该控件是否拥有自己的 ESC 语义；
 * 没有才在控件边界继续转发，不能为了省事恢复成抢占整个窗口的全局监听。
 *
 * responder 收到 ESC 后不直接调用页面回调，而是驱动一个标准 [NavigationEventInput]。该 input 注册在
 * Compose 窗口通过 [LocalCompatNavigationEventDispatcherOwner] 暴露的现有 dispatcher 上，所以 Dialog、
 * Popup、页面 BackHandler、NavController 和主分页逻辑仍在同一注册表中按框架规则竞争。这里绝不能
 * `NavigationEventDispatcher()`，也绝不能再次提供新的 owner。长按 ESC 产生的 repeat 被忽略，避免一次
 * 按住按键连续弹完整个返回栈；每次独立 KeyDown 只完成一次返回。
 *
 * 安装时保存 content view 原来的 next responder，让本 responder 再指向它，从而维持原 responder chain；
 * composition 离开时只在链仍由本实现持有的情况下恢复原引用，并从原 dispatcher 移除 input。这样不会
 * 永久劫持窗口，也不会在窗口重建或测试销毁后留下引用。若未来 Compose 自己补齐 macOS
 * `BackNavigationEventInput`，应删除本桥接而不是让两个输入源同时存在，否则同一次 ESC 可能派发两次。
 * 升级 Compose 时应优先检查 `ComposeWindow.macos.kt` 的 `keyDown`/`onKeyboardEvent`：一旦它像 JVM 一样
 * 在 `scene.sendKeyEvent` 之后调用 back input，这段兼容代码就已经完成历史使命。
 *
 * ## 为什么不把 Compose 源码放进 third_party
 *
 * `ComposeWindow.macos.kt` 属于 Compose UI 核心 macOS KLIB，不是可以靠复制一个同包同名文件覆盖的
 * 独立小模块。应用继续依赖官方 `compose.ui` 时复制源码会产生重复声明；排除官方 artifact 后，又必须
 * 一并维护 Compose UI 大量 internal API、Skiko、Kotlin/Native 编译器及二进制版本关系。为了一个几行的
 * 窗口 fallback 把整个 UI runtime vendoring 到 `third_party`，会显著增加仓库体积、构建时间、安全更新
 * 和 Compose 升级成本。当前 responder 桥接只使用 Compose 暴露的窗口 owner、NavigationEvent 公共输入
 * 契约和 AppKit 正式 responder chain，修改面小，也容易在上游修复发布后删除。长期应将 Native 窗口缺失
 * 的 fallback 报告或提交给 Compose Multiplatform 上游，而不是把框架 fork 永久变成应用的一部分。
 *
 * ## 文本输入焦点的取舍
 *
 * 本实现不主动调用 `LocalFocusManager.clearFocus()`。根节点无法可靠判断当前焦点是否属于可编辑文本，
 * 全局清焦点会使按钮等普通焦点目标也吞掉第一次 ESC，并让 Android 返回与桌面返回产生不必要差异；
 * 逐个修改项目中大量 TextField 又远超本功能范围。由于 responder 位于 `scene.sendKeyEvent` 之后，任何
 * 真正需要 ESC 取消编辑的输入组件都可以在自身焦点范围消费按键，消费后事件不会到达这里。如果未来
 * 产品明确要求某类搜索框或编辑器第一次 ESC 清除焦点，应在那个共享输入组件内部实现，并且只在它
 * 自己获得焦点时消费；不要把清焦点逻辑塞回这个窗口级返回适配器。
 */
@Composable
fun MacosBackNavigationHost(
    window: NSWindow,
    content: @Composable () -> Unit,
) {
    val dispatcherOwner = checkNotNull(LocalCompatNavigationEventDispatcherOwner.current) {
        "Compose window navigation event dispatcher is unavailable"
    }
    val dispatcher = dispatcherOwner.navigationEventDispatcher
    val input = remember { MacosEscapeNavigationInput() }
    val responder = remember(input) { MacosEscapeNavigationResponder(input) }

    DisposableEffect(window, dispatcher, input, responder) {
        val contentView = checkNotNull(window.contentView) {
            "Compose window content view is unavailable"
        }
        val originalNextResponder = contentView.nextResponder

        dispatcher.addInput(input)
        responder.nextResponder = originalNextResponder
        contentView.nextResponder = responder

        onDispose {
            if (contentView.nextResponder === responder) {
                contentView.nextResponder = originalNextResponder
            }
            responder.nextResponder = null
            dispatcher.removeInput(input)
        }
    }

    content()
}
