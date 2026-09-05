package com.github.zly2006.zhihu.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
internal expect fun CatalogButton(onClick: () -> Unit, modifier: Modifier, enabled: Boolean, containerColor: Color, contentColor: Color, content: @Composable RowScope.() -> Unit)

@Composable
internal expect fun CatalogSlider(value: Float, onValueChange: (Float) -> Unit, modifier: Modifier, enabled: Boolean, valueRange: ClosedFloatingPointRange<Float>, steps: Int, onValueChangeFinished: (() -> Unit)?)
