package com.github.zly2006.zhihu.util

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.MediaStore.MediaColumns
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.em
import androidx.core.net.toUri
import com.github.zly2006.zhihu.NavDestination
import com.github.zly2006.zhihu.resolveContent
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.readRawBytes
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode

/**
 * 从 img 元素提取最高质量的图片URL（无水印原图）
 * 按优先级尝试不同的属性
 */
fun extractImageUrl(imgElement: Element): String? = imgElement.attr("data-original-token").takeIf { it.startsWith("v2-") }?.let {
    "https://pic1.zhimg.com/$it"
}
    ?: imgElement.attr("data-original").takeIf { it.isNotBlank() }
    ?: imgElement.attr("data-default-watermark-src").takeIf { it.isNotBlank() }
    ?: imgElement.attr("data-actualsrc").takeIf { it.isNotBlank() }
    ?: imgElement.attr("data-thumbnail").takeIf { it.isNotBlank() }
    ?: imgElement.attr("src").takeIf { it.isNotBlank() }

/**
 * 内容块类型，用于Column渲染
 */
sealed class ContentBlock {
    /** 文本块，包含富文本和emoji */
    data class TextBlock(
        val text: AnnotatedString,
        // 行内公式或emoji等组件使用的key集合
        val componentUsed: Set<String> = emptySet(),
    ) : ContentBlock()

    /** 图片块 */
    data class ImageBlock(
        val url: String,
        val isGif: Boolean = false,
    ) : ContentBlock()

    /** 公式块 */
    data class FormulaBlock(
        val imageUrl: String,
        val formula: String,
    ) : ContentBlock()

    /** 标题块 */
    data class HeaderBlock(
        val text: AnnotatedString,
        val level: Int,
        val componentUsed: Set<String> = emptySet(),
    ) : ContentBlock()

    /** 列表块 */
    data class ListBlock(
        val items: List<ListItem>,
        val isOrdered: Boolean,
    ) : ContentBlock() {
        data class ListItem(
            val text: AnnotatedString,
            val componentUsed: Set<String> = emptySet(),
        )
    }

    /** 引用块 */
    data class QuoteBlock(
        val text: AnnotatedString,
        val componentUsed: Set<String> = emptySet(),
    ) : ContentBlock()

    /** 代码块 */
    data class CodeBlock(
        val code: String,
        val language: String = "",
    ) : ContentBlock()

    /** 分割线 */
    data object DividerBlock : ContentBlock()
}

/**
 * 处理文本节点中的emoji，提取emoji占位符并添加到AnnotatedString
 */
fun AnnotatedString.Builder.processTextWithEmoji(
    text: String,
    componentUsed: MutableSet<String>?,
) {
    var buffer = StringBuilder()
    var emojiBuffer = StringBuilder()
    var isEmoji = false

    for (ch in text) {
        if (ch == '[') {
            if (buffer.isNotEmpty()) {
                append(buffer.toString())
                buffer = StringBuilder()
            }
            isEmoji = true
            emojiBuffer.append(ch)
        } else if (ch == ']') {
            if (isEmoji) {
                emojiBuffer.append(ch)
                val placeholder = emojiBuffer.toString()
                val emojiPath = EmojiManager.getEmojiPath(placeholder)
                if (emojiPath != null) {
                    // 使用emoji文件名作为key
                    val emojiFileName = emojiPath.substringAfterLast('/')
                    val emojiKey = "emoji_$emojiFileName"
                    appendInlineContent(emojiKey, placeholder)
                    componentUsed?.add(emojiKey)
                } else {
                    append(placeholder)
                }
                emojiBuffer = StringBuilder()
                isEmoji = false
            } else {
                buffer.append(ch)
            }
        } else {
            if (isEmoji) {
                emojiBuffer.append(ch)
            } else {
                buffer.append(ch)
            }
        }
    }

    // 处理剩余的buffer内容
    if (buffer.isNotEmpty()) {
        append(buffer.toString())
    }
    // 如果还有未完成的emoji buffer（没有找到结束的']'），也添加进去
    if (isEmoji && emojiBuffer.isNotEmpty()) {
        append(emojiBuffer.toString())
    }
}

/**
 * 通用的HTML节点DFS处理函数
 * 用于评论区的简单文本处理
 */
fun AnnotatedString.Builder.dfsSimple(
    node: Node,
    onNavigate: (NavDestination) -> Unit,
    context: Context,
    componentUsed: MutableSet<String>? = null,
) {
    when (node) {
        is Element -> {
            when (node.tagName()) {
                "br" -> {
                    append("\n")
                }

                "a" -> {
                    val href = node.attr("href")
                    val linkText = node.text()
                    if (linkText.isNotEmpty()) {
                        withLink(
                            LinkAnnotation.Clickable(
                                href,
                                TextLinkStyles(style = SpanStyle(color = Color(0xff66CCFF))),
                            ) {
                                resolveContent(href.toUri())?.let(onNavigate)
                                    ?: luoTianYiUrlLauncher(context, href.toUri())
                            },
                        ) {
                            append(linkText)
                        }
                    }
                }

                else -> {
                    node.childNodes().forEach { dfsSimple(it, onNavigate, context, componentUsed) }
                }
            }
        }

        is TextNode -> {
            processTextWithEmoji(node.text(), componentUsed)
        }

        else -> {
            append(node.outerHtml())
        }
    }
}

