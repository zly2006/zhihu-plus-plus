/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 */

package com.github.zly2006.zhihu.ui.components

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
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.platform.isPageTurnSupported
import com.github.zly2006.zhihu.platform.rememberSettingsStore
import com.github.zly2006.zhihu.ui.rememberObservedSetting
import com.github.zly2006.zhihu.ui.subscreens.DEFAULT_FAB_OPACITY
import com.github.zly2006.zhihu.ui.subscreens.DEFAULT_PAGE_TURN_PERCENT
import com.github.zly2006.zhihu.ui.subscreens.DEFAULT_SHOW_PAGE_TURN_GUIDE
import com.github.zly2006.zhihu.ui.subscreens.PREF_FAB_OPACITY
import com.github.zly2006.zhihu.ui.subscreens.PREF_PAGE_TURN_PERCENT
import com.github.zly2006.zhihu.ui.subscreens.PREF_SHOW_PAGE_TURN_FAB
import com.github.zly2006.zhihu.ui.subscreens.PREF_SHOW_PAGE_TURN_GUIDE
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collect
import kotlin.math.roundToInt

enum class PageTurnCommand {
    PageUp,
    PageDown,
    JumpToTop,
    JumpToBottom,
}

/** Routes each command to the most recently registered eligible scroll surface. */
class PageTurnDispatcher {
    private val targets = mutableListOf<PageTurnTargetRegistration>()

    var hasActiveTarget by mutableStateOf(false)
        private set

    internal fun registerTarget(): PageTurnTargetRegistration {
        val registration = PageTurnTargetRegistration(this)
        targets += registration
        hasActiveTarget = true
        return registration
    }

    fun dispatch(command: PageTurnCommand): Boolean =
        targets
            .lastOrNull()
            ?.commands
            ?.trySend(command)
            ?.isSuccess == true

    internal fun unregisterTarget(registration: PageTurnTargetRegistration) {
        targets.remove(registration)
        registration.commands.close()
        hasActiveTarget = targets.isNotEmpty()
    }
}

internal class PageTurnTargetRegistration(
    private val dispatcher: PageTurnDispatcher,
) {
    val commands = Channel<PageTurnCommand>(Channel.BUFFERED)
    private var closed = false

    fun close() {
        if (closed) return
        closed = true
        dispatcher.unregisterTarget(this)
    }
}

val LocalPageTurnDispatcher = staticCompositionLocalOf(::PageTurnDispatcher)

/** Shared runtime configuration and optional guide state for targets under one dispatcher. */
internal class PageTurnRuntimeState(
    val dispatcher: PageTurnDispatcher,
    pageTurnPercent: Int,
    showGuide: Boolean,
    val guideColor: Color,
) {
    var pageTurnPercent by mutableIntStateOf(pageTurnPercent)
    var showGuide by mutableStateOf(showGuide)
    var lastPageTurnDirection by mutableIntStateOf(0)
    var pageTurnScrollInProgress by mutableStateOf(false)
}

@Composable
internal fun rememberPageTurnRuntimeState(): PageTurnRuntimeState {
    val dispatcher = LocalPageTurnDispatcher.current
    val settings = rememberSettingsStore()
    val guideColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
    val state = remember(dispatcher, guideColor) {
        PageTurnRuntimeState(
            dispatcher = dispatcher,
            pageTurnPercent = settings
                .getInt(PREF_PAGE_TURN_PERCENT, DEFAULT_PAGE_TURN_PERCENT)
                .coerceIn(50, 100),
            showGuide = settings.getBoolean(PREF_SHOW_PAGE_TURN_GUIDE, DEFAULT_SHOW_PAGE_TURN_GUIDE),
            guideColor = guideColor,
        )
    }
    DisposableEffect(settings, state) {
        val subscription = settings.observeKeyChanges { key ->
            when (key) {
                PREF_PAGE_TURN_PERCENT ->
                    state.pageTurnPercent = settings
                        .getInt(PREF_PAGE_TURN_PERCENT, DEFAULT_PAGE_TURN_PERCENT)
                        .coerceIn(50, 100)
                PREF_SHOW_PAGE_TURN_GUIDE ->
                    state.showGuide =
                        settings.getBoolean(PREF_SHOW_PAGE_TURN_GUIDE, DEFAULT_SHOW_PAGE_TURN_GUIDE).also { showGuide ->
                            if (!showGuide) state.lastPageTurnDirection = 0
                        }
            }
        }
        onDispose(subscription::close)
    }
    return state
}

