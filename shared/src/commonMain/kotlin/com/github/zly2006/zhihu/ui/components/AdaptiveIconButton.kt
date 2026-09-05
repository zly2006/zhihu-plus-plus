package com.github.zly2006.zhihu.ui.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.github.zly2006.zhihu.theme.LocalLiquidGlass

@Composable
fun AdaptiveIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = if (LocalLiquidGlass.current) {
        IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary)
    } else {
        IconButtonDefaults.iconButtonColors()
    },
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    androidx.compose.material3.IconButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = colors,
        interactionSource = interactionSource,
        content = content,
    )
}
