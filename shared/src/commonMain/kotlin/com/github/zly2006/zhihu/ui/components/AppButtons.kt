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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonElevation
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.FloatingActionButtonElevation
import androidx.compose.material3.IconButtonColors
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.theme.LocalUiStyle
import com.github.zly2006.zhihu.theme.UiStyle
import com.github.zly2006.zhihu.theme.isLiquidGlassSupported
import com.github.zly2006.zhihu.theme.liquidGlassSemanticTokens
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.basic.Button as MiuixButton
import top.yukonga.miuix.kmp.basic.ButtonDefaults as MiuixButtonDefaults
import top.yukonga.miuix.kmp.basic.FloatingActionButton as MiuixFloatingActionButton
import top.yukonga.miuix.kmp.basic.IconButton as MiuixIconButton

@Composable
fun AppButton(
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
    when (LocalUiStyle.current) {
        UiStyle.Material3 -> {
            androidx.compose.material3.Button(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled,
                shape = shape,
                colors = colors,
                elevation = elevation,
                border = border,
                contentPadding = contentPadding,
                interactionSource = interactionSource,
                content = content,
            )
        }
        UiStyle.Miuix -> {
            val defaultM3 = ButtonDefaults.buttonColors()
            val usesCustomColors = colors != defaultM3
            val miuixColors = if (usesCustomColors) {
                MiuixButtonDefaults.buttonColors(
                    color = colors.containerColor,
                    disabledColor = colors.disabledContainerColor,
                )
            } else {
                MiuixButtonDefaults.buttonColorsPrimary()
            }
            val resolvedContentColor = if (usesCustomColors) {
                if (enabled) colors.contentColor else colors.disabledContentColor
            } else {
                if (enabled) {
                    MiuixTheme.colorScheme.onPrimary
                } else {
                    MiuixTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
                }
            }
            MiuixButton(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled,
                colors = miuixColors,
                insideMargin = contentPadding,
                content = {
                    CompositionLocalProvider(LocalContentColor provides resolvedContentColor) {
                        content()
                    }
                },
            )
        }
        UiStyle.LiquidGlass -> {
            LiquidButton(
                onClick = onClick,
                modifier = modifier.defaultMinSize(minHeight = 48.dp),
                enabled = enabled,
                containerColor = if (enabled) colors.containerColor else colors.disabledContainerColor,
                contentColor = if (enabled) colors.contentColor else colors.disabledContentColor,
                contentPadding = contentPadding,
                content = content,
            )
        }
    }
}

@Composable
fun AppFilledTonalButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.filledTonalShape,
    colors: ButtonColors = ButtonDefaults.filledTonalButtonColors(),
    elevation: ButtonElevation? = ButtonDefaults.filledTonalButtonElevation(),
    border: BorderStroke? = null,
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    when (LocalUiStyle.current) {
        UiStyle.Material3 -> {
            androidx.compose.material3.FilledTonalButton(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled,
                shape = shape,
                colors = colors,
                elevation = elevation,
                border = border,
                contentPadding = contentPadding,
                interactionSource = interactionSource,
                content = content,
            )
        }
        UiStyle.Miuix -> {
            val defaultTonal = ButtonDefaults.filledTonalButtonColors()
            val usesCustomColors = colors != defaultTonal
            val miuixColors = if (usesCustomColors) {
                MiuixButtonDefaults.buttonColors(
                    color = colors.containerColor,
                    disabledColor = colors.disabledContainerColor,
                )
            } else {
                MiuixButtonDefaults.buttonColors()
            }
            val resolvedContent = if (usesCustomColors) {
                if (enabled) colors.contentColor else colors.disabledContentColor
            } else {
                if (enabled) {
                    MiuixTheme.colorScheme.onSecondaryVariant
                } else {
                    MiuixTheme.colorScheme.onSecondaryVariant.copy(alpha = 0.5f)
                }
            }
            MiuixButton(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled,
                colors = miuixColors,
                insideMargin = contentPadding,
                content = {
                    CompositionLocalProvider(LocalContentColor provides resolvedContent) {
                        content()
                    }
                },
            )
        }
        UiStyle.LiquidGlass -> {
            if (isLiquidGlassSupported) {
                LiquidButton(
                    onClick = onClick,
                    modifier = modifier.defaultMinSize(minHeight = 44.dp),
                    enabled = enabled,
                    containerColor = if (enabled) colors.containerColor else colors.disabledContainerColor,
                    contentColor = if (enabled) colors.contentColor else colors.disabledContentColor,
                    contentPadding = contentPadding,
                    content = content,
                )
            } else {
                AppleSurfaceButton(
                    onClick = onClick,
                    modifier = modifier,
                    enabled = enabled,
                    shape = shape,
                    colors = colors,
                    border = border,
                    contentPadding = contentPadding,
                    interactionSource = interactionSource,
                    content = content,
                )
            }
        }
    }
}

