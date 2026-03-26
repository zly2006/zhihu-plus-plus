package com.github.zly2006.zhihu.export

import com.github.zly2006.zhihu.util.ArticleExportData
import com.github.zly2006.zhihu.util.renderArticleExportHtml
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
        assertFalse("导出 HTML 不应继续使用 card 统计样式", html.contains("stat-card"))
        assertFalse("导出 HTML 不应显示赞同大字标签", html.contains(">赞同<"))
        assertFalse("导出 HTML 不应显示评论大字标签", html.contains(">评论<"))
    }

    private fun createExportHtml(
        title: String,
        authorName: String,
        authorBio: String,
        authorAvatarSrc: String,
        voteUpCount: Int,
        commentCount: Int,
        content: String,
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
            ),
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
