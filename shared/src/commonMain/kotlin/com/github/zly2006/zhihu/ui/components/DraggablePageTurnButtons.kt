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

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.shared.platform.rememberSettingsStore
import com.github.zly2006.zhihu.ui.subscreens.DEFAULT_FAB_OPACITY
import com.github.zly2006.zhihu.ui.subscreens.DEFAULT_PAGE_TURN_PERCENT
import com.github.zly2006.zhihu.ui.subscreens.PREF_FAB_OPACITY
import com.github.zly2006.zhihu.ui.subscreens.PREF_PAGE_TURN_PERCENT
import com.github.zly2006.zhihu.ui.subscreens.PREF_SHOW_PAGE_TURN_FAB
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlin.math.roundToInt

/**
 * Activity 层翻页事件流。+1 下翻，-1 上翻，[Int.MAX_VALUE] 跳底，[Int.MIN_VALUE] 跳顶。
 * Activity 拦截按键后 tryEmit，悬浮翻页按钮也直接 tryEmit，各屏幕 collect 执行滚动。
 */
val LocalPageTurnChannel: androidx.compose.runtime.ProvidableCompositionLocal<MutableSharedFlow<Int>> =
    staticCompositionLocalOf { MutableSharedFlow(extraBufferCapacity = 1) }

/**
 * 当前是否有模态弹层（如评论底部弹层）正在接管翻页事件。
 * 底层页面的翻页收集器应在此值 > 0 时跳过处理，避免穿透滚动。
 */
val pageTurnModalDepth = androidx.compose.runtime.mutableIntStateOf(0)

/** 翻页悬浮按钮的全局可见性，由设置页开关直接写入，PageTurnFab 读取。 */
val pageTurnFabVisible = androidx.compose.runtime.mutableStateOf(false)

/** 所有悬浮按钮的透明度百分比（10–100），由设置页写入，各 FAB 读取。 */
val fabOpacityPercent = androidx.compose.runtime.mutableIntStateOf(100)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DraggablePageTurnButtons(
    onPageUp: () -> Unit,
    onPageDown: () -> Unit,
    onLongPressUp: () -> Unit = {},
    onLongPressDown: () -> Unit = {},
    modifier: Modifier = Modifier,
    preferenceName: String = "fabPageTurn",
) {
    val density = LocalDensity.current
    val screenSize = LocalWindowInfo.current.containerSize
    val settings = rememberSettingsStore()

    var pressing by remember { mutableStateOf(false) }

    val buttonSize = 40.dp
    val columnHeight = buttonSize * 2 + 4.dp
    val defaultY = with(density) { screenSize.height - columnHeight.toPx() - 80.dp.toPx() }
    var offsetX by remember { mutableFloatStateOf(settings.getFloat("$preferenceName-x", 0f)) }
    var offsetY by remember { mutableFloatStateOf(settings.getFloat("$preferenceName-y", defaultY)) }

    fun adjustPosition() {
        with(density) {
            offsetX = offsetX.coerceIn(0f, screenSize.width - buttonSize.toPx())
            offsetY = offsetY.coerceIn(0f, screenSize.height - columnHeight.toPx())
        }
    }

    adjustPosition()

    val hapticFeedback = LocalHapticFeedback.current

    val opacityFraction = fabOpacityPercent.intValue / 100f
    val fabColor = FloatingActionButtonDefaults.containerColor.copy(alpha = opacityFraction)
    val iconTint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = opacityFraction)
    Column(
        modifier = modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        pressing = true
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onDragEnd = {
                        pressing = false
                        adjustPosition()
                        val screenWidth = screenSize.width.toFloat()
                        with(density) {
                            offsetX =
                                if (offsetX < screenWidth / 2) {
                                    0f
                                } else {
                                    screenWidth - buttonSize.toPx()
                                }
                        }
                        settings.putFloat("$preferenceName-x", offsetX)
                        settings.putFloat("$preferenceName-y", offsetY)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        if (pressing) {
                            offsetX += dragAmount.x
                            offsetY += dragAmount.y
                            adjustPosition()
                        }
                    },
                )
            },
    ) {
        Surface(
            shape = CircleShape,
            color = fabColor,
            modifier = Modifier
                .size(buttonSize)
                .combinedClickable(
                    onClick = onPageUp,
                    onLongClick = onLongPressUp,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(Icons.Default.KeyboardArrowUp, contentDescription = "上翻页", tint = iconTint)
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Surface(
            shape = CircleShape,
            color = fabColor,
            modifier = Modifier
                .size(buttonSize)
                .combinedClickable(
                    onClick = onPageDown,
                    onLongClick = onLongPressDown,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ),
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(Icons.Default.KeyboardArrowDown, contentDescription = "下翻页", tint = iconTint)
            }
        }
    }
}

