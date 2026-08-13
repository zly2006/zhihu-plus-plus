package com.hrm.markdown.parser

import com.hrm.markdown.parser.ast.ContainerNode
import com.hrm.markdown.parser.ast.Document
import com.hrm.markdown.parser.ast.Heading
import com.hrm.markdown.parser.ast.Node
import com.hrm.markdown.parser.ast.Paragraph
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class StableKeyTest {
    @Test
    fun parsedDocumentsKeepEveryNodeKeyUnique() {
        parserFixtures.forEach { fixture ->
            MarkdownParser().parse(fixture.markdown).assertUniqueStableKeys(fixture.name)
        }
    }

    @Test
    fun generatedAdversarialDocumentsKeepEveryNodeKeyUnique() {
        repeat(64) { round ->
            val markdown = buildString {
                repeat(24) { index ->
                    appendLine("[正文 $round-$index][ref-$round-$index] HTML")
                    appendLine("[ref-$round-$index]: https://example.com/$round/$index")
                    appendLine("*[HTML]: HyperText Markup Language")
                    appendLine("${'$'}${'$'}x_$index${'$'}${'$'} 同行尾随正文 $index")
                    appendLine("> 引用 **$index**")
                    appendLine("- 列表 $index")
                    appendLine()
                }
            }
            MarkdownParser().parse(markdown).assertUniqueStableKeys("generated round $round")
        }
    }

    @Test
    fun reparsingSameStructureProducesTheSameKeys() {
        parserFixtures.forEach { fixture ->
            val firstKeys = MarkdownParser().parse(fixture.markdown).allStableKeys()
            val secondKeys = MarkdownParser().parse(fixture.markdown).allStableKeys()
            assertEquals(firstKeys, secondKeys, "${fixture.name} must produce deterministic keys")
        }
    }

    @Test
    fun changingOnlyBlockContentPreservesStructuralKeys() {
        val first = MarkdownParser().parse("第一段\n\n第二段").allStableKeys()
        val second = MarkdownParser().parse("内容已替换\n\n仍是第二段").allStableKeys()

        assertEquals(first, second)
    }

    @Test
    fun sameLineAndSameTypeNodesStillReceiveDifferentKeys() {
        val document = Document()
        val first = Paragraph().apply { lineRange = LineRange(0, 1) }
        val second = Paragraph().apply { lineRange = LineRange(0, 1) }

        document.appendChild(first)
        document.appendChild(second)

        assertNotEquals(first.stableKey, second.stableKey)
        document.assertUniqueStableKeys("manually constructed same-line paragraphs")
    }

    @Test
    fun replacingNodePreservesItsStableKey() {
        val document = Document()
        val paragraph = Paragraph()
        document.appendChild(paragraph)
        val originalKey = paragraph.stableKey

        val heading = Heading(level = 2)
        document.replaceChild(paragraph, heading)

        assertEquals(originalKey, heading.stableKey)
        document.assertUniqueStableKeys("node replacement")
    }

    @Test
    fun replacingChildRangePreservesOverlappingStableKeys() {
        val document = Document()
        val oldChildren = List(3) { Paragraph().also(document::appendChild) }
        val oldKeys = oldChildren.map { it.stableKey }
        val replacements = List(4) { Paragraph() }

        document.replaceChildren(0, oldChildren.size, replacements)

        assertEquals(oldKeys, replacements.take(oldKeys.size).map { it.stableKey })
        assertTrue(replacements.last().stableKey !in oldKeys)
        document.assertUniqueStableKeys("child range replacement")
    }

    @Test
    fun insertingSiblingDoesNotRenumberExistingKeys() {
        val document = Document()
        val first = Paragraph()
        val second = Paragraph()
        document.appendChild(first)
        document.appendChild(second)
        val keysBeforeInsert = listOf(first.stableKey, second.stableKey)

        val inserted = Paragraph()
        document.insertChild(1, inserted)

        assertEquals(keysBeforeInsert, listOf(first.stableKey, second.stableKey))
        assertTrue(inserted.stableKey !in keysBeforeInsert)
        document.assertUniqueStableKeys("sibling insertion")
    }

    @Test
    fun nestedKeysAreRebasedWhenContainerIsAttached() {
        val nested = Paragraph()
        val container = Paragraph().apply { appendChild(nested) }
        val document = Document().apply { appendChild(container) }

        assertTrue(nested.stableKey.startsWith("${container.stableKey}/"))
        document.assertUniqueStableKeys("nested attachment")
    }

    @Test
    fun incrementalEditsAndStreamingAppendsKeepKeysUnique() {
        val editor = MarkdownParser()
        val initial = "第一段\n\n第二段\n\n[docs]: https://example.com/initial"
        editor.parse(initial).assertUniqueStableKeys("edit initial")
        editor.replace(0, 3, "# 第一段").assertUniqueStableKeys("edit replace block type")
        editor.insert(editor.currentText().length, "\n\n${'$'}${'$'}x${'$'}${'$'} 尾随段落")
            .assertUniqueStableKeys("edit append same-line blocks")

        val streaming = MarkdownParser()
        streaming.beginStream()
        streaming.append("[正文][docs]\n\n").assertUniqueStableKeys("stream paragraph")
        streaming.append("[docs]: https://example.com/").assertUniqueStableKeys("stream link definition")
        streaming.append("\n\n${'$'}${'$'}x${'$'}${'$'} 尾随段落")
            .assertUniqueStableKeys("stream same-line blocks")
        streaming.endStream().assertUniqueStableKeys("stream final document")
    }

    private fun Node.assertUniqueStableKeys(context: String) {
        val allKeys = allStableKeys(context)

        assertEquals(
            allKeys.size,
            allKeys.toSet().size,
            "$context contains duplicate stable keys: ${allKeys.groupingBy { it }.eachCount().filterValues { it > 1 }}",
        )
    }

    private fun Node.allStableKeys(context: String = "document"): List<String> {
        val allKeys = mutableListOf<String>()

        fun collect(node: Node) {
            if (node !is Document) {
                assertTrue(node.stableKey.isNotEmpty(), "$context contains an unassigned key")
                allKeys += node.stableKey
            }
            if (node is ContainerNode) node.children.forEach(::collect)
        }

        collect(this)
        return allKeys
    }

    private data class ParserFixture(
        val name: String,
        val markdown: String,
    )

    private companion object {
        val parserFixtures = listOf(
            ParserFixture(
                "answer preview reference definition",
                """
                    [回答预览正文][docs]

                    [docs]: https://example.com/preview
                """.trimIndent(),
            ),
            ParserFixture(
                "multiple metadata definitions",
                """
                    [正文][one] HTML

                    [one]: https://example.com/one
                    [two]: https://example.com/two
                    *[HTML]: HyperText Markup Language
                """.trimIndent(),
            ),
            ParserFixture(
                "multiline reference title",
                """
                    [正文][docs]

                    [docs]: https://example.com/docs
                      "跨行标题"
                """.trimIndent(),
            ),
            ParserFixture(
                "same-line math and trailing paragraph",
                "${'$'}${'$'}x + y${'$'}${'$'} 尾随正文",
            ),
            ParserFixture(
                "nested same-line blocks",
                """
                    > ${'$'}${'$'}x${'$'}${'$'} 引用内尾随正文

                    - ${'$'}${'$'}y${'$'}${'$'} 列表内尾随正文
                    - 第二项
                """.trimIndent(),
            ),
            ParserFixture(
                "post-processor replacements",
                """
                    ```mermaid
                    graph TD; A-->B
                    ```

                    ![图](https://example.com/image.png)
                """.trimIndent(),
            ),
            ParserFixture(
                "footnote and nested formatting",
                """
                    正文[^note]

                    [^note]: **粗体**、[链接](https://example.com) 与公式 ${'$'}x${'$'}
                """.trimIndent(),
            ),
        )
    }
}