/** Registers the target only while its owning page is active and consumes commands in composition scope. */
@Composable
private fun PageTurnTargetRegistrationEffect(
    dispatcher: PageTurnDispatcher,
    active: Boolean,
    onCommand: suspend (PageTurnCommand) -> Unit,
) {
    val currentHandler by rememberUpdatedState(onCommand)
    var registration by remember(dispatcher) { mutableStateOf<PageTurnTargetRegistration?>(null) }

    DisposableEffect(dispatcher, active) {
        if (active) registration = dispatcher.registerTarget()
        onDispose {
            registration?.close()
            registration = null
        }
    }
    registration?.let { target ->
        LaunchedEffect(target) {
            for (command in target.commands) currentHandler(command)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DraggablePageTurnButtons(
    onPageUp: () -> Unit,
    onPageDown: () -> Unit,
    onLongPressUp: () -> Unit,
    onLongPressDown: () -> Unit,
    modifier: Modifier = Modifier,
    preferenceName: String = "fabPageTurn",
) {
    val density = LocalDensity.current
    val screenSize = LocalWindowInfo.current.containerSize
    val settings = rememberSettingsStore()
    val buttonSize = 56.dp
    val gap = 4.dp
    val columnHeight = buttonSize * 2 + gap
    val defaultY = with(density) { screenSize.height - columnHeight.toPx() - 80.dp.toPx() }
    var offsetX by remember(preferenceName) { mutableFloatStateOf(settings.getFloat("$preferenceName-x", 0f)) }
    var offsetY by remember(preferenceName) { mutableFloatStateOf(settings.getFloat("$preferenceName-y", defaultY)) }

    fun clampPositionToViewport() {
        with(density) {
            val maxX = (screenSize.width - buttonSize.toPx()).coerceAtLeast(0f)
            val maxY = (screenSize.height - columnHeight.toPx()).coerceAtLeast(0f)
            offsetX = offsetX.coerceIn(0f, maxX)
            offsetY = offsetY.coerceIn(0f, maxY)
        }
    }

    LaunchedEffect(screenSize, density, preferenceName) {
        clampPositionToViewport()
    }
    val hapticFeedback = LocalHapticFeedback.current
    val fabOpacityValue by rememberObservedSetting(settings, PREF_FAB_OPACITY) {
        getInt(PREF_FAB_OPACITY, DEFAULT_FAB_OPACITY)
    }
    val opacityFraction = fabOpacityValue.coerceIn(10, 100) / 100f
    val fabColor = FloatingActionButtonDefaults.containerColor.copy(alpha = opacityFraction)
    val iconTint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = opacityFraction)
    var dragging by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .pointerInput(screenSize, density, preferenceName) {
                detectDragGestures(
                    onDragStart = {
                        dragging = true
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onDragEnd = {
                        dragging = false
                        clampPositionToViewport()
                        offsetX = if (offsetX < screenSize.width / 2f) {
                            0f
                        } else {
                            with(density) { (screenSize.width - buttonSize.toPx()).coerceAtLeast(0f) }
                        }
                        settings.putFloat("$preferenceName-x", offsetX)
                        settings.putFloat("$preferenceName-y", offsetY)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        if (dragging) {
                            offsetX += dragAmount.x
                            offsetY += dragAmount.y
                            clampPositionToViewport()
                        }
                    },
                )
            },
    ) {
        PageTurnButton(Icons.Default.KeyboardArrowUp, "上翻页", onPageUp, onLongPressUp, buttonSize, fabColor, iconTint)
        Spacer(modifier = Modifier.height(gap))
        PageTurnButton(
            Icons.Default.KeyboardArrowDown,
            "下翻页",
            onPageDown,
            onLongPressDown,
            buttonSize,
            fabColor,
            iconTint,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PageTurnButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    size: Dp,
    color: Color,
    iconTint: Color,
) {
    Surface(
        shape = CircleShape,
        color = color,
        modifier = Modifier.size(size).combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick,
            indication = null,
            interactionSource = remember { MutableInteractionSource() },
        ),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Icon(icon, contentDescription = description, tint = iconTint)
        }
    }
}

@Composable
fun PageTurnFab(
    dispatcher: PageTurnDispatcher = LocalPageTurnDispatcher.current,
    modifier: Modifier = Modifier,
    preferenceName: String = "fabPageTurn",
) {
    val settings = rememberSettingsStore()
    val showFab by rememberObservedSetting(settings, PREF_SHOW_PAGE_TURN_FAB) {
        getBoolean(PREF_SHOW_PAGE_TURN_FAB, false)
    }
    if (!isPageTurnSupported || !showFab || !dispatcher.hasActiveTarget) return
    DraggablePageTurnButtons(
        onPageUp = { dispatcher.dispatch(PageTurnCommand.PageUp) },
        onPageDown = { dispatcher.dispatch(PageTurnCommand.PageDown) },
        onLongPressUp = { dispatcher.dispatch(PageTurnCommand.JumpToTop) },
        onLongPressDown = { dispatcher.dispatch(PageTurnCommand.JumpToBottom) },
        modifier = modifier,
        preferenceName = preferenceName,
    )
}

/** Couples a page-turn command handler with the measured viewport used to calculate one-page distance. */
@Stable
class PageTurnTarget internal constructor(
    internal val state: PageTurnRuntimeState,
) {
    internal var viewportHeight = 0f
}

/**
 * Reports this scroll surface's visible height to [target] and, when enabled in settings, draws the
 * overlap guide left by the latest page turn. This modifier must wrap the actual scrolling viewport.
 */
fun Modifier.pageTurnViewportWithGuide(target: PageTurnTarget): Modifier =
    drawWithContent {
        drawContent()
        target.viewportHeight = size.height
        val state = target.state
        if (!state.showGuide || state.lastPageTurnDirection == 0) return@drawWithContent
        val overlapFraction = 1f - state.pageTurnPercent / 100f
        val y = if (state.lastPageTurnDirection > 0) {
            overlapFraction * size.height
        } else {
            state.pageTurnPercent / 100f * size.height
        }
        drawLine(
            color = state.guideColor,
            start = androidx.compose.ui.geometry
                .Offset(0f, y),
            end = androidx.compose.ui.geometry
                .Offset(size.width, y),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx())),
        )
    }

