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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import com.github.zly2006.zhihu.ui.components.liquidTopBarScrollScrim
import top.yukonga.miuix.kmp.blur.BlendColorEntry
import top.yukonga.miuix.kmp.blur.BlurColors
import top.yukonga.miuix.kmp.blur.LayerBackdrop
import top.yukonga.miuix.kmp.blur.isRuntimeShaderSupported
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.blur.rememberLayerBackdrop
import top.yukonga.miuix.kmp.blur.textureBlur
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 统一的背景采样与模糊/玻璃材质契约。
 *
 * [source] 只能挂载在页面内容容器上，且内容区子树必须把 [LocalUiBackdrop] 重置为 [NoOpUiBackdrop]，
 * 防止内联控件回采正在录制自身的内容层导致渲染递归崩溃。
 */
interface UiBackdrop {
    val isActive: Boolean
    val source: Modifier

    @Composable
    fun chromeSurface(
        shape: Shape = RectangleShape,
        tint: Color = Color.Unspecified,
        floating: Boolean = true,
    ): Modifier

    @Composable
    fun scrollScrimSurface(
        overlapFraction: () -> Float,
        canvasColor: Color,
    ): Modifier = liquidTopBarScrollScrim(
        overlappedFraction = overlapFraction,
        canvasColor = canvasColor,
    )
}

object NoOpUiBackdrop : UiBackdrop {
    override val isActive: Boolean = false
    override val source: Modifier = Modifier

    @Composable
    override fun chromeSurface(
        shape: Shape,
        tint: Color,
        floating: Boolean,
    ): Modifier = Modifier
}

val LocalUiBackdrop = staticCompositionLocalOf<UiBackdrop> { NoOpUiBackdrop }

expect val isLiquidGlassSupported: Boolean

@Composable
internal expect fun rememberLiquidGlassBackdrop(): UiBackdrop

@Composable
fun rememberUiBackdrop(
    style: UiStyle = LocalUiStyle.current,
    enabled: Boolean = true,
): UiBackdrop {
    if (!enabled) return NoOpUiBackdrop
    return when (style) {
        UiStyle.Material3 -> NoOpUiBackdrop
        UiStyle.LiquidGlass -> if (isLiquidGlassSupported) rememberLiquidGlassBackdrop() else NoOpUiBackdrop
        UiStyle.Miuix -> rememberMiuixUiBackdrop()
    }
}

@Composable
private fun rememberMiuixUiBackdrop(): UiBackdrop {
    if (!isRuntimeShaderSupported()) return NoOpUiBackdrop
    val currentSurfaceColor by rememberUpdatedState(MiuixTheme.colorScheme.surface)
    val layerBackdrop: LayerBackdrop = rememberLayerBackdrop {
        drawRect(currentSurfaceColor)
        drawContent()
    }
    return remember(layerBackdrop) {
        object : UiBackdrop {
            override val isActive: Boolean = true
            override val source: Modifier = Modifier.layerBackdrop(layerBackdrop)

            @Composable
            override fun chromeSurface(
                shape: Shape,
                tint: Color,
                floating: Boolean,
            ): Modifier {
                val blendColor = if (tint != Color.Unspecified) {
                    tint
                } else {
                    MiuixTheme.colorScheme.surface.copy(alpha = 0.8f)
                }
                return Modifier.textureBlur(
                    backdrop = layerBackdrop,
                    shape = shape,
                    blurRadius = 25f,
                    colors = BlurColors(blendColors = listOf(BlendColorEntry(color = blendColor))),
                )
            }
        }
    }
}
