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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CardElevation
import androidx.compose.material3.CheckboxColors
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.RadioButtonColors
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SliderColors
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.theme.LocalUiStyle
import com.github.zly2006.zhihu.theme.UiStyle
import com.github.zly2006.zhihu.theme.liquidGlassSemanticTokens
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.roundToInt
import top.yukonga.miuix.kmp.basic.Card as MiuixCard
import top.yukonga.miuix.kmp.basic.CardDefaults as MiuixCardDefaults
import top.yukonga.miuix.kmp.basic.Checkbox as MiuixCheckbox
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator as MiuixCircularProgressIndicator
import top.yukonga.miuix.kmp.basic.LinearProgressIndicator as MiuixLinearProgressIndicator
import top.yukonga.miuix.kmp.basic.Slider as MiuixSlider
import top.yukonga.miuix.kmp.basic.Switch as MiuixSwitch
import top.yukonga.miuix.kmp.basic.TextField as MiuixTextField

@Composable
fun AppSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    thumbContent: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    colors: SwitchColors = SwitchDefaults.colors(),
    interactionSource: MutableInteractionSource? = null,
) {
    when (LocalUiStyle.current) {
        UiStyle.Material3 -> {
            androidx.compose.material3.Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = modifier,
                thumbContent = thumbContent,
                enabled = enabled,
                colors = colors,
                interactionSource = interactionSource,
            )
        }
        UiStyle.Miuix -> {
            MiuixSwitch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = modifier,
                enabled = enabled,
            )
        }
        UiStyle.LiquidGlass -> {
            LiquidSwitch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = modifier,
                enabled = enabled,
            )
        }
    }
}