/**
 * 创建emoji的inline content映射
 */
fun createEmojiInlineContent(emojiKeys: Set<String>): Map<String, InlineTextContent> {
    return emojiKeys
        .filter { it.startsWith("emoji_") }
        .mapNotNull { emojiKey ->
            val fileName = emojiKey.removePrefix("emoji_")
            val path = EmojiManager.getEmojiPathByFileName(fileName) ?: return@mapNotNull null

            emojiKey to InlineTextContent(
                placeholder = Placeholder(
                    width = 1.3.em,
                    height = 1.3.em,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
                ),
            ) {
                val bitmap = android.graphics.BitmapFactory.decodeFile(path)
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = emojiKey,
                        modifier = Modifier,
                    )
                }
            }
        }.toMap()
}

/**
 * 保存图片到相册
 */
suspend fun saveImageToGallery(
    context: Context,
    httpClient: HttpClient,
    imageUrl: String,
) {
    try {
        val response = httpClient.get(imageUrl)
        val bytes = response.readRawBytes()
        val fileName = imageUrl.toUri().lastPathSegment ?: "downloaded_image.jpg"

        val contentValues = android.content.ContentValues().apply {
            put(MediaColumns.DISPLAY_NAME, fileName)
            put(MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES)
                put(MediaColumns.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media
                .getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val imageUri = resolver.insert(collection, contentValues)
        if (imageUri != null) {
            resolver.openOutputStream(imageUri).use { os ->
                os?.write(bytes)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaColumns.IS_PENDING, 0)
                resolver.update(imageUri, contentValues, null, null)
            }

            Toast
                .makeText(
                    context,
                    "图片已保存到相册",
                    Toast.LENGTH_SHORT,
                ).show()
        }
    } catch (e: Exception) {
        Toast
            .makeText(
                context,
                "保存失败: ${e.message}",
                Toast.LENGTH_SHORT,
            ).show()
    }
}

/**
 * 下载图片并保存到相册（不显示Toast）
 * @return 保存的图片 URI，如果失败则返回 null
 */
suspend fun downloadAndSaveImage(
    context: Context,
    httpClient: HttpClient,
    imageUrl: String,
    fileName: String,
    isPending: Boolean = false,
): android.net.Uri? {
    val response = httpClient.get(imageUrl)
    val bytes = response.readRawBytes()

    val contentValues = android.content.ContentValues().apply {
        put(MediaColumns.DISPLAY_NAME, fileName)
        put(MediaColumns.MIME_TYPE, "image/jpeg")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            if (isPending) put(MediaColumns.IS_PENDING, 1)
        }
    }

    val resolver = context.contentResolver
    val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Images.Media
            .getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    } else {
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    }

    val imageUri = resolver.insert(collection, contentValues)
    if (imageUri != null) {
        resolver.openOutputStream(imageUri).use { os ->
            os?.write(bytes)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && isPending) {
            contentValues.clear()
            contentValues.put(MediaColumns.IS_PENDING, 0)
            resolver.update(imageUri, contentValues, null, null)
        }
    }
    return imageUri
}

/**
 * 分享图片
 */
suspend fun shareImage(
    context: Context,
    httpClient: HttpClient,
    imageUrl: String,
) {
    try {
        val fileName = "share_${System.currentTimeMillis()}.jpg"
        val imageUri = downloadAndSaveImage(context, httpClient, imageUrl, fileName)
        if (imageUri != null) {
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, imageUri)
                type = "image/jpeg"
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "分享图片"))
        }
    } catch (e: Exception) {
        Toast
            .makeText(
                context,
                "分享失败: ${e.message}",
                Toast.LENGTH_SHORT,
            ).show()
    }
}

/**
 * 解析HTML节点为ContentBlock列表，用于Column渲染
 * 每个<p>标签解析成一个独立的TextBlock
 */
