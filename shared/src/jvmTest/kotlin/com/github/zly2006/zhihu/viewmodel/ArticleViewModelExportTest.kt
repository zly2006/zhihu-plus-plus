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

package com.github.zly2006.zhihu.viewmodel

import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import kotlin.test.Test
import kotlin.test.assertEquals

class ArticleViewModelExportTest {
    @Test
    fun convert_to_markdown_should_reuse_md_ast_html_conversion() {
        val viewModel = ArticleViewModel(
            article = Article(type = ArticleType.Answer, id = 1L),
            httpClient = null,
        )
        viewModel.title = "导出标题"
        viewModel.authorName = "导出作者"
        viewModel.authorBio = "导出简介"
        viewModel.content =
            """
            <p>正文包含脚注<sup data-draft-type="reference" data-numero="1" data-text="参考资料" data-url="https://example.com/ref">[1]</sup> 和 <a href="https://link.zhihu.com/?target=https%3A%2F%2Fexample.com%2Ftarget">链接</a>。</p>
            <a class="video-box" href="https://link.zhihu.com/?target=https%3A//www.zhihu.com/video/2029631316597973958" data-lens-id="2029631316597973958">
              <img src="https://example.com/cover.jpg" />
            </a>
            """.trimIndent()

        assertEquals(
            """
            # 导出标题

            **作者**: 导出作者

            **简介**: 导出简介

            ---

            正文包含脚注[^1] 和 [链接](https://example.com/target)。

            [视频](https://www.zhihu.com/video/2029631316597973958)

            [^1]: 参考资料[https://example.com/ref](https://example.com/ref)
            """.trimIndent(),
            viewModel.convertToMarkdown(),
        )
    }
}
