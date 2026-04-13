/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
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

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Wraps a settings item so it can be scrolled into view and briefly highlighted
 * when navigated to via [highlightedKey].
 *
 * @param settingKey     The preference key that identifies this item.
 * @param highlightedKey The key received from the nav destination (empty means no target).
 * @param onPositioned   Callback reporting this item's root-Y coordinate for scroll math.
 */
@Composable
fun HighlightableSettingContainer(
    settingKey: String,
    highlightedKey: String,
    onPositioned: (rootY: Int) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val isTarget = settingKey.isNotEmpty() && settingKey == highlightedKey
    var highlighted by remember { mutableStateOf(isTarget) }

    LaunchedEffect(isTarget) {
        if (isTarget) {
            highlighted = true
            delay(2000)
            highlighted = false
        }
    }

    val highlightAlpha by animateFloatAsState(
        targetValue = if (highlighted) 0.35f else 0f,
        animationSpec = tween(durationMillis = if (highlighted) 300 else 1200),
        label = "setting_highlight",
    )

    Column(
        modifier = modifier
            .onGloballyPositioned { coords ->
                onPositioned(coords.positionInRoot().y.toInt())
            }.background(
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = highlightAlpha),
                shape = RoundedCornerShape(8.dp),
            ),
        content = content,
    )
}
