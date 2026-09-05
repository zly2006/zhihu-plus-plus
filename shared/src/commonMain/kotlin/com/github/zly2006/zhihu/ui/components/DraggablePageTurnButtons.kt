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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.zly2006.zhihu.platform.rememberSettingsStore
import com.github.zly2006.zhihu.ui.rememberObservedSetting
import com.github.zly2006.zhihu.ui.subscreens.DEFAULT_FAB_OPACITY
import com.github.zly2006.zhihu.ui.subscreens.DEFAULT_FAB_SIZE
import com.github.zly2006.zhihu.ui.subscreens.DEFAULT_PAGE_TURN_PERCENT
import com.github.zly2006.zhihu.ui.subscreens.PREF_FAB_OPACITY
import com.github.zly2006.zhihu.ui.subscreens.PREF_FAB_SIZE
import com.github.zly2006.zhihu.ui.subscreens.PREF_PAGE_TURN_FILL_LAST_PAGE
import com.github.zly2006.zhihu.ui.subscreens.PREF_PAGE_TURN_PERCENT
import com.github.zly2006.zhihu.ui.subscreens.PREF_SHOW_PAGE_TURN_FAB
import com.github.zly2006.zhihu.ui.subscreens.PREF_SHOW_PAGE_TURN_GUIDE
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlin.math.roundToInt

/**
 * Activity 层翻页事件流。+1 下翻，-1 上翻，[Int.MAX_VALUE] 跳底，[Int.MIN_VALUE] 跳顶。
 */
val LocalPageTurnChannel =
    staticCompositionLocalOf { MutableSharedFlow<Int>(extraBufferCapacity = 1) }

/**
 * 翻页功能的统一状态持有者，收拢事件流、设置和引导线状态。
 * 通过 [rememberPageTurnState] 创建。
 * 设置项通过 [observeKeyChanges][com.github.zly2006.zhihu.platform.SettingsStore.observeKeyChanges]
 * 响应式更新，修改后无需重新进入页面。
 */
class PageTurnState(
    val channel: MutableSharedFlow<Int>,
    pageTurnPercent: Int,
    showGuide: Boolean,
    fillLastPage: Boolean,
) {
    var pageTurnPercent by mutableIntStateOf(pageTurnPercent)
    var showGuide by mutableStateOf(showGuide)
    var fillLastPage by mutableStateOf(fillLastPage)
    var guideLastDirection by mutableIntStateOf(0)
    var guideIsScrolling by mutableStateOf(false)

    fun emit(direction: Int) {
        channel.tryEmit(direction)
    }
}

@Composable
fun rememberPageTurnState(): PageTurnState {
    val channel = LocalPageTurnChannel.current
    val settings = rememberSettingsStore()
    val state = remember(channel) {
        PageTurnState(
            channel = channel,
            pageTurnPercent = settings.getInt(PREF_PAGE_TURN_PERCENT, DEFAULT_PAGE_TURN_PERCENT),
            showGuide = settings.getBoolean(PREF_SHOW_PAGE_TURN_GUIDE, true),
            fillLastPage = settings.getBoolean(PREF_PAGE_TURN_FILL_LAST_PAGE, false),
        )
    }
    DisposableEffect(settings, state) {
        val subscription = settings.observeKeyChanges { key ->
            when (key) {
                PREF_PAGE_TURN_PERCENT ->
                    state.pageTurnPercent =
                        settings.getInt(PREF_PAGE_TURN_PERCENT, DEFAULT_PAGE_TURN_PERCENT)
                PREF_SHOW_PAGE_TURN_GUIDE ->
                    state.showGuide =
                        settings.getBoolean(PREF_SHOW_PAGE_TURN_GUIDE, true)
                PREF_PAGE_TURN_FILL_LAST_PAGE ->
                    state.fillLastPage =
                        settings.getBoolean(PREF_PAGE_TURN_FILL_LAST_PAGE, false)
            }
        }
        onDispose(subscription::close)
    }
    return state
}

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

    val fabSizeValue by rememberObservedSetting(settings, PREF_FAB_SIZE) {
        getInt(PREF_FAB_SIZE, DEFAULT_FAB_SIZE)
    }
    val buttonSize = fabSizeValue.dp
    val gap = 4.dp
    val columnHeight = buttonSize * 2 + gap
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
    val fabOpacityValue by rememberObservedSetting(settings, PREF_FAB_OPACITY) {
        getInt(PREF_FAB_OPACITY, DEFAULT_FAB_OPACITY)
    }
    val opacityFraction = fabOpacityValue.coerceIn(10, 100) / 100f
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
        Spacer(modifier = Modifier.height(gap))
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
 * 读取设置后有条件地渲染翻页 FAB。
 * 应放在不随列表滚动移动的层级（通常是 Scaffold 内容区最外层 Box）。
 * 响应式监听 [PREF_SHOW_PAGE_TURN_FAB]，设置变更后立即生效。
 */
