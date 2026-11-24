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

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.CardColors
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.theme.LocalUiStyle
import com.github.zly2006.zhihu.theme.UiStyle
import com.github.zly2006.zhihu.theme.liquidGlassSemanticTokens
import com.github.zly2006.zhihu.util.ProvideContentColorTextStyle
import kotlinx.coroutines.delay
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.basic.Card as MiuixCard
import top.yukonga.miuix.kmp.basic.CardDefaults as MiuixCardDefaults
import top.yukonga.miuix.kmp.basic.SmallTitle as MiuixSmallTitle

/**
 * 设置行的交互语义契约，决定三套风格各自的右侧附件、文字层级与危险色。
 */
enum class SettingRowKind {
    Plain,
    Navigation,
    Value,
    Action,
    Destructive,
    Selection,
}

/** 分组内分隔线的起止对齐风格：普通表单 vs 带图标行对齐。 */
enum class SettingGroupDividerStyle { Form, IconAligned }

/** 设置行图标形态：彩色圆角方块（Apple）或保留原图（头像）。 */
enum class SettingIconPresentation { Tile, Avatar }

/**
 * 为设置项提供跳转高亮和滚动定位能力。
 */
fun Modifier.highlightSetting(
    settingKey: String?,
    highlightedKey: String,
    onPositioned: ((Int) -> Unit)? = null,
    bringIntoViewRequester: BringIntoViewRequester? = null,
    shape: Shape = RectangleShape,
    color: Color? = null,
): Modifier = composed {
    var modifier = this
    val actualBringIntoViewRequester = remember(settingKey, bringIntoViewRequester) {
        bringIntoViewRequester ?: if (settingKey != null) BringIntoViewRequester() else null
    }

    if (actualBringIntoViewRequester != null) {
        modifier = modifier.bringIntoViewRequester(actualBringIntoViewRequester)
    }

    if (onPositioned != null) {
        modifier = modifier.onGloballyPositioned { coords ->
            onPositioned(coords.positionInRoot().y.toInt())
        }
    }

    if (settingKey != null) {
        val isTarget = settingKey.isNotEmpty() && settingKey == highlightedKey
        val highlightAlpha = remember { Animatable(0f) }
        val highlightColor = color ?: MaterialTheme.colorScheme.primaryContainer

        LaunchedEffect(isTarget) {
            if (isTarget) {
                actualBringIntoViewRequester?.let {
                    delay(200)
                    it.bringIntoView()
                }
                repeat(2) {
                    highlightAlpha.animateTo(0.4f, tween(200, easing = LinearEasing))
                    highlightAlpha.animateTo(0.1f, tween(200, easing = LinearEasing))
                }
                highlightAlpha.animateTo(0.4f, tween(200))
                delay(2000)
                highlightAlpha.animateTo(0f, tween(1000, easing = FastOutSlowInEasing))
            } else {
                highlightAlpha.animateTo(0f, tween(500))
            }
        }

        modifier = modifier.drawWithContent {
            drawContent()
            if (highlightAlpha.value > 0f) {
                drawRect(highlightColor.copy(alpha = highlightAlpha.value))
            }
        }
    }

    modifier
}

/**
 * 设置页的分组容器，按当前 [UiStyle] 自动呈现：
 * - Material 3：原有 16dp 圆角分组与 2dp 行间距；
 * - Miuix：HyperOS `SmallTitle` + 16dp `MiuixCard` + 细分隔线；
 * - Liquid Glass：Apple Grouped Inset Form（26dp 圆角 + 0.5dp 内缩分隔线）。
 */
