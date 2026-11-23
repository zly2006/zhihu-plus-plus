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

package com.github.zly2006.zhihu.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.github.zly2006.zhihu.theme.LocalUiBackdrop
import com.github.zly2006.zhihu.theme.LocalUiStyle
import com.github.zly2006.zhihu.theme.NoOpUiBackdrop
import com.github.zly2006.zhihu.theme.UiStyle
import com.github.zly2006.zhihu.theme.rememberUiBackdrop
import com.github.zly2006.zhihu.theme.settingsAmbientBackground
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 三风格统一的页面外壳。
 *
 * - Material 3：直通 M3 `Scaffold`，零额外开销；
 * - Miuix：使用 HyperOS `surface` 底色，并录制 `miuix-blur` 背景供顶/底栏模糊采样；
 * - Liquid Glass：录制 `kyant0:backdrop`，支持设置页多色环境背景（[settingsAmbientBackground]）；
 *   内容子树强制重置 [LocalUiBackdrop] 为 [NoOpUiBackdrop] 防递归崩溃。
 */
@Composable
fun AppScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    containerColor: Color = if (LocalUiStyle.current == UiStyle.Miuix) {
        MiuixTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.background
    },
    contentColor: Color = contentColorFor(containerColor),
    contentWindowInsets: WindowInsets = ScaffoldDefaults.contentWindowInsets,
    enableBackdrop: Boolean = true,
    settingsAmbientBackground: Boolean = false,
    content: @Composable (PaddingValues) -> Unit,
) {
    val style = LocalUiStyle.current
    val isLiquidGlass = style == UiStyle.LiquidGlass
    val resolvedContainerColor = when {
        style == UiStyle.Miuix &&
            (settingsAmbientBackground || containerColor == MaterialTheme.colorScheme.surfaceContainer) ->
            MiuixTheme.colorScheme.surface
        else -> containerColor
    }
    val resolvedContentColor = if (resolvedContainerColor == containerColor) {
        contentColor
    } else {
        contentColorFor(resolvedContainerColor)
    }
    val backdrop = rememberUiBackdrop(
        style = style,
        enabled = enableBackdrop && style != UiStyle.Material3,
    )
    val bodyBackdropModifier = if (isLiquidGlass && settingsAmbientBackground) {
        Modifier
            .fillMaxSize()
            .then(backdrop.source)
            .settingsAmbientBackground()
    } else {
        backdrop.source
    }

    CompositionLocalProvider(LocalUiBackdrop provides backdrop) {
        Scaffold(
            modifier = modifier,
            topBar = topBar,
            bottomBar = bottomBar,
            snackbarHost = snackbarHost,
            floatingActionButton = floatingActionButton,
            floatingActionButtonPosition = floatingActionButtonPosition,
            containerColor = resolvedContainerColor,
            contentColor = resolvedContentColor,
            contentWindowInsets = contentWindowInsets,
        ) { padding ->
            Box(modifier = bodyBackdropModifier) {
                CompositionLocalProvider(LocalUiBackdrop provides NoOpUiBackdrop) {
                    content(padding)
                }
            }
        }
    }
}
