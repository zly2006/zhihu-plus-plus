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

fun prepareContentDocumentForExport(content: String): Document {
    val expandedNoscriptContent = Regex("(?is)<noscript>(.*?)</noscript>").replace(content) { match ->
        val rawHtml = Entities.unescape(match.groupValues[1])
        val parsed = Jsoup.parseBodyFragment(rawHtml)
        val image = parsed.selectFirst("img")
        val fallbackUrl = Regex(
            """(?:data-original|data-default-watermark-src|data-actualsrc|data-thumbnail|src)\s*=\s*"([^"]+)"""",
        ).find(rawHtml)?.groupValues?.getOrNull(1)
        val resolvedUrl = image?.let(::extractImageUrl) ?: fallbackUrl
        resolvedUrl
            ?.let {
                """
                <img
                    src="${escapeHtml(normalizeExportUrl(it))}"
                    loading="eager"
                />
                """.trimIndent()
            }.orEmpty()
    }

    return Jsoup.parse(expandedNoscriptContent).apply {
        select("noscript").forEach { noscript ->
            val rawHtml = sequenceOf(
                noscript.wholeText(),
                noscript.data(),
                noscript.html(),
            ).firstOrNull { it.isNotBlank() }.orEmpty()
            val parsed = Jsoup.parseBodyFragment(Entities.unescape(rawHtml))
            val image = parsed.selectFirst("img")
            val fallbackUrl = Regex(
                """(?:data-original|data-default-watermark-src|data-actualsrc|data-thumbnail|src)\s*=\s*"([^"]+)"""",
            ).find(rawHtml)?.groupValues?.getOrNull(1)
            val resolvedUrl = image?.let(::extractImageUrl) ?: fallbackUrl
            if (image != null && resolvedUrl != null) {
                image.attr("src", normalizeExportUrl(resolvedUrl))
                image.removeClass("lazy")
                image.attr("loading", "eager")
                noscript.after(image.outerHtml())
            } else if (resolvedUrl != null) {
                noscript.after(
                    """
                    <img
                        src="${escapeHtml(normalizeExportUrl(resolvedUrl))}"
                        loading="eager"
                    />
                    """.trimIndent(),
                )
            }
            noscript.remove()
        }

        select("img").forEach { image ->
            extractImageUrl(image)?.let { src ->
                image.attr("src", normalizeExportUrl(src))
            }
            image.removeClass("lazy")
            image.removeAttr("srcset")
            image.removeAttr("sizes")
            image.attr("loading", "eager")
        }

        select("a[href^=//]").forEach { anchor ->
            anchor.attr("href", normalizeExportUrl(anchor.attr("href")))
        }
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
                src="${escapeHtml(normalizeExportUrl(it))}"
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

private fun normalizeExportUrl(url: String): String = when {
    url.startsWith("//") -> "https:$url"
    else -> url
}
