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

package com.github.zly2006.zhihu.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBoxScope
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.MenuItemColors
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.PopupProperties
import com.github.zly2006.zhihu.theme.LocalLiquidGlassOverlay
import com.github.zly2006.zhihu.theme.LocalUiStyle
import com.github.zly2006.zhihu.theme.UiStyle
import top.yukonga.miuix.kmp.theme.MiuixTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppAlertDialog(
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable (() -> Unit)? = null,
    icon: @Composable (() -> Unit)? = null,
    title: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    shape: Shape = AlertDialogDefaults.shape,
    containerColor: Color = AlertDialogDefaults.containerColor,
    iconContentColor: Color = AlertDialogDefaults.iconContentColor,
    titleContentColor: Color = AlertDialogDefaults.titleContentColor,
    textContentColor: Color = AlertDialogDefaults.textContentColor,
    tonalElevation: Dp = AlertDialogDefaults.TonalElevation,
    properties: DialogProperties = DialogProperties(),
) {
    when (LocalUiStyle.current) {
        UiStyle.Material3 -> {
            androidx.compose.material3.AlertDialog(
                onDismissRequest = onDismissRequest,
                confirmButton = confirmButton,
                modifier = modifier,
                dismissButton = dismissButton,
                icon = icon,
                title = title,
                text = text,
                shape = shape,
                containerColor = containerColor,
                iconContentColor = iconContentColor,
                titleContentColor = titleContentColor,
                textContentColor = textContentColor,
                tonalElevation = tonalElevation,
                properties = properties,
            )
        }
        UiStyle.Miuix -> {
            BasicAlertDialog(onDismissRequest = onDismissRequest, modifier = modifier, properties = properties) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = MiuixTheme.colorScheme.surfaceContainer,
                    tonalElevation = 0.dp,
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        icon?.let {
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                CompositionLocalProvider(LocalContentColor provides MiuixTheme.colorScheme.primary, content = it)
                            }
                        }
                        title?.let {
                            CompositionLocalProvider(LocalContentColor provides MiuixTheme.colorScheme.onSurface) {
                                ProvideTextStyle(MaterialTheme.typography.titleLarge) {
                                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { it() }
                                }
                            }
                        }
                        text?.let {
                            Box(modifier = Modifier.weight(1f, fill = false)) {
                                CompositionLocalProvider(LocalContentColor provides MiuixTheme.colorScheme.onSurfaceSecondary) {
                                    ProvideTextStyle(MaterialTheme.typography.bodyMedium) { it() }
                                }
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            dismissButton?.invoke()
                            confirmButton()
                        }
                    }
                }
            }
        }
        UiStyle.LiquidGlass -> {
            BasicAlertDialog(onDismissRequest = onDismissRequest, modifier = modifier, properties = properties) {
                CompositionLocalProvider(LocalLiquidGlassOverlay provides true) {
                    Surface(
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.surfaceBright,
                        tonalElevation = 0.dp,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Column(
                                modifier = Modifier.weight(1f, fill = false).padding(24.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                icon?.let { CompositionLocalProvider(LocalContentColor provides iconContentColor, content = it) }
                                title?.let {
                                    CompositionLocalProvider(LocalContentColor provides titleContentColor) {
                                        ProvideTextStyle(MaterialTheme.typography.titleLarge.copy(textAlign = TextAlign.Center)) {
                                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { it() }
                                        }
                                    }
                                }
                                text?.let {
                                    CompositionLocalProvider(LocalContentColor provides textContentColor) {
                                        ProvideTextStyle(MaterialTheme.typography.bodyMedium) { it() }
                                    }
                                }
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center,
                                propagateMinConstraints = true,
                            ) { confirmButton() }
                            dismissButton?.let {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                                    contentAlignment = Alignment.Center,
                                    propagateMinConstraints = true,
                                ) { it() }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    scrollState: ScrollState = rememberScrollState(),
    properties: PopupProperties = PopupProperties(focusable = true),
    shape: Shape = MenuDefaults.shape,
    containerColor: Color = MenuDefaults.containerColor,
    tonalElevation: Dp = MenuDefaults.TonalElevation,
    shadowElevation: Dp = MenuDefaults.ShadowElevation,
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val style = LocalUiStyle.current
    val resolvedShape = when (style) {
        UiStyle.Material3 -> shape
        UiStyle.Miuix -> RoundedCornerShape(16.dp)
        UiStyle.LiquidGlass -> RoundedCornerShape(18.dp)
    }
    val resolvedContainerColor = when (style) {
        UiStyle.Material3 -> containerColor
        UiStyle.Miuix -> MiuixTheme.colorScheme.surfaceContainer
        UiStyle.LiquidGlass -> MaterialTheme.colorScheme.surfaceBright
    }
    val resolvedBorder = when (style) {
        UiStyle.LiquidGlass -> border ?: BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
        else -> border
    }
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        offset = offset,
        scrollState = scrollState,
        properties = properties,
        shape = resolvedShape,
        containerColor = resolvedContainerColor,
        tonalElevation = if (style == UiStyle.Material3) tonalElevation else 0.dp,
        shadowElevation = shadowElevation,
        border = resolvedBorder,
        content = content,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExposedDropdownMenuBoxScope.AppExposedDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    matchAnchorWidth: Boolean = true,
    shape: Shape = MenuDefaults.shape,
    containerColor: Color = MenuDefaults.containerColor,
    tonalElevation: Dp = MenuDefaults.TonalElevation,
    shadowElevation: Dp = MenuDefaults.ShadowElevation,
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val style = LocalUiStyle.current
    val resolvedShape = when (style) {
        UiStyle.Material3 -> shape
        UiStyle.Miuix -> RoundedCornerShape(16.dp)
        UiStyle.LiquidGlass -> RoundedCornerShape(18.dp)
    }
    val resolvedContainerColor = when (style) {
        UiStyle.Material3 -> containerColor
        UiStyle.Miuix -> MiuixTheme.colorScheme.surfaceContainer
        UiStyle.LiquidGlass -> MaterialTheme.colorScheme.surfaceBright
    }
    val resolvedBorder = when (style) {
        UiStyle.LiquidGlass -> border ?: BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
        else -> border
    }
    ExposedDropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        scrollState = scrollState,
        matchAnchorWidth = matchAnchorWidth,
        shape = resolvedShape,
        containerColor = resolvedContainerColor,
        tonalElevation = if (style == UiStyle.Material3) tonalElevation else 0.dp,
        shadowElevation = shadowElevation,
        border = resolvedBorder,
        content = content,
    )
}

@Composable
fun AppDropdownMenuItem(
    text: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    colors: MenuItemColors = MenuDefaults.itemColors(),
    contentPadding: PaddingValues = MenuDefaults.DropdownMenuItemContentPadding,
    interactionSource: MutableInteractionSource? = null,
) {
    DropdownMenuItem(
        text = text,
        onClick = onClick,
        modifier = modifier,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        enabled = enabled,
        colors = colors,
        contentPadding = contentPadding,
        interactionSource = interactionSource,
    )
}