@Composable
fun SettingItemGroup(
    modifier: Modifier = Modifier,
    title: String? = null,
    header: (@Composable () -> Unit)? = null,
    footer: (@Composable () -> Unit)? = null,
    settingKey: String? = null,
    highlightedKey: String = "",
    onPositioned: ((rootY: Int) -> Unit)? = null,
    bringIntoViewRequester: BringIntoViewRequester? = null,
    dividerStyle: SettingGroupDividerStyle = SettingGroupDividerStyle.Form,
    content: @Composable () -> Unit,
) {
    val style = LocalUiStyle.current
    val tokens = liquidGlassSemanticTokens()
    var dividerPositions by remember { mutableStateOf(emptyList<Int>()) }

    val horizontalPadding = when (style) {
        UiStyle.Material3 -> 16.dp
        UiStyle.Miuix -> 12.dp
        UiStyle.LiquidGlass -> tokens.formHorizontalInset
    }

    Column(
        Modifier
            .highlightSetting(
                settingKey = settingKey,
                highlightedKey = highlightedKey,
                onPositioned = onPositioned,
                bringIntoViewRequester = bringIntoViewRequester,
            ).padding(horizontal = horizontalPadding)
            .padding(bottom = 16.dp)
            .then(modifier),
    ) {
        title?.let { groupTitle ->
            when (style) {
                UiStyle.Miuix -> {
                    MiuixSmallTitle(
                        text = groupTitle,
                        insideMargin = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 16.dp,
                            vertical = 8.dp,
                        ),
                    )
                }
                UiStyle.LiquidGlass -> {
                    Text(
                        text = groupTitle,
                        modifier = Modifier.padding(8.dp, 0.dp, 8.dp, 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                UiStyle.Material3 -> {
                    Text(
                        text = groupTitle,
                        modifier = Modifier.padding(8.dp, 0.dp, 8.dp, 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        header?.let { it() }

        val drawInsetDividers = style == UiStyle.LiquidGlass || style == UiStyle.Miuix
        val itemSpacingDp = if (drawInsetDividers) 0.dp else 2.dp

        @Composable
        fun GroupedBody() {
            Box(Modifier.fillMaxWidth()) {
                Layout(
                    content = content,
                    modifier = Modifier.fillMaxWidth(),
                ) { measurables, constraints ->
                    val placeables = measurables.map { it.measure(constraints) }
                    val spacing = itemSpacingDp.roundToPx()
                    val baseItemHeight = 48.dp.toPx()

                    var yPosition = 0
                    val positions = mutableListOf<Int>()
                    var hasVisibleBefore = false

                    for (i in placeables.indices) {
                        val placeable = placeables[i]
                        val scale = (placeable.height / baseItemHeight).coerceIn(0f, 1f)

                        if (hasVisibleBefore && scale > 0f) {
                            yPosition += (spacing * scale).toInt()
                        }

                        positions.add(yPosition)
                        yPosition += placeable.height

                        if (scale > 0f) {
                            hasVisibleBefore = true
                        }
                    }

                    val visibleIndices = placeables.indices.filter { placeables[it].height > 0 }
                    val nextDividerPositions = visibleIndices.dropLast(1).map { positions[it] + placeables[it].height }
                    if (dividerPositions != nextDividerPositions) dividerPositions = nextDividerPositions
                    layout(constraints.maxWidth, yPosition) {
                        for (i in placeables.indices) {
                            placeables[i].placeRelative(0, positions[i])
                        }
                    }
                }
                if (drawInsetDividers && dividerPositions.isNotEmpty()) {
                    val dividerColor = if (style == UiStyle.Miuix) {
                        MiuixTheme.colorScheme.dividerLine
                    } else {
                        tokens.separator
                    }
                    Canvas(Modifier.matchParentSize()) {
                        val dividerStart = when {
                            style == UiStyle.Miuix -> 16.dp
                            dividerStyle == SettingGroupDividerStyle.IconAligned -> tokens.settingIconTitleStart
                            else -> tokens.formHorizontalInset
                        }
                        val dividerEnd = when {
                            style == UiStyle.Miuix -> 16.dp
                            dividerStyle == SettingGroupDividerStyle.Form -> tokens.formHorizontalInset
                            else -> 0.dp
                        }
                        dividerPositions.forEach { y ->
                            drawLine(
                                color = dividerColor,
                                start = Offset(dividerStart.toPx(), y - 0.5.dp.toPx()),
                                end = Offset(size.width - dividerEnd.toPx(), y - 0.5.dp.toPx()),
                                strokeWidth = 0.5.dp.toPx(),
                            )
                        }
                    }
                }
            }
        }

        when (style) {
            UiStyle.Miuix -> {
                MiuixCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 16.dp,
                    colors = MiuixCardDefaults.defaultColors(
                        color = MiuixTheme.colorScheme.surfaceContainer,
                    ),
                ) {
                    GroupedBody()
                }
            }
            UiStyle.LiquidGlass -> {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(tokens.formGroupCornerRadius)),
                    shape = RoundedCornerShape(tokens.formGroupCornerRadius),
                    color = tokens.contentSurface,
                ) {
                    GroupedBody()
                }
            }
            UiStyle.Material3 -> {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Transparent,
                ) {
                    GroupedBody()
                }
            }
        }

        footer?.let {
            Column(
                modifier = Modifier.padding(8.dp, 12.dp, 8.dp, 4.dp),
            ) {
                ProvideContentColorTextStyle(
                    textStyle = MaterialTheme.typography.bodyMedium,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ) {
                    it()
                }
            }
        }
    }
}

/**
 * 分组顶部的强调型总开关。
 */
@Composable
fun SettingItemOverall(
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    description: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    settingKey: String? = null,
    highlightedKey: String = "",
    onPositioned: ((rootY: Int) -> Unit)? = null,
    bringIntoViewRequester: BringIntoViewRequester? = null,
) {
    val style = LocalUiStyle.current
    val directSwitchInteraction = style != UiStyle.Material3
    SettingItem(
        modifier = modifier.padding(bottom = 16.dp),
        contentModifier = contentModifier.padding(horizontal = 8.dp),
        title = title,
        description = description,
        icon = icon,
        onClick = { onCheckedChange(!checked) },
        endAction = {
            SwitchWithIcon(
                checked = checked,
                onCheckedChange = if (directSwitchInteraction) onCheckedChange else null,
                enabled = enabled,
            )
        },
        shape = when (style) {
            UiStyle.Miuix -> RoundedCornerShape(16.dp)
            UiStyle.LiquidGlass -> RoundedCornerShape(26.dp)
            UiStyle.Material3 -> RoundedCornerShape(50)
        },
        colors = CardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.outlineVariant,
        ),
        preserveContainerColorInStyledModes = true,
        enabled = enabled,
        settingKey = settingKey,
        highlightedKey = highlightedKey,
        onPositioned = onPositioned,
        bringIntoViewRequester = bringIntoViewRequester,
    )
}

/**
 * 标准开关设置行。
 *
 * 在 Liquid Glass 与 Miuix 下把 [onCheckedChange] 直接交给开关组件，确保用户既能点击整行，
 * 也能直接拖动滑块切换开关状态。
 */
@Composable
fun SettingItemWithSwitch(
    modifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    description: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true,
    settingKey: String? = null,
    highlightedKey: String = "",
    onPositioned: ((rootY: Int) -> Unit)? = null,
    bringIntoViewRequester: BringIntoViewRequester? = null,
) {
    val directSwitchInteraction = LocalUiStyle.current != UiStyle.Material3
    SettingItem(
        modifier = modifier,
        title = title,
        description = description,
        icon = icon,
        onClick = { onCheckedChange(!checked) },
        endAction = {
            SwitchWithIcon(
                checked = checked,
                onCheckedChange = if (directSwitchInteraction) onCheckedChange else null,
                enabled = enabled,
            )
        },
        enabled = enabled,
        settingKey = settingKey,
        highlightedKey = highlightedKey,
        onPositioned = onPositioned,
        bringIntoViewRequester = bringIntoViewRequester,
    )
}

/**
 * 带选中/未选中图标的开关（M3），在 Miuix / Liquid Glass 下自动切到对应原生开关。
 */
@Composable
fun SwitchWithIcon(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: SwitchColors = SwitchDefaults.colors(),
    interactionSource: MutableInteractionSource? = null,
) {
    AppSwitch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        thumbContent = {
            if (checked) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    modifier = Modifier.size(SwitchDefaults.IconSize),
                    tint = if (enabled) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    },
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.Clear,
                    contentDescription = null,
                    modifier = Modifier.size(SwitchDefaults.IconSize),
                )
            }
        },
        enabled = enabled,
        colors = colors,
        interactionSource = interactionSource,
    )
}

