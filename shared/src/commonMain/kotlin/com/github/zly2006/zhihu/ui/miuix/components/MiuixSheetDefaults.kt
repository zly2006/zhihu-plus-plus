/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * Licensed under AGPL-3.0-only.
 */

package com.github.zly2006.zhihu.ui.miuix.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.platform.rememberSettingBoolean
import com.github.zly2006.zhihu.ui.components.DISABLE_BOTTOM_SHEET_ROUNDED_CORNERS_PREFERENCE_KEY
import top.yukonga.miuix.kmp.layout.BottomSheetDefaults

/**
 * miuix 弹层顶部圆角，跟随「禁用 popup 圆角」设置。
 *
 * M3 侧由 `MyModalBottomSheet` 消费同一个偏好；miuix 的 `WindowBottomSheet` 圆角是构造参数，
 * 没有全局钩子，所以每个弹层都要显式传这个值，新增弹层时别漏。
 */
@Composable
fun miuixSheetCornerRadius(): Dp =
    if (rememberSettingBoolean(DISABLE_BOTTOM_SHEET_ROUNDED_CORNERS_PREFERENCE_KEY, false)) {
        0.dp
    } else {
        BottomSheetDefaults.cornerRadius
    }

/**
 * miuix 弹层内容的底部安全区。
 *
 * miuix 的 `BottomSheetContentLayout` 只做了 `imePadding()`，完全没有处理手势导航条，
 * 内容会压在小白条下面。这里只补「未被键盘遮住」的那部分导航栏高度：键盘弹出时弹层整体已被
 * ime 抬起，再叠加导航栏会多出一截空隙。
 *
 * 必须加在**弹层内容内部**（`WindowBottomSheet` 的 content lambda 里），不能算好了走
 * `insideMargin`：内容跑在 Dialog 窗口里，而 `insideMargin` 的实参在外层主窗口求值，
 * 两个窗口拿到的 ime inset 不一定一致。背景仍然铺满到屏幕底部，只有内容被抬起。
 */
@Composable
fun Modifier.miuixSheetBottomInsets(): Modifier =
    windowInsetsPadding(WindowInsets.navigationBars.exclude(WindowInsets.ime))

/**
 * miuix 弹层的横向内边距。
 *
 * 横向留白只能有一个来源：`WindowBottomSheet` 默认就带 24dp 的 `insideMargin`，内容再自己写
 * `padding(horizontal = ...)` 会直接叠加，行和卡片会被挤成中间一条。所以内容不加横向 padding，
 * 一律由这个值决定。
 *
 * 例外是整幅列表（评论、赞同者名单等）：那里传 `DpSize(0.dp, 0.dp)`，把横向留白交给
 * `LazyColumn` 的 `contentPadding`，这样滚动条和点击涟漪才能铺满弹层宽度。
 */
val MiuixSheetInsideMargin = DpSize(16.dp, 0.dp)
