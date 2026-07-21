package com.hrm.markdown.renderer.inline

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.sp
import com.hrm.codehigh.theme.CodeTheme
import com.hrm.codehigh.theme.LocalCodeTheme
import com.hrm.latex.renderer.measure.LatexMeasurerState
import com.hrm.latex.renderer.measure.rememberLatexMeasurer
import com.hrm.markdown.parser.ast.Abbreviation
import com.hrm.markdown.parser.ast.Autolink
import com.hrm.markdown.parser.ast.CitationReference
import com.hrm.markdown.parser.ast.ContainerNode
import com.hrm.markdown.parser.ast.DirectiveInline
import com.hrm.markdown.parser.ast.Emoji
import com.hrm.markdown.parser.ast.Emphasis
import com.hrm.markdown.parser.ast.EscapedChar
import com.hrm.markdown.parser.ast.FootnoteReference
import com.hrm.markdown.parser.ast.HardLineBreak
import com.hrm.markdown.parser.ast.Highlight
import com.hrm.markdown.parser.ast.HtmlEntity
import com.hrm.markdown.parser.ast.Image
import com.hrm.markdown.parser.ast.InlineCode
import com.hrm.markdown.parser.ast.InlineHtml
import com.hrm.markdown.parser.ast.InlineMath
import com.hrm.markdown.parser.ast.InsertedText
import com.hrm.markdown.parser.ast.KeyboardInput
import com.hrm.markdown.parser.ast.Link
import com.hrm.markdown.parser.ast.Node
import com.hrm.markdown.parser.ast.RubyText
import com.hrm.markdown.parser.ast.SoftLineBreak
import com.hrm.markdown.parser.ast.Spoiler
import com.hrm.markdown.parser.ast.Strikethrough
import com.hrm.markdown.parser.ast.StrongEmphasis
import com.hrm.markdown.parser.ast.StyledText
import com.hrm.markdown.parser.ast.Subscript
import com.hrm.markdown.parser.ast.Superscript
import com.hrm.markdown.parser.ast.Text
import com.hrm.markdown.parser.ast.WikiLink
import com.hrm.markdown.renderer.LocalCodeHighlightTheme
import com.hrm.markdown.renderer.LocalMarkdownDirectiveRegistry
import com.hrm.markdown.renderer.LocalMarkdownTheme
import com.hrm.markdown.renderer.LocalOnFootnoteClick
import com.hrm.markdown.renderer.MarkdownTheme
import com.hrm.markdown.runtime.MarkdownDirectiveRegistry

@Composable
internal fun rememberInlineContent(
    parent: ContainerNode,
    onLinkClick: ((String) -> Unit)? = null,
    hostTextStyle: TextStyle = LocalMarkdownTheme.current.bodyStyle,
): InlineContentResult {
    val theme = LocalMarkdownTheme.current
    val directiveRegistry = LocalMarkdownDirectiveRegistry.current
    val onFootnoteClick = LocalOnFootnoteClick.current
    val latexMeasurer = rememberLatexMeasurer()
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val inlineCodeTheme = LocalCodeHighlightTheme.current ?: LocalCodeTheme.current
    val inlineRevision = remember(parent) {
        {
            var acc = parent.contentHash
            acc = acc * 31 + parent.lineRange.endLine.toLong()
            acc = acc * 31 + parent.childCount().toLong()
            acc
        }
    }
    return remember(
        parent,
        inlineRevision(),
        theme,
        directiveRegistry,
        onLinkClick,
        onFootnoteClick,
        hostTextStyle,
        latexMeasurer,
        density,
        textMeasurer,
        inlineCodeTheme
    ) {
        val inlineContents = mutableMapOf<String, InlineContentEntry>()
        val annotated = buildAnnotatedString {
            renderInlineChildren(
                parent.children,
                theme,
                hostTextStyle,
                inlineContents,
                directiveRegistry,
                onLinkClick,
                onFootnoteClick,
                latexMeasurer,
                density,
                textMeasurer,
                inlineCodeTheme,
            )
        }
        InlineContentResult(
            annotated = annotated,
            inlineContents = inlineContents,
        )
    }
}

