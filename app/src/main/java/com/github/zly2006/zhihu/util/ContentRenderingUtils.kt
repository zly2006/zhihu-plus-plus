package com.github.zly2006.zhihu.util

import android.content.Context
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
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
 * 通用的HTML节点DFS处理函数（完整版）
 * 用于文章的富文本处理，支持段落、标题、列表等
 */
fun AnnotatedString.Builder.dfsRichText(
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

                "p" -> {
                    append("\n") // 段前空行
                    node.childNodes().forEach { dfsRichText(it, onNavigate, context, componentUsed) }
                    append("\n") // 段后空行
                }

                "noscript" -> {
                    // 处理noscript标签，提取真实图片
                    // 检查下一个兄弟节点
                    node.nextSibling()?.let { actualImg ->
                        if (actualImg.nodeName() == "img" && actualImg is Element) {
                            // 优先使用 data-actualsrc
                            if (actualImg.attr("data-actualsrc").isNotEmpty()) {
                                dfsRichText(actualImg, onNavigate, context, componentUsed)
                                return@let
                            }
                        }
                    }

                    // 检查noscript内部的img标签
                    if (node.childrenSize() > 0) {
                        val imgNode = node.child(0)
                        if (imgNode.tagName() == "img") {
                            // 处理GIF：使用data-thumbnail
                            if (imgNode.attr("class").contains("content_image") &&
                                imgNode.attr("data-thumbnail").isNotEmpty()
                            ) {
                                imgNode.attr("src", imgNode.attr("data-thumbnail"))
                            }
                            // 如果src为空，尝试使用data-default-watermark-src
                            if (imgNode.attr("src").isEmpty() &&
                                imgNode.attr("data-default-watermark-src").isNotEmpty()
                            ) {
                                imgNode.attr("src", imgNode.attr("data-default-watermark-src"))
                            }
                            // 递归处理提取出的img节点
                            dfsRichText(imgNode, onNavigate, context, componentUsed)
                        }
                    }
                }

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
                        val isFormula = node.hasAttr("data-formula")
                        val componentKey = if (isFormula) {
                            "formula_${src.hashCode()}"
                        } else {
                            "image_${src.hashCode()}"
                        }
                        append("\n") // 图片前换行
                        appendInlineContent(componentKey, src)
                        componentUsed?.add(componentKey)
                        append("\n") // 图片后换行
                    }
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

                "b", "strong" -> {
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                    node.childNodes().forEach { dfsRichText(it, onNavigate, context, componentUsed) }
                    pop()
                }

                "i", "em" -> {
                    pushStyle(SpanStyle(fontWeight = FontWeight.Light))
                    node.childNodes().forEach { dfsRichText(it, onNavigate, context, componentUsed) }
                    pop()
                }

                "h1", "h2", "h3", "h4", "h5", "h6" -> {
                    val size = when (node.tagName()) {
                        "h1" -> 2.0f
                        "h2" -> 1.5f
                        "h3" -> 1.3f
                        "h4" -> 1.2f
                        "h5" -> 1.1f
                        else -> 1.0f
                    }
                    append("\n") // 标题前空行
                    pushStyle(SpanStyle(fontWeight = FontWeight.Bold, fontSize = (16 * size).sp))
                    node.childNodes().forEach { dfsRichText(it, onNavigate, context, componentUsed) }
                    pop()
                    append("\n") // 标题后空行
                }

                "ul", "ol" -> {
                    append("\n") // 列表前空行
                    node.children().forEach { li ->
                        append("• ")
                        li.childNodes().forEach { dfsRichText(it, onNavigate, context, componentUsed) }
                        append("\n")
                    }
                    append("\n") // 列表后空行
                }

                "blockquote" -> {
                    append("\n") // 引用前空行
                    pushStyle(SpanStyle(color = Color.Gray))
                    node.childNodes().forEach { dfsRichText(it, onNavigate, context, componentUsed) }
                    pop()
                    append("\n") // 引用后空行
                }

                else -> {
                    node.childNodes().forEach { dfsRichText(it, onNavigate, context, componentUsed) }
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
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES)
                put(android.provider.MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val collection = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            android.provider.MediaStore.Images.Media
                .getContentUri(android.provider.MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val imageUri = resolver.insert(collection, contentValues)
        if (imageUri != null) {
            resolver.openOutputStream(imageUri).use { os ->
                os?.write(bytes)
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(android.provider.MediaStore.MediaColumns.IS_PENDING, 0)
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

