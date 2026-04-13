package com.github.zly2006.zhihu.markdown

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.ui.components.OpenImageDislog
import com.github.zly2006.zhihu.util.extractImageUrl
import com.github.zly2006.zhihu.util.luoTianYiUrlLauncher
import com.github.zly2006.zhihu.util.saveImageToGallery
import com.github.zly2006.zhihu.util.shareImage
import com.hrm.markdown.parser.ast.BlockQuote
import com.hrm.markdown.parser.ast.ContainerNode
import com.hrm.markdown.parser.ast.Document
import com.hrm.markdown.parser.ast.Emphasis
import com.hrm.markdown.parser.ast.FencedCodeBlock
import com.hrm.markdown.parser.ast.Figure
import com.hrm.markdown.parser.ast.HardLineBreak
import com.hrm.markdown.parser.ast.Heading
import com.hrm.markdown.parser.ast.Highlight
import com.hrm.markdown.parser.ast.Image
import com.hrm.markdown.parser.ast.InlineCode
import com.hrm.markdown.parser.ast.InlineMath
import com.hrm.markdown.parser.ast.KeyboardInput
import com.hrm.markdown.parser.ast.Link
import com.hrm.markdown.parser.ast.ListBlock
import com.hrm.markdown.parser.ast.ListItem
import com.hrm.markdown.parser.ast.MathBlock
import com.hrm.markdown.parser.ast.Paragraph
import com.hrm.markdown.parser.ast.Strikethrough
import com.hrm.markdown.parser.ast.StrongEmphasis
import com.hrm.markdown.parser.ast.Subscript
import com.hrm.markdown.parser.ast.Superscript
import com.hrm.markdown.parser.ast.Table
import com.hrm.markdown.parser.ast.TableBody
import com.hrm.markdown.parser.ast.TableCell
import com.hrm.markdown.parser.ast.TableHead
import com.hrm.markdown.parser.ast.TableRow
import com.hrm.markdown.parser.ast.ThematicBreak
import com.hrm.markdown.renderer.Markdown
import com.hrm.markdown.renderer.MarkdownImageData
import io.ktor.http.Url
import kotlinx.coroutines.launch
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode
import com.hrm.markdown.parser.ast.Node as MarkdownNode
import org.jsoup.nodes.Node as HtmlNode

val LocalMarkdownOnInlineMathPositioned = compositionLocalOf<((InlineMath, Rect) -> Unit)?> { null }

@Composable
fun RenderImage(
    data: MarkdownImageData,
    modifier: Modifier,
) {
    val context = LocalContext.current
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
            model = data.url,
            contentDescription = data.altText,
            modifier = modifier
                .fillMaxWidth(0.8f)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            val dialog = OpenImageDislog(context, httpClient, data.url)
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
                    val dialog = OpenImageDislog(context, httpClient, data.url)
                    dialog.show()
                },
            )
            DropdownMenuItem(
                text = { Text("在浏览器中打开") },
                onClick = {
                    expanded = false
                    luoTianYiUrlLauncher(context, data.url.toUri())
                },
            )
            DropdownMenuItem(
                text = { Text("保存图片") },
                onClick = {
                    expanded = false
                    coroutineScope.launch {
                        saveImageToGallery(context, httpClient, data.url)
                    }
                },
            )
            DropdownMenuItem(
                text = { Text("分享图片") },
                onClick = {
                    expanded = false
                    coroutineScope.launch {
                        shareImage(context, httpClient, data.url)
                    }
                },
            )
        }
    }
}

@Composable
fun RenderMarkdown(
    html: String,
    modifier: Modifier = Modifier,
    selectable: Boolean = false,
) {
    val document = remember(html) { htmlToMdAst(html) }

    if (selectable) {
        SelectionContainer(modifier = modifier) {
            Markdown(
                document = document,
                imageContent = ::RenderImage,
                enableScroll = false,
            )
        }
    } else {
        Markdown(
            document = document,
            modifier = modifier,
            imageContent = ::RenderImage,
            enableScroll = false,
        )
    }
}

fun htmlToMdAst(html: String): Document {
    val document = Document()
    Jsoup
        .parse(html)
        .body()
        .childNodes()
        .appendBlocksTo(document)
    return document
}

private fun List<HtmlNode>.appendBlocksTo(parent: ContainerNode) {
    convertNodesToBlocks().forEach(parent::appendChild)
}

