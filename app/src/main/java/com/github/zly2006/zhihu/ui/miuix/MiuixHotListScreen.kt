/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * Licensed under AGPL-3.0-only.
 */

package com.github.zly2006.zhihu.ui.miuix

import android.content.Context
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.github.zly2006.zhihu.theme.getMiuixAppBarColor
import com.github.zly2006.zhihu.theme.installerMiuixBlurEffect
import com.github.zly2006.zhihu.theme.rememberMiuixBlurBackdrop
import com.github.zly2006.zhihu.ui.HotListScreen
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
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
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE) }
    var blurEnabled by remember { mutableStateOf(preferences.getBoolean("blurEnabled", true)) }
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
