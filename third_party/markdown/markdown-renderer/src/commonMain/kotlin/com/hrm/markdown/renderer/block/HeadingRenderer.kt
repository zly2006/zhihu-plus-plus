package com.hrm.markdown.renderer.block

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import com.hrm.markdown.parser.ast.Heading
import com.hrm.markdown.parser.ast.Node
import com.hrm.markdown.parser.ast.SetextHeading
import com.hrm.markdown.renderer.LocalMarkdownConfig
import com.hrm.markdown.renderer.LocalMarkdownTheme
import com.hrm.markdown.renderer.LocalOnLinkClick
import com.hrm.markdown.renderer.LocalRendererDocument
import com.hrm.markdown.renderer.inline.InlineFlowText
import com.hrm.markdown.renderer.inline.rememberInlineContent

/**
 * ATX 标题渲染器 (# ~ ######)
 */
@Composable
internal fun HeadingRenderer(
    node: Heading,
    modifier: Modifier = Modifier,
) {
    val theme = LocalMarkdownTheme.current
    val onLinkClick = LocalOnLinkClick.current
    val config = LocalMarkdownConfig.current
    val level = (node.level - 1).coerceIn(0, theme.headingStyles.lastIndex)
    val style = theme.headingStyles[level]
    val inlineResult = rememberInlineContent(
        parent = node,
        onLinkClick = onLinkClick,
        hostTextStyle = style,
    )
    val numbering = if (config.enableHeadingNumbering) {
        val document = LocalRendererDocument.current
        remember(document, node) { computeHeadingNumber(document.children, node) }
    } else null

    val finalAnnotated = if (numbering != null) {
        remember(numbering, inlineResult.annotated) {
            buildAnnotatedString {
                append("$numbering ")
                append(inlineResult.annotated)
            }
        }
    } else inlineResult.annotated

    Column(modifier = modifier.fillMaxWidth()) {
        InlineFlowText(
            annotated = finalAnnotated,
            inlineContents = inlineResult.inlineContents,
            style = style,
        )

        // h1 和 h2 下方添加分隔线（GitHub 风格）
        if (node.level <= 2) {
            HorizontalDivider(
                modifier = Modifier.padding(top = 4.dp),
                thickness = theme.dividerThickness,
                color = theme.dividerColor,
            )
        }
    }
}

/**
 * Setext 标题渲染器 (=== / ---)
 */
@Composable
internal fun SetextHeadingRenderer(
    node: SetextHeading,
    modifier: Modifier = Modifier,
) {
    val theme = LocalMarkdownTheme.current
    val onLinkClick = LocalOnLinkClick.current
    val config = LocalMarkdownConfig.current
    val level = (node.level - 1).coerceIn(0, theme.headingStyles.lastIndex)
    val style = theme.headingStyles[level]
    val inlineResult = rememberInlineContent(
        parent = node,
        onLinkClick = onLinkClick,
        hostTextStyle = style,
    )
    val numbering = if (config.enableHeadingNumbering) {
        val document = LocalRendererDocument.current
        remember(document, node) { computeHeadingNumberForSetext(document.children, node) }
    } else null

    val finalAnnotated = if (numbering != null) {
        remember(numbering, inlineResult.annotated) {
            buildAnnotatedString {
                append("$numbering ")
                append(inlineResult.annotated)
            }
        }
    } else inlineResult.annotated

    Column(modifier = modifier.fillMaxWidth()) {
        InlineFlowText(
            annotated = finalAnnotated,
            inlineContents = inlineResult.inlineContents,
            style = style,
        )

        HorizontalDivider(
            modifier = Modifier.padding(top = 4.dp),
            thickness = theme.dividerThickness,
            color = theme.dividerColor,
        )
    }
}

/**
 * 计算 ATX 标题的层级编号（如 "1", "1.1", "1.1.2"）。
 * 遍历文档顶层子节点，依次递增各级计数器。
 */
private fun computeHeadingNumber(children: List<Node>, target: Heading): String? {
    // 维护 6 级计数器
    val counters = IntArray(6)
    for (child in children) {
        val level = when (child) {
            is Heading -> child.level
            is SetextHeading -> child.level
            else -> continue
        }
        val idx = (level - 1).coerceIn(0, 5)
        counters[idx]++
        // 重置下级计数器
        for (i in idx + 1..5) counters[i] = 0

        if (child === target) {
            return buildNumberString(counters, idx)
        }
    }
    return null
}

private fun computeHeadingNumberForSetext(children: List<Node>, target: SetextHeading): String? {
    val counters = IntArray(6)
    for (child in children) {
        val level = when (child) {
            is Heading -> child.level
            is SetextHeading -> child.level
            else -> continue
        }
        val idx = (level - 1).coerceIn(0, 5)
        counters[idx]++
        for (i in idx + 1..5) counters[i] = 0

        if (child === target) {
            return buildNumberString(counters, idx)
        }
    }
    return null
}

private fun buildNumberString(counters: IntArray, maxIdx: Int): String {
    val parts = mutableListOf<Int>()
    for (i in 0..maxIdx) {
        parts.add(counters[i].coerceAtLeast(1))
    }
    return parts.joinToString(".")
}
