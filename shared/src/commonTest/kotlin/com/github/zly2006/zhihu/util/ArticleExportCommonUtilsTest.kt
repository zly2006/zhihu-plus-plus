package com.github.zly2006.zhihu.util

import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ArticleExportCommonUtilsTest {
    @Test
    fun renderArticleExportHtmlKeepsTemplatePlaceholdersAndNormalizesBody() {
        val html = renderArticleExportHtml(
            template =
                """
                <article>
                    <h1>{{title}}</h1>
                    {{authorAvatar}}
                    <div class="author">{{authorName}}</div>
                    {{authorBio}}
                    <div class="body">{{bodyHtml}}</div>
                    <footer>{{publishedDate}}|{{editedDate}}|{{appAttributionClass}}|{{githubUrl}}</footer>
                    {{extraSections}}
                </article>
                """.trimIndent(),
            exportData = ArticleExportData(
                title = "A&B",
                authorName = "作者",
                authorBio = "<bio>",
                authorAvatarSrc = "//pic.example/avatar.jpg",
                voteUpCount = 1,
                commentCount = 2,
                content =
                    """
                    <noscript>fallback</noscript>
                    <p><img class="lazy" data-original="https://pic.example/original.jpg" srcset="x 1x" sizes="1px"></p>
                    <a href="//www.zhihu.com/question/1">link</a>
                    """.trimIndent(),
                footerData = ArticleExportFooterData(
                    exportEpochMillis = 1_700_000_000_000L,
                    createdEpochSeconds = 1_600_000_000L,
                    updatedEpochSeconds = 1_600_000_060L,
                    includeAppAttribution = true,
                ),
            ),
            extraSectionsHtml = "<section>extra</section>",
        )

        assertContains(html, "<h1>A&amp;B</h1>")
        assertContains(html, "src=\"https://pic.example/avatar.jpg\"")
        assertContains(html, "<div class=\"author-bio\">&lt;bio&gt;</div>")
        assertContains(html, "src=\"https://pic.example/original.jpg\"")
        assertContains(html, "loading=\"eager\"")
        assertContains(html, "href=\"https://www.zhihu.com/question/1\"")
        assertContains(html, "export-credit")
        assertContains(html, "<section>extra</section>")
    }

    @Test
    fun prepareArticleExportContentHtmlRemovesNoscriptAndLazyImageAttrs() {
        val html = prepareArticleExportContentHtml(
            "<noscript>x</noscript><img class=\"lazy\" data-actualsrc=\"//pic.example/a.png\" srcset=\"a\" sizes=\"b\">",
        )

        assertEquals(
            "<img data-actualsrc=\"//pic.example/a.png\" src=\"https://pic.example/a.png\" loading=\"eager\">",
            html,
        )
    }

    @Test
    fun inlineArticleExportImagesInHtmlUsesOriginalUrlWhenConfigured() = kotlinx.coroutines.test.runTest {
        val html = inlineArticleExportImagesInHtml(
            html = "<html><body><img src=\"https://pic.example/a.jpg\"></body></html>",
            useOriginalOnImageFetchFailure = true,
        ) {
            error("download failed")
        }

        assertContains(html, "src=\"https://pic.example/a.jpg\"")
    }

    @Test
    fun inlineArticleExportImagesInHtmlPropagatesFailureByDefault() = kotlinx.coroutines.test.runTest {
        assertFailsWith<IllegalStateException> {
            inlineArticleExportImagesInHtml(
                html = "<html><body><img src=\"https://pic.example/a.jpg\"></body></html>",
            ) {
                error("download failed")
            }
        }
    }
}
