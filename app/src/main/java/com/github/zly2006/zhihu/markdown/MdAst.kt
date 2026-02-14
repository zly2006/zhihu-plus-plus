package com.github.zly2006.zhihu.markdown

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import com.github.zly2006.zhihu.NavDestination
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.resolveContent
import com.github.zly2006.zhihu.ui.components.OpenImageDislog
import com.github.zly2006.zhihu.util.extractImageUrl
import com.github.zly2006.zhihu.util.luoTianYiUrlLauncher
import com.github.zly2006.zhihu.util.saveImageToGallery
import com.github.zly2006.zhihu.util.shareImage
import kotlinx.coroutines.launch
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

sealed interface AstData

class MdAstLinks(
    val parent: MdAst? = null,
    val prev: MdAst? = null,
    val next: MdAst? = null,
    val firstChild: MdAst? = null,
    val lastChild: MdAst? = null,
)

class MdAst(
    val data: AstData,
    val links: MdAstLinks,
)

sealed interface InlineAstData

class AstSpan(
    val text: String,
    val textStyle: TextStyle,
) : InlineAstData

class AstHeader(
    val text: List<InlineAstData>,
    val level: Int,
) : AstData

class AstParagraph(
    val inlines: MutableList<InlineAstData>,
) : AstData

class AstBlockquote(
    val children: List<MdAst>,
) : AstData

class AstCodeBlock(
    val code: String,
    val language: String?,
) : AstData

class AstListItem(
    val children: List<MdAst>,
) : AstData

class AstList(
    val items: List<AstListItem>,
    val ordered: Boolean,
) : AstData

object AstHorizontalRule : AstData

class AstImage(
    val url: String,
    val altText: String,
) : AstData

class AstLink(
    val url: String,
    val title: InlineAstData,
) : InlineAstData

class AstCodeSpan(
    val code: String,
) : InlineAstData

class AstLineBreak : InlineAstData

class AstInlineMath(
    val math: String,
) : InlineAstData {
    override fun toString(): String = "AstInlineMath(math='$math')"
}

class AstDisplayMath(
    val math: String,
) : AstData

class AstTable(
    val headers: List<List<InlineAstData>>,
    val rows: List<List<List<InlineAstData>>>,
) : AstData

class AstTableAlignments(
    val alignments: List<Alignment>,
) : AstData {
    enum class Alignment {
        LEFT,
        CENTER,
        RIGHT,
        NONE,
    }
}

class MarkdownRenderContext(
    val requestedImages: MutableSet<AstImage> = mutableSetOf(),
    val requestedFormulas: MutableSet<AstInlineMath> = mutableSetOf(),
    val requestedFormulaBlocks: MutableSet<AstDisplayMath> = mutableSetOf(),
)

@Composable
fun AnnotatedString.Builder.RenderInline(
    ast: InlineAstData,
    renderContext: MarkdownRenderContext,
    onNavigate: (NavDestination) -> Unit,
) {
    val context = LocalContext.current
    when (val d = ast) {
        is AstSpan -> {
            pushStyle(d.textStyle.toSpanStyle())
            append(d.text)
            pop()
        }

        is AstLink -> {
            withLink(
                LinkAnnotation.Clickable(
                    d.url,
                    TextLinkStyles(style = SpanStyle(color = Color(0xff66CCFF))),
                ) {
                    resolveContent(d.url.toUri())?.let(onNavigate)
                        ?: luoTianYiUrlLauncher(context, d.url.toUri())
                },
            ) {
                RenderInline(d.title, renderContext, onNavigate)
            }
        }

        is AstCodeSpan -> {
            pushStyle(
                TextStyle(
                    fontFamily = FontFamily.Monospace,
                    background = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f),
                ).toSpanStyle(),
            )
            append(d.code)
            pop()
        }

        is AstLineBreak -> {
            append("\n")
        }

        is AstInlineMath -> {
            renderContext.requestedFormulas.add(d)
            appendInlineContent(d.toString(), d.math)
        }
    }
}

