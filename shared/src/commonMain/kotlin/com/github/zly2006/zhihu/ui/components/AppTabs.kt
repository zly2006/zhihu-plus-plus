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

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabIndicatorScope
import androidx.compose.material3.TabRowDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.github.zly2006.zhihu.theme.LocalUiStyle
import com.github.zly2006.zhihu.theme.ThemeManager
import com.github.zly2006.zhihu.theme.UiStyle
import com.github.zly2006.zhihu.theme.liquidGlassSemanticTokens
import top.yukonga.miuix.kmp.theme.MiuixTheme

/** True 表示由外层 AppPrimaryTabRow 统一绘制滑动选中底块；否则由单个 AppTab 自行绘制选中胶囊。 */
private val LocalAppSegmentedRow = staticCompositionLocalOf { false }

@Composable
fun AppTab(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    text: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    selectedContentColor: Color = LocalContentColor.current,
    unselectedContentColor: Color = selectedContentColor,
    interactionSource: MutableInteractionSource? = null,
) {
    val inSegmentedRow = LocalAppSegmentedRow.current
    when (LocalUiStyle.current) {
        UiStyle.Material3 -> {
            Tab(
                selected = selected,
                onClick = onClick,
                modifier = modifier,
                enabled = enabled,
                text = text,
                icon = icon,
                selectedContentColor = selectedContentColor,
                unselectedContentColor = unselectedContentColor,
                interactionSource = interactionSource,
            )
        }
        UiStyle.Miuix -> {
            val selectedBg = if (selected && !inSegmentedRow) {
                MiuixTheme.colorScheme.primaryContainer
            } else {
                Color.Transparent
            }
            val contentColor = if (selected) {
                if (inSegmentedRow) MiuixTheme.colorScheme.onSurface else MiuixTheme.colorScheme.onPrimaryContainer
            } else {
                MiuixTheme.colorScheme.onSurfaceSecondary
            }
            Box(
                modifier = modifier
                    .heightIn(min = 42.dp)
                    .padding(horizontal = if (inSegmentedRow) 0.dp else 4.dp, vertical = if (inSegmentedRow) 0.dp else 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(selectedBg)
                    .selectable(
                        selected = selected,
                        onClick = onClick,
                        enabled = enabled,
                        role = Role.Tab,
                        interactionSource = interactionSource ?: remember { MutableInteractionSource() },
                        indication = null,
                    ).padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                CompositionLocalProvider(LocalContentColor provides contentColor) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        icon?.invoke()
                        text?.invoke()
                    }
                }
            }
        }
        UiStyle.LiquidGlass -> {
            val tokens = liquidGlassSemanticTokens()
            val selectedSurfaceMod = if (selected && !inSegmentedRow) {
                Modifier.liquidGlassSurface(
                    shape = RoundedCornerShape(tokens.groupedCornerRadius),
                    tint = tokens.chromeTint,
                    floating = true,
                )
            } else {
                Modifier
            }
            Box(
                modifier = modifier
                    .fillMaxHeight()
                    .heightIn(min = tokens.floatingControlMinHeight)
                    .padding(horizontal = if (inSegmentedRow) 0.dp else 4.dp, vertical = if (inSegmentedRow) 0.dp else 4.dp)
                    .then(selectedSurfaceMod)
                    .clip(RoundedCornerShape(tokens.groupedCornerRadius))
                    .selectable(
                        selected = selected,
                        onClick = onClick,
                        enabled = enabled,
                        role = Role.Tab,
                        interactionSource = interactionSource ?: remember { MutableInteractionSource() },
                        indication = null,
                    ).padding(horizontal = 12.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        icon?.invoke()
                        text?.invoke()
                    }
                }
            }
        }
    }
}

