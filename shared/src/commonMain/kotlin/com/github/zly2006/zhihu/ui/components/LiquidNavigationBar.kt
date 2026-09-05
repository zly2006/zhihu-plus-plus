package com.github.zly2006.zhihu.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

class LiquidNavigationItem(
    val label: String,
    val icon: ImageVector,
    val selected: Boolean,
    val tag: String,
    val onClick: () -> Unit,
)

@Composable
expect fun LiquidNavigationBar(items: List<LiquidNavigationItem>)
