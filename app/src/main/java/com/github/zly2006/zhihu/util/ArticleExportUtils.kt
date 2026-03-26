package com.github.zly2006.zhihu.util

import android.content.Context
import com.github.zly2006.zhihu.data.DataHolder
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.Entities
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

const val ARTICLE_EXPORT_TEMPLATE_ASSET = "article_export_template.html"
const val ARTICLE_EXPORT_GITHUB_URL = "https://github.com/zly2006/zhihu-plus-plus"
private const val ARTICLE_EXPORT_FILE_TITLE_LIMIT = 24

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
    template = loadArticleExportTemplate(context),
    exportData = exportData,
    extraSectionsHtml = extraSectionsHtml,
)

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

suspend fun inlineArticleExportImagesInHtml(
    html: String,
    resolveDataUrl: suspend (String) -> String,
): String {
    val document = Jsoup.parse(html)
    document.outputSettings().prettyPrint(false)
    inlineArticleExportImages(document.select("img"), resolveDataUrl)
    return document.outerHtml()
}

suspend fun inlineArticleExportImages(
    imageNodes: Iterable<Element>,
    resolveDataUrl: suspend (String) -> String,
) {
    imageNodes.forEach { image ->
        val imageUrl = resolveArticleExportImageUrl(image) ?: return@forEach
        image.attr("src", resolveDataUrl(imageUrl))
        image.removeClass("lazy")
        image.removeAttr("srcset")
        image.removeAttr("sizes")
        image.attr("loading", "eager")
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
                    <div class="comment-author">${escapeHtml(comment.authorName)}</div>
                    <div class="comment-content">${comment.contentHtml}</div>
                    ${buildArticleExportCommentImageHtml(comment.imageSrc)}
                    <div class="comment-time">${escapeHtml(comment.createdTimeText)}</div>
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
            src="${escapeHtml(it)}"
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
    timestamp: String,
    extension: String,
): String {
    val (title, typeLabel, typeKey, articleId) = when (content) {
        is DataHolder.Answer -> ExportFileMeta(
            title = content.question.title,
            typeLabel = "回答",
            typeKey = "answer",
            articleId = content.id,
        )
        is DataHolder.Article -> ExportFileMeta(
            title = content.title,
            typeLabel = "文章",
            typeKey = "article",
            articleId = content.id,
        )
        else -> throw IllegalArgumentException("Unsupported export content type: ${content::class.simpleName}")
    }
    val safeTitle = sanitizeArticleExportFileNamePart(title)
        .take(ARTICLE_EXPORT_FILE_TITLE_LIMIT)
        .ifBlank { "untitled" }
    val normalizedExtension = extension.trimStart('.')

    return "zhihu++_title_${safeTitle}的${typeLabel}_${typeKey}_${articleId}_$timestamp.$normalizedExtension"
}

private fun resolveArticleExportImageUrl(image: Element): String? = extractImageUrl(image)
    ?.let(::normalizeArticleExportUrl)
    ?: image
        .attr("src")
        .takeIf { it.isNotBlank() && !it.startsWith("data:") }
        ?.let(::normalizeArticleExportUrl)

private fun sanitizeArticleExportFileNamePart(text: String): String = text
    .trim()
    .replace(Regex("\\s+"), "_")
    .replace(Regex("[\\\\/:*?\"<>|]"), "_")
    .replace(Regex("_+"), "_")
    .trim('_')

private data class ExportFileMeta(
    val title: String,
    val typeLabel: String,
    val typeKey: String,
    val articleId: Long,
)
