/*
 * Original work from Kyant0/AndroidLiquidGlass (v1.0.6, commit 896a94a3ade1cc1a940b92365f942a34971fecda)
 * Copyright (C) Kyant0 <https://github.com/Kyant0/AndroidLiquidGlass>
 * Licensed under the Apache License, Version 2.0 (https://www.apache.org/licenses/LICENSE-2.0)
 *
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

package com.github.zly2006.zhihu.ui.liquidglass.utils

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerId
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.changedToUpIgnoreConsumed
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.util.fastFirstOrNull
import kotlin.math.abs

suspend fun PointerInputScope.inspectDragGestures(
    onDragStart: (down: PointerInputChange) -> Unit = {},
    onDragEnd: (change: PointerInputChange) -> Unit = {},
    onDragCancel: () -> Unit = {},
    onDrag: (change: PointerInputChange, dragAmount: Offset) -> Unit,
) {
    awaitEachGesture {
        val initialDown = awaitFirstDown(false, PointerEventPass.Initial)

        val down = awaitFirstDown(false)
        val drag = initialDown

        onDragStart(down)
        onDrag(drag, Offset.Zero)
        val upEvent =
            drag(
                pointerId = drag.id,
                touchSlop = viewConfiguration.touchSlop,
                onDrag = { change, amount -> onDrag(change, amount) },
            )
        if (upEvent == null) {
            onDragCancel()
        } else {
            onDragEnd(upEvent)
        }
    }
}

private suspend inline fun AwaitPointerEventScope.drag(
    pointerId: PointerId,
    touchSlop: Float,
    onDrag: (PointerInputChange, Offset) -> Unit,
): PointerInputChange? {
    val isPointerUp = currentEvent.changes.fastFirstOrNull { it.id == pointerId }?.pressed != true
    if (isPointerUp) {
        return null
    }
    var pointer = pointerId
    var horizontalLocked = false
    var totalDx = 0f
    var totalDy = 0f
    while (true) {
        val change = awaitDragOrUp(pointer) ?: return null
        if (change.isConsumed) {
            return null
        }
        if (change.changedToUpIgnoreConsumed()) {
            if (horizontalLocked) {
                change.consume()
            }
            return change
        }
        val delta = change.positionChange()
        if (!horizontalLocked) {
            totalDx += delta.x
            totalDy += delta.y
            val absX = abs(totalDx)
            val absY = abs(totalDy)
            when {
                absX >= touchSlop && absX >= absY -> {
                    horizontalLocked = true
                    change.consume()
                    onDrag(change, Offset(totalDx, totalDy))
                }
                absY >= touchSlop && absY > absX -> {
                    // 让父级纵向滚动接管，取消当前横向控件拖动。
                    return null
                }
                else -> {
                    // 尚未超过 touchSlop，等待父级在 Final pass 是否消费。
                    val finalChange = awaitPointerEvent(PointerEventPass.Final)
                        .changes
                        .fastFirstOrNull { it.id == change.id }
                    if (finalChange?.isConsumed == true) return null
                }
            }
        } else {
            // 横向拖动已锁定，持续消费事件以防纵向漂移被父级滚动打断。
            change.consume()
            onDrag(change, delta)
        }
        pointer = change.id
    }
}

private suspend inline fun AwaitPointerEventScope.awaitDragOrUp(
    pointerId: PointerId,
): PointerInputChange? {
    var pointer = pointerId
    while (true) {
        val event = awaitPointerEvent()
        val dragEvent = event.changes.fastFirstOrNull { it.id == pointer } ?: return null
        if (dragEvent.changedToUpIgnoreConsumed()) {
            val otherDown = event.changes.fastFirstOrNull { it.pressed }
            if (otherDown == null) {
                return dragEvent
            } else {
                pointer = otherDown.id
            }
        } else {
            val hasDragged = dragEvent.previousPosition != dragEvent.position
            if (hasDragged) {
                return dragEvent
            }
        }
    }
}