@Composable
fun AppOutlinedButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = ButtonDefaults.outlinedShape,
    colors: ButtonColors = ButtonDefaults.outlinedButtonColors(),
    elevation: ButtonElevation? = null,
    border: BorderStroke? = ButtonDefaults.outlinedButtonBorder(enabled),
    contentPadding: PaddingValues = ButtonDefaults.ContentPadding,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable RowScope.() -> Unit,
) {
    when (LocalUiStyle.current) {
        UiStyle.Material3 -> {
            androidx.compose.material3.OutlinedButton(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled,
                shape = shape,
                colors = colors,
                elevation = elevation,
                border = border,
                contentPadding = contentPadding,
                interactionSource = interactionSource,
                content = content,
            )
        }
        UiStyle.Miuix -> {
            val containerColor = if (!enabled) {
                if (colors.disabledContainerColor != Color.Transparent) {
                    colors.disabledContainerColor
                } else {
                    MiuixTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)
                }
            } else if (colors.containerColor != Color.Transparent) {
                colors.containerColor
            } else {
                MiuixTheme.colorScheme.secondaryContainer
            }
            val contentColor = if (enabled) colors.contentColor else colors.disabledContentColor
            Surface(
                modifier = modifier
                    .semantics {
                        role = Role.Button
                        if (!enabled) disabled()
                    }.clickable(
                        interactionSource = interactionSource ?: remember { MutableInteractionSource() },
                        indication = null,
                        enabled = enabled,
                        role = Role.Button,
                        onClick = onClick,
                    ),
                shape = RoundedCornerShape(14.dp),
                color = containerColor,
                contentColor = contentColor,
                border = border ?: BorderStroke(0.5.dp, MiuixTheme.colorScheme.outline),
            ) {
                CompositionLocalProvider(LocalContentColor provides contentColor) {
                    ProvideTextStyle(MaterialTheme.typography.labelLarge) {
                        Row(
                            modifier = Modifier
                                .defaultMinSize(minWidth = 48.dp, minHeight = 40.dp)
                                .padding(contentPadding),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            content = content,
                        )
                    }
                }
            }
        }
        UiStyle.LiquidGlass -> {
            if (isLiquidGlassSupported) {
                LiquidButton(
                    onClick = onClick,
                    modifier = modifier.defaultMinSize(minHeight = 44.dp),
                    enabled = enabled,
                    containerColor = if (enabled) colors.containerColor else colors.disabledContainerColor,
                    contentColor = if (enabled) colors.contentColor else colors.disabledContentColor,
                    contentPadding = contentPadding,
                    content = content,
                )
            } else {
                AppleSurfaceButton(
                    onClick = onClick,
                    modifier = modifier,
                    enabled = enabled,
                    shape = shape,
                    colors = colors,
                    border = border ?: BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
                    contentPadding = contentPadding,
                    interactionSource = interactionSource,
                    content = content,
                )
            }
        }
    }
}

@Composable
fun AppTextButton(
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
    when (LocalUiStyle.current) {
        UiStyle.Material3 -> {
            androidx.compose.material3.TextButton(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled,
                shape = shape,
                colors = colors,
                elevation = elevation,
                border = border,
                contentPadding = contentPadding,
                interactionSource = interactionSource,
                content = content,
            )
        }
        UiStyle.Miuix -> {
            val contentColor = if (enabled) colors.contentColor else colors.disabledContentColor
            Surface(
                modifier = modifier
                    .semantics {
                        role = Role.Button
                        if (!enabled) disabled()
                    }.clickable(
                        interactionSource = interactionSource ?: remember { MutableInteractionSource() },
                        indication = null,
                        enabled = enabled,
                        role = Role.Button,
                        onClick = onClick,
                    ),
                shape = RoundedCornerShape(12.dp),
                color = colors.containerColor,
                contentColor = contentColor,
                border = border,
            ) {
                CompositionLocalProvider(LocalContentColor provides contentColor) {
                    ProvideTextStyle(MaterialTheme.typography.labelLarge) {
                        Row(
                            modifier = Modifier
                                .defaultMinSize(minWidth = 44.dp, minHeight = 40.dp)
                                .padding(contentPadding),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                            content = content,
                        )
                    }
                }
            }
        }
        UiStyle.LiquidGlass -> {
            if (isLiquidGlassSupported) {
                LiquidButton(
                    onClick = onClick,
                    modifier = modifier.defaultMinSize(minHeight = 44.dp),
                    enabled = enabled,
                    containerColor = if (enabled) colors.containerColor else colors.disabledContainerColor,
                    contentColor = if (enabled) colors.contentColor else colors.disabledContentColor,
                    contentPadding = contentPadding,
                    content = content,
                )
            } else {
                AppleSurfaceButton(
                    onClick = onClick,
                    modifier = modifier,
                    enabled = enabled,
                    shape = shape,
                    colors = colors,
                    border = border,
                    contentPadding = contentPadding,
                    interactionSource = interactionSource,
                    content = content,
                )
            }
        }
    }
}

