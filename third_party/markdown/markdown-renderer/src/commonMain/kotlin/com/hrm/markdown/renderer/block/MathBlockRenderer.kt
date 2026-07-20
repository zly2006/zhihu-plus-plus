package com.hrm.markdown.renderer.block

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.sp
import com.hrm.latex.renderer.Latex
import com.hrm.latex.renderer.model.LatexConfig
import com.hrm.markdown.renderer.LocalMarkdownTheme

/**
 * 数学公式块渲染器 ($$...$$)
 * 使用 LaTeX 库渲染数学公式。
 *
 * 公式编号（`\tag{N}`）、环境自动编号（equation/align 等）、引用（`\ref`/`\eqref`）
 * 均由 LaTeX 渲染库原生处理，无需额外的编号展示逻辑。
 */
@Composable
internal fun MathBlockRenderer(
    latex: String,
    modifier: Modifier = Modifier,
) {
    val theme = LocalMarkdownTheme.current
    val trimmedLatex = latex.trim()
    val config = LatexConfig(
        fontSize = (theme.mathFontSize * 1.2f).sp,
        mathFont = theme.mathFont,
        theme = theme.latexTheme,
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(theme.codeBlockCornerRadius))
            .background(theme.mathBlockBackground)
            .padding(theme.codeBlockPadding),
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
        ) {
            Box(
                modifier = Modifier.widthIn(min = maxWidth),
                contentAlignment = Alignment.Center,
            ) {
                Latex(
                    latex = trimmedLatex,
                    config = config,
                )
            }
        }
    }
}
