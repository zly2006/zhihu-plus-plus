package com.github.zly2006.zhihu.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.theme.LocalGlassBackdrop
import com.github.zly2006.zhihu.theme.ThemeManager

/** 只放在 Scaffold 的操作槽中，采样不包含操作层自身的正文。 */
@Composable
fun LiquidToolbar(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    val backdrop = checkNotNull(LocalGlassBackdrop.current) { "LiquidToolbar 必须位于玻璃 Scaffold 的操作层中" }
    val tint = if (ThemeManager.isDarkTheme()) Color(0xFF202126).copy(alpha = 0.78f) else Color.White.copy(alpha = 0.72f)
    Row(
        modifier.then(backdrop.surface(RoundedCornerShape(28.dp), tint)).heightIn(min = 56.dp).padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}
