package com.hrm.markdown.renderer.block

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 分页符渲染器：`***pagebreak***`。
 *
 * 在屏幕预览中以虚线 + 标签形式展示分页位置。
 * PDF 导出/打印场景下，渲染器可替换为实际分页样式。
 */
@Composable
internal fun PageBreakRenderer(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        HorizontalDivider(
            modifier = Modifier.fillMaxWidth(),
            thickness = 1.dp,
            color = Color(0xFFBDBDBD),
        )
        Text(
            text = "— Page Break —",
            fontSize = 10.sp,
            fontStyle = FontStyle.Italic,
            color = Color(0xFF9E9E9E),
            modifier = Modifier.padding(horizontal = 8.dp),
        )
    }
}
