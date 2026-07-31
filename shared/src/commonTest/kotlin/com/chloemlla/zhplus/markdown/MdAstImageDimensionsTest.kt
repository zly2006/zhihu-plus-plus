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

package com.chloemlla.zhplus.markdown

import com.hrm.markdown.parser.ast.Figure
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class MdAstImageDimensionsTest {
    @Test
    fun standaloneZhihuImagePreservesItsRawDimensions() {
        val document = htmlToMdAst(
            """
            <img
                src="https://picx.zhimg.com/example.jpg"
                data-rawwidth="1200"
                data-rawheight="880"
                width="1200"
            />
            """.trimIndent(),
        )

        val figure = assertIs<Figure>(document.children.single())
        assertEquals(1200, figure.imageWidth)
        assertEquals(880, figure.imageHeight)
    }
}
