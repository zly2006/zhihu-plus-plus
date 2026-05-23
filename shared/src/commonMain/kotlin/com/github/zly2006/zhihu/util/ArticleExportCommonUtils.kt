/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 */

package com.github.zly2006.zhihu.util

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Element
import com.github.zly2006.zhihu.shared.data.DataHolder
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

data class ArticleExportComment(
    val authorName: String,
    val contentHtml: String,
    val createdTimeText: String,
    val imageSrc: String = "",
)

fun prepareArticleExportComment(
    authorName: String,
    content: String,
    createdTimeText: String,
): ArticleExportComment {
    val document = Ksoup.parseBodyFragment(content)
    val imageSrc = document
        .selectFirst("a.comment_img, a.comment_gif, a.comment_sticker")
        ?.attr("href")
        ?.takeIf { it.isNotBlank() }
        ?.let(::normalizeArticleExportUrl)
        .orEmpty()

    document.select("noscript").remove()
    document.select("a.comment_img, a.comment_gif, a.comment_sticker").remove()
    document.select("a[href^=//]").forEach { anchor ->
        anchor.attr("href", normalizeArticleExportUrl(anchor.attr("href")))
    }
    document.select("img").forEach { image ->
        extractArticleExportImageUrl(image)?.let { src ->
            image.attr("src", normalizeArticleExportUrl(src))
        }
        image.removeClass("lazy")
        image.removeAttr("srcset")
        image.removeAttr("sizes")
        image.attr("loading", "eager")
    }
    document.body().select("p, div").forEach { element ->
        if (element.text().isBlank() && element.select("img").isEmpty()) {
            element.remove()
        }
    }

    return ArticleExportComment(
        authorName = authorName,
        contentHtml = document.body().html().trim(),
        createdTimeText = createdTimeText,
        imageSrc = imageSrc,
    )
}

fun buildArticleExportCommentsHtml(
    comments: List<ArticleExportComment>,
    requestedCount: Int? = null,
): String {
    if (comments.isEmpty()) return ""
    val titleSuffix = requestedCount
        ?.takeIf { it > 0 }
        ?.let { " (前 ${minOf(it, comments.size)} 条)" }
        .orEmpty()

    return buildString {
        append("<div class='comments-title'>热门评论$titleSuffix</div>")
        comments.forEach { comment ->
            append(
                """
                <div class="comment">
                    <div class="comment-author">${escapeArticleExportHtml(comment.authorName)}</div>
                    <div class="comment-content">${comment.contentHtml}</div>
                    ${buildArticleExportCommentImageHtml(comment.imageSrc)}
                    <div class="comment-time">${escapeArticleExportHtml(comment.createdTimeText)}</div>
                </div>
                """.trimIndent(),
            )
        }
    }
}

private fun buildArticleExportCommentImageHtml(imageSrc: String): String = imageSrc
    .takeIf { it.isNotBlank() }
    ?.let {
        """
        <img
            class="comment-image"
            src="${escapeArticleExportHtml(it)}"
            alt="评论图片"
        />
        """.trimIndent()
    }.orEmpty()

fun normalizeArticleExportUrl(url: String): String = when {
    url.startsWith("//") -> "https:$url"
    else -> url
}

fun buildArticleExportFileName(
    content: DataHolder.Content,
    extension: String,
): String {
    val timestamp = formatArticleExportFileTimestamp()
    val (title, authorName, typeLabel, typeKey, articleId) = when (content) {
        is DataHolder.Answer -> ExportFileMeta(
            title = content.question.title,
            authorName = content.author.name,
            typeLabel = "回答",
            typeKey = "answer",
            articleId = content.id,
        )
        is DataHolder.Article -> ExportFileMeta(
            title = content.title,
            authorName = content.author.name,
            typeLabel = "文章",
            typeKey = "article",
            articleId = content.id,
        )
        else -> throw IllegalArgumentException("Unsupported export content type: ${content::class.simpleName}")
    }
    val safeTitle = sanitizeArticleExportFileNamePart(title).ifBlank { "无标题" }
    val safeAuthorName = sanitizeArticleExportFileNamePart(authorName)
        .ifBlank { "匿名作者" }
    val normalizedExtension = extension.trimStart('.')

    return "zhihu++_${safeTitle}_${safeAuthorName}的${typeLabel}_${typeKey}_${articleId}_$timestamp.$normalizedExtension"
}

fun sanitizeArticleExportFileNamePart(text: String): String = text
    .trim()
    .replace(Regex("\\s+"), "_")
    .replace(Regex("[\\\\/:*?\"<>|]"), "_")
    .replace(Regex("_+"), "_")
    .trim('_')

fun escapeArticleExportHtml(text: String): String = buildString(text.length) {
    text.forEach { char ->
        when (char) {
            '&' -> append("&amp;")
            '<' -> append("&lt;")
            '>' -> append("&gt;")
            '"' -> append("&quot;")
            '\'' -> append("&#x27;")
            else -> append(char)
        }
    }
}

private fun extractArticleExportImageUrl(image: Element): String? =
    image.attr("data-original").takeIf { it.isNotBlank() }
        ?: image.attr("data-actualsrc").takeIf { it.isNotBlank() }
        ?: image.attr("data-src").takeIf { it.isNotBlank() }
        ?: image.attr("src").takeIf { it.isNotBlank() && !it.startsWith("data:") }

@OptIn(ExperimentalTime::class)
private fun formatArticleExportFileTimestamp(): String {
    val dateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    return buildString {
        append(dateTime.year.toString().padStart(4, '0'))
        append((dateTime.month.ordinal + 1).toString().padStart(2, '0'))
        append(dateTime.day.toString().padStart(2, '0'))
        append('_')
        append(dateTime.hour.toString().padStart(2, '0'))
        append(dateTime.minute.toString().padStart(2, '0'))
        append(dateTime.second.toString().padStart(2, '0'))
    }
}

@OptIn(ExperimentalTime::class)
fun formatArticleExportDate(epochMillis: Long): String {
    val dateTime = Instant
        .fromEpochMilliseconds(epochMillis)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    return buildString {
        append(dateTime.year.toString().padStart(4, '0'))
        append('-')
        append((dateTime.month.ordinal + 1).toString().padStart(2, '0'))
        append('-')
        append(dateTime.day.toString().padStart(2, '0'))
        append(' ')
        append(dateTime.hour.toString().padStart(2, '0'))
        append(':')
        append(dateTime.minute.toString().padStart(2, '0'))
    }
}

private data class ExportFileMeta(
    val title: String,
    val authorName: String,
    val typeLabel: String,
    val typeKey: String,
    val articleId: Long,
)