enum class AppIconButtonStyle { Inline, Floating }

val LocalAppIconButtonStyle = staticCompositionLocalOf { AppIconButtonStyle.Inline }

@Composable
fun AppIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: IconButtonColors = if (LocalUiStyle.current == UiStyle.LiquidGlass) {
        IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary)
    } else {
        IconButtonDefaults.iconButtonColors()
    },
    interactionSource: MutableInteractionSource? = null,
    style: AppIconButtonStyle? = null,
    content: @Composable () -> Unit,
) {
    when (LocalUiStyle.current) {
        UiStyle.Material3 -> {
            androidx.compose.material3.IconButton(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled,
                colors = colors,
                interactionSource = interactionSource,
                content = content,
            )
        }
        UiStyle.Miuix -> {
            val containerColor = if (enabled) colors.containerColor else colors.disabledContainerColor
            val hasVisibleContainer = containerColor != Color.Unspecified && containerColor.alpha > 0f
            val containerModifier = if (hasVisibleContainer) {
                modifier.background(containerColor, CircleShape)
            } else {
                modifier
            }
            MiuixIconButton(
                onClick = onClick,
                modifier = containerModifier,
                enabled = enabled,
                content = {
                    val tint = if (enabled) colors.contentColor else colors.disabledContentColor
                    CompositionLocalProvider(LocalContentColor provides tint, content = content)
                },
            )
        }
        UiStyle.LiquidGlass -> {
            val resolvedStyle = style ?: LocalAppIconButtonStyle.current
            val source = interactionSource ?: remember { MutableInteractionSource() }
            val containerColor = if (enabled) colors.containerColor else colors.disabledContainerColor
            val contentColor = if (enabled) colors.contentColor else colors.disabledContentColor
            if (resolvedStyle == AppIconButtonStyle.Floating && isLiquidGlassSupported) {
                val chromeBase = liquidGlassSemanticTokens().chromeTint
                LiquidButton(
                    onClick = onClick,
                    modifier = modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                    enabled = enabled,
                    // LiquidButton 内部会按 0.55 缩放容器 alpha，这里反推回 chromeTint 的目标透明度避免双重衰减。
                    containerColor = chromeBase.copy(alpha = (chromeBase.alpha / 0.55f).coerceIn(0f, 1f)),
                    contentColor = contentColor,
                    contentPadding = PaddingValues(12.dp),
                ) {
                    content()
                }
                return
            }
            val hasVisibleContainer = containerColor != Color.Unspecified && containerColor.alpha > 0f
            val pressed by source.collectIsPressedAsState()
            Box(
                modifier = modifier
                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                    .then(
                        if (hasVisibleContainer) {
                            Modifier.padding(4.dp).background(containerColor, CircleShape)
                        } else {
                            Modifier
                        },
                    ).alpha(
                        if (enabled) {
                            if (pressed) 0.62f else 1f
                        } else {
                            0.38f
                        },
                    ).semantics {
                        role = Role.Button
                        if (!enabled) disabled()
                    }.clickable(
                        interactionSource = source,
                        indication = null,
                        enabled = enabled,
                        role = Role.Button,
                        onClick = onClick,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                CompositionLocalProvider(LocalContentColor provides contentColor, content = content)
            }
        }
    }
}

@Composable
fun AppFilledTonalIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = IconButtonDefaults.filledShape,
    colors: IconButtonColors = IconButtonDefaults.filledTonalIconButtonColors(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    when (LocalUiStyle.current) {
        UiStyle.Material3 -> {
            androidx.compose.material3.FilledTonalIconButton(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled,
                shape = shape,
                colors = colors,
                interactionSource = interactionSource,
                content = content,
            )
        }
        UiStyle.Miuix -> {
            val containerColor = if (enabled) colors.containerColor else colors.disabledContainerColor
            val contentColor = if (enabled) colors.contentColor else colors.disabledContentColor
            Surface(
                modifier = modifier
                    .defaultMinSize(minWidth = 40.dp, minHeight = 40.dp)
                    .semantics {
                        role = Role.Button
                        if (!enabled) disabled()
                    }.clickable(
                        interactionSource = interactionSource ?: remember { MutableInteractionSource() },
                        indication = null,
                        enabled = enabled,
                        role = Role.Button,
                        onClick = onClick,
                    ),
                shape = RoundedCornerShape(14.dp),
                color = containerColor,
                contentColor = contentColor,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    CompositionLocalProvider(LocalContentColor provides contentColor, content = content)
                }
            }
        }
        UiStyle.LiquidGlass -> {
            val containerColor = if (enabled) colors.containerColor else colors.disabledContainerColor
            val contentColor = if (enabled) colors.contentColor else colors.disabledContentColor
            LiquidButton(
                onClick = onClick,
                modifier = modifier.defaultMinSize(minWidth = 40.dp, minHeight = 40.dp),
                enabled = enabled,
                containerColor = containerColor,
                contentColor = contentColor,
                contentPadding = PaddingValues(8.dp),
            ) {
                content()
            }
        }
    }
}