/**
 * 读取设置后有条件地渲染翻页 FAB，点击时通过 [LocalPageTurnChannel] 发送方向。
 * 应放在不随列表滚动移动的层级（通常是 Scaffold 内容区最外层 Box）。
 */
@Composable
fun PageTurnFab(
    modifier: Modifier = Modifier,
    preferenceName: String = "fabPageTurn",
) {
    val settings = rememberSettingsStore()
    LaunchedEffect(Unit) {
        pageTurnFabVisible.value = settings.getBoolean(PREF_SHOW_PAGE_TURN_FAB, false)
        fabOpacityPercent.intValue = settings.getInt(PREF_FAB_OPACITY, DEFAULT_FAB_OPACITY).coerceIn(10, 100)
    }
    if (!pageTurnFabVisible.value) return
    val channel = LocalPageTurnChannel.current
    DraggablePageTurnButtons(
        onPageUp = { channel.tryEmit(-1) },
        onPageDown = { channel.tryEmit(1) },
        onLongPressUp = { channel.tryEmit(Int.MIN_VALUE) },
        onLongPressDown = { channel.tryEmit(Int.MAX_VALUE) },
        modifier = modifier,
        preferenceName = preferenceName,
    )
}

/**
 * 为使用 [ScrollState] 的页面接入翻页事件。
 * 在 composable 中调用一次即可，会自动收集 [LocalPageTurnChannel] 并滚动。
 * [viewportHeight] 为可见区域高度（像素），可通过 Modifier.onSizeChanged 获取。
 * [topBarState] 可选，传入可收起标题栏的 TopAppBarState：
 * 下翻/跳底时自动收起，跳顶或上翻到达顶部时展开，普通上翻不展开。
 * 翻页距离会扣除标题栏收起产生的高度变化量，避免重复滚动。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageTurnScrollEffect(
    scrollState: ScrollState,
    viewportHeight: Int,
    topBarState: TopAppBarState? = null,
) {
    val settings = rememberSettingsStore()
    val pageTurnPercent = remember { settings.getInt(PREF_PAGE_TURN_PERCENT, DEFAULT_PAGE_TURN_PERCENT) }
    val channel = LocalPageTurnChannel.current
    val currentViewportHeight by rememberUpdatedState(viewportHeight)
    LaunchedEffect(channel) {
        channel.collect { direction ->
            if (pageTurnModalDepth.intValue > 0) return@collect
            var barCollapseDelta = 0f
            if (topBarState != null) {
                val oldOffset = topBarState.heightOffset
                if (direction > 0 || direction == Int.MAX_VALUE) {
                    topBarState.heightOffset = topBarState.heightOffsetLimit
                } else if (direction == Int.MIN_VALUE) {
                    topBarState.heightOffset = 0f
                }
                barCollapseDelta = topBarState.heightOffset - oldOffset
            }
            when (direction) {
                Int.MAX_VALUE -> scrollState.scrollTo(scrollState.maxValue)
                Int.MIN_VALUE -> scrollState.scrollTo(0)
                else -> if (currentViewportHeight > 0) {
                    val afterCollapseViewport = currentViewportHeight - barCollapseDelta
                    val scrollAmount = afterCollapseViewport * pageTurnPercent / 100f * direction + barCollapseDelta
                    scrollState.scrollBy(scrollAmount)
                    if (topBarState != null && direction < 0 && scrollState.value == 0) {
                        topBarState.heightOffset = 0f
                    }
                }
            }
        }
    }
}

/**
 * 为使用 [LazyListState] 的页面接入翻页事件。
 * viewport 从 [listState].layoutInfo 自动计算，无需外部传入。
 * [topBarState] 可选，传入可收起标题栏的 TopAppBarState：
 * 下翻/跳底时自动收起，跳顶或上翻到达顶部时展开。
 * 返回 [PageTurnGuideState] 供翻页引导线使用。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageTurnLazyListEffect(
    listState: LazyListState,
    pageTurnPercent: Int,
    guideState: PageTurnGuideState = remember { PageTurnGuideState() },
    skip: Boolean = pageTurnModalDepth.intValue > 0,
    topBarState: TopAppBarState? = null,
) {
    val channel = LocalPageTurnChannel.current
    val currentSkip by rememberUpdatedState(skip)
    LaunchedEffect(channel) {
        channel.collect { direction ->
            if (currentSkip) return@collect
            guideState.lastDirection = direction.coerceIn(-1, 1)
            guideState.isScrolling = true
            try {
                if (topBarState != null) {
                    if (direction > 0 || direction == Int.MAX_VALUE) {
                        topBarState.heightOffset = topBarState.heightOffsetLimit
                    } else if (direction == Int.MIN_VALUE) {
                        topBarState.heightOffset = 0f
                    }
                }
                when (direction) {
                    Int.MAX_VALUE -> {
                        val lastIndex = listState.layoutInfo.totalItemsCount - 1
                        if (lastIndex >= 0) {
                            listState.scrollToItem(lastIndex)
                        }
                    }
                    Int.MIN_VALUE -> {
                        if (listState.layoutInfo.totalItemsCount > 0) {
                            listState.scrollToItem(0)
                        }
                    }
                    else -> {
                        val layout = listState.layoutInfo
                        val viewport = layout.viewportEndOffset - layout.viewportStartOffset -
                            layout.beforeContentPadding - layout.afterContentPadding
                        if (viewport > 0) {
                            listState.scrollBy(viewport * pageTurnPercent / 100f * direction)
                        }
                    }
                }
                if (topBarState != null &&
                    direction < 0 &&
                    listState.firstVisibleItemIndex == 0 &&
                    listState.firstVisibleItemScrollOffset == 0
                ) {
                    topBarState.heightOffset = 0f
                }
            } finally {
                guideState.isScrolling = false
            }
        }
    }
}

class PageTurnGuideState {
    var lastDirection by mutableIntStateOf(0)
    var isScrolling by mutableStateOf(false)
}

/**
 * 在可见区域绘制水平虚线标记翻页边界。
 * [lastDirection] 控制只显示与最近翻页方向相关的那条线：
 * 下翻(1)后只显示上线（标记重叠区起点），上翻(-1)后只显示下线；
 * 0 表示尚未翻页，不绘制任何线。
 */