private fun List<HtmlNode>.convertNodesToBlocks(): List<MarkdownNode> {
    val blocks = mutableListOf<MarkdownNode>()
    var currentParagraph: Paragraph? = null

    fun paragraph(): Paragraph = currentParagraph ?: Paragraph().also {
        blocks.add(it)
        currentParagraph = it
    }

    for (node in this) {
        when (node) {
            is TextNode -> {
                val text = node.text().trim()
                if (text.isNotEmpty()) {
                    paragraph().appendChild(com.hrm.markdown.parser.ast.Text(text))
                }
            }

            is Element -> {
                val blockNode = convertElementToBlock(node)
                if (blockNode != null) {
                    blocks.add(blockNode)
                    currentParagraph = null
                } else {
                    val inlineNodes = extractInlineNode(node)
                    if (inlineNodes.isNotEmpty()) {
                        paragraph().appendChildren(inlineNodes)
                    }
                }
            }
        }
    }

    return blocks
}

private fun convertElementToBlock(element: Element): MarkdownNode? = when (element.tagName().lowercase()) {
    "h1", "h2", "h3", "h4", "h5", "h6" -> Heading(level = element.tagName()[1].digitToInt()).apply {
        appendChildren(extractInlineChildren(element))
    }

    "p" -> Paragraph().apply {
        appendChildren(extractInlineChildren(element))
    }

    "blockquote" -> BlockQuote().apply {
        element.childNodes().appendBlocksTo(this)
    }

    "pre" -> createCodeBlock(element)

    "ul" -> createListBlock(element, ordered = false)

    "ol" -> createListBlock(element, ordered = true)

    "hr" -> ThematicBreak()

    "img" -> createBlockImage(element)

    "figure" -> createFigureBlock(element)

    "table" -> createTableBlock(element)

    "div", "span" -> {
        if (element.classNames().any { it.contains("highlight") }) {
            createCodeBlock(element)
        } else {
            extractInlineChildren(element).takeIf { it.isNotEmpty() }?.let { inlines ->
                Paragraph().apply { appendChildren(inlines) }
            }
        }
    }

    else -> null
}

private fun createCodeBlock(element: Element): FencedCodeBlock {
    val codeElement = element.selectFirst("code")
    val language = codeElement
        ?.classNames()
        ?.firstOrNull { it.startsWith("language-") }
        ?.removePrefix("language-")
        .orEmpty()

    return FencedCodeBlock(
        info = language,
        language = language,
        literal = codeElement?.text() ?: element.text(),
    )
}

private fun createListBlock(
    element: Element,
    ordered: Boolean,
): ListBlock = ListBlock(
    ordered = ordered,
    startNumber = element.attr("start").toIntOrNull() ?: 1,
).apply {
    element.select("> li").forEach { listItemElement ->
        appendChild(
            ListItem().apply {
                val children = listItemElement.childNodes().convertNodesToBlocks()
                if (children.isEmpty()) {
                    appendChild(
                        Paragraph().apply {
                            appendChildren(extractInlineChildren(listItemElement))
                        },
                    )
                } else {
                    appendChildren(children)
                }
            },
        )
    }
}

private fun createBlockImage(element: Element): MarkdownNode? {
    element.attr("data-formula").takeIf { it.isNotBlank() }?.let { formula ->
        return MathBlock(formula)
    }

    val src = extractImageUrl(element) ?: return null
    val caption = element.attr("alt").ifBlank { "image" }
    return Figure(
        imageUrl = src,
        caption = caption,
        imageWidth = element.attr("width").toIntOrNull(),
        imageHeight = element.attr("height").toIntOrNull(),
    )
}

private fun createFigureBlock(element: Element): MarkdownNode? {
    element.selectFirst("img[data-formula]")?.attr("data-formula")?.takeIf { it.isNotBlank() }?.let { formula ->
        return MathBlock(formula)
    }

    element.selectFirst("img")?.let { image ->
        val src = extractImageUrl(image) ?: return@let null
        val caption = element.selectFirst("figcaption")?.text()?.ifBlank { null } ?: ""
        return Figure(
            imageUrl = src,
            caption = caption,
            imageWidth = image.attr("width").toIntOrNull(),
            imageHeight = image.attr("height").toIntOrNull(),
        )
    }

    if (element.classNames().any { it.contains("highlight") }) {
        return createCodeBlock(element)
    }

    val inlines = extractInlineChildren(element)
    return inlines.takeIf { it.isNotEmpty() }?.let {
        Paragraph().apply { appendChildren(it) }
    }
}

