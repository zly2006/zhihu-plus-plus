/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * Licensed under AGPL-3.0-only.
 */

package com.github.zly2006.zhihu.ui.miuix.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
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