private fun parseArticleContentBlocks(
    node: Node,
    onNavigate: (NavDestination) -> Unit,
    context: Context,
    blocks: MutableList<ContentBlock>,
) {
    when (node) {
        is Element -> {
            when (node.tagName()) {
                "noscript" -> { }

                "img" -> {
                    // 提取图片URL
                    val src = node.attr("data-original-token").takeIf { it.startsWith("v2-") }?.let {
                        "https://pic1.zhimg.com/$it"
                    } ?: node.attr("data-original").takeIf { it.isNotBlank() }
                        ?: node.attr("data-default-watermark-src").takeIf { it.isNotBlank() }
                        ?: node.attr("data-actualsrc").takeIf { it.isNotBlank() }
                        ?: node.attr("data-thumbnail").takeIf { it.isNotBlank() }
                        ?: node.attr("src").takeIf { it.isNotBlank() }

                    if (src != null) {
                        if (node.hasAttr("data-formula")) {
                            // 公式
                            blocks.add(ContentBlock.FormulaBlock(src, node.attr("data-formula")))
                        } else {
                            // 普通图片或GIF
                            val isGif = node.attr("class").contains("content_image")
                            blocks.add(ContentBlock.ImageBlock(src, isGif))
                        }
                    }
                }

                "p" -> {
                    // 每个<p>标签作为一个独立的TextBlock
                    val componentUsed = mutableSetOf<String>()
                    val text = buildAnnotatedString {
                        node.childNodes().forEach { child ->
                            buildParagraphText(child, onNavigate, context, this, componentUsed)
                        }
                    }
                    if (text.isNotEmpty()) {
                        blocks.add(ContentBlock.TextBlock(text, componentUsed))
                    }
                }

                "h1", "h2", "h3", "h4", "h5", "h6" -> {
                    val level = node.tagName().substring(1).toIntOrNull() ?: 1
                    val componentUsed = mutableSetOf<String>()
                    val text = buildAnnotatedString {
                        node.childNodes().forEach { child ->
                            buildParagraphText(child, onNavigate, context, this, componentUsed)
                        }
                    }
                    if (text.isNotEmpty()) {
                        blocks.add(ContentBlock.HeaderBlock(text, level, componentUsed))
                    }
                }

                "ul", "ol" -> {
                    val isOrdered = node.tagName() == "ol"
                    val listItems = mutableListOf<ContentBlock.ListBlock.ListItem>()

                    node.children().forEach { li ->
                        if (li.tagName() == "li") {
                            val componentUsed = mutableSetOf<String>()
                            val text = buildAnnotatedString {
                                li.childNodes().forEach { child ->
                                    buildParagraphText(child, onNavigate, context, this, componentUsed)
                                }
                            }
                            if (text.isNotEmpty()) {
                                listItems.add(ContentBlock.ListBlock.ListItem(text, componentUsed))
                            }
                        }
                    }

                    if (listItems.isNotEmpty()) {
                        blocks.add(ContentBlock.ListBlock(listItems, isOrdered))
                    }
                }

                "blockquote" -> {
                    val componentUsed = mutableSetOf<String>()
                    val text = buildAnnotatedString {
                        node.childNodes().forEach { child ->
                            buildParagraphText(child, onNavigate, context, this, componentUsed)
                        }
                    }
                    if (text.isNotEmpty()) {
                        blocks.add(ContentBlock.QuoteBlock(text, componentUsed))
                    }
                }

                "pre" -> {
                    val codeNode = node.getElementsByTag("code").firstOrNull() ?: node
                    val language = codeNode
                        .attr("class")
                        .split(" ")
                        .find { it.startsWith("language-") }
                        ?.removePrefix("language-") ?: ""
                    val code = codeNode.wholeText() // Use wholeText to preserve newlines
                    if (code.isNotBlank()) {
                        blocks.add(ContentBlock.CodeBlock(code, language))
                    }
                }

                "hr" -> {
                    blocks.add(ContentBlock.DividerBlock)
                }

                else -> {
                    // 递归处理其他元素
                    node.childNodes().forEach {
                        parseArticleContentBlocks(it, onNavigate, context, blocks)
                    }
                }
            }
        }

        else -> {
            // 其他节点类型递归处理
        }
    }
}

/**
 * 构建段落内的文本内容（用于单个TextBlock内部）
 */
private fun buildParagraphText(
    node: Node,
    onNavigate: (NavDestination) -> Unit,
    context: Context,
    builder: AnnotatedString.Builder,
    componentUsed: MutableSet<String>? = null,
) {
    when (node) {
        is Element -> {
            when (node.tagName()) {
                "br" -> {
                    builder.append("\n")
                }

                "a" -> {
                    val href = node.attr("href")
                    val linkText = node.text()
                    if (linkText.isNotEmpty()) {
                        builder.withLink(
                            LinkAnnotation.Clickable(
                                href,
                                TextLinkStyles(style = SpanStyle(color = Color(0xff66CCFF))),
                            ) {
                                resolveContent(href.toUri())?.let(onNavigate)
                                    ?: luoTianYiUrlLauncher(context, href.toUri())
                            },
                        ) {
                            append(linkText)
                        }
                    }
                }

                "b", "strong" -> {
                    builder.pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    node.childNodes().forEach { child ->
                        buildParagraphText(child, onNavigate, context, builder, componentUsed)
                    }
                    builder.pop()
                }

                "i", "em" -> {
                    builder.pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                    node.childNodes().forEach { child ->
                        buildParagraphText(child, onNavigate, context, builder, componentUsed)
                    }
                    builder.pop()
                }

                "img" -> {
                    // 段落内的图片忽略或显示占位符
                    builder.append("[图片]")
                }

                else -> {
                    node.childNodes().forEach { child ->
                        buildParagraphText(child, onNavigate, context, builder, componentUsed)
                    }
                }
            }
        }

        is TextNode -> {
            // 处理emoji
            builder.processTextWithEmoji(node.text(), componentUsed)
        }

        else -> {
            builder.append(node.outerHtml())
        }
    }
}