private fun createTableBlock(element: Element): Table = Table().apply {
    val directRows = element.select("> tr")
    val headRows = element.select("> thead > tr")
    val bodyRows = element.select("> tbody > tr")

    if (headRows.isNotEmpty()) {
        appendChild(
            TableHead().apply {
                headRows.forEach { appendChild(createTableRow(it, isHeader = true)) }
            },
        )
    }

    val rowsForBody = when {
        bodyRows.isNotEmpty() -> bodyRows
        headRows.isEmpty() -> directRows
        else -> emptyList()
    }
    if (rowsForBody.isNotEmpty()) {
        appendChild(
            TableBody().apply {
                rowsForBody.forEach { appendChild(createTableRow(it, isHeader = false)) }
            },
        )
    }

    val referenceRow = headRows.firstOrNull() ?: rowsForBody.firstOrNull()
    columnAlignments = referenceRow
        ?.select("> th, > td")
        ?.map { it.toAlignment() }
        .orEmpty()
}

private fun createTableRow(
    row: Element,
    isHeader: Boolean,
): TableRow = TableRow().apply {
    row.select("> th, > td").forEach { cell ->
        appendChild(
            TableCell(
                alignment = cell.toAlignment(),
                isHeader = isHeader || cell.tagName().equals("th", ignoreCase = true),
            ).apply {
                appendChildren(extractInlineChildren(cell))
            },
        )
    }
}

private fun Element.toAlignment(): Table.Alignment = when (attr("align").lowercase()) {
    "left" -> Table.Alignment.LEFT
    "center" -> Table.Alignment.CENTER
    "right" -> Table.Alignment.RIGHT
    else -> Table.Alignment.NONE
}

private fun extractInlineChildren(element: Element): List<MarkdownNode> = element.childNodes().flatMap(::extractInlineNode)

private fun extractInlineNode(node: HtmlNode): List<MarkdownNode> = when (node) {
    is TextNode -> {
        val text = node.text()
        if (text.isBlank()) {
            emptyList()
        } else {
            listOf(com.hrm.markdown.parser.ast.Text(text))
        }
    }

    is Element -> when (node.tagName().lowercase()) {
        "strong", "b" -> listOf(StrongEmphasis().apply { appendChildren(extractInlineChildren(node)) })

        "em", "i" -> listOf(Emphasis().apply { appendChildren(extractInlineChildren(node)) })

        "del", "s", "strike" -> listOf(Strikethrough().apply { appendChildren(extractInlineChildren(node)) })

        "mark" -> listOf(Highlight().apply { appendChildren(extractInlineChildren(node)) })

        "sub" -> listOf(Subscript().apply { appendChildren(extractInlineChildren(node)) })

        "sup" -> listOf(Superscript().apply { appendChildren(extractInlineChildren(node)) })

        "kbd" -> listOf(KeyboardInput(node.text()))

        "code" -> listOf(InlineCode(node.text()))

        "a" -> {
            val href = node.attr("href")
            val destination = if (href.contains("link.zhihu.com")) {
                href.toUri().getQueryParameter("target") ?: href
            } else {
                href
            }
            listOf(
                Link(destination = destination).apply {
                    appendChildren(extractInlineChildren(node).ifEmpty { listOf(com.hrm.markdown.parser.ast.Text(node.text())) })
                },
            )
        }

        "br" -> listOf(HardLineBreak())

        "img" -> {
            val src = node.attr("src")
            if (src.startsWith("https://www.zhihu.com/equation?tex=")) {
                listOf(InlineMath(Url(src).parameters["tex"].orEmpty()))
            } else {
                extractImageUrl(node)
                    ?.let { url ->
                        listOf(
                            Image(
                                destination = url,
                                title = node.attr("title").ifBlank { null },
                                imageWidth = node.attr("width").toIntOrNull(),
                                imageHeight = node.attr("height").toIntOrNull(),
                            ).apply {
                                node.attr("alt").takeIf { it.isNotBlank() }?.let { appendChild(com.hrm.markdown.parser.ast.Text(it)) }
                            },
                        )
                    }.orEmpty()
            }
        }

        else -> {
            val children = extractInlineChildren(node)
            if (children.isNotEmpty()) {
                children
            } else {
                node
                    .text()
                    .takeIf { it.isNotBlank() }
                    ?.let { listOf(com.hrm.markdown.parser.ast.Text(it)) }
                    .orEmpty()
            }
        }
    }

    else -> emptyList()
}

private fun ContainerNode.appendChildren(children: List<MarkdownNode>) {
    children.forEach(::appendChild)
}