internal fun buildInlineAnnotatedString(
    nodes: List<Node>,
    theme: MarkdownTheme,
    hostTextStyle: TextStyle,
    inlineContents: MutableMap<String, InlineContentEntry>,
    directiveRegistry: MarkdownDirectiveRegistry,
    onLinkClick: ((String) -> Unit)? = null,
    onFootnoteClick: ((String) -> Unit)? = null,
    latexMeasurer: LatexMeasurerState? = null,
    density: Density? = null,
    textMeasurer: TextMeasurer? = null,
    codeTheme: CodeTheme? = null,
): AnnotatedString = buildAnnotatedString {
    renderInlineChildren(
        nodes,
        theme,
        hostTextStyle,
        inlineContents,
        directiveRegistry,
        onLinkClick,
        onFootnoteClick,
        latexMeasurer,
        density,
        textMeasurer,
        codeTheme,
    )
}

internal fun AnnotatedString.Builder.renderInlineChildren(
    nodes: List<Node>,
    theme: MarkdownTheme,
    hostTextStyle: TextStyle,
    inlineContents: MutableMap<String, InlineContentEntry>,
    directiveRegistry: MarkdownDirectiveRegistry,
    onLinkClick: ((String) -> Unit)?,
    onFootnoteClick: ((String) -> Unit)?,
    latexMeasurer: LatexMeasurerState? = null,
    density: Density? = null,
    textMeasurer: TextMeasurer? = null,
    inlineCodeTheme: CodeTheme? = null,
) {
    for (node in nodes) {
        renderInlineNode(
            node,
            theme,
            hostTextStyle,
            inlineContents,
            directiveRegistry,
            onLinkClick,
            onFootnoteClick,
            latexMeasurer,
            density,
            textMeasurer,
            inlineCodeTheme,
        )
    }
}

