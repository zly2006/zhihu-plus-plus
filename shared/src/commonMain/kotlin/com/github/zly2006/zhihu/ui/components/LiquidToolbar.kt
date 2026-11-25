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

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.theme.LocalUiBackdrop
import com.github.zly2006.zhihu.theme.NoOpUiBackdrop
import com.github.zly2006.zhihu.theme.liquidGlassSemanticTokens

/** Apple iOS 26 浮空玻璃工具条标准胶囊高度，与右侧独立圆形按钮保持同一基准线。 */
internal val LiquidToolbarHeight = 52.dp

/**
 * 浮空玻璃工具条分组胶囊（仅用于 AppScaffold 的操作槽，采样不含操作层自身的正文）。
 *
 * 遵循 Apple iOS 26 Liquid Glass HIG：
 * - 单层玻璃胶囊包裹同组操作，内部重置 [LocalUiBackdrop] 为 [NoOpUiBackdrop]，禁止 Glass-on-Glass 嵌套折射；
 * - 胶囊高度 52dp、圆角 `CircleShape`（半径 26dp），内边距 4dp，配合内部 44dp `CircleShape`（半径 22dp）
 *   的 [LiquidToolbarItem] 保持同心圆角几何。
 */
@Composable
fun LiquidToolbar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    val backdrop = LocalUiBackdrop.current
    val tokens = liquidGlassSemanticTokens()
    val surfaceModifier = if (backdrop.isActive) {
        backdrop.chromeSurface(
            shape = CircleShape,
            tint = tokens.chromeTint,
            floating = true,
        )
    } else {
        Modifier.background(tokens.groupedSurface, CircleShape)
    }
    Row(
        modifier = modifier
            .then(surfaceModifier)
            .height(LiquidToolbarHeight)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalUiBackdrop provides NoOpUiBackdrop) {
            content()
        }
    }
}

/**
 * [LiquidToolbar] 内部的平铺交互项：无嵌套玻璃边框/折射，仅在按压或激活态提供柔和同心圆角底色。
 */
@Composable
fun LiquidToolbarItem(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    active: Boolean = false,
    contentColor: Color = MaterialTheme.colorScheme.onSurface,
    activeContainerColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
    contentPadding: PaddingValues = PaddingValues(horizontal = 12.dp),
    content: @Composable RowScope.() -> Unit,
) {
    val tokens = liquidGlassSemanticTokens()
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val backgroundColor by animateColorAsState(
        targetValue = when {
            !enabled -> Color.Transparent
            pressed && active -> activeContainerColor.copy(alpha = (activeContainerColor.alpha + 0.08f).coerceAtMost(0.35f))
            pressed -> tokens.chromePressedTint
            active -> activeContainerColor
            else -> Color.Transparent
        },
        animationSpec = tween(durationMillis = 120),
        label = "liquidToolbarItemBg",
    )
    val resolvedContentColor by animateColorAsState(
        targetValue = if (enabled) contentColor else contentColor.copy(alpha = 0.38f),
        animationSpec = tween(durationMillis = 120),
        label = "liquidToolbarItemContent",
    )
    Row(
        modifier = modifier
            .height(44.dp)
            .defaultMinSize(minWidth = 44.dp)
            .clip(CircleShape)
            .background(backgroundColor, CircleShape)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ).padding(contentPadding),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(LocalContentColor provides resolvedContentColor) {
            ProvideTextStyle(MaterialTheme.typography.labelLarge) {
                content()
            }
        }
    }
}

/** [LiquidToolbar] 同组内相邻控件之间的柔和细分割线。 */
@Composable
fun LiquidToolbarDivider(
    visible: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val color by animateColorAsState(
        targetValue = if (visible) {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)
        } else {
            Color.Transparent
        },
        animationSpec = tween(durationMillis = 120),
        label = "liquidToolbarDivider",
    )
    Box(
        modifier = modifier
            .width(1.dp)
            .height(18.dp)
            .background(color),
    )
}
