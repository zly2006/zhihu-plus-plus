package com.github.zly2006.zhihu.ui

import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val SinglePaneCardHorizontalPadding = 16.dp
private val MultiPaneCardHorizontalPadding = 4.dp

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun rememberAdaptiveCardHorizontalPadding(): Dp {
    val directive = calculatePaneScaffoldDirective(currentWindowAdaptiveInfo())
    return if (directive.maxHorizontalPartitions > 1) {
        MultiPaneCardHorizontalPadding
    } else {
        SinglePaneCardHorizontalPadding
    }
}