@Composable
fun AppTab(
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    selectedContentColor: Color = LocalContentColor.current,
    unselectedContentColor: Color = selectedContentColor,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val inSegmentedRow = LocalAppSegmentedRow.current
    when (LocalUiStyle.current) {
        UiStyle.Material3 -> {
            Tab(
                selected = selected,
                onClick = onClick,
                modifier = modifier,
                enabled = enabled,
                selectedContentColor = selectedContentColor,
                unselectedContentColor = unselectedContentColor,
                interactionSource = interactionSource,
                content = content,
            )
        }
        UiStyle.Miuix -> {
            val selectedBg = if (selected && !inSegmentedRow) {
                MiuixTheme.colorScheme.primaryContainer
            } else {
                Color.Transparent
            }
            val contentColor = if (selected) {
                if (inSegmentedRow) MiuixTheme.colorScheme.onSurface else MiuixTheme.colorScheme.onPrimaryContainer
            } else {
                MiuixTheme.colorScheme.onSurfaceSecondary
            }
            Column(
                modifier = modifier
                    .heightIn(min = 42.dp)
                    .padding(horizontal = if (inSegmentedRow) 0.dp else 4.dp, vertical = if (inSegmentedRow) 0.dp else 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(selectedBg)
                    .selectable(
                        selected = selected,
                        onClick = onClick,
                        enabled = enabled,
                        role = Role.Tab,
                        interactionSource = interactionSource ?: remember { MutableInteractionSource() },
                        indication = null,
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CompositionLocalProvider(LocalContentColor provides contentColor) {
                    content()
                }
            }
        }
        UiStyle.LiquidGlass -> {
            val tokens = liquidGlassSemanticTokens()
            val selectedSurfaceMod = if (selected && !inSegmentedRow) {
                Modifier.liquidGlassSurface(
                    shape = RoundedCornerShape(tokens.groupedCornerRadius),
                    tint = tokens.chromeTint,
                    floating = true,
                )
            } else {
                Modifier
            }
            Column(
                modifier = modifier
                    .heightIn(min = tokens.floatingControlMinHeight)
                    .padding(horizontal = if (inSegmentedRow) 0.dp else 4.dp, vertical = if (inSegmentedRow) 0.dp else 4.dp)
                    .then(selectedSurfaceMod)
                    .clip(RoundedCornerShape(tokens.groupedCornerRadius))
                    .selectable(
                        selected = selected,
                        onClick = onClick,
                        enabled = enabled,
                        role = Role.Tab,
                        interactionSource = interactionSource ?: remember { MutableInteractionSource() },
                        indication = null,
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
                    content()
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPrimaryTabRow(
    selectedTabIndex: Int,
    modifier: Modifier = Modifier,
    selectionPosition: Float? = null,
    containerColor: Color = TabRowDefaults.primaryContainerColor,
    contentColor: Color = TabRowDefaults.primaryContentColor,
    indicator: @Composable TabIndicatorScope.() -> Unit = {
        TabRowDefaults.PrimaryIndicator(
            Modifier.tabIndicatorOffset(selectedTabIndex, matchContentSize = true),
            width = Dp.Unspecified,
        )
    },
    divider: @Composable () -> Unit = { HorizontalDivider() },
    tabs: @Composable () -> Unit,
) {
    when (LocalUiStyle.current) {
        UiStyle.Material3 -> {
            androidx.compose.material3.PrimaryTabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = modifier,
                containerColor = containerColor,
                contentColor = contentColor,
                indicator = indicator,
                divider = divider,
                tabs = tabs,
            )
        }
        UiStyle.Miuix -> {
            val isDark = ThemeManager.isDarkTheme()
            val trackColor = if (isDark) {
                MiuixTheme.colorScheme.surfaceContainer
            } else {
                MiuixTheme.colorScheme.secondaryContainer
            }
            val indicatorColor = if (isDark) {
                MiuixTheme.colorScheme.secondaryContainer
            } else {
                MiuixTheme.colorScheme.surfaceContainer
            }
            val animatedPosition by animateFloatAsState(
                targetValue = selectedTabIndex.coerceAtLeast(0).toFloat(),
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "MiuixTabIndicator",
            )
            CompositionLocalProvider(LocalAppSegmentedRow provides true) {
                androidx.compose.material3.PrimaryTabRow(
                    selectedTabIndex = selectedTabIndex.coerceAtLeast(0),
                    modifier = modifier
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(trackColor)
                        .padding(3.dp)
                        .fillMaxWidth(),
                    containerColor = Color.Transparent,
                    contentColor = MiuixTheme.colorScheme.onSurface,
                    indicator = {
                        Box(
                            Modifier
                                .fillMaxHeight()
                                .tabIndicatorLayout { measurable, constraints, positions ->
                                    if (positions.isEmpty()) {
                                        layout(constraints.maxWidth, constraints.maxHeight) {}
                                    } else {
                                        val position = (selectionPosition ?: animatedPosition)
                                            .coerceIn(0f, positions.lastIndex.toFloat())
                                        val fromIndex = position.toInt()
                                        val toIndex = (fromIndex + 1).coerceAtMost(positions.lastIndex)
                                        val fraction = position - fromIndex
                                        val left = positions[fromIndex].left +
                                            (positions[toIndex].left - positions[fromIndex].left) * fraction
                                        val width = positions[fromIndex].width +
                                            (positions[toIndex].width - positions[fromIndex].width) * fraction
                                        val indicatorWidth = width.roundToPx()
                                        val placeable = measurable.measure(
                                            constraints.copy(
                                                minWidth = indicatorWidth,
                                                maxWidth = indicatorWidth,
                                            ),
                                        )
                                        layout(constraints.maxWidth, constraints.maxHeight) {
                                            placeable.placeRelative(left.roundToPx(), 0)
                                        }
                                    }
                                }.zIndex(-1f)
                                .clip(RoundedCornerShape(11.dp))
                                .background(indicatorColor),
                        )
                    },
                    divider = {},
                    tabs = tabs,
                )
            }
        }
        UiStyle.LiquidGlass -> {
            val tokens = liquidGlassSemanticTokens()
            val animatedPosition by animateFloatAsState(
                targetValue = selectedTabIndex.coerceAtLeast(0).toFloat(),
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "LiquidGlassTabIndicator",
            )
            CompositionLocalProvider(LocalAppSegmentedRow provides true) {
                androidx.compose.material3.PrimaryTabRow(
                    selectedTabIndex = selectedTabIndex.coerceAtLeast(0),
                    modifier = modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .heightIn(min = tokens.floatingControlMinHeight)
                        .liquidGlassSurface(
                            shape = RoundedCornerShape(tokens.groupedCornerRadius),
                            tint = tokens.segmentedContainer.copy(alpha = 0.52f),
                            floating = false,
                        ).padding(3.dp)
                        .fillMaxWidth(),
                    containerColor = Color.Transparent,
                    contentColor = contentColor,
                    indicator = {
                        Box(
                            Modifier
                                .fillMaxHeight()
                                .tabIndicatorLayout { measurable, constraints, positions ->
                                    if (positions.isEmpty()) {
                                        layout(constraints.maxWidth, constraints.maxHeight) {}
                                    } else {
                                        val position = (selectionPosition ?: animatedPosition)
                                            .coerceIn(0f, positions.lastIndex.toFloat())
                                        val fromIndex = position.toInt()
                                        val toIndex = (fromIndex + 1).coerceAtMost(positions.lastIndex)
                                        val fraction = position - fromIndex
                                        val left = positions[fromIndex].left +
                                            (positions[toIndex].left - positions[fromIndex].left) * fraction
                                        val width = positions[fromIndex].width +
                                            (positions[toIndex].width - positions[fromIndex].width) * fraction
                                        val indicatorWidth = width.roundToPx()
                                        val placeable = measurable.measure(
                                            constraints.copy(
                                                minWidth = indicatorWidth,
                                                maxWidth = indicatorWidth,
                                            ),
                                        )
                                        layout(constraints.maxWidth, constraints.maxHeight) {
                                            placeable.placeRelative(left.roundToPx(), 0)
                                        }
                                    }
                                }.zIndex(-1f)
                                .liquidGlassSurface(
                                    shape = RoundedCornerShape(tokens.groupedCornerRadius),
                                    tint = tokens.chromeTint,
                                    floating = true,
                                ),
                        )
                    },
                    divider = {},
                    tabs = tabs,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPrimaryScrollableTabRow(
    selectedTabIndex: Int,
    modifier: Modifier = Modifier,
    containerColor: Color = TabRowDefaults.primaryContainerColor,
    contentColor: Color = TabRowDefaults.primaryContentColor,
    edgePadding: Dp = TabRowDefaults.ScrollableTabRowEdgeStartPadding,
    indicator: @Composable TabIndicatorScope.() -> Unit = {
        TabRowDefaults.PrimaryIndicator(
            Modifier.tabIndicatorOffset(selectedTabIndex, matchContentSize = true),
            width = Dp.Unspecified,
        )
    },
    divider: @Composable () -> Unit = { HorizontalDivider() },
    tabs: @Composable () -> Unit,
) {
    when (LocalUiStyle.current) {
        UiStyle.Material3 -> {
            androidx.compose.material3.PrimaryScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = modifier,
                containerColor = containerColor,
                contentColor = contentColor,
                edgePadding = edgePadding,
                indicator = indicator,
                divider = divider,
                tabs = tabs,
            )
        }
        UiStyle.Miuix -> {
            androidx.compose.material3.PrimaryScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = modifier,
                containerColor = Color.Transparent,
                contentColor = MiuixTheme.colorScheme.onSurface,
                edgePadding = 12.dp,
                indicator = {},
                divider = {},
                tabs = tabs,
            )
        }
        UiStyle.LiquidGlass -> {
            androidx.compose.material3.PrimaryScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                modifier = modifier,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.onSurface,
                edgePadding = 12.dp,
                indicator = {},
                divider = {},
                tabs = tabs,
            )
        }
    }
}
