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

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.zly2006.zhihu.ui.liquidglass.components.AndroidLiquidBottomTab
import com.github.zly2006.zhihu.ui.liquidglass.components.AndroidLiquidBottomTabs
import com.github.zly2006.zhihu.ui.liquidglass.components.AndroidLiquidButton
import com.github.zly2006.zhihu.ui.liquidglass.components.AndroidLiquidSlider
import com.github.zly2006.zhihu.ui.liquidglass.components.AndroidLiquidToggle

@Composable
internal actual fun LiquidButton(
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    containerColor: Color,
    contentColor: Color,
    contentPadding: PaddingValues,
    content: @Composable RowScope.() -> Unit,
) {
    val action by rememberUpdatedState(onClick)
    CompositionLocalProvider(LocalContentColor provides contentColor) {
        ProvideTextStyle(MaterialTheme.typography.labelLarge) {
            AndroidLiquidButton(
                onClick = { action() },
                modifier = modifier,
                enabled = enabled,
                surfaceColor = containerColor.copy(alpha = containerColor.alpha * 0.72f),
                contentPadding = contentPadding,
                content = content,
            )
        }
    }
}

@Composable
internal actual fun LiquidSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier,
    enabled: Boolean,
) {
    val currentChecked by rememberUpdatedState(checked)
    val change by rememberUpdatedState(onCheckedChange)
    val readValue = remember { { currentChecked } }
    val select = remember {
        { value: Boolean ->
            change?.invoke(value)
            Unit
        }
    }
    AndroidLiquidToggle(
        selected = readValue,
        onSelect = select,
        modifier = modifier,
        enabled = enabled && onCheckedChange != null,
    )
}

@Composable
internal actual fun LiquidSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int,
    onValueChangeFinished: (() -> Unit)?,
) {
    val currentValue by rememberUpdatedState(value)
    val change by rememberUpdatedState(onValueChange)
    val finished by rememberUpdatedState(onValueChangeFinished)
    val readValue = remember { { currentValue } }
    val select = remember { { next: Float -> change(next) } }
    val finish = remember {
        {
            finished?.invoke()
            Unit
        }
    }
    AndroidLiquidSlider(
        value = readValue,
        onValueChange = select,
        valueRange = valueRange,
        modifier = modifier,
        enabled = enabled,
        steps = steps,
        onValueChangeFinished = finish,
    )
}

@Composable
internal actual fun LiquidNavigationBar(
    items: List<AppNavigationItem>,
) {
    if (items.isEmpty()) return
    val currentItems by rememberUpdatedState(items)
    val selectedIndex by rememberUpdatedState(items.indexOfFirst { it.selected }.coerceAtLeast(0))
    val readSelection = remember { { selectedIndex } }
    val onDragSelection = remember {
        { index: Int ->
            currentItems
                .getOrNull(index)
                ?.takeUnless { it.selected }
                ?.onClick
                ?.invoke()
            Unit
        }
    }
    AndroidLiquidBottomTabs(
        selectedTabIndex = readSelection,
        onTabSelected = onDragSelection,
        tabsCount = items.size,
        modifier = Modifier
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp, bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() + 8.dp),
    ) {
        items.forEach { item ->
            AndroidLiquidBottomTab(
                onClick = item.onClick,
                modifier = Modifier.testTag(item.tag).semantics { selected = item.selected },
            ) {
                Icon(
                    imageVector = if (item.selected) item.icon else item.unselectedIcon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(26.dp),
                )
                Text(
                    text = item.label,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 11.sp,
                    lineHeight = 13.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
