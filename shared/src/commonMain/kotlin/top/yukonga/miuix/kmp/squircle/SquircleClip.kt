// SPDX-License-Identifier: Apache-2.0
//
// Minimal squircle clip shim for the vendored miuix-nav.
//
// Upstream miuix-squircle's absoluteSquircleClip is shader-backed (AGSL RuntimeShader + a baked
// SDF) and falls back to AbsoluteRoundedCornerShape when runtime shaders are unavailable. Vendoring
// that whole chain (miuix-shader + AGSL platform actuals) for the single corner clip miuix-nav
// performs during navigation transitions is disproportionate, so this provides the same public
// signature using a pure-path GenericShape built from the vendored [addSquircleRect] math. It keeps
// the MIUIX G2 corner silhouette (incl. asymmetric left/right-edge modes) without the shader tree.

package top.yukonga.miuix.kmp.squircle

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.max
import kotlin.math.min

/**
 * Clips [this] to a squircle with independent physical corner radii (never flipped by layout
 * direction), matching the upstream `absoluteSquircleClip` contract used by miuix-nav.
 */
@Composable
fun Modifier.absoluteSquircleClip(
    topLeft: Dp,
    topRight: Dp,
    bottomRight: Dp,
    bottomLeft: Dp,
    extension: Float = SquircleDefaults.Extension,
): Modifier {
    val shape =
        remember(topLeft, topRight, bottomRight, bottomLeft, extension) {
            AbsoluteSquircleShape(topLeft, topRight, bottomRight, bottomLeft, extension)
        }
    return clip(shape)
}

private class AbsoluteSquircleShape(
    private val topLeft: Dp,
    private val topRight: Dp,
    private val bottomRight: Dp,
    private val bottomLeft: Dp,
    private val extension: Float,
) : Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val path =
            Path().apply {
                addSquircleRect(
                    width = size.width,
                    height = size.height,
                    topLeftPx = with(density) { topLeft.toPx() },
                    topRightPx = with(density) { topRight.toPx() },
                    bottomRightPx = with(density) { bottomRight.toPx() },
                    bottomLeftPx = with(density) { bottomLeft.toPx() },
                    extension = extension,
                )
            }
        return Outline.Generic(path)
    }
}

/**
 * Appends a squircle rectangle with per-corner radii. A corner whose radius resolves to `0` becomes
 * a sharp 90° vertex. Mirrors the cubic construction in the uniform [addSquircleRect] in this module.
 */
private fun Path.addSquircleRect(
    width: Float,
    height: Float,
    topLeftPx: Float,
    topRightPx: Float,
    bottomRightPx: Float,
    bottomLeftPx: Float,
    extension: Float,
) {
    if (width <= 0f || height <= 0f) return
    val ext = extension.coerceIn(SquircleDefaults.ExtensionMin, SquircleDefaults.ExtensionMax)
    val halfMin = min(width, height) * 0.5f

    fun tile(r: Float) = max(0f, r * ext).coerceAtMost(halfMin)
    val tTL = tile(topLeftPx)
    val tTR = tile(topRightPx)
    val tBR = tile(bottomRightPx)
    val tBL = tile(bottomLeftPx)
    val k = 1f - SQUIRCLE_CONTROL

    moveTo(tTL, 0f)
    lineTo(width - tTR, 0f)
    if (tTR > 0f) cubicTo(width - tTR * k, 0f, width, tTR * k, width, tTR)
    lineTo(width, height - tBR)
    if (tBR > 0f) cubicTo(width, height - tBR * k, width - tBR * k, height, width - tBR, height)
    lineTo(tBL, height)
    if (tBL > 0f) cubicTo(tBL * k, height, 0f, height - tBL * k, 0f, height - tBL)
    lineTo(0f, tTL)
    if (tTL > 0f) cubicTo(0f, tTL * k, tTL * k, 0f, tTL, 0f)
    close()
}