@Composable
fun MdAst.Render(
    renderContext: MarkdownRenderContext,
    onNavigate: (NavDestination) -> Unit,
) {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences("webview_settings", android.content.Context.MODE_PRIVATE) }
    val fontSizePercent = remember { preferences.getInt("webviewFontSize", 100) }
    val lineHeightPercent = remember { preferences.getInt("webviewLineHeight", 160) }

    val baseStyle = MaterialTheme.typography.bodyLarge
    val fontSizeMultiplier = fontSizePercent / 100f
    val lineHeightMultiplier = lineHeightPercent / 100f

    when (val d = data) {
        is AstHeader -> {
            val headerStyle = when (d.level) {
                1 -> MaterialTheme.typography.headlineLarge
                2 -> MaterialTheme.typography.headlineMedium
                3 -> MaterialTheme.typography.headlineSmall
                4 -> MaterialTheme.typography.titleLarge
                5 -> MaterialTheme.typography.titleMedium
                else -> MaterialTheme.typography.titleSmall
            }
            Text(
                text = buildAnnotatedString {
                    d.text.forEach {
                        RenderInline(it, renderContext, onNavigate)
                    }
                },
                style = headerStyle.copy(
                    fontSize = baseStyle.fontSize * fontSizeMultiplier * (headerStyle.fontSize.value / baseStyle.fontSize.value),
                    lineHeight = baseStyle.fontSize * fontSizeMultiplier * lineHeightMultiplier,
                ),
            )
        }

        is AstParagraph -> {
            Text(
                text = buildAnnotatedString {
                    d.inlines.forEach {
                        RenderInline(it, renderContext, onNavigate)
                    }
                },
                style = baseStyle.copy(
                    fontSize = baseStyle.fontSize * fontSizeMultiplier,
                    lineHeight = baseStyle.fontSize * fontSizeMultiplier * lineHeightMultiplier,
                ),
            )
        }

        is AstBlockquote -> {
            androidx.compose.ui.layout.Layout(
                content = {
                    Box(
                        Modifier
                            .padding(start = 6.dp, end = 6.dp)
                            .width(3.dp)
                            .background(
                                MaterialTheme.colorScheme.onBackground.copy(alpha = 0.25f),
                                RoundedCornerShape(50),
                            ),
                    )
                    Column(
                        modifier = Modifier.padding(start = 4.dp),
                    ) {
                        d.children.forEach {
                            it.Render(renderContext, onNavigate)
                        }
                    }
                },
            ) { measurables, constraints ->
                val gutterMeasurable = measurables[0]
                val contentMeasurable = measurables[1]

                // Get gutter's intrinsic width
                val gutterWidth = gutterMeasurable.minIntrinsicWidth(constraints.maxHeight)

                // Measure content with remaining width
                val contentConstraints = constraints.copy(
                    maxWidth = if (constraints.hasBoundedWidth) {
                        (constraints.maxWidth - gutterWidth).coerceAtLeast(0)
                    } else {
                        constraints.maxWidth
                    },
                )
                val contentPlaceable = contentMeasurable.measure(contentConstraints)

                val layoutWidth = contentPlaceable.width + gutterWidth
                val layoutHeight = contentPlaceable.height

                // Measure gutter to match content height
                val gutterConstraints = constraints.copy(
                    maxWidth = gutterWidth,
                    minHeight = layoutHeight,
                    maxHeight = layoutHeight,
                )
                val gutterPlaceable = gutterMeasurable.measure(gutterConstraints)

                layout(layoutWidth, layoutHeight) {
                    gutterPlaceable.place(0, 0)
                    contentPlaceable.place(gutterWidth, 0)
                }
            }
        }

        is AstCodeBlock -> {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.1f))
                    .horizontalScroll(rememberScrollState()),
            ) {
                Text(
                    text = d.code,
                    style = baseStyle.copy(
                        fontFamily = FontFamily.Monospace,
                        fontSize = baseStyle.fontSize * fontSizeMultiplier,
                        lineHeight = baseStyle.fontSize * fontSizeMultiplier * lineHeightMultiplier,
                    ),
                )
            }
        }
        is AstDisplayMath -> {
            renderContext.requestedFormulaBlocks.add(d)
            Text(
                text = d.math,
                style = baseStyle.copy(
                    fontSize = baseStyle.fontSize * fontSizeMultiplier,
                    lineHeight = baseStyle.fontSize * fontSizeMultiplier * lineHeightMultiplier,
                ),
            )
        }
        AstHorizontalRule -> {
            Box(
                Modifier
                    .padding(vertical = 8.dp)
                    .height(1.5.dp)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)),
            )
        }
        is AstImage -> {
            var expanded by remember { mutableStateOf(false) }
            var pressOffset by remember { mutableStateOf(DpOffset.Zero) }
            val density = LocalDensity.current
            val view = LocalView.current
            val coroutineScope = rememberCoroutineScope()
            val httpClient = AccountData.httpClient(context)

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                AsyncImage(
                    model = d.url,
                    contentDescription = d.altText,
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .pointerInput(Unit) {
                            detectTapGestures(
                                onTap = {
                                    val dialog = OpenImageDislog(context, httpClient, d.url)
                                    dialog.show()
                                },
                                onLongPress = { offset ->
                                    view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                    pressOffset = with(density) {
                                        DpOffset(offset.x.toDp(), offset.y.toDp() - 20.dp)
                                    }
                                    expanded = true
                                },
                            )
                        },
                )

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    offset = pressOffset,
                ) {
                    DropdownMenuItem(
                        text = { Text("查看图片") },
                        onClick = {
                            expanded = false
                            val dialog = OpenImageDislog(context, httpClient, d.url)
                            dialog.show()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("在浏览器中打开") },
                        onClick = {
                            expanded = false
                            luoTianYiUrlLauncher(context, d.url.toUri())
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("保存图片") },
                        onClick = {
                            expanded = false
                            coroutineScope.launch {
                                saveImageToGallery(context, httpClient, d.url)
                            }
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("分享图片") },
                        onClick = {
                            expanded = false
                            coroutineScope.launch {
                                shareImage(context, httpClient, d.url)
                            }
                        },
                    )
                }
            }
        }
        is AstList -> {}
        is AstListItem -> {}
        is AstTable -> {}
        is AstTableAlignments -> {}
    }
}

