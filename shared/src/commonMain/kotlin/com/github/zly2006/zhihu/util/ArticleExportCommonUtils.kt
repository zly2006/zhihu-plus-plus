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

const val ARTICLE_EXPORT_TEMPLATE_ASSET = "article_export_template.html"
const val ARTICLE_EXPORT_GITHUB_URL = "https://github.com/zly2006/zhihu-plus-plus"

data class ArticleExportFooterData(
    val exportEpochMillis: Long = Clock.System.now().toEpochMilliseconds(),
    val createdEpochSeconds: Long = 0L,
    val updatedEpochSeconds: Long = 0L,
    val includeAppAttribution: Boolean = true,
    val githubUrl: String = ARTICLE_EXPORT_GITHUB_URL,
)

data class ArticleExportData(
    val title: String,
    val authorName: String,
    val authorBio: String,
    val authorAvatarSrc: String,
    val voteUpCount: Int,
    val commentCount: Int,
    val content: String,
    val footerData: ArticleExportFooterData = ArticleExportFooterData(),
)

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

fun buildArticleExportData(
    content: DataHolder.Content,
    includeAppAttribution: Boolean,
    exportEpochMillis: Long = Clock.System.now().toEpochMilliseconds(),
): ArticleExportData = when (content) {
    is DataHolder.Answer -> ArticleExportData(
        title = content.question.title,
        authorName = content.author.name,
        authorBio = content.author.headline,
        authorAvatarSrc = content.author.avatarUrl,
        voteUpCount = content.voteupCount,
        commentCount = content.commentCount,
        content = content.content,
        footerData = ArticleExportFooterData(
            exportEpochMillis = exportEpochMillis,
            createdEpochSeconds = content.createdTime,
            updatedEpochSeconds = content.updatedTime,
            includeAppAttribution = includeAppAttribution,
        ),
    )

    is DataHolder.Article -> ArticleExportData(
        title = content.title,
        authorName = content.author.name,
        authorBio = content.author.headline,
        authorAvatarSrc = content.author.avatarUrl,
        voteUpCount = content.voteupCount,
        commentCount = content.commentCount,
        content = content.content,
        footerData = ArticleExportFooterData(
            exportEpochMillis = exportEpochMillis,
            createdEpochSeconds = content.created,
            updatedEpochSeconds = content.updated,
            includeAppAttribution = includeAppAttribution,
        ),
    )

    else -> throw IllegalArgumentException("Unsupported export content type: ${content::class.simpleName}")
}

fun renderArticleExportHtml(
    template: String,
    exportData: ArticleExportData,
    extraSectionsHtml: String = "",
): String {
    if (template.isBlank()) {
        return prepareArticleExportContentHtml(exportData.content) + extraSectionsHtml
    }

    val footerPlaceholders = buildArticleExportFooterPlaceholders(exportData.footerData)
    val authorAvatarHtml = exportData.authorAvatarSrc
        .takeIf { it.isNotBlank() }
        ?.let {
            """
            <img
                class="author-avatar"
                src="${escapeArticleExportHtml(normalizeArticleExportUrl(it))}"
                alt="作者头像"
            />
            """.trimIndent()
        } ?: "<div class=\"author-avatar author-avatar-placeholder\"></div>"

    val authorBioHtml = exportData.authorBio
        .takeIf { it.isNotBlank() }
        ?.let { "<div class=\"author-bio\">${escapeArticleExportHtml(it)}</div>" }
        .orEmpty()

    return template.replacePlaceholders(
        mapOf(
            "{{title}}" to escapeArticleExportHtml(exportData.title),
            "{{authorAvatar}}" to authorAvatarHtml,
            "{{authorName}}" to escapeArticleExportHtml(exportData.authorName),
            "{{authorBio}}" to authorBioHtml,
            "{{voteCount}}" to exportData.voteUpCount.toString(),
            "{{commentCount}}" to exportData.commentCount.toString(),
            "{{bodyHtml}}" to prepareArticleExportContentHtml(exportData.content),
            "{{extraSections}}" to extraSectionsHtml,
            "{{exportedDate}}" to footerPlaceholders.exportedDate,
            "{{publishedDate}}" to footerPlaceholders.publishedDate,
            "{{editedDate}}" to footerPlaceholders.editedDate,
            "{{editedDateClass}}" to footerPlaceholders.editedDateClass,
            "{{appAttributionClass}}" to footerPlaceholders.appAttributionClass,
            "{{githubUrl}}" to escapeArticleExportHtml(footerPlaceholders.githubUrl),
        ),
    )
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

fun prepareArticleExportContentHtml(content: String): String {
    val document = Ksoup.parseBodyFragment(content)
    document.select("noscript").remove()
    document.select("img").forEach { image ->
        extractArticleExportImageUrl(image)?.let { src ->
            image.attr("src", normalizeArticleExportUrl(src))
        }
        image.removeClass("lazy")
        image.removeAttr("srcset")
        image.removeAttr("sizes")
        image.attr("loading", "eager")
    }

    document.select("a[href^=//]").forEach { anchor ->
        anchor.attr("href", normalizeArticleExportUrl(anchor.attr("href")))
    }
    return document.body().html()
}

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

private data class ArticleExportFooterPlaceholders(
    val exportedDate: String,
    val publishedDate: String,
    val editedDate: String,
    val editedDateClass: String,
    val appAttributionClass: String,
    val githubUrl: String,
)

private fun buildArticleExportFooterPlaceholders(footerData: ArticleExportFooterData): ArticleExportFooterPlaceholders {
    val publishedDate = footerData.createdEpochSeconds
        .takeIf { it > 0L }
        ?.let { "发布日期：" + formatArticleExportDate(it * 1000L) }
        .orEmpty()
    val editedDate = footerData.updatedEpochSeconds
        .takeIf { it > 0L && it != footerData.createdEpochSeconds }
        ?.let { "编辑日期：" + formatArticleExportDate(it * 1000L) }
        .orEmpty()

    return ArticleExportFooterPlaceholders(
        exportedDate = "导出日期：" + formatArticleExportDate(footerData.exportEpochMillis),
        publishedDate = publishedDate,
        editedDate = editedDate,
        editedDateClass = if (editedDate.isBlank()) "export-footer-line is-hidden" else "export-footer-line",
        appAttributionClass = if (footerData.includeAppAttribution) "export-credit" else "export-credit is-hidden",
        githubUrl = footerData.githubUrl,
    )
}

private fun String.replacePlaceholders(placeholders: Map<String, String>): String =
    placeholders.entries.fold(this) { html, entry ->
        html.replace(entry.key, entry.value)
    }

private fun extractArticleExportImageUrl(image: Element): String? =
    com.github.zly2006.zhihu.shared.util
        .extractImageUrl(image::attr)
        ?.takeIf { !it.startsWith("data:") }

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
