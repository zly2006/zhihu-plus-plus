package com.github.zly2006.zhihu.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.github.zly2006.zhihu.theme.LocalLiquidGlass

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors = SliderDefaults.colors(),
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    if (LocalLiquidGlass.current) {
        CatalogSlider(value, onValueChange, modifier, enabled, valueRange, steps, onValueChangeFinished)
    } else {
        androidx.compose.material3.Slider(value, onValueChange, modifier, enabled, valueRange, steps, onValueChangeFinished, colors, interactionSource)
    }
}
