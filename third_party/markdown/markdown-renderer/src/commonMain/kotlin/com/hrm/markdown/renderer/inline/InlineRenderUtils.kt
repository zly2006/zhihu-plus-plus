package com.hrm.markdown.renderer.inline

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.hrm.markdown.parser.ast.ContainerNode
import com.hrm.markdown.parser.ast.DirectiveInline
import com.hrm.markdown.parser.ast.InlineCode
import com.hrm.markdown.parser.ast.Node
import com.hrm.markdown.parser.ast.Text
import com.hrm.markdown.renderer.MarkdownTheme

internal fun buildInlineDirectiveFallbackText(node: DirectiveInline): String {
    val argsText = if (node.args.isNotEmpty()) {
        " " + node.args.entries.joinToString(" ") { (k, v) ->
            if (k.startsWith("_")) v else "$k=$v"
        }
    } else ""
    return "{% ${node.tagName}$argsText %}"
}

internal fun estimateLatexWidth(latex: String): Float {
    val baseLen = latex.length.toFloat()
    return (baseLen * 0.7f).coerceIn(1.5f, 20f)
}

internal fun parseCssStyleToSpanStyle(css: String): SpanStyle? {
    if (css.isBlank()) return null
    var color: Color? = null
    var background: Color? = null
    var fontWeight: FontWeight? = null
    var fontStyle: FontStyle? = null
    var textDecoration: TextDecoration? = null

    val pairs = css.split(";").map { it.trim() }.filter { it.isNotEmpty() }
    for (pair in pairs) {
        val colonIdx = pair.indexOf(':')
        if (colonIdx < 0) continue
        val key = pair.substring(0, colonIdx).trim().lowercase()
        val value = pair.substring(colonIdx + 1).trim().lowercase()
        when (key) {
            "color" -> color = parseCssColor(value)
            "background", "background-color" -> background = parseCssColor(value)
            "font-weight" -> fontWeight = when (value) {
                "bold" -> FontWeight.Bold
                "normal" -> FontWeight.Normal
                "lighter" -> FontWeight.Light
                else -> null
            }

            "font-style" -> fontStyle = when (value) {
                "italic" -> FontStyle.Italic
                "normal" -> FontStyle.Normal
                else -> null
            }

            "text-decoration" -> textDecoration = when {
                "underline" in value -> TextDecoration.Underline
                "line-through" in value -> TextDecoration.LineThrough
                else -> null
            }
        }
    }
    return SpanStyle(
        color = color ?: Color.Unspecified,
        background = background ?: Color.Unspecified,
        fontWeight = fontWeight,
        fontStyle = fontStyle,
        textDecoration = textDecoration,
    )
}

internal fun parseCssColor(value: String): Color? {
    return when (value.trim().lowercase()) {
        "red" -> Color.Red
        "blue" -> Color.Blue
        "green" -> Color.Green
        "yellow" -> Color.Yellow
        "white" -> Color.White
        "black" -> Color.Black
        "gray", "grey" -> Color.Gray
        "cyan" -> Color.Cyan
        "magenta" -> Color.Magenta
        "orange" -> Color(0xFFFFA500)
        "purple" -> Color(0xFF800080)
        "pink" -> Color(0xFFFF69B4)
        else -> {
            val hex = value.removePrefix("#")
            when (hex.length) {
                6 -> try {
                    Color(("FF$hex").toLong(16))
                } catch (_: Exception) {
                    null
                }

                8 -> try {
                    Color(hex.toLong(16))
                } catch (_: Exception) {
                    null
                }

                3 -> try {
                    val r = hex[0].toString().repeat(2)
                    val g = hex[1].toString().repeat(2)
                    val b = hex[2].toString().repeat(2)
                    Color(("FF$r$g$b").toLong(16))
                } catch (_: Exception) {
                    null
                }

                else -> null
            }
        }
    }
}

internal fun inferStyleFromClasses(classes: List<String>, theme: MarkdownTheme): SpanStyle? {
    if (classes.isEmpty()) return null
    var color: Color? = null
    var background: Color? = null
    var fontWeight: FontWeight? = null
    var fontStyle: FontStyle? = null
    var textDecoration: TextDecoration? = null

    for (cls in classes) {
        when (cls.lowercase()) {
            "red" -> color = Color.Red
            "blue" -> color = Color.Blue
            "green" -> color = Color.Green
            "yellow" -> color = Color.Yellow
            "orange" -> color = Color(0xFFFFA500)
            "purple" -> color = Color(0xFF800080)
            "pink" -> color = Color(0xFFFF69B4)
            "gray", "grey" -> color = Color.Gray
            "bold" -> fontWeight = FontWeight.Bold
            "italic" -> fontStyle = FontStyle.Italic
            "underline" -> textDecoration = TextDecoration.Underline
            "line-through", "strikethrough" -> textDecoration = TextDecoration.LineThrough
            "highlight" -> background = theme.highlightColor
        }
    }
    return SpanStyle(
        color = color ?: Color.Unspecified,
        background = background ?: Color.Unspecified,
        fontWeight = fontWeight,
        fontStyle = fontStyle,
        textDecoration = textDecoration,
    )
}

internal fun extractPlainText(node: Node): String = buildString {
    when (node) {
        is Text -> append(node.literal)
        is InlineCode -> append(node.literal)
        is ContainerNode -> node.children.forEach { append(extractPlainText(it)) }
        else -> {}
    }
}