@Composable
fun AppFloatingActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = FloatingActionButtonDefaults.shape,
    containerColor: Color = FloatingActionButtonDefaults.containerColor,
    contentColor: Color = contentColorFor(containerColor),
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
    interactionSource: MutableInteractionSource? = null,
    content: @Composable () -> Unit,
) {
    when (LocalUiStyle.current) {
        UiStyle.Material3 -> {
            androidx.compose.material3.FloatingActionButton(
                onClick = onClick,
                modifier = modifier,
                shape = shape,
                containerColor = containerColor,
                contentColor = contentColor,
                elevation = elevation,
                interactionSource = interactionSource,
                content = content,
            )
        }
        UiStyle.Miuix -> {
            val defaultContainer = FloatingActionButtonDefaults.containerColor
            val isDefaultBaseColor = containerColor.copy(alpha = 1f) == defaultContainer.copy(alpha = 1f)
            val resolvedContainer = if (!isDefaultBaseColor) {
                containerColor
            } else {
                MiuixTheme.colorScheme.primary.copy(alpha = containerColor.alpha)
            }
            val resolvedContent = if (!isDefaultBaseColor) {
                contentColor
            } else {
                MiuixTheme.colorScheme.onPrimary.copy(alpha = contentColor.alpha)
            }
            MiuixFloatingActionButton(
                onClick = onClick,
                modifier = modifier,
                containerColor = resolvedContainer,
                content = {
                    CompositionLocalProvider(LocalContentColor provides resolvedContent, content = content)
                },
            )
        }
        UiStyle.LiquidGlass -> {
            LiquidButton(
                onClick = onClick,
                modifier = modifier.defaultMinSize(minWidth = 56.dp, minHeight = 56.dp),
                enabled = true,
                containerColor = containerColor,
                contentColor = contentColor,
                contentPadding = PaddingValues(16.dp),
            ) {
                content()
            }
        }
    }
}

