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

package com.github.zly2006.zhihu.export

import com.github.zly2006.zhihu.util.ArticleExportComment
import com.github.zly2006.zhihu.util.ArticleExportData
import com.github.zly2006.zhihu.util.ArticleExportFooterData
import com.github.zly2006.zhihu.util.buildArticleExportCommentsHtml
import com.github.zly2006.zhihu.util.inlineArticleExportImagesInHtml
import com.github.zly2006.zhihu.util.prepareArticleExportComment
import com.github.zly2006.zhihu.util.renderArticleExportHtml
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ArticleExportHtmlTest {
    @Test
    fun exportTemplateAssetContainsPreviewPlaceholders() {
        val template = loadExportTemplateAsset()

        assertTrue("导出模板需要保存在 assets 下，便于预览", template.isNotBlank())
        assertTrue(template.contains("{{title}}"))
        assertTrue(template.contains("{{authorAvatar}}"))
        assertTrue(template.contains("{{authorName}}"))
        assertTrue(template.contains("{{authorBio}}"))
        assertTrue(template.contains("{{voteCount}}"))
        assertTrue(template.contains("{{commentCount}}"))
        assertTrue(template.contains("{{bodyHtml}}"))
        assertTrue(template.contains("{{exportedDate}}"))
        assertTrue(template.contains("{{publishedDate}}"))
        assertTrue(template.contains("{{editedDate}}"))
        assertTrue(template.contains("{{editedDateClass}}"))
        assertTrue(template.contains("{{appAttributionClass}}"))
        assertTrue(template.contains("{{githubUrl}}"))
    }

    @Test
    fun exportHtmlContainsRequiredArticleMetadata() {
        val html = createExportHtml(
            title = "导出标题",
            authorName = "导出作者",
            authorBio = "作者简介",
            authorAvatarSrc = "https://pic1.zhimg.com/avatar.jpg",
            voteUpCount = 128,
            commentCount = 7,
            content = "<p>正文内容</p>",
        )

        assertTrue(html.contains("导出标题"))
        assertTrue(html.contains("导出作者"))
        assertTrue(html.contains("作者简介"))
        assertTrue("导出 HTML 需要包含作者头像", html.contains("https://pic1.zhimg.com/avatar.jpg"))
        assertTrue("导出 HTML 需要包含赞同数", html.contains("128"))
        assertTrue("导出 HTML 需要包含评论数", html.contains("7"))
    }

    @Test
    fun exportHtmlUsesReadableMobileLayoutAndPromotesImageSources() {
        val html = createExportHtml(
            title = "移动端导出",
            authorName = "作者",
            authorBio = "",
            authorAvatarSrc = "",
            voteUpCount = 1,
            commentCount = 2,
            content =
                """
                <noscript><img class="content_image" data-thumbnail="https://pic1.zhimg.com/thumbnail.jpg" /></noscript>
                <figure>
                    <img
                        class="origin_image"
                        src="https://pic1.zhimg.com/low-res.jpg"
                        data-original="https://pic1.zhimg.com/original.jpg"
                    />
                </figure>
                """.trimIndent(),
        )

        assertTrue("导出 HTML 需要限制到移动端阅读宽度", html.contains("max-width: 720px"))
        assertTrue("导出 HTML 需要使用原图地址", html.contains("https://pic1.zhimg.com/original.jpg"))
        assertFalse("导出 HTML 处理 content 时应忽略 noscript 内容", html.contains("https://pic1.zhimg.com/thumbnail.jpg"))
        assertTrue("统计信息需要改成 chip 样式", html.contains("stat-chip"))
        assertTrue("导出 HTML 需要内嵌赞同图标", html.contains("vote-chip-icon"))
        assertTrue("导出 HTML 需要内嵌评论图标", html.contains("comment-chip-icon"))
        assertTrue("导出模板需要提供打印样式", html.contains("@media print"))
        assertTrue("打印导出时不应保留卡片阴影", html.contains("box-shadow: none"))
        assertFalse("导出 HTML 不应继续使用 card 统计样式", html.contains("stat-card"))
        assertFalse("导出 HTML 不应显示赞同大字标签", html.contains(">赞同<"))
        assertFalse("导出 HTML 不应显示评论大字标签", html.contains(">评论<"))
    }

    @Test
    fun exportHtmlShowsDatesAndHidesEditedDateWhenSameAsPublished() {
        val html = createExportHtml(
            title = "日期导出",
            authorName = "作者",
            authorBio = "",
            authorAvatarSrc = "",
            voteUpCount = 12,
            commentCount = 3,
            content = "<p>正文内容</p>",
            footerData = ArticleExportFooterData(
                exportEpochMillis = 1_774_519_200_000,
                createdEpochSeconds = 1_773_914_400,
                updatedEpochSeconds = 1_773_914_400,
                includeAppAttribution = true,
            ),
        )

        // 避免时区等因素导致日期显示不一致，测试中只验证日期标签的存在与否，而不验证具体日期文本
        assertTrue(html.contains("导出日期："))
        assertTrue(html.contains("发布日期："))
        assertFalse("编辑和发布同一时间时不应显示编辑日期", html.contains("编辑日期："))
        assertTrue(html.contains("知乎++"))
        assertTrue(html.contains("GitHub地址：https://github.com/zly2006/zhihu-plus-plus"))
    }

    @Test
    fun exportHtmlCanHideAppAttributionAndShowEditedDate() {
        val html = createExportHtml(
            title = "日期导出",
            authorName = "作者",
            authorBio = "",
            authorAvatarSrc = "",
            voteUpCount = 12,
            commentCount = 3,
            content = "<p>正文内容</p>",
            footerData = ArticleExportFooterData(
                exportEpochMillis = 1_774_519_200_000,
                createdEpochSeconds = 1_773_914_400,
                updatedEpochSeconds = 1_774_000_800,
                includeAppAttribution = false,
            ),
        )

        // 同样不验证具体日期文本，只验证标签显示与否
        assertTrue(html.contains("导出日期："))
        assertTrue(html.contains("编辑日期："))
        assertTrue(html.contains("export-credit is-hidden"))
    }

    @Test
    fun exportCommentContentUsesRealCommentDataInsteadOfMockPlaceholders() {
        val commentsHtml = buildArticleExportCommentsHtml(
            listOf(
                ArticleExportComment(
                    authorName = "真实评论用户",
                    contentHtml = "<p>这是一条真实评论</p>",
                    createdTimeText = "2026-03-26 18:30",
                    imageSrc = "https://pic1.zhimg.com/comment-image.jpg",
                ),
            ),
        )
        val html = createExportHtml(
            title = "带评论导出",
            authorName = "作者",
            authorBio = "",
            authorAvatarSrc = "",
            voteUpCount = 12,
            commentCount = 3,
            content = "<p>正文内容</p>",
            extraSectionsHtml = commentsHtml,
        )

        assertTrue(html.contains("热门评论"))
        assertTrue(html.contains("真实评论用户"))
        assertTrue(html.contains("这是一条真实评论"))
        assertTrue(html.contains("2026-03-26 18:30"))
        assertTrue(html.contains("https://pic1.zhimg.com/comment-image.jpg"))
        assertFalse("导出评论不应继续包含 mock 用户名", html.contains("用户1"))
        assertFalse("导出评论不应继续包含 mock 文案", html.contains("这篇文章写得很好"))
    }

    @Test
    fun exportCommentPreparationStripsCommentAnchorsAndPromotesCommentImage() {
        val comment = prepareArticleExportComment(
            authorName = "评论作者",
            content =
                """
                <p>评论正文<a class="comment_img" href="https://pic1.zhimg.com/comment-image.jpg">[图片]</a></p>
                <p><a class="comment_sticker" href="https://pic1.zhimg.com/sticker.png">[表情]</a></p>
                """.trimIndent(),
            createdTimeText = "2026-03-26 18:31",
        )

        assertTrue(comment.contentHtml.contains("评论正文"))
        assertFalse(comment.contentHtml.contains("comment_img"))
        assertFalse(comment.contentHtml.contains("comment_sticker"))
        assertTrue(comment.imageSrc == "https://pic1.zhimg.com/comment-image.jpg")
    }

    @Test
    fun inlineArticleExportImagesInHtmlCanKeepRemoteUrlsWhenImageExportDisabled() = runBlocking {
        var resolveCallCount = 0

        val html = inlineArticleExportImagesInHtml(
            html =
                """
                <html>
                    <body>
                        <img src="https://pic1.zhimg.com/example.jpg" />
                    </body>
                </html>
                """.trimIndent(),
            includeImages = false,
        ) {
            resolveCallCount++
            "data:image/jpeg;base64,ignored"
        }

        assertTrue(html.contains("https://pic1.zhimg.com/example.jpg"))
        assertFalse(html.contains("data:image/jpeg;base64"))
        assertTrue("关闭图片导出时不应请求 base64 数据", resolveCallCount == 0)
    }

    @Test
    fun inlineArticleExportImagesInHtmlStillEmbedsBase64WhenImageExportEnabled() = runBlocking {
        val html = inlineArticleExportImagesInHtml(
            html =
                """
                <html>
                    <body>
                        <img src="https://pic1.zhimg.com/example.jpg" />
                    </body>
                </html>
                """.trimIndent(),
            includeImages = true,
        ) {
            "data:image/jpeg;base64,embedded"
        }

        assertTrue(html.contains("data:image/jpeg;base64,embedded"))
        assertFalse(html.contains("https://pic1.zhimg.com/example.jpg"))
    }

    @Test
    fun inlineArticleExportImagesInHtmlFetchesImagesConcurrentlyWithLimitSix() = runBlocking {
        val inputHtml = buildString {
            append("<html><body>")
            repeat(10) { index ->
                append("""<img src="https://pic1.zhimg.com/example-$index.jpg" />""")
            }
            append("</body></html>")
        }
        val lock = Any()
        var activeCount = 0
        var maxActiveCount = 0
        var resolveCallCount = 0

        val html = inlineArticleExportImagesInHtml(
            html = inputHtml,
            includeImages = true,
        ) { imageUrl ->
            synchronized(lock) {
                resolveCallCount++
                activeCount++
                maxActiveCount = maxOf(maxActiveCount, activeCount)
            }
            try {
                delay(50)
                "data:image/jpeg;base64,${imageUrl.substringAfterLast('/').substringBefore('.')}"
            } finally {
                synchronized(lock) {
                    activeCount--
                }
            }
        }

        assertTrue(resolveCallCount == 10)
        assertTrue("图片导出应并行抓取，而不是完全串行", maxActiveCount >= 2)
        assertTrue("图片导出并发数不应超过 6", maxActiveCount <= 6)
        assertTrue(html.contains("data:image/jpeg;base64,example-0"))
        assertTrue(html.contains("data:image/jpeg;base64,example-9"))
    }

    private fun createExportHtml(
        title: String,
        authorName: String,
        authorBio: String,
        authorAvatarSrc: String,
        voteUpCount: Int,
        commentCount: Int,
        content: String,
        extraSectionsHtml: String = "",
        footerData: ArticleExportFooterData = ArticleExportFooterData(
            exportEpochMillis = 1_774_519_200_000,
            createdEpochSeconds = 1_773_914_400,
            updatedEpochSeconds = 1_774_000_800,
            includeAppAttribution = true,
        ),
    ): String {
        val template = loadExportTemplateAsset()
        return renderArticleExportHtml(
            template = template,
            exportData = ArticleExportData(
                title = title,
                authorName = authorName,
                authorBio = authorBio,
                authorAvatarSrc = authorAvatarSrc,
                voteUpCount = voteUpCount,
                commentCount = commentCount,
                content = content,
                footerData = footerData,
            ),
            extraSectionsHtml = extraSectionsHtml,
        )
    }

    private fun loadExportTemplateAsset(): String {
        val templateFile = listOf(
            File("app/src/main/assets/article_export_template.html"),
            File("src/main/assets/article_export_template.html"),
        ).firstOrNull(File::exists)
            ?: throw java.io.FileNotFoundException("article_export_template.html")
        return templateFile.readText()
    }
}