@Composable
fun AppCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: CheckboxColors = CheckboxDefaults.colors(),
    interactionSource: MutableInteractionSource? = null,
) {
    when (LocalUiStyle.current) {
        UiStyle.Material3 -> {
            androidx.compose.material3.Checkbox(
                checked = checked,
                onCheckedChange = onCheckedChange,
                modifier = modifier,
                enabled = enabled,
                colors = colors,
                interactionSource = interactionSource,
            )
        }
        UiStyle.Miuix -> {
            MiuixCheckbox(
                state = if (checked) ToggleableState.On else ToggleableState.Off,
                onClick = onCheckedChange?.let { cb -> { cb(!checked) } },
                modifier = modifier,
                enabled = enabled,
            )
        }
        UiStyle.LiquidGlass -> {
            val clickableModifier = if (onCheckedChange != null) {
                Modifier.clickable(
                    interactionSource = interactionSource ?: remember { MutableInteractionSource() },
                    indication = null,
                    enabled = enabled,
                    role = Role.Checkbox,
                    onClick = { onCheckedChange(!checked) },
                )
            } else {
                Modifier
            }
            Box(
                modifier = modifier
                    .defaultMinSize(minWidth = 40.dp, minHeight = 40.dp)
                    .alpha(if (enabled) 1f else 0.45f)
                    .semantics {
                        role = Role.Checkbox
                        toggleableState = if (checked) ToggleableState.On else ToggleableState.Off
                        if (!enabled) disabled()
                    }.then(clickableModifier),
                contentAlignment = Alignment.Center,
            ) {
                Surface(
                    modifier = Modifier.size(22.dp),
                    shape = CircleShape,
                    color = if (checked) MaterialTheme.colorScheme.primary else Color.Transparent,
                    border = BorderStroke(
                        1.5.dp,
                        if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    ),
                ) {
                    if (checked) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppRadioButton(
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: RadioButtonColors = RadioButtonDefaults.colors(),
    interactionSource: MutableInteractionSource? = null,
) {
    when (LocalUiStyle.current) {
        UiStyle.Material3 -> {
            androidx.compose.material3.RadioButton(
                selected = selected,
                onClick = onClick,
                modifier = modifier,
                enabled = enabled,
                colors = colors,
                interactionSource = interactionSource,
            )
        }
        UiStyle.Miuix -> {
            MiuixCheckbox(
                state = if (selected) ToggleableState.On else ToggleableState.Off,
                onClick = onClick,
                modifier = modifier,
                enabled = enabled,
            )
        }
        UiStyle.LiquidGlass -> {
            val clickableModifier = if (onClick != null) {
                Modifier.clickable(
                    interactionSource = interactionSource ?: remember { MutableInteractionSource() },
                    indication = null,
                    enabled = enabled,
                    role = Role.RadioButton,
                    onClick = onClick,
                )
            } else {
                Modifier
            }
            Box(
                modifier = modifier
                    .defaultMinSize(minWidth = 40.dp, minHeight = 40.dp)
                    .alpha(if (enabled) 1f else 0.45f)
                    .semantics {
                        role = Role.RadioButton
                        this.selected = selected
                        if (!enabled) disabled()
                    }.then(clickableModifier),
                contentAlignment = Alignment.Center,
            ) {
                if (selected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun AppSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    colors: SliderColors = SliderDefaults.colors(),
    interactionSource: MutableInteractionSource? = null,
) {
    when (LocalUiStyle.current) {
        UiStyle.Material3 -> {
            if (interactionSource != null) {
                androidx.compose.material3.Slider(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = modifier,
                    enabled = enabled,
                    valueRange = valueRange,
                    steps = steps,
                    onValueChangeFinished = onValueChangeFinished,
                    colors = colors,
                    interactionSource = interactionSource,
                )
            } else {
                androidx.compose.material3.Slider(
                    value = value,
                    onValueChange = onValueChange,
                    modifier = modifier,
                    enabled = enabled,
                    valueRange = valueRange,
                    steps = steps,
                    onValueChangeFinished = onValueChangeFinished,
                    colors = colors,
                )
            }
        }
        UiStyle.Miuix -> {
            val snappedOnValueChange: (Float) -> Unit = if (steps > 0 && valueRange.endInclusive > valueRange.start) {
                { raw ->
                    val bounded = raw.coerceIn(valueRange)
                    val increment = (valueRange.endInclusive - valueRange.start) / (steps + 1)
                    val snapped = valueRange.start + ((bounded - valueRange.start) / increment).roundToInt() * increment
                    onValueChange(snapped.coerceIn(valueRange))
                }
            } else {
                onValueChange
            }
            MiuixSlider(
                value = value,
                onValueChange = snappedOnValueChange,
                modifier = modifier,
                enabled = enabled,
                valueRange = valueRange,
                onValueChangeFinished = onValueChangeFinished,
            )
        }
        UiStyle.LiquidGlass -> {
            LiquidSlider(
                value = value,
                onValueChange = onValueChange,
                modifier = modifier,
                enabled = enabled,
                valueRange = valueRange,
                steps = steps,
                onValueChangeFinished = onValueChangeFinished,
            )
        }
    }
}

@Composable
fun AppCircularProgressIndicator(
    modifier: Modifier = Modifier,
    color: Color = ProgressIndicatorDefaults.circularColor,
    strokeWidth: Dp = ProgressIndicatorDefaults.CircularStrokeWidth,
    trackColor: Color = ProgressIndicatorDefaults.circularIndeterminateTrackColor,
    strokeCap: StrokeCap = ProgressIndicatorDefaults.CircularIndeterminateStrokeCap,
) {
    when (LocalUiStyle.current) {
        UiStyle.Miuix -> {
            MiuixCircularProgressIndicator(modifier = modifier)
        }
        UiStyle.LiquidGlass -> {
            val resolvedStroke = if (strokeWidth == ProgressIndicatorDefaults.CircularStrokeWidth) 2.5.dp else strokeWidth
            androidx.compose.material3.CircularProgressIndicator(
                modifier = modifier.defaultMinSize(minWidth = 28.dp, minHeight = 28.dp),
                color = color,
                strokeWidth = resolvedStroke,
                trackColor = trackColor,
                strokeCap = StrokeCap.Round,
            )
        }
        UiStyle.Material3 -> {
            androidx.compose.material3.CircularProgressIndicator(
                modifier = modifier,
                color = color,
                strokeWidth = strokeWidth,
                trackColor = trackColor,
                strokeCap = strokeCap,
            )
        }
    }
}

@Composable
fun AppLinearProgressIndicator(
    modifier: Modifier = Modifier,
    progress: (() -> Float)? = null,
    color: Color = ProgressIndicatorDefaults.linearColor,
    trackColor: Color = ProgressIndicatorDefaults.linearTrackColor,
) {
    when (LocalUiStyle.current) {
        UiStyle.Miuix -> {
            MiuixLinearProgressIndicator(
                progress = progress?.invoke(),
                modifier = modifier,
            )
        }
        UiStyle.LiquidGlass -> {
            if (progress != null) {
                LinearProgressIndicator(
                    progress = progress,
                    modifier = modifier,
                    color = color,
                    trackColor = trackColor,
                    strokeCap = StrokeCap.Round,
                )
            } else {
                LinearProgressIndicator(
                    modifier = modifier,
                    color = color,
                    trackColor = trackColor,
                    strokeCap = StrokeCap.Round,
                )
            }
        }
        UiStyle.Material3 -> {
            if (progress != null) {
                LinearProgressIndicator(
                    progress = progress,
                    modifier = modifier,
                    color = color,
                    trackColor = trackColor,
                )
            } else {
                LinearProgressIndicator(
                    modifier = modifier,
                    color = color,
                    trackColor = trackColor,
                )
            }
        }
    }
}

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    shape: Shape = CardDefaults.shape,
    colors: CardColors = CardDefaults.cardColors(),
    elevation: CardElevation = CardDefaults.cardElevation(),
    border: BorderStroke? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    when (LocalUiStyle.current) {
        UiStyle.Material3 -> {
            Card(
                modifier = modifier,
                shape = shape,
                colors = colors,
                elevation = elevation,
                border = border,
                content = content,
            )
        }
        UiStyle.Miuix -> {
            val defaultColors = CardDefaults.cardColors()
            val containerColor = if (colors.containerColor != defaultColors.containerColor &&
                colors.containerColor != MaterialTheme.colorScheme.surface
            ) {
                colors.containerColor
            } else {
                MiuixTheme.colorScheme.surfaceContainer
            }
            val resolvedContentColor = if (colors.contentColor != defaultColors.contentColor) {
                colors.contentColor
            } else {
                MiuixTheme.colorScheme.onSurface
            }
            MiuixCard(
                modifier = modifier,
                cornerRadius = 16.dp,
                colors = MiuixCardDefaults.defaultColors(
                    color = containerColor,
                    contentColor = resolvedContentColor,
                ),
                content = content,
            )
        }
        UiStyle.LiquidGlass -> {
            val tokens = liquidGlassSemanticTokens()
            val defaultColors = CardDefaults.cardColors()
            val containerColor = if (colors.containerColor != defaultColors.containerColor &&
                colors.containerColor != MaterialTheme.colorScheme.surface
            ) {
                colors.containerColor
            } else {
                tokens.contentSurface
            }
            Surface(
                modifier = modifier,
                shape = RoundedCornerShape(tokens.groupedCornerRadius),
                color = containerColor,
                contentColor = colors.contentColor,
                border = border,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
            ) {
                Column(content = content)
            }
        }
    }
}

@Composable
fun AppCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    shape: Shape = CardDefaults.shape,
    colors: CardColors = CardDefaults.cardColors(),
    elevation: CardElevation = CardDefaults.cardElevation(),
    border: BorderStroke? = null,
    interactionSource: MutableInteractionSource? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    when (LocalUiStyle.current) {
        UiStyle.Material3 -> {
            Card(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled,
                shape = shape,
                colors = colors,
                elevation = elevation,
                border = border,
                interactionSource = interactionSource,
                content = content,
            )
        }
        UiStyle.Miuix -> {
            val defaultColors = CardDefaults.cardColors()
            val containerColor = if (colors.containerColor != defaultColors.containerColor &&
                colors.containerColor != MaterialTheme.colorScheme.surface
            ) {
                colors.containerColor
            } else {
                MiuixTheme.colorScheme.surfaceContainer
            }
            val resolvedContentColor = if (colors.contentColor != defaultColors.contentColor) {
                colors.contentColor
            } else {
                MiuixTheme.colorScheme.onSurface
            }
            MiuixCard(
                modifier = modifier,
                cornerRadius = 16.dp,
                colors = MiuixCardDefaults.defaultColors(
                    color = containerColor,
                    contentColor = resolvedContentColor,
                ),
                onClick = if (enabled) onClick else null,
                content = content,
            )
        }
        UiStyle.LiquidGlass -> {
            val tokens = liquidGlassSemanticTokens()
            val defaultColors = CardDefaults.cardColors()
            val containerColor = if (colors.containerColor != defaultColors.containerColor &&
                colors.containerColor != MaterialTheme.colorScheme.surface
            ) {
                colors.containerColor
            } else {
                tokens.contentSurface
            }
            Surface(
                onClick = onClick,
                modifier = modifier,
                enabled = enabled,
                shape = RoundedCornerShape(tokens.groupedCornerRadius),
                color = containerColor,
                contentColor = colors.contentColor,
                border = border,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                interactionSource = interactionSource,
            ) {
                Column(content = content)
            }
        }
    }
}

@Composable
fun AppOutlinedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    shape: Shape = OutlinedTextFieldDefaults.shape,
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(),
) {
    when (LocalUiStyle.current) {
        UiStyle.Material3 -> {
            androidx.compose.material3.OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = modifier,
                enabled = enabled,
                readOnly = readOnly,
                textStyle = textStyle,
                label = label,
                placeholder = placeholder,
                leadingIcon = leadingIcon,
                trailingIcon = trailingIcon,
                supportingText = supportingText,
                isError = isError,
                visualTransformation = visualTransformation,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                singleLine = singleLine,
                maxLines = maxLines,
                minLines = minLines,
                shape = shape,
                colors = colors,
            )
        }
        UiStyle.Miuix -> {
            StyledBasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = modifier,
                enabled = enabled,
                readOnly = readOnly,
                textStyle = textStyle,
                label = label,
                placeholder = placeholder,
                leadingIcon = leadingIcon,
                trailingIcon = trailingIcon,
                supportingText = supportingText,
                isError = isError,
                visualTransformation = visualTransformation,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                singleLine = singleLine,
                maxLines = maxLines,
                minLines = minLines,
                containerColor = MiuixTheme.colorScheme.secondaryContainer,
                fieldShape = RoundedCornerShape(16.dp),
            )
        }
        UiStyle.LiquidGlass -> {
            StyledBasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = modifier,
                enabled = enabled,
                readOnly = readOnly,
                textStyle = textStyle,
                label = label,
                placeholder = placeholder,
                leadingIcon = leadingIcon,
                trailingIcon = trailingIcon,
                supportingText = supportingText,
                isError = isError,
                visualTransformation = visualTransformation,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                singleLine = singleLine,
                maxLines = maxLines,
                minLines = minLines,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                fieldShape = RoundedCornerShape(14.dp),
            )
        }
    }
}

@Composable
fun AppOutlinedTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    shape: Shape = OutlinedTextFieldDefaults.shape,
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(),
) {
    when (LocalUiStyle.current) {
        UiStyle.Material3 -> {
            androidx.compose.material3.OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = modifier,
                enabled = enabled,
                readOnly = readOnly,
                textStyle = textStyle,
                label = label,
                placeholder = placeholder,
                leadingIcon = leadingIcon,
                trailingIcon = trailingIcon,
                supportingText = supportingText,
                isError = isError,
                visualTransformation = visualTransformation,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                singleLine = singleLine,
                maxLines = maxLines,
                minLines = minLines,
                shape = shape,
                colors = colors,
            )
        }
        UiStyle.Miuix, UiStyle.LiquidGlass -> {
            val isMiuix = LocalUiStyle.current == UiStyle.Miuix
            val source = remember { MutableInteractionSource() }
            val focused by source.collectIsFocusedAsState()
            val resolvedStyle = textStyle.copy(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.45f),
            )
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = modifier.fillMaxWidth().semantics(mergeDescendants = true) {
                    if (isError) error("输入内容有误")
                },
                enabled = enabled,
                readOnly = readOnly,
                textStyle = resolvedStyle,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
                singleLine = singleLine,
                maxLines = maxLines,
                minLines = minLines,
                visualTransformation = visualTransformation,
                interactionSource = source,
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            ) { inner ->
                StyledTextFieldContainer(
                    enabled = enabled,
                    isError = isError,
                    focused = focused,
                    fieldShape = RoundedCornerShape(if (isMiuix) 16.dp else 14.dp),
                    containerColor = if (isMiuix) {
                        MiuixTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHighest
                    },
                    label = label,
                    supportingText = supportingText,
                ) {
                    StyledTextFieldContent(
                        isEmpty = value.text.isEmpty(),
                        placeholder = placeholder,
                        leadingIcon = leadingIcon,
                        trailingIcon = trailingIcon,
                        inner = inner,
                    )
                }
            }
        }
    }
}

