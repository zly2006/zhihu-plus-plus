package com.hrm.markdown.renderer.diagram

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hrm.markdown.renderer.LocalMarkdownTheme

/**
 * 无法识别的图表类型的回退渲染器，以代码形式展示。
 */
@Composable
internal fun DiagramFallback(
    code: String,
    typeName: String,
    modifier: Modifier = Modifier,
) {
    val theme = LocalMarkdownTheme.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(theme.codeBlockCornerRadius))
            .background(Color(0xFFF0F4F8))
            .padding(theme.codeBlockPadding),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 8.dp),
        ) {
            Text(
                text = "📊",
                modifier = Modifier.padding(end = 6.dp),
            )
            Text(
                text = "$typeName Diagram",
                style = theme.bodyStyle.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    color = Color(0xFF57606A),
                ),
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
        ) {
            Text(
                text = code.trimEnd('\n'),
                style = theme.codeBlockStyle.copy(fontFamily = FontFamily.Monospace),
            )
        }
    }
}
