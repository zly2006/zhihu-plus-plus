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

import androidx.compose.foundation.layout.height
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.theme.LocalUiBackdrop
import com.github.zly2006.zhihu.theme.LocalUiStyle
import com.github.zly2006.zhihu.theme.UiStyle
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.basic.NavigationBar as MiuixNavigationBar
import top.yukonga.miuix.kmp.basic.NavigationBarItem as MiuixNavigationBarItem

/**
 * 主壳底部导航栏三风格统一实现。
 *
 * 所有风格均保留 `nav_tab_<destination>` 测试 tag 与相同的点击/重选语义。
 */
@Composable
fun AppMainNavigationBar(
    items: List<AppNavigationItem>,
    bottomPadding: Dp,
    isDarkTheme: Boolean,
) {
    when (LocalUiStyle.current) {
        UiStyle.Material3 -> {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.height(64.dp + bottomPadding),
            ) {
                items.forEach { item ->
                    NavigationBarItem(
                        selected = item.selected,
                        onClick = item.onClick,
                        label = { Text(item.label) },
                        alwaysShowLabel = true,
                        colors = if (!isDarkTheme) {
                            NavigationBarItemDefaults.colors().copy(
                                selectedIndicatorColor = MaterialTheme.colorScheme.secondaryContainer
                                    .copy(alpha = 0.92f)
                                    .compositeOver(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                            )
                        } else {
                            NavigationBarItemDefaults.colors()
                        },
                        icon = { Icon(item.icon, contentDescription = null) },
                        modifier = Modifier.testTag(item.tag),
                    )
                }
            }
        }
        UiStyle.Miuix -> {
            val backdrop = LocalUiBackdrop.current
            MiuixNavigationBar(
                modifier = Modifier
                    .height(64.dp + bottomPadding)
                    .then(backdrop.chromeSurface(floating = false)),
                color = if (backdrop.isActive) Color.Transparent else MiuixTheme.colorScheme.surface,
            ) {
                items.forEach { item ->
                    MiuixNavigationBarItem(
                        selected = item.selected,
                        onClick = item.onClick,
                        icon = item.icon,
                        label = item.label,
                        modifier = Modifier.testTag(item.tag),
                    )
                }
            }
        }
        UiStyle.LiquidGlass -> {
            LiquidNavigationBar(items = items)
        }
    }
}