@Composable
private fun StyledBasicTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    readOnly: Boolean,
    textStyle: TextStyle,
    label: (@Composable () -> Unit)?,
    placeholder: (@Composable () -> Unit)?,
    leadingIcon: (@Composable () -> Unit)?,
    trailingIcon: (@Composable () -> Unit)?,
    supportingText: (@Composable () -> Unit)?,
    isError: Boolean,
    visualTransformation: VisualTransformation,
    keyboardOptions: KeyboardOptions,
    keyboardActions: KeyboardActions,
    singleLine: Boolean,
    maxLines: Int,
    minLines: Int,
    containerColor: Color,
    fieldShape: Shape,
) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val resolvedStyle = textStyle.copy(
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (enabled) 1f else 0.45f),
    )
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth().semantics(mergeDescendants = true) {
            if (isError) error("输入内容有误")
        },
        enabled = enabled,
        readOnly = readOnly,
        textStyle = resolvedStyle,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
        visualTransformation = visualTransformation,
        interactionSource = source,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
    ) { inner ->
        StyledTextFieldContainer(
            enabled = enabled,
            isError = isError,
            focused = focused,
            fieldShape = fieldShape,
            containerColor = containerColor,
            label = label,
            supportingText = supportingText,
        ) {
            StyledTextFieldContent(
                isEmpty = value.isEmpty(),
                placeholder = placeholder,
                leadingIcon = leadingIcon,
                trailingIcon = trailingIcon,
                inner = inner,
            )
        }
    }
}

