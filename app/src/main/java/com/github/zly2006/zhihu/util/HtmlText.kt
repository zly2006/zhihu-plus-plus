package com.github.zly2006.zhihu.util

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

/**
 * Parse HTML text and convert it to AnnotatedString with styling.
 * Currently supports <em> tags which are styled with the provided emphasis color.
 *
 * @param html The HTML string to parse
 * @param emphasisColor The color to use for emphasized text (content within <em> tags)
 * @return AnnotatedString with styled text
 */
fun parseHtmlText(html: String, emphasisColor: Color): AnnotatedString {
    // Parse HTML using Jsoup
    val document = Jsoup.parse(html)
    val body = document.body()

    return buildAnnotatedString {
        processNode(body, emphasisColor)
    }
}

/**
 * Recursively process nodes and append text with styling
 */
private fun AnnotatedString.Builder.processNode(node: Node, emphasisColor: Color) {
    when (node) {
        is TextNode -> {
            // Append plain text
            append(node.text())
        }
        is Element -> {
            when (node.tagName().lowercase()) {
                "em" -> {
                    // Push emphasis style
                    val startIndex = length
                    node.childNodes().forEach { childNode ->
                        processNode(childNode, emphasisColor)
                    }
                    val endIndex = length
                    // Apply emphasis color to the text within <em> tags
                    if (startIndex < endIndex) {
                        addStyle(
                            style = SpanStyle(color = emphasisColor),
                            start = startIndex,
                            end = endIndex,
                        )
                    }
                }
                "body" -> {
                    // Process body children without adding any styling
                    node.childNodes().forEach { childNode ->
                        processNode(childNode, emphasisColor)
                    }
                }
                else -> {
                    // For other tags, just process children (ignore the tag itself)
                    node.childNodes().forEach { childNode ->
                        processNode(childNode, emphasisColor)
                    }
                }
            }
        }
    }
}

/**
 * Composable function to parse HTML text with Material Theme primary color for emphasis.
 * This is a convenience function that uses the current theme's primary color.
 *
 * @param html The HTML string to parse
 * @return AnnotatedString with styled text using theme colors
 */
@Composable
fun parseHtmlTextWithTheme(html: String): AnnotatedString {
    val emphasisColor = MaterialTheme.colorScheme.primary
    return parseHtmlText(html, emphasisColor)
}