@Composable
fun AppExtendedFloatingActionButton(
    text: @Composable () -> Unit,
    icon: @Composable () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    expanded: Boolean = true,
    shape: Shape = FloatingActionButtonDefaults.extendedFabShape,
    containerColor: Color = FloatingActionButtonDefaults.containerColor,
    contentColor: Color = contentColorFor(containerColor),
    elevation: FloatingActionButtonElevation = FloatingActionButtonDefaults.elevation(),
    interactionSource: MutableInteractionSource? = null,
) {
    when (LocalUiStyle.current) {
        UiStyle.Material3 -> {
            androidx.compose.material3.ExtendedFloatingActionButton(
                text = text,
                icon = icon,
                onClick = onClick,
                modifier = modifier,
                expanded = expanded,
                shape = shape,
                containerColor = containerColor,
                contentColor = contentColor,
                elevation = elevation,
                interactionSource = interactionSource,
            )
        }
        UiStyle.Miuix -> {
            val defaultContainer = FloatingActionButtonDefaults.containerColor
            val isDefaultBaseColor = containerColor.copy(alpha = 1f) == defaultContainer.copy(alpha = 1f)
            val resolvedColors = if (!isDefaultBaseColor) {
                MiuixButtonDefaults.buttonColors(color = containerColor)
            } else {
                MiuixButtonDefaults.buttonColorsPrimary()
            }
            val resolvedContent = if (!isDefaultBaseColor) {
                contentColor
            } else {
                MiuixTheme.colorScheme.onPrimary.copy(alpha = contentColor.alpha)
            }
            MiuixButton(
                onClick = onClick,
                modifier = modifier,
                colors = resolvedColors,
                insideMargin = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            ) {
                CompositionLocalProvider(LocalContentColor provides resolvedContent) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        icon()
                        if (expanded) text()
                    }
                }
            }
        }
        UiStyle.LiquidGlass -> {
            LiquidButton(
                onClick = onClick,
                modifier = modifier.defaultMinSize(minHeight = 52.dp),
                enabled = true,
                containerColor = containerColor,
                contentColor = contentColor,
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    icon()
                    if (expanded) text()
                }
            }
        }
    }
}

@Composable
fun AppIconToggleButton(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: androidx.compose.material3.IconToggleButtonColors = IconButtonDefaults.iconToggleButtonColors(),
    content: @Composable () -> Unit,
) {
    when (LocalUiStyle.current) {
        UiStyle.Material3 -> {
            androidx.compose.material3.IconToggleButton(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = modifier,
                enabled = enabled,
                colors = colors,
                content = content,
            )
        }
        UiStyle.Miuix, UiStyle.LiquidGlass -> {
            val containerColor = when {
                !enabled -> colors.disabledContainerColor
                checked -> colors.checkedContainerColor
                else -> colors.containerColor
            }
            val contentColor = when {
                !enabled -> colors.disabledContentColor
                checked -> if (colors.checkedContentColor != Color.Unspecified) {
                    colors.checkedContentColor
                } else {
                    MaterialTheme.colorScheme.primary
                }
                else -> if (colors.contentColor != Color.Unspecified) {
                    colors.contentColor
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            }
            val hasVisibleContainer = containerColor != Color.Unspecified && containerColor.alpha > 0f
            if (LocalUiStyle.current == UiStyle.LiquidGlass && isLiquidGlassSupported) {
                val resolvedContainer = when {
                    hasVisibleContainer -> containerColor
                    checked -> MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
                    else -> Color.Transparent
                }
                LiquidButton(
                    onClick = { onCheckedChange(!checked) },
                    modifier = modifier
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .semantics {
                            role = Role.Checkbox
                            toggleableState = ToggleableState(checked)
                        },
                    enabled = enabled,
                    containerColor = resolvedContainer,
                    contentColor = contentColor,
                    contentPadding = PaddingValues(8.dp),
                ) {
                    content()
                }
            } else {
                Box(
                    modifier = modifier
                        .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                        .then(
                            if (hasVisibleContainer) {
                                Modifier.padding(4.dp).background(containerColor, CircleShape)
                            } else {
                                Modifier
                            },
                        ).toggleable(
                            value = checked,
                            enabled = enabled,
                            role = Role.Checkbox,
                            onValueChange = onCheckedChange,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    CompositionLocalProvider(LocalContentColor provides contentColor, content = content)
                }
            }
        }
    }
}

@Composable
private fun AppleSurfaceButton(
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    shape: Shape,
    colors: ButtonColors,
    border: BorderStroke?,
    contentPadding: PaddingValues,
    interactionSource: MutableInteractionSource?,
    content: @Composable RowScope.() -> Unit,
) {
    val containerColor = if (enabled) colors.containerColor else colors.disabledContainerColor
    val contentColor = if (enabled) colors.contentColor else colors.disabledContentColor
    Surface(
        modifier = modifier
            .semantics {
                role = Role.Button
                if (!enabled) disabled()
            }.clickable(
                interactionSource = interactionSource ?: remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            ),
        shape = shape,
        color = containerColor,
        contentColor = contentColor,
        border = border,
    ) {
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            ProvideTextStyle(MaterialTheme.typography.labelLarge) {
                Row(
                    modifier = Modifier
                        .defaultMinSize(minWidth = 48.dp, minHeight = 44.dp)
                        .padding(contentPadding),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    content = content,
                )
            }
        }
    }
}