@Composable
fun PageTurnFab(
    state: PageTurnState,
    modifier: Modifier = Modifier,
    preferenceName: String = "fabPageTurn",
) {
    val settings = rememberSettingsStore()
    val showFab by rememberObservedSetting(settings, PREF_SHOW_PAGE_TURN_FAB) {
        getBoolean(PREF_SHOW_PAGE_TURN_FAB, false)
    }
    if (!showFab) return
    DraggablePageTurnButtons(
        onPageUp = { state.emit(-1) },
        onPageDown = { state.emit(1) },
        onLongPressUp = { state.emit(Int.MIN_VALUE) },
        onLongPressDown = { state.emit(Int.MAX_VALUE) },
        modifier = modifier,
        preferenceName = preferenceName,
    )
}

/**
 * 为使用 [ScrollState] 的页面接入翻页事件。
 * 在 composable 中调用一次即可，会自动收集 channel 并滚动。
 * [viewportHeight] 为可见区域高度（像素），可通过 Modifier.onSizeChanged 获取。
 * [skip] 为 true 时跳过翻页处理（如评论弹层打开时底层页面应跳过）。
 * [topBarState] 可选，传入可收起标题栏的 TopAppBarState：
 * 下翻/跳底时自动收起，跳顶或上翻到达顶部时展开。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageTurnScrollEffect(
    state: PageTurnState,
    scrollState: ScrollState,
    viewportHeight: Int,
    skip: Boolean = false,
    topBarState: TopAppBarState? = null,
) {
    if (state.showGuide &&
        state.guideLastDirection != 0 &&
        scrollState.isScrollInProgress &&
        !state.guideIsScrolling
    ) {
        state.guideLastDirection = 0
    }
    val currentViewportHeight by rememberUpdatedState(viewportHeight)
    val currentSkip by rememberUpdatedState(skip)
    LaunchedEffect(state.channel) {
        state.channel.collect { direction ->
            if (currentSkip) return@collect
            state.guideLastDirection = direction.coerceIn(-1, 1)
            state.guideIsScrolling = true
            try {
                if (topBarState != null) {
                    if (direction > 0 || direction == Int.MAX_VALUE) {
                        topBarState.heightOffset = topBarState.heightOffsetLimit
                    } else if (direction == Int.MIN_VALUE) {
                        topBarState.heightOffset = 0f
                    }
                }
                when (direction) {
                    Int.MAX_VALUE -> scrollState.scrollTo(scrollState.maxValue)
                    Int.MIN_VALUE -> scrollState.scrollTo(0)
                    else -> if (currentViewportHeight > 0) {
                        val scrollAmount =
                            currentViewportHeight * state.pageTurnPercent / 100f * direction
                        scrollState.scrollBy(scrollAmount)
                        if (topBarState != null && direction < 0 && scrollState.value == 0) {
                            topBarState.heightOffset = 0f
                        }
                    }
                }
            } finally {
                state.guideIsScrolling = false
            }
        }
    }
}

/**
 * 在 Scaffold 的 content lambda 中调用，自动完成：
 * 1. 从 [innerPadding] 计算有效 viewport 高度
 * 2. 注入 [PageTurnScrollEffect]（带可选 topBarState）
 * 3. 包裹 Box + [PageTurnGuideOverlay]（使用 innerPadding 作为 inset）
 *
 * [content] lambda 接收已配置好 onSizeChanged 的 Modifier，应用到可滚动的 Column 上。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageTurnScrollContent(
    pageTurnState: PageTurnState,
    scrollState: ScrollState,
    innerPadding: PaddingValues,
    topBarState: TopAppBarState? = null,
    content: @Composable (sizeTrackingModifier: Modifier) -> Unit,
) {
    val density = LocalDensity.current
    var rawViewportHeight by remember { mutableIntStateOf(0) }
    val topPx = with(density) { innerPadding.calculateTopPadding().toPx().toInt() }
    val bottomPx = with(density) { innerPadding.calculateBottomPadding().toPx().toInt() }
    val viewportHeight = (rawViewportHeight - topPx - bottomPx).coerceAtLeast(0)
    PageTurnScrollEffect(
        state = pageTurnState,
        scrollState = scrollState,
        viewportHeight = viewportHeight,
        topBarState = topBarState,
    )
    Box(modifier = Modifier.fillMaxSize()) {
        content(Modifier.onSizeChanged { rawViewportHeight = it.height })
        PageTurnGuideOverlay(
            state = pageTurnState,
            topInsetPx = with(density) { innerPadding.calculateTopPadding().toPx() },
            bottomInsetPx = with(density) { innerPadding.calculateBottomPadding().toPx() },
        )
    }
}

/**
 * 为使用 [LazyListState] 的页面接入翻页事件。
 * viewport 从 [listState].layoutInfo 自动计算，无需外部传入。
 * [skip] 为 true 时跳过翻页处理。
 * [topBarState] 可选，传入可收起标题栏的 TopAppBarState。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageTurnLazyListEffect(
    state: PageTurnState,
    listState: LazyListState,
    skip: Boolean = false,
    topBarState: TopAppBarState? = null,
) {
    if (state.showGuide &&
        state.guideLastDirection != 0 &&
        listState.isScrollInProgress &&
        !state.guideIsScrolling
    ) {
        state.guideLastDirection = 0
    }
    val currentSkip by rememberUpdatedState(skip)
    LaunchedEffect(state.channel) {
        state.channel.collect { direction ->
            if (currentSkip) return@collect
            state.guideLastDirection = direction.coerceIn(-1, 1)
            state.guideIsScrolling = true
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
                        if (lastIndex >= 0) listState.scrollToItem(lastIndex)
                    }
                    Int.MIN_VALUE -> {
                        if (listState.layoutInfo.totalItemsCount > 0) listState.scrollToItem(0)
                    }
                    else -> {
                        val layout = listState.layoutInfo
                        val viewport = layout.viewportEndOffset - layout.viewportStartOffset -
                            layout.beforeContentPadding - layout.afterContentPadding
                        if (viewport > 0) {
                            listState.scrollBy(viewport.toFloat() * state.pageTurnPercent / 100f * direction)
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
                state.guideIsScrolling = false
            }
        }
    }
}

/**
 * 在 LazyColumn 末尾添加 "— · —" 结束标记和可选的末页补全空白。
 * 供 [PaginatedList][com.github.zly2006.zhihu.ui.components.PaginatedList] 和 CommentScreen 等使用 LazyColumn 的页面复用。
 */
