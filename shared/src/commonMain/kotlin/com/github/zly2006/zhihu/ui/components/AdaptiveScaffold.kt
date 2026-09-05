package com.github.zly2006.zhihu.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FabPosition
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.theme.LocalGlassBackdrop
import com.github.zly2006.zhihu.theme.LocalLiquidGlass
import com.github.zly2006.zhihu.theme.ThemeManager
import com.github.zly2006.zhihu.theme.rememberGlassBackdrop

/** 内容层独立录制，玻璃操作层在其上采样，避免把玻璃自身递归录入背景。 */
@Composable
fun AdaptiveScaffold(
    modifier: Modifier = Modifier,
    topBar: @Composable () -> Unit = {},
    bottomBar: @Composable () -> Unit = {},
    snackbarHost: @Composable () -> Unit = {},
    floatingActionButton: @Composable () -> Unit = {},
    floatingActionButtonPosition: FabPosition = FabPosition.End,
    containerColor: Color = MaterialTheme.colorScheme.background,
    contentColor: Color = contentColorFor(containerColor),
    contentWindowInsets: WindowInsets = ScaffoldDefaults.contentWindowInsets,
    glassBottomBar: Boolean = true,
    content: @Composable (PaddingValues) -> Unit,
) {
    if (!LocalLiquidGlass.current) {
        Scaffold(
            modifier,
            topBar,
            bottomBar,
            snackbarHost,
            floatingActionButton,
            floatingActionButtonPosition,
            containerColor,
            contentColor,
            contentWindowInsets,
            content,
        )
        return
    }
    val backdrop = rememberGlassBackdrop()
    val dark = ThemeManager.isDarkTheme()
    val tint = if (dark) Color(0xFF202126).copy(alpha = 0.78f) else Color.White.copy(alpha = 0.72f)
    val colors = MaterialTheme.colorScheme
    val chromeColors = colors.copy(
        surface = Color.Transparent,
        surfaceContainer = Color.Transparent,
        surfaceContainerHigh = Color.Transparent,
        surfaceContainerLow = Color.Transparent,
        surfaceVariant = Color.Transparent,
        surfaceTint = Color.Transparent,
    )
    Scaffold(
        modifier = modifier,
        topBar = {
            Box(backdrop.surface(RoundedCornerShape(0.dp), tint, floating = false)) {
                CompositionLocalProvider(LocalGlassBackdrop provides backdrop) {
                    MaterialTheme(colorScheme = chromeColors, content = topBar)
                }
            }
        },
        bottomBar = {
            CompositionLocalProvider(LocalGlassBackdrop provides backdrop) {
                if (!glassBottomBar) {
                    bottomBar()
                } else {
                    val bottomInset = contentWindowInsets.asPaddingValues().calculateBottomPadding()
                    Layout(content = {
                        Box(backdrop.surface(RoundedCornerShape(32.dp), tint)) {
                            MaterialTheme(colorScheme = chromeColors, content = bottomBar)
                        }
                    }) { measurables, constraints ->
                        val margin = 12.dp.roundToPx()
                        val child = measurables.single().measure(constraints.copy(minWidth = 0, minHeight = 0, maxWidth = (constraints.maxWidth - margin * 2).coerceAtLeast(0)))
                        // Scaffold treats a measured empty slot as a bottom bar; preserve its system inset.
                        val height = bottomInset.roundToPx() + if (child.height == 0) 0 else child.height + 8.dp.roundToPx()
                        layout(constraints.maxWidth, height) { child.placeRelative(margin, 0) }
                    }
                }
            }
        },
        snackbarHost = snackbarHost,
        floatingActionButton = { CompositionLocalProvider(LocalGlassBackdrop provides backdrop, content = floatingActionButton) },
        floatingActionButtonPosition = floatingActionButtonPosition,
        containerColor = containerColor,
        contentColor = contentColor,
        contentWindowInsets = contentWindowInsets,
    ) { padding ->
        Box(backdrop.source) { content(padding) }
    }
}
