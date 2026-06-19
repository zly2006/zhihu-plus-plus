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

package com.github.zly2006.zhihu.editor

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class ZhihuMarkdownCompilerTest {
    @Test
    fun headings_should_normalize_by_used_levels() = runTest {
        val html = compileMdToZhihuHtml(
            markdown =
                """
                # 一级

                ### 二级

                ###### 低级
                """.trimIndent(),
        )

        assertEquals(
            """
            <h2>一级</h2>
            <h3>二级</h3>
            <p><strong>低级</strong></p>
            """.replace("\n", "").replace(" ", ""),
            html.replace("\n", "").replace(" ", ""),
        )
    }

    @Test
    fun math_should_render_as_zhihu_equation_images() = runTest {
        val html = compileMdToZhihuHtml(
            markdown =
                """
                行内 $1/2$

                $$
                E=mc^2
                $$
                """.trimIndent(),
        )

        assertContains(html, """<img eeimg="1" src="//www.zhihu.com/equation?tex=1%2F2" alt="1/2" />""")
        assertContains(html, """<img eeimg="2" src="//www.zhihu.com/equation?tex=E%3Dmc%5E2" alt="E=mc^2" />""")
    }
}
