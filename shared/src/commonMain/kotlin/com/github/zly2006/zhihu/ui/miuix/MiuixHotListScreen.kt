/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * Licensed under AGPL-3.0-only.
 */

package com.github.zly2006.zhihu.ui.miuix

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.shared.platform.rememberSettingBoolean
import com.github.zly2006.zhihu.shared.platform.rememberSettingsStore
import com.github.zly2006.zhihu.theme.getMiuixAppBarColor
import com.github.zly2006.zhihu.theme.installerMiuixBlurEffect
import com.github.zly2006.zhihu.theme.rememberMiuixBlurBackdrop
import com.github.zly2006.zhihu.ui.HotListScreen
import com.github.zly2006.zhihu.ui.components.AutoHideTopBar
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.TopAppBar

@Composable
fun MiuixHotListScreen(
    innerPadding: PaddingValues = PaddingValues(0.dp),
    onTestRefreshClick: (() -> Unit)? = null,
    onTestLoadMore: (() -> Unit)? = null,
) {
    val settings = rememberSettingsStore()
    val blurEnabled = rememberSettingBoolean("blurEnabled", true, settings)
    val backdrop = rememberMiuixBlurBackdrop(blurEnabled)
    val scrollBehavior = MiuixScrollBehavior()

    Scaffold(
        topBar = {
            AutoHideTopBar {
                TopAppBar(
                    modifier = Modifier.installerMiuixBlurEffect(backdrop),
                    color = backdrop.getMiuixAppBarColor(),
                    title = "热榜",
                    scrollBehavior = scrollBehavior,
                )
            }
        },
    ) { padding ->
        HotListScreen(
            innerPadding = PaddingValues(0.dp),
            onTestRefreshClick = onTestRefreshClick,
            onTestLoadMore = onTestLoadMore,
            backdrop = backdrop,
            scrollBehavior = scrollBehavior,
            contentTopPadding = padding.calculateTopPadding(),
        )
    }
}
