package com.github.zly2006.zhihu.util

import android.content.Context
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Entities

const val ARTICLE_EXPORT_MAX_WIDTH_PX = 720
const val ARTICLE_EXPORT_TEMPLATE_ASSET = "article_export_template.html"

data class ArticleExportData(
    val title: String,
    val authorName: String,
    val authorBio: String,
    val authorAvatarSrc: String,
    val voteUpCount: Int,
    val commentCount: Int,
    val content: String,
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
        ),
    )
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

fun normalizeArticleExportUrl(url: String): String = when {
    url.startsWith("//") -> "https:$url"
    else -> url
}
