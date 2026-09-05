package com.github.zly2006.zhihu.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.FloatingActionButtonElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.theme.LocalLiquidGlass

@Composable
fun AdaptiveFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = if (LocalLiquidGlass.current) CircleShape else FloatingActionButtonDefaults.shape,
    containerColor: Color = if (LocalLiquidGlass.current) MaterialTheme.colorScheme.primary else FloatingActionButtonDefaults.containerColor,
    contentColor: Color = contentColorFor(containerColor),
    elevation: FloatingActionButtonElevation = if (LocalLiquidGlass.current) FloatingActionButtonDefaults.elevation(defaultElevation = 2.dp) else FloatingActionButtonDefaults.elevation(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    if (LocalLiquidGlass.current) {
        CatalogButton(onClick, modifier.size(56.dp), true, containerColor, contentColor) { content() }
        return
    }
    androidx.compose.material3.FloatingActionButton(onClick, modifier, shape, containerColor, contentColor, elevation, interactionSource, content)
}