/**
 * Creates a target for a continuous [ScrollState]. Boundary callbacks optionally turn an extra page command at the
 * start or end into navigation to adjacent content.
 */
@Composable
fun rememberPageTurnTarget(
    scrollState: ScrollState,
    enabled: Boolean,
    maxScrollValue: Int = scrollState.maxValue,
    onPageUpAtStart: (() -> Unit)? = null,
    onPageDownAtEnd: (() -> Unit)? = null,
): PageTurnTarget {
    val state = rememberPageTurnRuntimeState()
    val target = remember(state) { PageTurnTarget(state) }
    val currentOnPageUpAtStart by rememberUpdatedState(onPageUpAtStart)
    val currentOnPageDownAtEnd by rememberUpdatedState(onPageDownAtEnd)
    val currentMaxScrollValue by rememberUpdatedState(maxScrollValue)
    LaunchedEffect(state, scrollState) {
        snapshotFlow { scrollState.isScrollInProgress }.collect { scrolling ->
            if (scrolling && !state.pageTurnScrollInProgress) state.lastPageTurnDirection = 0
        }
    }
    PageTurnTargetRegistrationEffect(state.dispatcher, enabled && isPageTurnSupported) { command ->
        state.lastPageTurnDirection = command.scrollDirection.takeIf { state.showGuide } ?: 0
        state.pageTurnScrollInProgress = true
        try {
            when (command) {
                PageTurnCommand.JumpToTop -> scrollState.scrollTo(0)
                PageTurnCommand.JumpToBottom -> scrollState.scrollTo(currentMaxScrollValue)
                PageTurnCommand.PageUp,
                PageTurnCommand.PageDown,
                -> {
                    val reachedStart = command == PageTurnCommand.PageUp && scrollState.value <= 0
                    val reachedEnd = command == PageTurnCommand.PageDown &&
                        currentMaxScrollValue != Int.MAX_VALUE &&
                        scrollState.value >= currentMaxScrollValue
                    when {
                        reachedStart && currentOnPageUpAtStart != null -> currentOnPageUpAtStart?.invoke()
                        reachedEnd && currentOnPageDownAtEnd != null -> currentOnPageDownAtEnd?.invoke()
                        target.viewportHeight > 0f -> {
                            scrollState.scrollBy(
                                target.viewportHeight * state.pageTurnPercent / 100f * command.scrollDirection,
                            )
                        }
                    }
                }
            }
        } finally {
            state.pageTurnScrollInProgress = false
        }
    }
    return target
}

/** Creates a target for a lazy list while preserving ownership of [listState] in the calling page. */
@Composable
fun rememberPageTurnTarget(
    listState: LazyListState,
    enabled: Boolean,
): PageTurnTarget {
    val state = rememberPageTurnRuntimeState()
    val target = remember(state) { PageTurnTarget(state) }
    LaunchedEffect(state, listState) {
        snapshotFlow { listState.isScrollInProgress }.collect { scrolling ->
            if (scrolling && !state.pageTurnScrollInProgress) state.lastPageTurnDirection = 0
        }
    }
    PageTurnTargetRegistrationEffect(state.dispatcher, enabled && isPageTurnSupported) { command ->
        state.lastPageTurnDirection = command.scrollDirection.takeIf { state.showGuide } ?: 0
        state.pageTurnScrollInProgress = true
        try {
            when (command) {
                PageTurnCommand.JumpToTop -> listState.scrollToItem(0)
                PageTurnCommand.JumpToBottom -> {
                    val lastIndex = listState.layoutInfo.totalItemsCount - 1
                    if (lastIndex >= 0) listState.scrollToItem(lastIndex)
                }
                PageTurnCommand.PageUp,
                PageTurnCommand.PageDown,
                -> {
                    if (target.viewportHeight > 0f) {
                        listState.scrollBy(
                            target.viewportHeight * state.pageTurnPercent / 100f * command.scrollDirection,
                        )
                    }
                }
            }
        } finally {
            state.pageTurnScrollInProgress = false
        }
    }
    return target
}

private val PageTurnCommand.scrollDirection: Int
    get() = when (this) {
        PageTurnCommand.PageUp -> -1
        PageTurnCommand.PageDown -> 1
        PageTurnCommand.JumpToTop,
        PageTurnCommand.JumpToBottom,
        -> 0
    }