// HTML 转 MdAst 的辅助函数
fun htmlToMdAst(html: String): List<MdAst> {
    val doc = org.jsoup.Jsoup.parse(html)
    return doc.body().childNodes().convertNodesToAst()
}

private fun List<Node>.convertNodesToAst(): List<MdAst> {
    val ret = mutableListOf<MdAst>()

    for (node in this) {
        if (node is TextNode) {
            if (ret.lastOrNull()?.data !is AstParagraph) {
                ret.add(
                    MdAst(
                        AstParagraph(
                            mutableListOf(
                                AstSpan(
                                    node.text().trim(),
                                    TextStyle.Default,
                                ),
                            ),
                        ),
                        MdAstLinks(),
                    ),
                )
                continue
            }
            val lastParagraph = ret.last().data as AstParagraph
            lastParagraph.inlines.add(
                AstSpan(
                    node.text().trim(),
                    TextStyle.Default,
                ),
            )
        } else if (node is Element) {
            val paraAst = convertElementToAst(node)
            if (paraAst != null) {
                ret.add(paraAst)
            } else {
                if (ret.lastOrNull()?.data !is AstParagraph) {
                    ret.add(
                        MdAst(
                            AstParagraph(mutableListOf()),
                            MdAstLinks(),
                        ),
                    )
                }
                val lastParagraph = ret.last().data as AstParagraph
                // parse inline ast
                extractInlineElement(node, lastParagraph.inlines)
            }
        }
    }

    return ret
}

