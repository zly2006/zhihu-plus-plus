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

import com.github.zly2006.zhihu.shared.data.DataHolder
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
    fun buildArticleExportHtmlCentralizesAssetLoadingAndContentMapping() {
        val loadedAssets = mutableListOf<String>()
        val html = buildArticleExportHtml(
            loadAssetText = { fileName ->
                loadedAssets += fileName
                "<h1>{{title}}</h1><main>{{bodyHtml}}</main>{{extraSections}}"
            },
            content = sampleArticleContent(),
            includeAppAttribution = true,
            extraSectionsHtml = "<aside>comments</aside>",
        )

        assertEquals(listOf(ARTICLE_EXPORT_TEMPLATE_ASSET), loadedAssets)
        assertContains(html, "<h1>导出公共路径</h1>")
        assertContains(html, "<p>正文</p>")
        assertContains(html, "<aside>comments</aside>")
    }

    @Test
    fun resolveArticleExportImageMimeTypeUsesHeaderNameAndBytes() {
        assertEquals(
            "image/webp",
            resolveArticleExportImageMimeType(
                contentTypeHeader = "image/webp; charset=binary",
                imageUrl = "https://pic.example/a",
                imageBytes = byteArrayOf(),
            ),
        )
        assertEquals(
            "image/png",
            resolveArticleExportImageMimeType(
                contentTypeHeader = null,
                imageUrl = "https://pic.example/a.png?x=1",
                imageBytes = byteArrayOf(),
            ),
        )
        assertEquals(
            "image/jpeg",
            resolveArticleExportImageMimeType(
                contentTypeHeader = null,
                imageUrl = "https://pic.example/a",
                imageBytes = byteArrayOf(0xff.toByte(), 0xd8.toByte(), 0xff.toByte()),
            ),
        )
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

    private fun sampleArticleContent(): DataHolder.Article = DataHolder.Article(
        id = 1001L,
        author = DataHolder.Author(
            avatarUrl = "",
            gender = 0,
            headline = "作者简介",
            id = "export-author",
            isAdvertiser = false,
            isOrg = false,
            name = "作者",
            type = "people",
            url = "https://www.zhihu.com/people/export-author",
            urlToken = "export-author",
            userType = "people",
        ),
        canComment = DataHolder.CanComment(status = true, reason = ""),
        title = "导出公共路径",
        content = "<p>正文</p>",
        excerpt = "正文",
        type = "article",
        created = 1_710_000_000L,
        updated = 1_710_000_600L,
        url = "https://zhuanlan.zhihu.com/p/1001",
        voteupCount = 12,
    )
}
