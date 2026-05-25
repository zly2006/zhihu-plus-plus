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
import org.jsoup.nodes.Element
import java.io.ByteArrayInputStream
import java.net.URLConnection
import java.util.Base64

private const val ARTICLE_EXPORT_IMAGE_FETCH_CONCURRENCY = 6

fun buildArticleExportHtml(
    context: Context,
    exportData: ArticleExportData,
    extraSectionsHtml: String = "",
): String = renderArticleExportHtml(
    template = loadArticleExportTemplate(context),
    exportData = exportData,
    extraSectionsHtml = extraSectionsHtml,
)

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
