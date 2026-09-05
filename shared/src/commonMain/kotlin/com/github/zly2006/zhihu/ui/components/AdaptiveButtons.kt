package com.github.zly2006.zhihu.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.theme.LocalLiquidGlass

@Composable
fun AdaptiveButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.shape,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    elevation: ButtonElevation? = ButtonDefaults.buttonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val glass = LocalLiquidGlass.current
    if (glass) {
        CatalogButton(
            onClick,
            modifier,
            enabled,
            if (enabled) colors.containerColor else colors.disabledContainerColor,
            if (enabled) colors.contentColor else colors.disabledContentColor,
            content,
        )
        return
    }
    val interactions = interactionSource ?: remember { MutableInteractionSource() }
    androidx.compose.material3.Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactions,
        content = content,
    )
}

@Composable
fun AdaptiveFilledTonalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.filledTonalShape,
    colors: ButtonColors = if (LocalLiquidGlass.current) ButtonDefaults.filledTonalButtonColors(containerColor = MaterialTheme.colorScheme.surfaceBright, contentColor = MaterialTheme.colorScheme.primary) else ButtonDefaults.filledTonalButtonColors(),
    elevation: ButtonElevation? = ButtonDefaults.filledTonalButtonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val glass = LocalLiquidGlass.current
    if (glass) {
        CatalogButton(
            onClick,
            modifier,
            enabled,
            if (enabled) colors.containerColor else colors.disabledContainerColor,
            if (enabled) colors.contentColor else colors.disabledContentColor,
            content,
        )
        return
    }
    val interactions = interactionSource ?: remember { MutableInteractionSource() }
    androidx.compose.material3.FilledTonalButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactions,
        content = content,
    )
}

@Composable
fun AdaptiveOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.outlinedShape,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = if (LocalLiquidGlass.current) BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant) else ButtonDefaults.outlinedButtonBorder(enabled),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val glass = LocalLiquidGlass.current
    if (glass) {
        CatalogButton(
            onClick,
            modifier,
            enabled,
            if (enabled) colors.containerColor else colors.disabledContainerColor,
            if (enabled) colors.contentColor else colors.disabledContentColor,
            content,
        )
        return
    }
    val interactions = interactionSource ?: remember { MutableInteractionSource() }
    androidx.compose.material3.OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactions,
        content = content,
    )
}

@Composable
fun AdaptiveTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.textShape,
    colors: ButtonColors = ButtonDefaults.textButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.TextButtonContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    val glass = LocalLiquidGlass.current
    if (glass) {
        CatalogButton(
            onClick,
            modifier,
            enabled,
            if (enabled) colors.containerColor else colors.disabledContainerColor,
            if (enabled) colors.contentColor else colors.disabledContentColor,
            content,
        )
        return
    }
    val interactions = interactionSource ?: remember { MutableInteractionSource() }
    androidx.compose.material3.TextButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        shape = shape,
        colors = colors,
        elevation = elevation,
        border = border,
        contentPadding = contentPadding,
        interactionSource = interactions,
        content = content,
    )
}