@Composable
private fun StyledTextFieldContainer(
    enabled: Boolean,
    isError: Boolean,
    focused: Boolean,
    fieldShape: Shape,
    containerColor: Color,
    label: (@Composable () -> Unit)?,
    supportingText: (@Composable () -> Unit)?,
    field: @Composable () -> Unit,
) {
    val borderColor = when {
        isError -> MaterialTheme.colorScheme.error
        focused -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        label?.let {
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant) {
                ProvideTextStyle(MaterialTheme.typography.labelMedium, content = it)
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth().defaultMinSize(minHeight = 48.dp),
            shape = fieldShape,
            color = containerColor.copy(alpha = if (enabled) 1f else 0.5f),
            border = BorderStroke(if (focused || isError) 1.5.dp else 0.5.dp, borderColor),
        ) {
            Box(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                field()
            }
        }
        supportingText?.let {
            CompositionLocalProvider(
                LocalContentColor provides if (isError) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            ) {
                ProvideTextStyle(MaterialTheme.typography.bodySmall, content = it)
            }
        }
    }
}

@Composable
private fun StyledTextFieldContent(
    isEmpty: Boolean,
    placeholder: (@Composable () -> Unit)?,
    leadingIcon: (@Composable () -> Unit)?,
    trailingIcon: (@Composable () -> Unit)?,
    inner: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        leadingIcon?.let {
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant, content = it)
        }
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (isEmpty && placeholder != null) {
                CompositionLocalProvider(
                    LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant,
                    content = placeholder,
                )
            }
            inner()
        }
        trailingIcon?.let {
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant, content = it)
        }
    }
}

