package com.github.zly2006.zhihu.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.CardColors
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.util.ProvideContentColorTextStyle
import kotlinx.coroutines.delay

fun Modifier.highlightSetting(
    settingKey: String?,
    highlightedKey: String,
    onPositioned: ((Int) -> Unit)? = null,
    shape: Shape = RectangleShape,
    color: Color? = null,
): Modifier = composed {
    var modifier = this

    if (onPositioned != null) {
        modifier = modifier.onGloballyPositioned { coords ->
            onPositioned(coords.positionInRoot().y.toInt())
        }
    }

    if (settingKey != null) {
        val isTarget = settingKey.isNotEmpty() && settingKey == highlightedKey
        val highlightAlpha = remember { Animatable(0f) }
        val highlightColor = color ?: MaterialTheme.colorScheme.outline

        LaunchedEffect(isTarget) {
            if (isTarget) {
                // Flash 2 times (In, Out, In, Out, In)
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

        modifier = modifier.background(
            highlightColor.copy(alpha = highlightAlpha.value),
            shape = shape,
        )
    }

    modifier
}

@Composable
fun SettingItemGroup(
    modifier: Modifier = Modifier,
    title: String? = null,
    header: (@Composable () -> Unit)? = null,
    footer: (@Composable () -> Unit)? = null,
    settingKey: String? = null,
    highlightedKey: String = "",
    onPositioned: ((rootY: Int) -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Column(
        Modifier
            .highlightSetting(settingKey, highlightedKey, onPositioned)
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp)
            .then(modifier),
    ) {
        title?.let {
            Text(
                text = title,
                modifier = Modifier.padding(8.dp, 0.dp, 8.dp, 8.dp),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }

        header?.let { it() }

        Column(
            modifier = modifier
                .clip(RoundedCornerShape(16.dp)),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            content()
        }

        footer?.let {
            Column(
                modifier = Modifier.padding(8.dp, 16.dp, 8.dp, 8.dp),
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
) {
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
                onCheckedChange = null,
                enabled = enabled,
            )
        },
        shape = RoundedCornerShape(50),
        colors = CardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContentColor = MaterialTheme.colorScheme.outlineVariant,
        ),
        enabled = enabled,
        settingKey = settingKey,
        highlightedKey = highlightedKey,
        onPositioned = onPositioned,
    )
}

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
) {
    SettingItem(
        modifier = modifier,
        title = title,
        description = description,
        icon = icon,
        onClick = { onCheckedChange(!checked) },
        endAction = {
            SwitchWithIcon(
                checked = checked,
                onCheckedChange = null,
                enabled = enabled,
            )
        },
        enabled = enabled,
        settingKey = settingKey,
        highlightedKey = highlightedKey,
        onPositioned = onPositioned,
    )
}

@Composable
fun SwitchWithIcon(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    colors: SwitchColors = SwitchDefaults.colors(),
    interactionSource: MutableInteractionSource? = null,
) {
    Switch(
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

@Composable
fun SettingItem(
    modifier: Modifier = Modifier,
    contentModifier: Modifier = Modifier,
    title: @Composable () -> Unit,
    description: (@Composable () -> Unit)? = null,
    icon: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    endAction: (@Composable () -> Unit)? = null,
    bottomAction: (@Composable () -> Unit)? = null,
    shape: Shape = RoundedCornerShape(4.dp),
    colors: CardColors = CardColors(
        containerColor = MaterialTheme.colorScheme.surfaceBright,
        contentColor = MaterialTheme.colorScheme.onSurface,
        disabledContainerColor = MaterialTheme.colorScheme.surfaceBright,
        disabledContentColor = MaterialTheme.colorScheme.outlineVariant,
    ),
    enabled: Boolean = true,
    settingKey: String? = null,
    highlightedKey: String = "",
    onPositioned: ((rootY: Int) -> Unit)? = null,
) {
    Surface(
        modifier = if (onClick != null) {
            modifier
                .clip(shape)
                .highlightSetting(settingKey, highlightedKey, onPositioned, shape)
                .clickable(
                    enabled = enabled,
                    onClick = onClick,
                )
        } else {
            modifier
                .clip(shape)
                .highlightSetting(settingKey, highlightedKey, onPositioned, shape)
        },
        shape = shape,
        color = colors.containerColor,
        contentColor = if (enabled) {
            colors.contentColor
        } else {
            colors.disabledContentColor
        },
    ) {
        Column(contentModifier.padding(16.dp, 12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                icon?.let {
                    Box(
                        Modifier.size(40.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Surface(
                            color = Color.Transparent,
                            contentColor = if (enabled) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.outlineVariant
                            },
                        ) { icon() }
                    }
                }
                Column(
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .weight(1f),
                    verticalArrangement = Arrangement.Center,
                ) {
                    ProvideContentColorTextStyle(
                        textStyle = MaterialTheme.typography.titleMedium,
                        contentColor = if (enabled) {
                            colors.contentColor
                        } else {
                            colors.disabledContentColor
                        },
                    ) {
                        title()
                    }
                    description?.let {
                        ProvideContentColorTextStyle(
                            textStyle = MaterialTheme.typography.bodyMedium,
                            contentColor = if (enabled) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                colors.disabledContentColor
                            },
                        ) {
                            description()
                        }
                    }
                }
                endAction?.let { it() }
            }
            bottomAction?.let { it() }
        }
    }
}

@Preview
@Composable
fun SettingItemPreview() {
    Column(
        Modifier
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(vertical = 16.dp)
            .width(412.dp)
            .heightIn(min = 256.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        SettingItemGroup(
            title = "芭乐",
            header = {
                SettingItemOverall(
                    title = { Text("我好") },
                    checked = true,
                    onCheckedChange = {},
                )
            },
            footer = {
                Text("这是一个脚注")
            },
        ) {
            SettingItemWithSwitch(
                title = { Text("你好") },
                description = { Text("欢迎光临") },
                icon = { Icon(Icons.Default.AutoFixHigh, null) },
                checked = true,
                onCheckedChange = {},
            )
            SettingItemWithSwitch(
                title = { Text("再见") },
                checked = false,
                onCheckedChange = {},
            )
            SettingItemWithSwitch(
                title = { Text("你好") },
                description = { Text("欢迎光临") },
                checked = true,
                onCheckedChange = {},
                enabled = false,
            )
        }
    }
}
