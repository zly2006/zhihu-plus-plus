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

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.shared.platform.rememberSettingsStore
import kotlin.math.roundToInt

/**
 * 可拖动并自动贴边的刷新按钮。
 *
 * 这个 FAB 用于首页和其他列表页的手动刷新入口。位置按 [preferenceName] 分别保存到 `-x`、`-y` 两个 preference key，
 * 拖动结束后会限制在屏幕内并贴近左右边缘，避免遮挡内容或被系统栏吞掉。需要多个可拖动按钮时必须使用不同的 [preferenceName]。
 */
@Composable
fun DraggableRefreshButton(
    modifier: Modifier = Modifier,
    preferenceName: String = "fabRefresh",
    onClick: () -> Unit,
    content: @Composable () -> Unit = {
        Icon(Icons.Default.Refresh, contentDescription = "刷新")
    },
) {
    val density = LocalDensity.current
    val screenSize = LocalWindowInfo.current.containerSize
    val settings = rememberSettingsStore()

    var offsetX by remember { mutableFloatStateOf(settings.getFloat("$preferenceName-x", Float.MAX_VALUE)) }
    var offsetY by remember { mutableFloatStateOf(settings.getFloat("$preferenceName-y", Float.MAX_VALUE)) }
    var pressing by remember { mutableStateOf(false) }

    fun adjustFabPosition() {
        with(density) {
            offsetX = offsetX.coerceIn(0f, screenSize.width - 56.dp.toPx())
            offsetY = offsetY.coerceIn(0f, screenSize.height - 250.dp.toPx())
        }
    }

    adjustFabPosition()

    val animatedOffsetX by animateFloatAsState(
        targetValue = offsetX,
        animationSpec = tween(if (pressing) 1 else 300),
        label = "offsetX",
    )
    val animatedOffsetY by animateFloatAsState(
        targetValue = offsetY,
        animationSpec = tween(if (pressing) 1 else 300),
        label = "offsetY",
    )
    val hapticFeedback = LocalHapticFeedback.current

    FloatingActionButton(
        onClick = onClick,
        shape = CircleShape,
        modifier = modifier
            .offset { IntOffset(animatedOffsetX.roundToInt(), animatedOffsetY.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = {
                        pressing = true
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    onDragEnd = {
                        pressing = false
                        adjustFabPosition()
                        val screenWidth = screenSize.width.toFloat()
                        with(density) {
                            offsetX =
                                if (offsetX < screenWidth / 2) {
                                    0f
                                } else {
                                    screenWidth - 56.dp.toPx()
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
                            adjustFabPosition()
                        }
                    },
                )
            },
        content = content,
    )
}
