/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 * Licensed under AGPL-3.0-only.
 */

package com.github.zly2006.zhihu.ui.miuix.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator

/**
 * 列表首次加载（无数据且正在加载）时居中显示的转圈，避免白屏。
 * 下拉刷新进行时不显示，避免与下拉刷新动画叠加。
 */
@Composable
fun MiuixListLoadingIndicator(
    isLoading: Boolean,
    isEmpty: Boolean,
    isPullToRefresh: Boolean = false,
    modifier: Modifier = Modifier,
) {
    if (isEmpty && isLoading && !isPullToRefresh) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}