internal fun AnnotatedString.Builder.renderInlineNode(
    node: Node,
    theme: MarkdownTheme,
    hostTextStyle: TextStyle,
    inlineContents: MutableMap<String, InlineContentEntry>,
    directiveRegistry: MarkdownDirectiveRegistry,
    onLinkClick: ((String) -> Unit)?,
    onFootnoteClick: ((String) -> Unit)?,
    latexMeasurer: LatexMeasurerState? = null,
    density: Density? = null,
    textMeasurer: TextMeasurer? = null,
    inlineCodeTheme: CodeTheme? = null,
) {
    when (node) {
        is Text -> append(node.literal)

        is SoftLineBreak -> append(" ")

        is HardLineBreak -> append("\n")

        is Emphasis -> {
            withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                renderInlineChildren(
                    node.children,
                    theme,
                    hostTextStyle,
                    inlineContents,
                    directiveRegistry,
                    onLinkClick,
                    onFootnoteClick,
                    latexMeasurer,
                    density,
                    textMeasurer,
                    inlineCodeTheme
                )
            }
        }

        is StrongEmphasis -> {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                renderInlineChildren(
                    node.children,
                    theme,
                    hostTextStyle,
                    inlineContents,
                    directiveRegistry,
                    onLinkClick,
                    onFootnoteClick,
                    latexMeasurer,
                    density,
                    textMeasurer,
                    inlineCodeTheme
                )
            }
        }

        is Strikethrough -> {
            withStyle(theme.strikethroughStyle) {
                renderInlineChildren(
                    node.children,
                    theme,
                    hostTextStyle,
                    inlineContents,
                    directiveRegistry,
                    onLinkClick,
                    onFootnoteClick,
                    latexMeasurer,
                    density,
                    textMeasurer,
                    inlineCodeTheme
                )
            }
        }

        is InlineCode -> {
            renderInlineCodeNode(
                node,
                theme,
                inlineContents,
                density,
                textMeasurer,
                inlineCodeTheme
            )
        }

        is Link -> {
            val linkAnnotation = LinkAnnotation.Clickable(
                tag = "link",
                styles = TextLinkStyles(
                    style = SpanStyle(
                        color = theme.linkColor,
                        textDecoration = TextDecoration.Underline,
                    ),
                ),
                linkInteractionListener = {
                    onLinkClick?.invoke(node.destination)
                },
            )
            withLink(linkAnnotation) {
                renderInlineChildren(
                    node.children,
                    theme,
                    hostTextStyle,
                    inlineContents,
                    directiveRegistry,
                    onLinkClick,
                    onFootnoteClick,
                    latexMeasurer,
                    density,
                    textMeasurer,
                    inlineCodeTheme
                )
            }
        }

        is Image -> {
            renderImageNode(node, inlineContents)
        }

        is Autolink -> {
            val linkAnnotation = LinkAnnotation.Clickable(
                tag = "link",
                styles = TextLinkStyles(
                    style = SpanStyle(
                        color = theme.linkColor,
                        textDecoration = TextDecoration.Underline,
                    ),
                ),
                linkInteractionListener = {
                    onLinkClick?.invoke(node.destination)
                },
            )
            withLink(linkAnnotation) {
                append(node.destination)
            }
        }

        is InlineHtml -> {
            withStyle(
                SpanStyle(
                    color = Color.Gray,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 14.sp
                )
            ) {
                append(node.literal)
            }
        }

        is HtmlEntity -> {
            append(node.resolved.ifEmpty { node.literal })
        }

        is EscapedChar -> {
            append(node.literal)
        }

        is FootnoteReference -> {
            val referenceText = "[${node.index}]"
            val linkAnnotation = LinkAnnotation.Clickable(
                tag = "footnote",
                styles = TextLinkStyles(
                    style = SpanStyle(
                        color = theme.linkColor,
                        fontSize = theme.footnoteStyle.fontSize,
                        baselineShift = BaselineShift.Superscript,
                    ),
                ),
                linkInteractionListener = {
                    onFootnoteClick?.invoke(node.label)
                },
            )
            withLink(linkAnnotation) {
                append(referenceText)
            }
        }

        is InlineMath -> {
            renderInlineMathNode(
                node,
                theme,
                hostTextStyle,
                inlineContents,
                latexMeasurer,
                density,
                textMeasurer
            )
        }

        is Highlight -> {
            withStyle(SpanStyle(background = theme.highlightColor)) {
                renderInlineChildren(
                    node.children,
                    theme,
                    hostTextStyle,
                    inlineContents,
                    directiveRegistry,
                    onLinkClick,
                    onFootnoteClick,
                    latexMeasurer,
                    density,
                    textMeasurer,
                    inlineCodeTheme
                )
            }
        }

        is Superscript -> {
            withStyle(
                theme.superscriptStyle.merge(
                    SpanStyle(baselineShift = BaselineShift.Superscript)
                )
            ) {
                renderInlineChildren(
                    node.children,
                    theme,
                    hostTextStyle,
                    inlineContents,
                    directiveRegistry,
                    onLinkClick,
                    onFootnoteClick,
                    latexMeasurer,
                    density,
                    textMeasurer,
                    inlineCodeTheme
                )
            }
        }

        is Subscript -> {
            withStyle(
                theme.subscriptStyle.merge(
                    SpanStyle(baselineShift = BaselineShift.Subscript)
                )
            ) {
                renderInlineChildren(
                    node.children,
                    theme,
                    hostTextStyle,
                    inlineContents,
                    directiveRegistry,
                    onLinkClick,
                    onFootnoteClick,
                    latexMeasurer,
                    density,
                    textMeasurer,
                    inlineCodeTheme
                )
            }
        }

        is InsertedText -> {
            withStyle(theme.insertedTextStyle) {
                renderInlineChildren(
                    node.children,
                    theme,
                    hostTextStyle,
                    inlineContents,
                    directiveRegistry,
                    onLinkClick,
                    onFootnoteClick,
                    latexMeasurer,
                    density,
                    textMeasurer,
                    inlineCodeTheme
                )
            }
        }

        is Emoji -> {
            // 优先显示 unicode（已映射），否则显示 literal
            append(node.unicode ?: node.literal.ifEmpty { ":${node.shortcode}:" })
        }

        is StyledText -> {
            // 从属性中提取样式信息
            val styleStr = node.style
            val spanStyle = if (styleStr != null) {
                parseCssStyleToSpanStyle(styleStr)
            } else {
                // 根据 CSS class 推断样式
                inferStyleFromClasses(node.cssClasses, theme)
            }
            if (spanStyle != null) {
                withStyle(spanStyle) {
                    renderInlineChildren(
                        node.children,
                        theme,
                        hostTextStyle,
                        inlineContents,
                        directiveRegistry,
                        onLinkClick,
                        onFootnoteClick,
                        latexMeasurer,
                        density,
                        textMeasurer,
                        inlineCodeTheme
                    )
                }
            } else {
                renderInlineChildren(
                    node.children,
                    theme,
                    hostTextStyle,
                    inlineContents,
                    directiveRegistry,
                    onLinkClick,
                    onFootnoteClick,
                    latexMeasurer,
                    density,
                    textMeasurer,
                    inlineCodeTheme
                )
            }
        }

        is Abbreviation -> {
            // embed fullText as annotation so consumers can show tooltip on hover/click
            if (node.fullText.isNotEmpty()) {
                pushStringAnnotation(tag = "abbreviation", annotation = node.fullText)
                withStyle(theme.abbreviationStyle) {
                    append(node.abbreviation)
                }
                pop()
            } else {
                withStyle(theme.abbreviationStyle) {
                    append(node.abbreviation)
                }
            }
        }

        is KeyboardInput -> {
            // 渲染键盘按键：等宽字体 + 背景
            withStyle(theme.kbdStyle) {
                append(node.literal)
            }
        }

        is CitationReference -> {
            val linkAnnotation = LinkAnnotation.Clickable(
                tag = "citation",
                styles = TextLinkStyles(
                    style = SpanStyle(
                        color = theme.linkColor,
                        fontSize = theme.footnoteStyle.fontSize,
                        baselineShift = BaselineShift.Superscript,
                    ),
                ),
                linkInteractionListener = {
                    // 引用点击暂不处理，可扩展
                },
            )
            withLink(linkAnnotation) {
                append("[${node.key}]")
            }
        }

        is Spoiler -> {
            renderSpoilerNode(
                node,
                theme,
                hostTextStyle,
                inlineContents,
                directiveRegistry,
                onLinkClick,
                onFootnoteClick,
                latexMeasurer,
                density,
                textMeasurer,
                inlineCodeTheme,
            )
        }

        is DirectiveInline -> {
            renderDirectiveInlineNode(node, theme, inlineContents, directiveRegistry)
        }

        is WikiLink -> {
            val linkAnnotation = LinkAnnotation.Clickable(
                tag = "wikilink",
                styles = TextLinkStyles(
                    style = SpanStyle(
                        color = theme.linkColor,
                        textDecoration = TextDecoration.Underline,
                    ),
                ),
                linkInteractionListener = {
                    onLinkClick?.invoke(node.target)
                },
            )
            withLink(linkAnnotation) {
                append(node.label ?: node.target)
            }
        }

        is RubyText -> {
            renderRubyTextNode(node, theme, inlineContents)
        }

        else -> {
            if (node is ContainerNode) {
                renderInlineChildren(
                    node.children,
                    theme,
                    hostTextStyle,
                    inlineContents,
                    directiveRegistry,
                    onLinkClick,
                    onFootnoteClick,
                    latexMeasurer,
                    density,
                    textMeasurer,
                    inlineCodeTheme
                )
            }
        }
    }
}