private fun convertElementToAst(element: Element): MdAst? = when (element.tagName().lowercase()) {
    "h1", "h2", "h3", "h4", "h5", "h6" -> {
        val level = element.tagName()[1].digitToInt()
        val inlineData = extractInlineElements(element)
        MdAst(AstHeader(inlineData, level), MdAstLinks())
    }

    "p" -> {
        val inlines = extractInlineElements(element).toMutableList()
        MdAst(AstParagraph(inlines), MdAstLinks())
    }

    "blockquote" -> {
        val children = element.childNodes().convertNodesToAst()
        MdAst(AstBlockquote(children), MdAstLinks())
    }

    "pre" -> {
        val code = element.selectFirst("code")?.text() ?: element.text()
        val lang = element
            .selectFirst("code")
            ?.attr("class")
            ?.removePrefix("language-")
            ?.trim()
        MdAst(AstCodeBlock(code, lang), MdAstLinks())
    }

    "ul" -> {
        val items = element.select("> li").map { li ->
            val children = li.childNodes().convertNodesToAst()
            AstListItem(children.ifEmpty { listOf(MdAst(AstParagraph(extractInlineElements(li).toMutableList()), MdAstLinks())) })
        }
        MdAst(AstList(items, ordered = false), MdAstLinks())
    }

    "ol" -> {
        val items = element.select("> li").map { li ->
            val children = li.childNodes().convertNodesToAst()
            AstListItem(children.ifEmpty { mutableListOf(MdAst(AstParagraph(extractInlineElements(li).toMutableList()), MdAstLinks())) })
        }
        MdAst(AstList(items, ordered = true), MdAstLinks())
    }

    "hr" -> {
        MdAst(AstHorizontalRule, MdAstLinks())
    }

    "img" -> {
        val src = extractImageUrl(element)
        val alt = element.attr("alt").ifEmpty { "image" }
        if (src != null) {
            MdAst(AstImage(src, alt), MdAstLinks())
        } else {
            null
        }
    }

    "figure" -> {
        element.selectFirst("img")?.let { img ->
            val src = extractImageUrl(img)
            val alt = img.attr("alt").ifEmpty { "image" }
            if (src != null) {
                MdAst(AstImage(src, alt), MdAstLinks())
            } else {
                null
            }
        }
    }

    "div", "span" -> {
        if (element.attr("class").contains("highlight")) {
            element.selectFirst("code")?.let { code ->
                val lang = code.attr("class").removePrefix("language-").trim()
                MdAst(AstCodeBlock(code.text(), lang), MdAstLinks())
            }
        } else {
            val inlines = extractInlineElements(element).toMutableList()
            if (inlines.isNotEmpty()) {
                MdAst(AstParagraph(inlines), MdAstLinks())
            } else {
                null
            }
        }
    }

    else -> null
}

private fun extractInlineContent(element: Element): InlineAstData {
    val inlines = extractInlineElements(element)
    return if (inlines.size == 1) inlines[0] else AstSpan(element.text(), TextStyle.Default)
}

private fun extractInlineElements(element: Element): List<InlineAstData> {
    val result = mutableListOf<InlineAstData>()

    for (node in element.childNodes()) {
        when (node) {
            is TextNode -> {
                val text = node.text()
                if (text.isNotBlank()) {
                    result.add(AstSpan(text, TextStyle.Default))
                }
            }

            is Element -> {
                extractInlineElement(node, result)
            }
        }
    }

    return result
}

private fun extractInlineElement(
    node: Element,
    result: MutableList<InlineAstData>,
) {
    when (node.tagName().lowercase()) {
        "strong", "b" -> {
            result.add(
                AstSpan(
                    node.text(),
                    TextStyle(fontWeight = FontWeight.Bold),
                ),
            )
        }

        "em", "i" -> {
            result.add(
                AstSpan(
                    node.text(),
                    TextStyle(fontStyle = FontStyle.Italic),
                ),
            )
        }

        "code" -> {
            result.add(AstCodeSpan(node.text()))
        }

        "a" -> {
            val href = node.attr("href")
            val url = if (href.contains("link.zhihu.com")) {
                href.toUri().getQueryParameter("target") ?: href
            } else {
                href
            }
            val title = extractInlineContent(node)
            result.add(AstLink(url, title))
        }

        "br" -> {
            result.add(AstLineBreak())
        }

        else -> {
            val text = node.text()
            if (text.isNotBlank()) {
                result.add(AstSpan(text, TextStyle.Default))
            }
        }
    }
}
