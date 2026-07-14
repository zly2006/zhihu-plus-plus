/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 */

package com.github.zly2006.zhihu.ui.components

import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.Velocity
import kotlin.math.absoluteValue

internal object NoOpPagerNestedScrollConnection : NestedScrollConnection

/**
 * Hands same-axis boundary drags from a child pager to its direct parent.
 *
 * Adapted from the Apache-2.0 implementation at
 * https://github.com/CodeIdeal/nested-horizontal-pager. Parent and child pagers disable their
 * default same-axis nested-scroll connections so this connection exclusively owns the hand-off.
 */
@Composable
internal fun rememberNestedHorizontalPagerConnection(
    parentState: PagerState,
    childState: PagerState,
): NestedScrollConnection {
    val minimumFlingVelocity = LocalViewConfiguration.current.minimumFlingVelocity

    return remember(parentState, childState, minimumFlingVelocity) {
        object : NestedScrollConnection {
            var parentDragActive = false

            fun scrollParent(deltaX: Float): Offset {
                if (!parentState.isBetweenPages() && !parentState.canScrollInGestureDirection(deltaX)) {
                    parentDragActive = false
                    return Offset.Zero
                }
                val consumed = -parentState.dispatchRawDelta(-deltaX)
                if (consumed.absoluteValue > 0f) parentDragActive = true
                return Offset(consumed, 0f)
            }

            suspend fun settleParent(velocityX: Float) {
                parentDragActive = false
                val targetPage = when {
                    velocityX < -minimumFlingVelocity && parentState.canScrollForward -> parentState.currentPage + 1
                    velocityX > minimumFlingVelocity && parentState.canScrollBackward -> parentState.currentPage - 1
                    else -> parentState.currentPage
                }.coerceIn(0, parentState.pageCount - 1)
                parentState.animateScrollToPage(targetPage)
            }

            fun shouldParentHandleFling(velocityX: Float): Boolean {
                val childAtBoundary = !childState.canScrollInGestureDirection(velocityX)
                val parentCanMove =
                    parentDragActive ||
                        parentState.canScrollInGestureDirection(velocityX) ||
                        parentState.isBetweenPages()
                return parentCanMove &&
                    (parentDragActive || childAtBoundary) &&
                    velocityX.absoluteValue >= minimumFlingVelocity
            }

            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput || available.x == 0f) return Offset.Zero
                if (parentDragActive) return scrollParent(available.x)
                if (!childState.isScrollInProgress) return Offset.Zero
                return if (!childState.canScrollInGestureDirection(available.x)) {
                    scrollParent(available.x)
                } else {
                    Offset.Zero
                }
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                if (source != NestedScrollSource.UserInput || available.x == 0f) return Offset.Zero
                return if (parentDragActive || parentState.canScrollInGestureDirection(available.x)) {
                    scrollParent(available.x)
                } else {
                    Offset.Zero
                }
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                val childOwnsGesture = parentDragActive || childState.isScrollInProgress
                if (available.x == 0f || !childOwnsGesture || !shouldParentHandleFling(available.x)) {
                    return Velocity.Zero
                }
                settleParent(available.x)
                return available.copy(y = 0f)
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                val shouldSettle = parentDragActive || parentState.isBetweenPages()
                if (available.x == 0f || !(shouldSettle || shouldParentHandleFling(available.x))) {
                    return Velocity.Zero
                }
                settleParent(available.x)
                return available.copy(y = 0f)
            }
        }
    }
}

private fun PagerState.canScrollInGestureDirection(deltaX: Float): Boolean = when {
    deltaX < 0f -> canScrollForward
    deltaX > 0f -> canScrollBackward
    else -> false
}

private fun PagerState.isBetweenPages(): Boolean = currentPageOffsetFraction.absoluteValue >= 0.001f