/**
 * 设置页的基础行组件。
 *
 * - Material 3：保持原有卡片行视觉；
 * - Miuix：透明底融入 Miuix `Card`，使用 HyperOS 字号/次要色，可点击项自动带右侧箭头；
 * - Liquid Glass：Apple 分组行，带彩色圆角图标方块（[iconTileColor]）、右对齐数值与展开箭头。
 */
@Composable
fun SettingItem(
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    description: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    endAction: (@Composable () -> Unit)? = null,
    rowKind: SettingRowKind = SettingRowKind.Plain,
    value: String? = null,
    iconTileColor: Color? = null,
    iconGlyphColor: Color? = null,
    iconPresentation: SettingIconPresentation = SettingIconPresentation.Tile,
    bottomAction: (@Composable () -> Unit)? = null,
    shape: Shape = RoundedCornerShape(4.dp),
    colors: CardColors = CardColors(
        containerColor = MaterialTheme.colorScheme.surfaceBright,
        contentColor = MaterialTheme.colorScheme.onSurface,
        disabledContainerColor = MaterialTheme.colorScheme.surfaceBright,
        disabledContentColor = MaterialTheme.colorScheme.outlineVariant,
    ),
    preserveContainerColorInStyledModes: Boolean = false,
    enabled: Boolean = true,
    settingKey: String? = null,
    highlightedKey: String = "",
    onPositioned: ((rootY: Int) -> Unit)? = null,
    bringIntoViewRequester: BringIntoViewRequester? = null,
) {
    val style = LocalUiStyle.current
    val isLiquidGlass = style == UiStyle.LiquidGlass
    val isMiuix = style == UiStyle.Miuix
    val tokens = liquidGlassSemanticTokens()
    val rowShape = if (!preserveContainerColorInStyledModes && (isLiquidGlass || isMiuix)) {
        RectangleShape
    } else {
        shape
    }
    val rowContainerColor = if (!preserveContainerColorInStyledModes && (isLiquidGlass || isMiuix)) {
        Color.Transparent
    } else {
        colors.containerColor
    }
    val rowContentColor = when {
        rowKind == SettingRowKind.Destructive && enabled -> MaterialTheme.colorScheme.error
        enabled -> colors.contentColor
        else -> colors.disabledContentColor
    }
    val showTrailingChevron = when (style) {
        UiStyle.LiquidGlass ->
            rowKind == SettingRowKind.Navigation ||
                rowKind == SettingRowKind.Value ||
                (onClick != null && endAction == null && bottomAction == null && rowKind == SettingRowKind.Plain)
        UiStyle.Miuix ->
            rowKind == SettingRowKind.Navigation ||
                rowKind == SettingRowKind.Value ||
                (onClick != null && endAction == null && bottomAction == null && rowKind == SettingRowKind.Plain)
        UiStyle.Material3 -> false
    }

    Surface(
        modifier = if (onClick != null) {
            modifier
                .clip(rowShape)
                .highlightSetting(
                    settingKey = settingKey,
                    highlightedKey = highlightedKey,
                    onPositioned = onPositioned,
                    bringIntoViewRequester = bringIntoViewRequester,
                    shape = rowShape,
                ).clickable(
                    enabled = enabled,
                    onClick = onClick,
                )
        } else {
            modifier
                .clip(rowShape)
                .highlightSetting(
                    settingKey = settingKey,
                    highlightedKey = highlightedKey,
                    onPositioned = onPositioned,
                    bringIntoViewRequester = bringIntoViewRequester,
                    shape = rowShape,
                )
        },
        shape = rowShape,
        color = rowContainerColor,
        contentColor = rowContentColor,
    ) {
        Column(
            contentModifier.padding(
                horizontal = 16.dp,
                vertical = when (style) {
                    UiStyle.LiquidGlass -> 8.dp
                    UiStyle.Miuix -> 14.dp
                    UiStyle.Material3 -> 12.dp
                },
            ),
        ) {
            Row(
                modifier = Modifier.heightIn(
                    min = when (style) {
                        UiStyle.LiquidGlass -> tokens.listRowMinHeight - 16.dp
                        UiStyle.Miuix -> 32.dp
                        UiStyle.Material3 -> 48.dp
                    },
                ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                icon?.let {
                    Box(
                        Modifier.size(
                            when (style) {
                                UiStyle.LiquidGlass -> tokens.settingIconTileSize
                                UiStyle.Miuix -> 28.dp
                                UiStyle.Material3 -> 40.dp
                            },
                        ),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isLiquidGlass && iconPresentation == SettingIconPresentation.Avatar) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                icon()
                            }
                        } else if (isLiquidGlass) {
                            Surface(
                                modifier = Modifier.fillMaxSize(),
                                shape = RoundedCornerShape(8.dp),
                                color = iconTileColor ?: tokens.settingIconBlue,
                                contentColor = if (enabled) {
                                    iconGlyphColor ?: Color.White
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                },
                            ) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Box(Modifier.size(tokens.settingIconGlyphSize), contentAlignment = Alignment.Center) {
                                        icon()
                                    }
                                }
                            }
                        } else {
                            Surface(
                                color = Color.Transparent,
                                contentColor = if (enabled) {
                                    if (isMiuix) {
                                        MiuixTheme.colorScheme.onSurfaceVariantActions
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                },
                            ) {
                                icon()
                            }
                        }
                    }
                }
                Column(
                    modifier = Modifier
                        .heightIn(min = if (isLiquidGlass || isMiuix) 0.dp else 48.dp)
                        .weight(
                            if ((isLiquidGlass || isMiuix) && value != null && endAction == null) {
                                0.6f
                            } else {
                                1f
                            },
                        ),
                    verticalArrangement = Arrangement.Center,
                ) {
                    ProvideContentColorTextStyle(
                        textStyle = when (style) {
                            UiStyle.LiquidGlass -> MaterialTheme.typography.bodyLarge
                            UiStyle.Miuix -> MiuixTheme.textStyles.headline1
                            UiStyle.Material3 -> MaterialTheme.typography.titleMedium
                        },
                        contentColor = rowContentColor,
                    ) {
                        title()
                    }
                    description?.let {
                        ProvideContentColorTextStyle(
                            textStyle = if (isMiuix) {
                                MiuixTheme.textStyles.body2
                            } else {
                                MaterialTheme.typography.bodyMedium
                            },
                            contentColor = if (enabled) {
                                if (isMiuix) {
                                    MiuixTheme.colorScheme.onSurfaceVariantSummary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            } else {
                                colors.disabledContentColor
                            },
                        ) {
                            description()
                        }
                    }
                }
                endAction?.let { it() } ?: run {
                    if ((isLiquidGlass || isMiuix) && value != null) {
                        Column(
                            modifier = Modifier.weight(0.4f),
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                text = value,
                                modifier = Modifier.fillMaxWidth(),
                                style = if (isMiuix) MiuixTheme.textStyles.body2 else MaterialTheme.typography.bodyMedium,
                                color = if (isMiuix) {
                                    MiuixTheme.colorScheme.onSurfaceVariantActions
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                textAlign = TextAlign.End,
                            )
                        }
                    }
                    if (showTrailingChevron) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "打开",
                            tint = if (isMiuix) {
                                MiuixTheme.colorScheme.onSurfaceVariantActions
                            } else {
                                MaterialTheme.colorScheme.outline
                            },
                        )
                    }
                }
            }
            bottomAction?.let { it() }
        }
    }
}