fun LazyListScope.pageTurnEndItems(
    pageTurnState: PageTurnState,
    listState: LazyListState,
) {
    item(key = "end_indicator") {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "— · —",
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                fontSize = 14.sp,
            )
        }
    }
    if (pageTurnState.fillLastPage &&
        (listState.canScrollForward || listState.canScrollBackward)
    ) {
        item(key = "page_turn_bottom_spacer") {
            val viewport = listState.layoutInfo.let {
                it.viewportEndOffset - it.viewportStartOffset
            }
            Spacer(
                modifier = Modifier.height(
                    with(LocalDensity.current) {
                        (viewport * pageTurnState.pageTurnPercent / 100).toDp()
                    },
                ),
            )
        }
    }
}

/**
 * 末页补全空白。放在可滚动 Column 的末尾，
 * 补足一个 viewport 高度的空白以保证最后一页可以完整翻页。
 */
@Composable
fun ContentEndSpacer(
    state: PageTurnState,
    viewportHeight: Int,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "— · —",
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            fontSize = 14.sp,
        )
    }
    if (state.fillLastPage && viewportHeight > 0) {
        val density = LocalDensity.current
        Spacer(
            modifier = Modifier.height(
                with(density) { (viewportHeight * state.pageTurnPercent / 100).toDp() },
            ),
        )
    }
}

/**
 * 在可见区域绘制水平虚线标记翻页边界。
 * [PageTurnState.guideLastDirection] 控制只显示与最近翻页方向相关的那条线：
 * 下翻(1)后只显示上线（标记重叠区起点），上翻(-1)后只显示下线。
 */
@Composable
fun PageTurnGuideOverlay(
    state: PageTurnState,
    modifier: Modifier = Modifier,
    topInsetPx: Float = 0f,
    bottomInsetPx: Float = 0f,
) {
    if (!state.showGuide || state.guideLastDirection == 0) return
    val guideColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
    val density = LocalDensity.current
    Canvas(modifier = modifier.fillMaxSize()) {
        val effectiveHeight = size.height - topInsetPx - bottomInsetPx
        if (effectiveHeight <= 0f) return@Canvas
        val overlapFraction = 1f - state.pageTurnPercent / 100f
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
            drawLine(guideColor, Offset(0f, y - arrowSize), Offset(arrowSize, y), strokeWidth = 2f)
            drawLine(guideColor, Offset(0f, y + arrowSize), Offset(arrowSize, y), strokeWidth = 2f)
            drawLine(guideColor, Offset(size.width, y - arrowSize), Offset(size.width - arrowSize, y), strokeWidth = 2f)
            drawLine(guideColor, Offset(size.width, y + arrowSize), Offset(size.width - arrowSize, y), strokeWidth = 2f)
        }
        if (state.guideLastDirection > 0) {
            drawGuideLineWithArrows(topInsetPx + overlapFraction * effectiveHeight)
        }
        if (state.guideLastDirection < 0) {
            drawGuideLineWithArrows(topInsetPx + (state.pageTurnPercent / 100f) * effectiveHeight)
        }
    }
}
