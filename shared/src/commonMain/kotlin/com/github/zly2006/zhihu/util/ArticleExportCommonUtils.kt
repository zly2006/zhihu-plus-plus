/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
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

import com.fleeksoft.ksoup.Ksoup
import com.fleeksoft.ksoup.nodes.Element
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.util.twoDigitString
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
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

const val ARTICLE_EXPORT_TEMPLATE_ASSET = "article_export_template.html"
const val ARTICLE_EXPORT_GITHUB_URL = "https://github.com/zly2006/zhihu-plus-plus"
private const val ARTICLE_EXPORT_IMAGE_FETCH_CONCURRENCY = 6

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
            val imageHtml = comment.imageSrc
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
            append(
                """
                <div class="comment">
                    <div class="comment-author">${escapeArticleExportHtml(comment.authorName)}</div>
                    <div class="comment-content">${comment.contentHtml}</div>
                    $imageHtml
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

fun buildArticleExportHtml(
    loadAssetText: (String) -> String,
    exportData: ArticleExportData,
    extraSectionsHtml: String = "",
): String = renderArticleExportHtml(
    template = loadAssetText(ARTICLE_EXPORT_TEMPLATE_ASSET),
    exportData = exportData,
    extraSectionsHtml = extraSectionsHtml,
)

fun buildArticleExportHtml(
    loadAssetText: (String) -> String,
    content: DataHolder.Content,
    includeAppAttribution: Boolean,
    extraSectionsHtml: String = "",
): String = buildArticleExportHtml(
    loadAssetText = loadAssetText,
    exportData = buildArticleExportData(
        content = content,
        includeAppAttribution = includeAppAttribution,
    ),
    extraSectionsHtml = extraSectionsHtml,
)

suspend fun buildOfflineArticleExportHtml(
    loadAssetText: (String) -> String,
    exportData: ArticleExportData,
    httpClient: HttpClient,
    includeImages: Boolean = true,
    extraSectionsHtml: String = "",
    useOriginalOnImageFetchFailure: Boolean = false,
): String = inlineArticleExportImagesInHtml(
    html = buildArticleExportHtml(
        loadAssetText = loadAssetText,
        exportData = exportData,
        extraSectionsHtml = extraSectionsHtml,
    ),
    includeImages = includeImages,
    useOriginalOnImageFetchFailure = useOriginalOnImageFetchFailure,
) { imageUrl ->
    fetchArticleExportImageDataUrl(httpClient, imageUrl)
}

suspend fun buildOfflineArticleExportHtml(
    loadAssetText: (String) -> String,
    content: DataHolder.Content,
    includeAppAttribution: Boolean,
    httpClient: HttpClient,
    includeImages: Boolean = true,
    extraSectionsHtml: String = "",
    useOriginalOnImageFetchFailure: Boolean = false,
): String = buildOfflineArticleExportHtml(
    loadAssetText = loadAssetText,
    exportData = buildArticleExportData(
        content = content,
        includeAppAttribution = includeAppAttribution,
    ),
    httpClient = httpClient,
    includeImages = includeImages,
    extraSectionsHtml = extraSectionsHtml,
    useOriginalOnImageFetchFailure = useOriginalOnImageFetchFailure,
)

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

    val placeholders = mapOf(
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
    )
    return placeholders.entries.fold(template) { html, entry ->
        html.replace(entry.key, entry.value)
    }
}

@OptIn(ExperimentalEncodingApi::class)
suspend fun fetchArticleExportImageDataUrl(
    httpClient: HttpClient,
    imageUrl: String,
): String {
    val response = httpClient.get(imageUrl)
    if (!response.status.isSuccess()) {
        throw IllegalStateException("下载图片失败: ${response.status.value}")
    }

    val bytes = response.readRawBytes()
    if (bytes.isEmpty()) {
        throw IllegalStateException("图片内容为空")
    }

    val mimeType = resolveArticleExportImageMimeType(
        contentTypeHeader = response.headers[HttpHeaders.ContentType],
        imageUrl = imageUrl,
        imageBytes = bytes,
    )
    return "data:$mimeType;base64,${Base64.Default.encode(bytes)}"
}

fun resolveArticleExportImageMimeType(
    contentTypeHeader: String?,
    imageUrl: String,
    imageBytes: ByteArray,
): String {
    contentTypeHeader
        ?.substringBefore(';')
        ?.trim()
        ?.takeIf { it.startsWith("image/") }
        ?.let { return it }

    guessArticleExportImageMimeTypeFromName(imageUrl)?.let { return it }
    guessArticleExportImageMimeTypeFromBytes(imageBytes)?.let { return it }
    return "image/jpeg"
}

private fun guessArticleExportImageMimeTypeFromName(imageUrl: String): String? =
    imageUrl
        .substringBefore('?')
        .substringBefore('#')
        .substringAfterLast('.', missingDelimiterValue = "")
        .lowercase()
        .let { extension ->
            when (extension) {
                "jpg", "jpeg" -> "image/jpeg"
                "png" -> "image/png"
                "gif" -> "image/gif"
                "webp" -> "image/webp"
                "bmp" -> "image/bmp"
                "svg", "svgz" -> "image/svg+xml"
                "avif" -> "image/avif"
                "heic" -> "image/heic"
                "heif" -> "image/heif"
                else -> null
            }
        }

private fun guessArticleExportImageMimeTypeFromBytes(imageBytes: ByteArray): String? {
    fun matches(vararg values: Int): Boolean =
        imageBytes.size >= values.size &&
            values.indices.all { index -> imageBytes[index].toInt() and 0xff == values[index] }

    return when {
        matches(0xff, 0xd8, 0xff) -> "image/jpeg"
        matches(0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a) -> "image/png"
        matches(0x47, 0x49, 0x46, 0x38) -> "image/gif"
        matches(0x42, 0x4d) -> "image/bmp"
        imageBytes.size >= 12 &&
            matches(0x52, 0x49, 0x46, 0x46) &&
            imageBytes[8].toInt().toChar() == 'W' &&
            imageBytes[9].toInt().toChar() == 'E' &&
            imageBytes[10].toInt().toChar() == 'B' &&
            imageBytes[11].toInt().toChar() == 'P' -> "image/webp"
        else -> null
    }
}

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

suspend fun inlineArticleExportImagesInHtml(
    html: String,
    includeImages: Boolean = true,
    useOriginalOnImageFetchFailure: Boolean = false,
    resolveDataUrl: suspend (String) -> String,
): String {
    if (!includeImages) {
        return html
    }
    val document = Ksoup.parse(html)
    inlineArticleExportImages(
        imageNodes = document.select("img"),
        useOriginalOnImageFetchFailure = useOriginalOnImageFetchFailure,
        resolveDataUrl = resolveDataUrl,
    )
    return document.outerHtml()
}

suspend fun inlineArticleExportImages(
    imageNodes: Iterable<Element>,
    useOriginalOnImageFetchFailure: Boolean = false,
    resolveDataUrl: suspend (String) -> String,
) {
    val imagesByUrl = linkedMapOf<String, MutableList<Element>>()
    imageNodes.forEach { image ->
        val imageUrl = extractArticleExportImageUrl(image)
            ?.let(::normalizeArticleExportUrl)
            ?: image
                .attr("src")
                .takeIf { it.isNotBlank() && !it.startsWith("data:") }
                ?.let(::normalizeArticleExportUrl)
            ?: return@forEach
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
                    imageUrl to if (useOriginalOnImageFetchFailure) {
                        runCatching {
                            semaphore.withPermit {
                                resolveDataUrl(imageUrl)
                            }
                        }.getOrDefault(imageUrl)
                    } else {
                        semaphore.withPermit {
                            resolveDataUrl(imageUrl)
                        }
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

@OptIn(ExperimentalTime::class)
fun buildCollectionExportZipFileName(
    collectionTitle: String,
    timestampMillis: Long = Clock.System.now().toEpochMilliseconds(),
): String {
    val safeTitle = sanitizeArticleExportFileNamePart(collectionTitle).ifBlank { "收藏夹" }
    return "zhihu++_${safeTitle}_${formatArticleExportFileTimestamp(timestampMillis)}.zip"
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

private fun extractArticleExportImageUrl(image: Element): String? =
    com.github.zly2006.zhihu.shared.util
        .extractImageUrl(image::attr)
        ?.takeIf { !it.startsWith("data:") }

@OptIn(ExperimentalTime::class)
private fun formatArticleExportFileTimestamp(timestampMillis: Long = Clock.System.now().toEpochMilliseconds()): String {
    val dateTime = Instant
        .fromEpochMilliseconds(timestampMillis)
        .toLocalDateTime(TimeZone.currentSystemDefault())
    return buildString {
        append(dateTime.year.toString().padStart(4, '0'))
        append((dateTime.month.ordinal + 1).twoDigitString())
        append(dateTime.day.twoDigitString())
        append('_')
        append(dateTime.hour.twoDigitString())
        append(dateTime.minute.twoDigitString())
        append(dateTime.second.twoDigitString())
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
        append((dateTime.month.ordinal + 1).twoDigitString())
        append('-')
        append(dateTime.day.twoDigitString())
        append(' ')
        append(dateTime.hour.twoDigitString())
        append(':')
        append(dateTime.minute.twoDigitString())
    }
}

private data class ExportFileMeta(
    val title: String,
    val authorName: String,
    val typeLabel: String,
    val typeKey: String,
    val articleId: Long,
)
