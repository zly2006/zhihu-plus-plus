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

package com.github.zly2006.zhihu.theme

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.ui.components.liquidTopBarScrollScrim
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCanvasBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow

actual val isLiquidGlassSupported: Boolean
    get() = Build.VERSION.SDK_INT >= 33

internal interface AndroidLiquidBackdrop : UiBackdrop {
    val nativeBackdrop: Backdrop
}

@Composable
internal fun rememberDefaultLiquidBackdrop(): Backdrop {
    val overlay = LocalUiBackdrop.current as? AndroidLiquidBackdrop
    val surface = MaterialTheme.colorScheme.surfaceBright
    // 内联控件不采样正在录制自身的内容层，防止递归渲染。
    val canvas = rememberCanvasBackdrop { drawRect(surface) }
    return overlay?.nativeBackdrop ?: canvas
}

@Composable
internal actual fun rememberLiquidGlassBackdrop(): UiBackdrop {
    val backgroundColor by rememberUpdatedState(MaterialTheme.colorScheme.background)
    val backdrop = rememberLayerBackdrop {
        drawRect(backgroundColor)
        drawContent()
    }
    return remember(backdrop) {
        object : AndroidLiquidBackdrop {
            override val isActive: Boolean = true
            override val nativeBackdrop: Backdrop = backdrop
            override val source: Modifier = Modifier.layerBackdrop(backdrop)

            @Composable
            override fun chromeSurface(
                shape: Shape,
                tint: Color,
                floating: Boolean,
            ): Modifier {
                val resolvedTint = if (tint != Color.Unspecified) {
                    tint
                } else {
                    liquidGlassSemanticTokens().chromeTint
                }
                return Modifier.drawBackdrop(
                    backdrop = backdrop,
                    shape = { shape },
                    effects = {
                        vibrancy()
                        blur(if (floating) 2.dp.toPx() else 8.dp.toPx())
                        if (floating) {
                            lens(
                                refractionHeight = 22.dp.toPx(),
                                refractionAmount = 42.dp.toPx(),
                                depthEffect = true,
                                chromaticAberration = true,
                            )
                        }
                    },
                    highlight = { if (floating) Highlight.Default else null },
                    shadow = { if (floating) Shadow(radius = 12.dp, color = Color.Black.copy(alpha = 0.08f)) else null },
                    onDrawSurface = { drawRect(resolvedTint) },
                )
            }

            @Composable
            override fun scrollScrimSurface(
                overlapFraction: () -> Float,
                canvasColor: Color,
            ): Modifier = Modifier
                .drawBackdrop(
                    backdrop = backdrop,
                    shape = { RectangleShape },
                    effects = {
                        val fraction = overlapFraction().coerceIn(0f, 1f)
                        if (fraction > 0.01f) {
                            blur(16.dp.toPx() * fraction)
                        }
                    },
                    highlight = { null },
                    shadow = { null },
                ).then(
                    liquidTopBarScrollScrim(
                        overlappedFraction = overlapFraction,
                        canvasColor = canvasColor,
                    ),
                )
        }
    }
}
