package com.github.zly2006.zhihu.markdown

import com.hrm.markdown.parser.ast.NativeBlock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MdAstTest {
    @Test
    fun video_box_anchor_should_convert_to_native_block() {
        val document = htmlToMdAst(
            """
            <a class="video-box" href="https://link.zhihu.com/?target=https%3A//www.zhihu.com/video/2029631316597973958" data-lens-id="2029631316597973958">
              <img src="https://example.com/cover.jpg" />
            </a>
            """.trimIndent(),
        )

        assertEquals(1, document.children.size)
        assertTrue(document.children.single() is NativeBlock)
    }
}
