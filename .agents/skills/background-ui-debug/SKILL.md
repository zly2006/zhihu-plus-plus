---
name: background-ui-debug
description: 在不创建、激活或切换任何前台窗口的前提下，通过项目的 debug-only 离屏 Compose UI 控制接口检查语义树、点击、输入、滚动、等待和截图。适用于 macOS Kotlin/Native UI 回归、页面卡死、导航完成度和逐按钮验收；禁止用 AppleScript、System Events、open、桌面截图或坐标点击替代。
---

<!--
Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
Copyright (C) 2024-2026, zly2006 <i@zly2006.me>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License as published by
the Free Software Foundation (version 3 only).

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program. If not, see <https://www.gnu.org/licenses/>.
-->

# 后台 UI 调试

## 理念

UI 自动化的目标是证明用户可达状态，不是表演鼠标操作。调试器应直接驱动产品的 Compose 语义树，在内存中的离屏 Skia 画布完成布局和绘制，并留下可审计的输入、输出与截图。

必须遵守以下边界：

- 严禁创建、显示、激活或切换应用窗口；严禁让 Dock 图标、菜单栏或焦点发生变化。
- 严禁使用 `open`、`osascript`、AppleScript、System Events、桌面截图、全局键鼠注入和屏幕坐标。
- 只能运行独立的 debug 调试二进制。正式应用和 release 二进制不得依赖、注册或包含控制协议。
- 只能用 `testTag`、文本、content description 等语义选择器操作；找不到目标就是失败，不能退回坐标猜测。
- 截图必须来自离屏 Compose 画布，不得捕获用户桌面或其他应用。
- 每次动作前先读取当前语义状态，动作后等待明确终态并再次读取；超时、异常、空白画面和状态未变化都算失败。
- 默认不修改真实账号和远端数据。涉及发布、关注、投票、删除等副作用时，必须已有明确任务授权并记录副作用。

## 工作流

1. 先确认生产应用没有运行，并检查当前任务不会启动 `macosApp`。
2. 用 `scripts/start_background_ui_debug.sh` 构建并启动离屏调试器。脚本只 `exec` 调试 kexe，不调用任何窗口 API。
3. 发送一行一个 JSON 命令。先 `state` 和 `dump`，再按语义节点执行 `click`、`input`、`scroll`、`back`、`wait` 或 `screenshot`。
4. 对每个页面枚举所有可点击节点；逐项操作后检查目标页面、返回路径、异常输出和耗时。破坏性动作只验证到提交前状态。
5. 发现卡死时保留最后一个命令、动作前后语义树、离屏截图、耗时和 stderr；先定位确定根因，再修改生产代码。
6. 修改后重跑相同命令序列，随后构建 release，并验证 release 二进制不含协议标记 `ZHPP_BACKGROUND_UI_DEBUG_V1`。

协议字段、选择器和命令示例见 [references/protocol.md](references/protocol.md)。

## 证据标准

一次有效验收至少包含：

- 调试二进制的构建类型和进程路径；
- 每个动作的请求 id、语义选择器、成功或失败响应及耗时；
- 关键页面动作前后的语义树差异；
- 来自离屏画布的 PNG；
- 页面级超时与进程终态；
- release 隔离检查。

进程存活、命令返回 `ok` 或生成非空 PNG 都不能单独证明页面可用。必须验证目标语义状态出现，且离屏图像包含真实绘制内容。
