package com.github.zly2006.zhihu.theme

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.Shadow

actual val isLiquidGlassSupported: Boolean get() = Build.VERSION.SDK_INT >= 33

@Composable
actual fun rememberGlassBackdrop(): GlassBackdrop {
    val backdrop = rememberLayerBackdrop()
    return remember(backdrop) {
        object : AndroidGlassBackdrop {
            override val nativeBackdrop = backdrop
            override val source = Modifier.layerBackdrop(backdrop)

            override fun surface(shape: Shape, tint: Color, floating: Boolean) = Modifier.drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    vibrancy()
                    blur(10.dp.toPx())
                    if (floating) lens(6.dp.toPx(), 8.dp.toPx())
                },
                highlight = { if (floating) Highlight(width = 0.5.dp, blurRadius = 0.5.dp, alpha = 0.4f) else null },
                shadow = { if (floating) Shadow(radius = 12.dp, color = Color.Black.copy(alpha = 0.07f)) else null },
                onDrawSurface = { drawRect(tint) },
            )
        }
    }
}

internal interface AndroidGlassBackdrop : GlassBackdrop {
    val nativeBackdrop: com.kyant.backdrop.Backdrop
}
