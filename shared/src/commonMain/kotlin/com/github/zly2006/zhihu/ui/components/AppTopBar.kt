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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.TopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.lerp
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.theme.LiquidGlassCompactNavigationTitle
import com.github.zly2006.zhihu.theme.LocalUiBackdrop
import com.github.zly2006.zhihu.theme.LocalUiStyle
import com.github.zly2006.zhihu.theme.NoOpUiBackdrop
import com.github.zly2006.zhihu.theme.UiStyle
import com.github.zly2006.zhihu.theme.liquidGlassSemanticTokens
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    when (LocalUiStyle.current) {
        UiStyle.Material3 -> {
            TopAppBar(
                title = title,
                modifier = modifier,
                navigationIcon = navigationIcon,
                actions = actions,
                windowInsets = windowInsets,
                colors = colors,
                scrollBehavior = scrollBehavior,
            )
        }
        UiStyle.Miuix -> {
            val backdrop = LocalUiBackdrop.current
            TopAppBar(
                title = {
                    ProvideTextStyle(MiuixTheme.textStyles.title2, content = title)
                },
                modifier = modifier.then(backdrop.chromeSurface(floating = false)),
                navigationIcon = navigationIcon,
                actions = actions,
                windowInsets = windowInsets,
                colors = miuixTopBarColors(backdrop.isActive, colors),
                scrollBehavior = scrollBehavior,
            )
        }
        UiStyle.LiquidGlass -> {
            AppleNavigationLayout(
                modifier = modifier,
                navigationIcon = navigationIcon,
                actions = actions,
                title = title,
                windowInsets = windowInsets,
                scrollBehavior = scrollBehavior,
                largeTitle = false,
                centerTitle = false,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppCenterAlignedTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    when (LocalUiStyle.current) {
        UiStyle.Material3 -> {
            CenterAlignedTopAppBar(
                title = title,
                modifier = modifier,
                navigationIcon = navigationIcon,
                actions = actions,
                windowInsets = windowInsets,
                colors = colors,
                scrollBehavior = scrollBehavior,
            )
        }
        UiStyle.Miuix -> {
            val backdrop = LocalUiBackdrop.current
            CenterAlignedTopAppBar(
                title = {
                    ProvideTextStyle(MiuixTheme.textStyles.title2, content = title)
                },
                modifier = modifier.then(backdrop.chromeSurface(floating = false)),
                navigationIcon = navigationIcon,
                actions = actions,
                windowInsets = windowInsets,
                colors = miuixTopBarColors(backdrop.isActive, colors),
                scrollBehavior = scrollBehavior,
            )
        }
        UiStyle.LiquidGlass -> {
            AppleNavigationLayout(
                modifier = modifier,
                navigationIcon = navigationIcon,
                actions = actions,
                title = title,
                windowInsets = windowInsets,
                scrollBehavior = scrollBehavior,
                largeTitle = false,
                centerTitle = true,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppLargeTopAppBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    when (LocalUiStyle.current) {
        UiStyle.Material3 -> {
            LargeTopAppBar(
                title = title,
                modifier = modifier,
                navigationIcon = navigationIcon,
                actions = actions,
                windowInsets = windowInsets,
                colors = colors,
                scrollBehavior = scrollBehavior,
            )
        }
        UiStyle.Miuix -> {
            val backdrop = LocalUiBackdrop.current
            LargeTopAppBar(
                title = {
                    val fraction = (scrollBehavior?.state?.collapsedFraction ?: 0f).coerceIn(0f, 1f)
                    val interpolatedStyle = lerp(
                        MiuixTheme.textStyles.title1,
                        MiuixTheme.textStyles.title2,
                        fraction,
                    )
                    ProvideTextStyle(interpolatedStyle, content = title)
                },
                modifier = modifier.then(backdrop.chromeSurface(floating = false)),
                navigationIcon = navigationIcon,
                actions = actions,
                windowInsets = windowInsets,
                colors = miuixTopBarColors(backdrop.isActive, colors),
                scrollBehavior = scrollBehavior,
            )
        }
        UiStyle.LiquidGlass -> {
            AppleNavigationLayout(
                modifier = modifier,
                navigationIcon = navigationIcon,
                actions = actions,
                title = title,
                windowInsets = windowInsets,
                scrollBehavior = scrollBehavior,
                largeTitle = true,
                centerTitle = false,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun miuixTopBarColors(
    backdropActive: Boolean,
    defaultColors: TopAppBarColors,
): TopAppBarColors = defaultColors.copy(
    containerColor = if (backdropActive) Color.Transparent else MiuixTheme.colorScheme.surface,
    scrolledContainerColor = if (backdropActive) Color.Transparent else MiuixTheme.colorScheme.surface,
    titleContentColor = MiuixTheme.colorScheme.onSurface,
    navigationIconContentColor = MiuixTheme.colorScheme.onSurface,
    actionIconContentColor = MiuixTheme.colorScheme.onSurface,
)

@OptIn(ExperimentalMaterial3Api::class)
internal fun liquidTopBarOverlapFraction(
    state: TopAppBarState?,
    rampDistancePx: Float,
): Float {
    if (state == null) return 0f
    val contentOverlap = (-state.contentOffset / rampDistancePx.coerceAtLeast(1f)).coerceIn(0f, 1f)
    return max(contentOverlap, state.overlappedFraction.coerceIn(0f, 1f))
}

internal fun liquidTopBarScrollScrim(
    overlappedFraction: () -> Float,
    canvasColor: Color,
): Modifier = Modifier.drawBehind {
    val fraction = overlappedFraction().coerceIn(0f, 1f)
    if (fraction <= 0.01f || size.height <= 0f) return@drawBehind
    val featherPx = 16.dp.toPx()
    val totalHeight = size.height + featherPx
    val solidEndStop = (size.height / totalHeight).coerceIn(0.75f, 0.96f)
    drawRect(
        brush = Brush.verticalGradient(
            colorStops = arrayOf(
                0f to canvasColor.copy(alpha = 0.96f * fraction),
                solidEndStop to canvasColor.copy(alpha = 0.90f * fraction),
                1f to canvasColor.copy(alpha = 0f),
            ),
            startY = 0f,
            endY = totalHeight,
        ),
        size = size.copy(height = totalHeight),
    )
}

private enum class AppleNavigationSlot { Navigation, Actions, ExpandedTitle, CompactTitle }

/**
 * Apple Liquid Glass 导航栏布局：
 * - 返回/导航按钮与操作组使用浮空玻璃胶囊；
 * - 大标题按实际字号测量高度并平滑过渡到紧凑居中标题，大字号下不裁切。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppleNavigationLayout(
    modifier: Modifier,
    navigationIcon: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit,
    title: @Composable () -> Unit,
    windowInsets: WindowInsets,
    scrollBehavior: TopAppBarScrollBehavior?,
    largeTitle: Boolean,
    centerTitle: Boolean = false,
) {
    val tokens = liquidGlassSemanticTokens()
    val backdrop = LocalUiBackdrop.current
    val density = LocalDensity.current
    val verticalPaddingPx = with(density) { 8.dp.roundToPx() }
    var measuredNavigationHeight by remember { mutableIntStateOf(0) }
    var measuredActionHeight by remember { mutableIntStateOf(0) }
    var measuredActionWidth by remember { mutableIntStateOf(0) }
    var measuredCompactTitleHeight by remember { mutableIntStateOf(0) }
    var measuredExpandedTitleHeight by remember { mutableIntStateOf(0) }
    val rememberedCompactHeight = max(
        with(density) { tokens.floatingControlMinHeight.roundToPx() },
        max(measuredNavigationHeight, max(measuredActionHeight, measuredCompactTitleHeight)) +
            verticalPaddingPx * 2,
    )
    val rememberedCollapseRange = (measuredExpandedTitleHeight + verticalPaddingPx * 2).coerceAtLeast(0)

    SideEffect {
        val limit = when {
            largeTitle && measuredExpandedTitleHeight > 0 -> -rememberedCollapseRange.toFloat()
            !largeTitle &&
                (measuredCompactTitleHeight > 0 || measuredNavigationHeight > 0 || measuredActionHeight > 0) ->
                -rememberedCompactHeight.toFloat()
            else -> null
        }
        val state = scrollBehavior?.state
        if (limit != null && state?.heightOffsetLimit != limit) {
            val previousLimit = state?.heightOffsetLimit ?: 0f
            val previousOffset = state?.heightOffset ?: 0f
            val collapsedProgress = if (previousLimit.isFinite() && previousLimit < 0f) {
                (previousOffset / previousLimit).coerceIn(0f, 1f)
            } else if (previousOffset < 0f) {
                1f
            } else {
                0f
            }
            state?.heightOffsetLimit = limit
            state?.let { it.heightOffset = (limit * collapsedProgress).coerceIn(limit, 0f) }
        }
    }

    val scrollOffset = scrollBehavior?.state?.heightOffset ?: 0f
    val scrimRampPx = with(density) { 24.dp.toPx() }
    // 静止态保持透明；一旦内容滚入顶栏下方（24dp 内拉满），在顶栏区域叠 Backdrop 模糊与高不透明底色遮罩并在底缘柔和羽化，防止正文文字与标题/状态栏重叠。
    val scrollScrimModifier = backdrop.scrollScrimSurface(
        overlapFraction = { liquidTopBarOverlapFraction(scrollBehavior?.state, scrimRampPx) },
        canvasColor = tokens.canvas,
    )
    SubcomposeLayout(
        modifier = modifier
            .then(scrollScrimModifier)
            .windowInsetsPadding(windowInsets)
            .clipToBounds(),
    ) { constraints ->
        val horizontalPadding = 16.dp.roundToPx()
        val verticalPadding = 8.dp.roundToPx()
        val minimumBarHeight = tokens.floatingControlMinHeight.roundToPx()

        val navigation = subcompose(AppleNavigationSlot.Navigation) {
            Box(Modifier.onSizeChanged { measuredNavigationHeight = it.height }) {
                CompositionLocalProvider(LocalAppIconButtonStyle provides AppIconButtonStyle.Floating) {
                    navigationIcon()
                }
            }
        }.map {
            it.measure(Constraints(maxWidth = constraints.maxWidth, maxHeight = constraints.maxHeight))
        }

        val actionItems = subcompose(AppleNavigationSlot.Actions) {
            val actionSurface = if (measuredActionWidth > 0) {
                if (backdrop.isActive) {
                    backdrop.chromeSurface(
                        shape = RoundedCornerShape(tokens.floatingCornerRadius),
                        tint = tokens.chromeTint,
                        floating = true,
                    )
                } else {
                    Modifier.background(tokens.groupedSurface, RoundedCornerShape(tokens.floatingCornerRadius))
                }
            } else {
                Modifier
            }
            Row(
                modifier = actionSurface.onSizeChanged {
                    measuredActionHeight = it.height
                    measuredActionWidth = it.width
                },
            ) {
                // 动作组外层已承载玻璃胶囊，避免内部嵌套 AppButton/AppIconButton 产生双层折射。
                CompositionLocalProvider(LocalUiBackdrop provides NoOpUiBackdrop) {
                    actions()
                }
            }
        }.map {
            it.measure(Constraints(maxWidth = constraints.maxWidth, maxHeight = constraints.maxHeight))
        }

        val titleGap = 12.dp.roundToPx()
        val navigationWidth = navigation.maxOfOrNull { it.width } ?: 0
        val actionWidth = actionItems.maxOfOrNull { it.width } ?: 0
        val navigationSlot = if (navigationWidth > 0) navigationWidth + titleGap else 0
        val actionSlot = if (actionWidth > 0) actionWidth + titleGap else 0
        val commandHeight = max(
            navigation.maxOfOrNull { it.height } ?: 0,
            actionItems.maxOfOrNull { it.height } ?: 0,
        )
        val sideReservation = if (centerTitle || largeTitle) {
            max(navigationSlot, actionSlot) * 2
        } else {
            navigationSlot + actionSlot
        }
        val expandedTitleMaxWidth = (constraints.maxWidth - horizontalPadding * 2).coerceAtLeast(0)
        val compactAvailableWidth = (constraints.maxWidth - horizontalPadding * 2 - sideReservation).coerceAtLeast(0)

        val expandedTitle = if (largeTitle) {
            subcompose(AppleNavigationSlot.ExpandedTitle) {
                Box(Modifier.onSizeChanged { measuredExpandedTitleHeight = it.height }) {
                    ProvideTextStyle(MaterialTheme.typography.headlineLarge, title)
                }
            }.single().measure(
                Constraints(maxWidth = expandedTitleMaxWidth, maxHeight = Constraints.Infinity),
            )
        } else {
            null
        }

        val compactTitle = subcompose(AppleNavigationSlot.CompactTitle) {
            Box(Modifier.onSizeChanged { measuredCompactTitleHeight = it.height }) {
                ProvideTextStyle(LiquidGlassCompactNavigationTitle, title)
            }
        }.single().measure(
            Constraints(maxWidth = compactAvailableWidth, maxHeight = Constraints.Infinity),
        )

        val compactTitleWidth = compactTitle.width
        val compactTitleHeight = compactTitle.height
        val compactHeight = max(minimumBarHeight, max(commandHeight, compactTitleHeight) + verticalPadding * 2)
        val collapseRange = if (largeTitle) checkNotNull(expandedTitle).height + verticalPadding * 2 else compactHeight
        val expandedHeight = compactHeight + if (largeTitle) collapseRange else 0
        val currentHeight = if (largeTitle) {
            (expandedHeight + scrollOffset).roundToInt().coerceIn(compactHeight, expandedHeight)
        } else {
            (compactHeight + scrollOffset).roundToInt().coerceIn(0, compactHeight)
        }

        layout(constraints.maxWidth, currentHeight) {
            val commandY = (compactHeight - commandHeight) / 2
            val compactTranslationY = if (largeTitle) 0 else scrollOffset.roundToInt()
            val compactTitleStart = horizontalPadding + navigationSlot
            val compactTitleMaxX = (constraints.maxWidth - horizontalPadding - actionSlot - compactTitleWidth)
                .coerceAtLeast(compactTitleStart)
            val compactTitleX = when {
                centerTitle || largeTitle -> (constraints.maxWidth - compactTitleWidth) / 2
                else -> compactTitleStart
            }.coerceIn(compactTitleStart, compactTitleMaxX)
            val compactTitleY = (compactHeight - compactTitleHeight) / 2
            val expandedTitleX = horizontalPadding

            if (largeTitle) {
                val expandedTitleY = (compactHeight + verticalPadding + scrollOffset.roundToInt())
                    .coerceAtLeast(compactHeight)
                val titleTravel = -scrollOffset
                val takeoverStart = verticalPadding.toFloat()
                val takeoverEnd = min(
                    (compactHeight + verticalPadding - compactTitleY).toFloat(),
                    collapseRange.toFloat(),
                )
                val compactAlpha = (
                    (titleTravel - takeoverStart) /
                        (takeoverEnd - takeoverStart).coerceAtLeast(1f)
                ).coerceIn(0f, 1f)
                if (compactAlpha < 1f) {
                    checkNotNull(expandedTitle).placeRelativeWithLayer(expandedTitleX, expandedTitleY) {
                        alpha = 1f - compactAlpha
                    }
                }
                if (compactAlpha > 0f) {
                    compactTitle.placeRelativeWithLayer(compactTitleX, compactTitleY) {
                        alpha = compactAlpha
                    }
                }
            } else {
                compactTitle.placeRelative(compactTitleX, compactTitleY + compactTranslationY)
            }

            navigation.forEach { it.placeRelative(horizontalPadding, commandY + compactTranslationY) }
            var actionX = constraints.maxWidth - horizontalPadding - actionWidth
            actionItems.forEach { item ->
                item.placeRelative(actionX, commandY + compactTranslationY)
                actionX += item.width
            }
        }
    }
}