/** 语义化单行/多行文本输入，登录与纯表单页可直接获得 Miuix 原生 TextField 体验。 */
@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    leadingText: String? = null,
) {
    when (LocalUiStyle.current) {
        UiStyle.Miuix -> {
            MiuixTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = modifier,
                enabled = enabled,
                label = leadingText?.let { "$label（$it）" } ?: label,
                useLabelAsPlaceholder = true,
                singleLine = singleLine,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
            )
        }
        UiStyle.Material3, UiStyle.LiquidGlass -> {
            AppOutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                modifier = modifier,
                enabled = enabled,
                label = { Text(label) },
                leadingIcon = leadingText?.let { { Text(it) } },
                singleLine = singleLine,
                keyboardOptions = keyboardOptions,
                keyboardActions = keyboardActions,
            )
        }
    }
}

@Composable
fun AppFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null,
) {
    when (LocalUiStyle.current) {
        UiStyle.Material3 -> {
            FilterChip(
                selected = selected,
                onClick = onClick,
                label = label,
                modifier = modifier,
                enabled = enabled,
                leadingIcon = leadingIcon,
            )
        }
        UiStyle.Miuix -> {
            val containerColor = if (selected) {
                MiuixTheme.colorScheme.primaryContainer
            } else {
                MiuixTheme.colorScheme.secondaryContainer
            }
            val contentColor = if (selected) {
                MiuixTheme.colorScheme.onPrimaryContainer
            } else {
                MiuixTheme.colorScheme.onSecondaryVariant
            }
            Surface(
                modifier = modifier
                    .then(if (!enabled) Modifier.alpha(0.38f) else Modifier)
                    .semantics {
                        this.selected = selected
                        if (!enabled) disabled()
                    }.clickable(enabled = enabled, role = Role.Checkbox, onClick = onClick),
                shape = RoundedCornerShape(12.dp),
                color = containerColor,
                contentColor = contentColor,
                border = if (selected) null else BorderStroke(0.5.dp, MiuixTheme.colorScheme.outline.copy(alpha = 0.5f)),
            ) {
                ProvideTextStyle(MaterialTheme.typography.labelLarge) {
                    Row(
                        modifier = Modifier.defaultMinSize(minHeight = 36.dp).padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        leadingIcon?.invoke()
                        label()
                    }
                }
            }
        }
        UiStyle.LiquidGlass -> {
            val tokens = liquidGlassSemanticTokens()
            val isOverlay = com.github.zly2006.zhihu.theme.LocalLiquidGlassOverlay.current
            val color = if (selected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else if (isOverlay) {
                tokens.segmentedContainer
            } else {
                tokens.contentSurface
            }
            val contentColor = if (selected) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            }
            Surface(
                modifier = modifier
                    .then(if (!enabled) Modifier.alpha(0.38f) else Modifier)
                    .semantics {
                        this.selected = selected
                        if (!enabled) disabled()
                    }.clickable(enabled = enabled, role = Role.Checkbox, onClick = onClick),
                shape = CircleShape,
                color = color,
                contentColor = contentColor,
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                ProvideTextStyle(MaterialTheme.typography.labelLarge) {
                    Row(
                        modifier = Modifier.defaultMinSize(minHeight = 36.dp).padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        leadingIcon?.invoke()
                        label()
                    }
                }
            }
        }
    }
}
