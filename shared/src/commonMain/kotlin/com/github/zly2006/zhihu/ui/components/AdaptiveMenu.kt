package com.github.zly2006.zhihu.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.github.zly2006.zhihu.theme.LocalLiquidGlass

@Composable
fun AdaptiveDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    scrollState: ScrollState = rememberScrollState(),
    properties: PopupProperties = PopupProperties(focusable = true),
    shape: Shape = if (LocalLiquidGlass.current) MaterialTheme.shapes.large else MenuDefaults.shape,
    containerColor: Color = if (LocalLiquidGlass.current) MaterialTheme.colorScheme.surfaceBright else MenuDefaults.containerColor,
    tonalElevation: Dp = if (LocalLiquidGlass.current) 0.dp else MenuDefaults.TonalElevation,
    shadowElevation: Dp = if (LocalLiquidGlass.current) 8.dp else MenuDefaults.ShadowElevation,
    border: BorderStroke? = if (LocalLiquidGlass.current) BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant) else null,
    content: @Composable ColumnScope.() -> Unit,
) {
    androidx.compose.material3.DropdownMenu(
        expanded,
        onDismissRequest,
        modifier,
        offset,
        scrollState,
        properties,
        shape,
        containerColor,
        tonalElevation,
        shadowElevation,
        border,
        content,
    )
}