@Composable
fun PageTurnGuideOverlay(
    pageTurnPercent: Int,
    modifier: Modifier = Modifier,
    topInsetPx: Float = 0f,
    bottomInsetPx: Float = 0f,
    lastDirection: Int = 0,
) {
    if (lastDirection == 0) return
    val guideColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val density = LocalDensity.current
    Canvas(modifier = modifier.fillMaxSize()) {
        val effectiveHeight = size.height - topInsetPx - bottomInsetPx
        if (effectiveHeight <= 0f) return@Canvas
        val overlapFraction = 1f - pageTurnPercent / 100f
        val dash = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
        val arrowSize = with(density) { 10.dp.toPx() }

        fun drawGuideLineWithArrows(y: Float) {
            drawLine(
                color = guideColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 2f,
                pathEffect = dash,
            )
            // 左端向内箭头 (>)
            drawLine(guideColor, Offset(0f, y - arrowSize), Offset(arrowSize, y), strokeWidth = 2f)
            drawLine(guideColor, Offset(0f, y + arrowSize), Offset(arrowSize, y), strokeWidth = 2f)
            // 右端向内箭头 (<)
            drawLine(guideColor, Offset(size.width, y - arrowSize), Offset(size.width - arrowSize, y), strokeWidth = 2f)
            drawLine(guideColor, Offset(size.width, y + arrowSize), Offset(size.width - arrowSize, y), strokeWidth = 2f)
        }
        if (lastDirection > 0) {
            drawGuideLineWithArrows(topInsetPx + overlapFraction * effectiveHeight)
        }
        if (lastDirection < 0) {
            drawGuideLineWithArrows(topInsetPx + (pageTurnPercent / 100f) * effectiveHeight)
        }
    }
}
