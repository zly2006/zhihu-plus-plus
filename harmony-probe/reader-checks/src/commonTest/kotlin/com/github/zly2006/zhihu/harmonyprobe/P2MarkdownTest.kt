package com.github.zly2006.zhihu.harmonyprobe

import com.hrm.markdown.parser.MarkdownParser
import com.hrm.markdown.parser.ast.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

class P2MarkdownTest {
    @Test
    fun preservesCodeWhitespaceAndDecodesEntities() {
        val markdown = dailyHtmlToMarkdown("<pre><code>if (x &lt; 2) {\n    return &#x4e2d;&#25991;;\n}</code></pre>")
        val code = MarkdownParser().parse(markdown).children.filterIsInstance<FencedCodeBlock>().single()
        assertTrue(code.literal.contains("if (x < 2) {\n    return 中文;\n}"))
    }

    @Test
    fun keepsBasicFormattingAndOnlyTrustedImages() {
        val markdown = dailyHtmlToMarkdown(
            """<h2>标题</h2><p><strong>粗体</strong></p><blockquote><p>引用</p></blockquote>
                <img src="https://pic1.zhimg.com/test.jpg" alt="图">
                <img src="http://pic1.zhimg.com/test.jpg">
                <img src="https://evil.example/a.jpg">
                <script>alert('hidden')</script>""",
        )
        val nodes = descendants(MarkdownParser().parse(markdown))
        assertTrue(nodes.any { it is Heading })
        assertTrue(nodes.any { it is StrongEmphasis })
        assertTrue(nodes.any { it is BlockQuote })
        // ExtendedFlavour promotes a standalone image into a Figure node.
        assertEquals("https://pic1.zhimg.com/test.jpg", nodes.filterIsInstance<Figure>().single().imageUrl)
        assertFalse(markdown.contains("hidden"))
        assertFalse(markdown.contains("evil.example"))
    }

    @Test
    fun stressSampleContainsRealFormulaCodeListAndTableNodes() {
        val nodes = descendants(MarkdownParser().parse(p2StressMarkdown()))
        assertEquals(80, nodes.count { it is MathBlock })
        assertEquals(300, nodes.count { it is ListItem })
        assertEquals(201, nodes.count { it is TableRow }) // includes header
        assertEquals("kotlin", nodes.filterIsInstance<FencedCodeBlock>().single().language)
    }

    private fun descendants(node: Node): List<Node> =
        listOf(node) + (node as? ContainerNode)?.children.orEmpty().flatMap { descendants(it) }
}
