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

import android.graphics.RenderEffect
import android.os.Build
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.ui.components.liquidTopBarScrollScrim
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.BackdropEffectScope
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCanvasBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.effect
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow
import com.kyant.shapes.RoundedRectangularShape

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
                        if (floating) floatingGlassLens()
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

/**
 * 浮空玻璃透镜折射：解耦 `refractionHeight` / `refractionAmount`（扭曲强度）与 `chromaticAberration`（色散系数），
 * 避免 `backdrop` 库内置布尔开关在拉高折射扭曲时硬编码 `1.0f` 导致边缘彩虹色散过重。
 */
internal fun BackdropEffectScope.floatingGlassLens(
    refractionHeight: Float = 24.dp.toPx(),
    refractionAmount: Float = 48.dp.toPx(),
    depthEffect: Boolean = true,
    chromaticAberration: Float = 0.25f,
) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    if (refractionHeight <= 0f || refractionAmount <= 0f) return
    if (chromaticAberration <= 0f) {
        lens(refractionHeight, refractionAmount, depthEffect = depthEffect, chromaticAberration = false)
        return
    }
    val cornerRadii = resolveLensCornerRadii() ?: run {
        lens(refractionHeight, refractionAmount, depthEffect = depthEffect, chromaticAberration = false)
        return
    }
    if (padding > 0f) {
        padding = (padding - refractionHeight).coerceAtLeast(0f)
    }
    val shader = obtainRuntimeShader(
        key = "FloatingGlassRefractionWithDispersion",
        string = FLOATING_GLASS_REFRACTION_SHADER,
    ).apply {
        setFloatUniform("size", size.width, size.height)
        setFloatUniform("offset", -padding, -padding)
        setFloatUniform("cornerRadii", cornerRadii)
        setFloatUniform("refractionHeight", refractionHeight)
        setFloatUniform("refractionAmount", -refractionAmount)
        setFloatUniform("depthEffect", if (depthEffect) 1f else 0f)
        setFloatUniform("chromaticAberration", chromaticAberration)
    }
    effect(RenderEffect.createRuntimeShaderEffect(shader, "content"))
}

private const val FLOATING_GLASS_REFRACTION_SHADER = """
uniform shader content;

uniform float2 size;
uniform float2 offset;
uniform float4 cornerRadii;
uniform float refractionHeight;
uniform float refractionAmount;
uniform float depthEffect;
uniform float chromaticAberration;

float radiusAt(float2 coord, float4 radii) {
    if (coord.x >= 0.0) {
        if (coord.y <= 0.0) return radii.y;
        else return radii.z;
    } else {
        if (coord.y <= 0.0) return radii.x;
        else return radii.w;
    }
}

float sdRoundedRect(float2 coord, float2 halfSize, float radius) {
    float2 cornerCoord = abs(coord) - (halfSize - float2(radius));
    float outside = length(max(cornerCoord, 0.0)) - radius;
    float inside = min(max(cornerCoord.x, cornerCoord.y), 0.0);
    return outside + inside;
}

float2 gradSdRoundedRect(float2 coord, float2 halfSize, float radius) {
    float2 cornerCoord = abs(coord) - (halfSize - float2(radius));
    if (cornerCoord.x >= 0.0 || cornerCoord.y >= 0.0) {
        return sign(coord) * normalize(max(cornerCoord, 0.0));
    } else {
        float gradX = step(cornerCoord.y, cornerCoord.x);
        return sign(coord) * float2(gradX, 1.0 - gradX);
    }
}

float circleMap(float x) {
    return 1.0 - sqrt(1.0 - x * x);
}

half4 main(float2 coord) {
    float2 halfSize = size * 0.5;
    float2 centeredCoord = (coord + offset) - halfSize;
    float radius = radiusAt(coord, cornerRadii);

    float sd = sdRoundedRect(centeredCoord, halfSize, radius);
    if (-sd >= refractionHeight) {
        return content.eval(coord);
    }
    sd = min(sd, 0.0);

    float d = circleMap(1.0 - -sd / refractionHeight) * refractionAmount;
    float gradRadius = min(radius * 1.5, min(halfSize.x, halfSize.y));
    float2 grad = normalize(gradSdRoundedRect(centeredCoord, halfSize, gradRadius) + depthEffect * normalize(centeredCoord));

    float2 refractedCoord = coord + d * grad;
    float dispersionIntensity = chromaticAberration * ((centeredCoord.x * centeredCoord.y) / (halfSize.x * halfSize.y));
    float2 dispersedCoord = d * grad * dispersionIntensity;

    half4 color = half4(0.0);

    half4 red = content.eval(refractedCoord + dispersedCoord);
    color.r += red.r / 3.5;
    color.a += red.a / 7.0;

    half4 orange = content.eval(refractedCoord + dispersedCoord * (2.0 / 3.0));
    color.r += orange.r / 3.5;
    color.g += orange.g / 7.0;
    color.a += orange.a / 7.0;

    half4 yellow = content.eval(refractedCoord + dispersedCoord * (1.0 / 3.0));
    color.r += yellow.r / 3.5;
    color.g += yellow.g / 3.5;
    color.a += yellow.a / 7.0;

    half4 green = content.eval(refractedCoord);
    color.g += green.g / 3.5;
    color.a += green.a / 7.0;

    half4 cyan = content.eval(refractedCoord - dispersedCoord * (1.0 / 3.0));
    color.g += cyan.g / 3.5;
    color.b += cyan.b / 3.0;
    color.a += cyan.a / 7.0;

    half4 blue = content.eval(refractedCoord - dispersedCoord * (2.0 / 3.0));
    color.b += blue.b / 3.0;
    color.a += blue.a / 7.0;

    half4 purple = content.eval(refractedCoord - dispersedCoord);
    color.r += purple.r / 7.0;
    color.b += purple.b / 3.0;
    color.a += purple.a / 7.0;

    return color;
}
"""

private fun BackdropEffectScope.resolveLensCornerRadii(): FloatArray? = when (val currentShape = shape) {
    is RoundedRectangularShape -> {
        val corners = currentShape.corners(size, layoutDirection, this)
        floatArrayOf(corners.topLeft, corners.topRight, corners.bottomRight, corners.bottomLeft)
    }

    is CornerBasedShape -> {
        val maxRadius = size.minDimension * 0.5f
        val isLtr = layoutDirection == LayoutDirection.Ltr
        val topLeft = (if (isLtr) currentShape.topStart else currentShape.topEnd).toPx(size, this).coerceAtMost(maxRadius)
        val topRight = (if (isLtr) currentShape.topEnd else currentShape.topStart).toPx(size, this).coerceAtMost(maxRadius)
        val bottomRight = (if (isLtr) currentShape.bottomEnd else currentShape.bottomStart).toPx(size, this).coerceAtMost(maxRadius)
        val bottomLeft = (if (isLtr) currentShape.bottomStart else currentShape.bottomEnd).toPx(size, this).coerceAtMost(maxRadius)
        floatArrayOf(topLeft, topRight, bottomRight, bottomLeft)
    }

    else -> null
}
