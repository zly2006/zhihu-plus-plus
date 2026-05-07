/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.github.zly2006.zhihu.util

import android.content.Context
import com.github.zly2006.zhihu.R
import com.github.zly2006.zhihu.data.DataHolder
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.readRawBytes
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.Entities
import java.io.ByteArrayInputStream
import java.net.URLConnection
import java.text.SimpleDateFormat
import java.util.Base64
import java.util.Date
import java.util.Locale

const val ARTICLE_EXPORT_TEMPLATE_ASSET = "article_export_template.html"
const val ARTICLE_EXPORT_GITHUB_URL = "https://github.com/zly2006/zhihu-plus-plus"
private const val ARTICLE_EXPORT_IMAGE_FETCH_CONCURRENCY = 6

data class ArticleExportFooterData(
    val exportEpochMillis: Long = System.currentTimeMillis(),
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

fun prepareContentDocumentForExport(content: String): Document = Jsoup.parse(content).apply {
    select("noscript").remove()
    select("img").forEach { image ->
        extractImageUrl(image)?.let { src ->
            image.attr("src", normalizeArticleExportUrl(src))
        }
        image.removeClass("lazy")
        image.removeAttr("srcset")
        image.removeAttr("sizes")
        image.attr("loading", "eager")
    }

    select("a[href^=//]").forEach { anchor ->
        anchor.attr("href", normalizeArticleExportUrl(anchor.attr("href")))
    }
}

fun buildArticleExportHtml(
    context: Context,
    exportData: ArticleExportData,
    extraSectionsHtml: String = "",
): String = renderArticleExportHtml(
    context = context,
    template = loadArticleExportTemplate(context),
    exportData = exportData,
    extraSectionsHtml = extraSectionsHtml,
)

fun buildArticleExportData(
    content: DataHolder.Content,
    includeAppAttribution: Boolean,
    exportEpochMillis: Long = System.currentTimeMillis(),
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
    context: Context,
    template: String,
    exportData: ArticleExportData,
    extraSectionsHtml: String = "",
): String = renderArticleExportHtml(
    text = articleExportText(context),
    template = template,
    exportData = exportData,
    extraSectionsHtml = extraSectionsHtml,
)

fun renderArticleExportHtml(
    template: String,
    exportData: ArticleExportData,
    extraSectionsHtml: String = "",
): String = renderArticleExportHtml(
    text = defaultArticleExportText,
    template = template,
    exportData = exportData,
    extraSectionsHtml = extraSectionsHtml,
)

private fun renderArticleExportHtml(
    text: ArticleExportText,
    template: String,
    exportData: ArticleExportData,
    extraSectionsHtml: String = "",
): String {
    val footerPlaceholders = buildArticleExportFooterPlaceholders(text, exportData.footerData)
    val authorAvatarHtml = exportData.authorAvatarSrc
        .takeIf { it.isNotBlank() }
        ?.let {
            """
            <img
                class="author-avatar"
                src="${escapeHtml(normalizeArticleExportUrl(it))}"
                alt="${escapeHtml(text.authorAvatarAlt)}"
            />
            """.trimIndent()
        } ?: "<div class=\"author-avatar author-avatar-placeholder\"></div>"

    val authorBioHtml = exportData.authorBio
        .takeIf { it.isNotBlank() }
        ?.let { "<div class=\"author-bio\">${escapeHtml(it)}</div>" }
        .orEmpty()

    val processedContent = prepareContentDocumentForExport(exportData.content).body().html()

    return template.replacePlaceholders(
        mapOf(
            "{{title}}" to escapeHtml(exportData.title),
            "{{authorAvatar}}" to authorAvatarHtml,
            "{{authorName}}" to escapeHtml(exportData.authorName),
            "{{authorBio}}" to authorBioHtml,
            "{{voteCount}}" to exportData.voteUpCount.toString(),
            "{{commentCount}}" to exportData.commentCount.toString(),
            "{{bodyHtml}}" to processedContent,
            "{{extraSections}}" to extraSectionsHtml,
            "{{exportedDate}}" to footerPlaceholders.exportedDate,
            "{{publishedDate}}" to footerPlaceholders.publishedDate,
            "{{editedDate}}" to footerPlaceholders.editedDate,
            "{{editedDateClass}}" to footerPlaceholders.editedDateClass,
            "{{appAttributionClass}}" to footerPlaceholders.appAttributionClass,
            "{{appName}}" to escapeHtml(text.appName),
            "{{appAttributionPrefix}}" to escapeHtml(text.appAttributionPrefix),
            "{{appAttributionSuffix}}" to escapeHtml(text.appAttributionSuffix(footerPlaceholders.githubUrl)),
            "{{githubUrl}}" to escapeHtml(footerPlaceholders.githubUrl),
        ),
    )
}

suspend fun buildOfflineArticleExportHtml(
    context: Context,
    exportData: ArticleExportData,
    httpClient: HttpClient,
    includeImages: Boolean = true,
    extraSectionsHtml: String = "",
): String {
    val htmlContent = buildArticleExportHtml(
        context = context,
        exportData = exportData,
        extraSectionsHtml = extraSectionsHtml,
    )
    if (!includeImages) {
        return htmlContent
    }

    return inlineArticleExportImagesInHtml(htmlContent) { imageUrl ->
        fetchArticleExportImageDataUrl(context, httpClient, imageUrl)
    }
}

suspend fun buildOfflineArticleExportHtml(
    context: Context,
    content: DataHolder.Content,
    includeAppAttribution: Boolean,
    httpClient: HttpClient,
    includeImages: Boolean = true,
    extraSectionsHtml: String = "",
): String = buildOfflineArticleExportHtml(
    context = context,
    exportData = buildArticleExportData(
        content = content,
        includeAppAttribution = includeAppAttribution,
    ),
    httpClient = httpClient,
    includeImages = includeImages,
    extraSectionsHtml = extraSectionsHtml,
)

suspend fun inlineArticleExportImagesInHtml(
    html: String,
    includeImages: Boolean = true,
    resolveDataUrl: suspend (String) -> String,
): String {
    if (!includeImages) {
        return html
    }
    val document = Jsoup.parse(html)
    document.outputSettings().prettyPrint(false)
    inlineArticleExportImages(document.select("img"), resolveDataUrl)
    return document.outerHtml()
}

suspend fun inlineArticleExportImages(
    imageNodes: Iterable<Element>,
    resolveDataUrl: suspend (String) -> String,
) {
    val imagesByUrl = linkedMapOf<String, MutableList<Element>>()
    imageNodes.forEach { image ->
        val imageUrl = resolveArticleExportImageUrl(image) ?: return@forEach
        imagesByUrl.getOrPut(imageUrl) { mutableListOf() }.add(image)
    }
    if (imagesByUrl.isEmpty()) {
        return
    }

    val dataUrlsByUrl = coroutineScope {
        val semaphore = Semaphore(ARTICLE_EXPORT_IMAGE_FETCH_CONCURRENCY)
        imagesByUrl.keys
            .map { imageUrl ->
                async {
                    imageUrl to semaphore.withPermit {
                        resolveDataUrl(imageUrl)
                    }
                }
            }.awaitAll()
            .toMap()
    }

    imagesByUrl.forEach { (imageUrl, images) ->
        val dataUrl = dataUrlsByUrl.getValue(imageUrl)
        images.forEach { image ->
            image.attr("src", dataUrl)
            image.removeClass("lazy")
            image.removeAttr("srcset")
            image.removeAttr("sizes")
            image.attr("loading", "eager")
        }
    }
}

private fun loadArticleExportTemplate(context: Context): String = context.assets.open(ARTICLE_EXPORT_TEMPLATE_ASSET).use { inputStream ->
    inputStream.bufferedReader().use { reader ->
        reader.readText()
    }
}

private fun String.replacePlaceholders(placeholders: Map<String, String>): String = placeholders.entries.fold(this) { html, entry ->
    html.replace(entry.key, entry.value)
}

private fun escapeHtml(text: String): String = Entities.escape(text)

private data class ArticleExportFooterPlaceholders(
    val exportedDate: String,
    val publishedDate: String,
    val editedDate: String,
    val editedDateClass: String,
    val appAttributionClass: String,
    val githubUrl: String,
)

private data class ArticleExportText(
    val appName: String,
    val authorAvatarAlt: String,
    val commentImageAlt: String,
    val publishedDate: (String) -> String,
    val editedDate: (String) -> String,
    val exportedDate: (String) -> String,
    val topComments: String,
    val topCommentsLimited: (Int) -> String,
    val typeAnswer: String,
    val typeArticle: String,
    val fileUntitled: String,
    val fileAnonymousAuthor: String,
    val appAttributionPrefix: String,
    val appAttributionSuffix: (String) -> String,
    val fileName: (String, String, String, String, Long, String, String) -> String,
)

private val defaultArticleExportText = ArticleExportText(
    appName = "Zhihu++",
    authorAvatarAlt = "Author avatar",
    commentImageAlt = "Comment image",
    publishedDate = { "Published: $it" },
    editedDate = { "Edited: $it" },
    exportedDate = { "Exported: $it" },
    topComments = "Top comments",
    topCommentsLimited = { "Top comments (first $it)" },
    typeAnswer = "answer",
    typeArticle = "article",
    fileUntitled = "untitled",
    fileAnonymousAuthor = "anonymous",
    appAttributionPrefix = "Exported with",
    appAttributionSuffix = { ", a free, open-source, ad-free third-party Zhihu client. Please star it. (GitHub: $it)" },
    fileName = { safeTitle, safeAuthorName, typeLabel, typeKey, articleId, timestamp, extension ->
        "zhihu++_${safeTitle}_${safeAuthorName}_${typeLabel}_${typeKey}_${articleId}_$timestamp.$extension"
    },
)

private fun articleExportText(context: Context): ArticleExportText = ArticleExportText(
    appName = context.getString(R.string.app_name),
    authorAvatarAlt = context.getString(R.string.article_export_author_avatar_alt),
    commentImageAlt = context.getString(R.string.article_export_comment_image_alt),
    publishedDate = { context.getString(R.string.article_export_published_date, it) },
    editedDate = { context.getString(R.string.article_export_edited_date, it) },
    exportedDate = { context.getString(R.string.article_export_exported_date, it) },
    topComments = context.getString(R.string.article_export_top_comments),
    topCommentsLimited = { context.getString(R.string.article_export_top_comments_limited, it) },
    typeAnswer = context.getString(R.string.article_export_type_answer),
    typeArticle = context.getString(R.string.article_export_type_article),
    fileUntitled = context.getString(R.string.article_export_file_untitled),
    fileAnonymousAuthor = context.getString(R.string.article_export_file_anonymous_author),
    appAttributionPrefix = context.getString(R.string.article_export_app_attribution_prefix),
    appAttributionSuffix = { context.getString(R.string.article_export_app_attribution_suffix, it) },
    fileName = { safeTitle, safeAuthorName, typeLabel, typeKey, articleId, timestamp, extension ->
        context.getString(
            R.string.article_export_file_name_format,
            safeTitle,
            safeAuthorName,
            typeLabel,
            typeKey,
            articleId,
            timestamp,
            extension,
        )
    },
)

private fun buildArticleExportFooterPlaceholders(
    text: ArticleExportText,
    footerData: ArticleExportFooterData,
): ArticleExportFooterPlaceholders {
    val publishedDate = footerData.createdEpochSeconds
        .takeIf { it > 0L }
        ?.let { text.publishedDate(formatArticleExportDate(it * 1000L)) }
        .orEmpty()
    val editedDate = footerData.updatedEpochSeconds
        .takeIf { it > 0L && it != footerData.createdEpochSeconds }
        ?.let { text.editedDate(formatArticleExportDate(it * 1000L)) }
        .orEmpty()

    return ArticleExportFooterPlaceholders(
        exportedDate = text.exportedDate(formatArticleExportDate(footerData.exportEpochMillis)),
        publishedDate = publishedDate,
        editedDate = editedDate,
        editedDateClass = if (editedDate.isBlank()) "export-footer-line is-hidden" else "export-footer-line",
        appAttributionClass = if (footerData.includeAppAttribution) "export-credit" else "export-credit is-hidden",
        githubUrl = footerData.githubUrl,
    )
}

private fun formatArticleExportDate(epochMillis: Long): String = SimpleDateFormat(
    "yyyy-MM-dd HH:mm",
    Locale.getDefault(),
).format(Date(epochMillis))

fun prepareArticleExportComment(
    authorName: String,
    content: String,
    createdTimeText: String,
): ArticleExportComment {
    val document = Jsoup.parseBodyFragment(content)
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
        extractImageUrl(image)?.let { src ->
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
    context: Context,
    comments: List<ArticleExportComment>,
    requestedCount: Int? = null,
): String = buildArticleExportCommentsHtml(
    text = articleExportText(context),
    comments = comments,
    requestedCount = requestedCount,
)

fun buildArticleExportCommentsHtml(
    comments: List<ArticleExportComment>,
    requestedCount: Int? = null,
): String = buildArticleExportCommentsHtml(
    text = defaultArticleExportText,
    comments = comments,
    requestedCount = requestedCount,
)

private fun buildArticleExportCommentsHtml(
    text: ArticleExportText,
    comments: List<ArticleExportComment>,
    requestedCount: Int? = null,
): String {
    if (comments.isEmpty()) return ""
    val title = requestedCount
        ?.takeIf { it > 0 }
        ?.let { text.topCommentsLimited(minOf(it, comments.size)) }
        ?: text.topComments

    return buildString {
        append("<div class='comments-title'>${escapeHtml(title)}</div>")
        comments.forEach { comment ->
            append(
                """
                <div class="comment">
                    <div class="comment-author">${escapeHtml(comment.authorName)}</div>
                    <div class="comment-content">${comment.contentHtml}</div>
                    ${buildArticleExportCommentImageHtml(text, comment.imageSrc)}
                    <div class="comment-time">${escapeHtml(comment.createdTimeText)}</div>
                </div>
                """.trimIndent(),
            )
        }
    }
}

private fun buildArticleExportCommentImageHtml(text: ArticleExportText, imageSrc: String): String = imageSrc
    .takeIf { it.isNotBlank() }
    ?.let {
        """
        <img
            class="comment-image"
            src="${escapeHtml(it)}"
            alt="${escapeHtml(text.commentImageAlt)}"
        />
        """.trimIndent()
    }.orEmpty()

fun normalizeArticleExportUrl(url: String): String = when {
    url.startsWith("//") -> "https:$url"
    else -> url
}

fun buildArticleExportFileName(
    context: Context,
    content: DataHolder.Content,
    extension: String,
): String = buildArticleExportFileName(
    text = articleExportText(context),
    content = content,
    extension = extension,
)

fun buildArticleExportFileName(
    content: DataHolder.Content,
    extension: String,
): String = buildArticleExportFileName(
    text = defaultArticleExportText,
    content = content,
    extension = extension,
)

private fun buildArticleExportFileName(
    text: ArticleExportText,
    content: DataHolder.Content,
    extension: String,
): String {
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val (title, authorName, typeLabel, typeKey, articleId) = when (content) {
        is DataHolder.Answer -> ExportFileMeta(
            title = content.question.title,
            authorName = content.author.name,
            typeLabel = text.typeAnswer,
            typeKey = "answer",
            articleId = content.id,
        )
        is DataHolder.Article -> ExportFileMeta(
            title = content.title,
            authorName = content.author.name,
            typeLabel = text.typeArticle,
            typeKey = "article",
            articleId = content.id,
        )
        else -> throw IllegalArgumentException("Unsupported export content type: ${content::class.simpleName}")
    }
    val safeTitle = sanitizeArticleExportFileNamePart(title).ifBlank { text.fileUntitled }
    val safeAuthorName = sanitizeArticleExportFileNamePart(authorName)
        .ifBlank { text.fileAnonymousAuthor }
    val normalizedExtension = extension.trimStart('.')

    return text.fileName(
        safeTitle,
        safeAuthorName,
        typeLabel,
        typeKey,
        articleId,
        timestamp,
        normalizedExtension,
    )
}

suspend fun fetchArticleExportImageDataUrl(
    context: Context,
    httpClient: HttpClient,
    imageUrl: String,
): String {
    val response = httpClient.get(imageUrl)
    if (!response.status.isSuccess()) {
        throw IllegalStateException(context.getString(R.string.article_export_image_download_failed, response.status.value))
    }

    val bytes = response.readRawBytes()
    if (bytes.isEmpty()) {
        throw IllegalStateException(context.getString(R.string.article_export_image_empty))
    }

    val mimeType = resolveArticleExportImageMimeType(
        contentTypeHeader = response.headers[HttpHeaders.ContentType],
        imageUrl = imageUrl,
        imageBytes = bytes,
    )

    return "data:$mimeType;base64,${Base64.getEncoder().encodeToString(bytes)}"
}

private fun resolveArticleExportImageUrl(image: Element): String? = extractImageUrl(image)
    ?.let(::normalizeArticleExportUrl)
    ?: image
        .attr("src")
        .takeIf { it.isNotBlank() && !it.startsWith("data:") }
        ?.let(::normalizeArticleExportUrl)

fun sanitizeArticleExportFileNamePart(text: String): String = text
    .trim()
    .replace(Regex("\\s+"), "_")
    .replace(Regex("[\\\\/:*?\"<>|]"), "_")
    .replace(Regex("_+"), "_")
    .trim('_')

private fun resolveArticleExportImageMimeType(
    contentTypeHeader: String?,
    imageUrl: String,
    imageBytes: ByteArray,
): String {
    contentTypeHeader
        ?.substringBefore(';')
        ?.trim()
        ?.takeIf { it.startsWith("image/") }
        ?.let { return it }

    URLConnection.guessContentTypeFromName(imageUrl.substringBefore('?'))?.let { return it }
    ByteArrayInputStream(imageBytes).use { stream ->
        URLConnection.guessContentTypeFromStream(stream)?.let { return it }
    }

    return "image/jpeg"
}

private data class ExportFileMeta(
    val title: String,
    val authorName: String,
    val typeLabel: String,
    val typeKey: String,
    val articleId: Long,
)
