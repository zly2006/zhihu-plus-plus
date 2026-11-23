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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.theme.LocalUiBackdrop
import com.github.zly2006.zhihu.theme.NoOpUiBackdrop
import com.github.zly2006.zhihu.theme.liquidGlassSemanticTokens

/**
 * 浮空玻璃工具条（仅用于 AppScaffold 的操作槽，采样不含操作层自身的正文）。
 * 内部重置 [LocalUiBackdrop] 为 [NoOpUiBackdrop]，避免子按钮产生双层折射。
 */
@Composable
fun LiquidToolbar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val backdrop = LocalUiBackdrop.current
    val tokens = liquidGlassSemanticTokens()
    val shape = RoundedCornerShape(tokens.floatingCornerRadius)
    val surfaceModifier = if (backdrop.isActive) {
        backdrop.chromeSurface(
            shape = shape,
            tint = tokens.chromeTint,
            floating = true,
        )
    } else {
        Modifier.background(tokens.groupedSurface, shape)
    }
    Row(
        modifier = modifier
            .then(surfaceModifier)
            .heightIn(min = tokens.floatingControlMinHeight)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalUiBackdrop provides NoOpUiBackdrop) {
            content()
        }
    }
}
