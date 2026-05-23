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
import com.github.zly2006.zhihu.shared.data.DataHolder
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
import java.io.ByteArrayInputStream
import java.net.URLConnection
import java.util.Base64

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
    template: String,
    exportData: ArticleExportData,
    extraSectionsHtml: String = "",
): String {
    val footerPlaceholders = buildArticleExportFooterPlaceholders(exportData.footerData)
    val authorAvatarHtml = exportData.authorAvatarSrc
        .takeIf { it.isNotBlank() }
        ?.let {
            """
            <img
                class="author-avatar"
                src="${escapeHtml(normalizeArticleExportUrl(it))}"
                alt="作者头像"
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
        fetchArticleExportImageDataUrl(httpClient, imageUrl)
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

private fun escapeHtml(text: String): String = escapeArticleExportHtml(text)

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

    return "data:$mimeType;base64,${Base64.getEncoder().encodeToString(bytes)}"
}

private fun resolveArticleExportImageUrl(image: Element): String? = extractImageUrl(image)
    ?.let(::normalizeArticleExportUrl)
    ?: image
        .attr("src")
        .takeIf { it.isNotBlank() && !it.startsWith("data:") }
        ?.let(::normalizeArticleExportUrl)

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
