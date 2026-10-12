package com.hrm.markdown.renderer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.layout.AlignmentLine
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hrm.markdown.parser.MarkdownParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class MarkdownListTableLayoutTest {
    @Test
    fun orderedMarkersAlignWithFirstTextBaseline() = runDesktopComposeUiTest(width = 360, height = 400) {
        val document = MarkdownParser().parse("1. 中文列表正文\n2. English list text")
        setContent {
            Markdown(
                document = document,
                modifier = Modifier.fillMaxSize().background(Color.White),
                theme = MarkdownTheme.light(),
                enableScroll = false,
                enableSelection = false,
            )
        }
        waitForIdle()
        val markerBaseline = onNodeWithText("1.").firstBaseline()
        val textBaseline = onNodeWithText("中文列表正文").firstBaseline()
        assertEquals(textBaseline, markerBaseline, "Ordered marker must align with the first text baseline")
        assertEquals(onNodeWithText("English list text").firstBaseline(), onNodeWithText("2.").firstBaseline())
    }

    @Test
    fun bulletAlignsWithFirstLineOfWrappedChineseText() = runDesktopComposeUiTest(width = 180, height = 300) {
        val text = "中文列表正文比较长，需要换行以后仍然对齐第一行。"
        val document = MarkdownParser().parse("- $text")
        setContent {
            Markdown(
                document = document,
                modifier = Modifier.fillMaxSize().background(Color.White),
                theme = MarkdownTheme.light().copy(bodyStyle = TextStyle(fontSize = 20.sp, lineHeight = 32.sp)),
                enableScroll = false,
                enableSelection = false,
            )
        }
        waitForIdle()
        val markerBaseline = onNodeWithText("•").firstBaseline()
        val textBaseline = onNodeWithText(text).firstBaseline()
        assertEquals(textBaseline, markerBaseline, "Bullet must align with the first text baseline")
        val layouts = mutableListOf<TextLayoutResult>()
        onNodeWithText(text).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        assertTrue(layouts.single().lineCount > 1)
    }

    @Test
    fun nestedListsKeepCompactIndentation() = runDesktopComposeUiTest(width = 360, height = 400) {
        val document = MarkdownParser().parse("1. 外层列表\n   - 内层列表\n     - 第三层列表")
        setContent {
            Markdown(
                document = document,
                modifier = Modifier.fillMaxSize().background(Color.White),
                theme = MarkdownTheme.light(),
                enableScroll = false,
                enableSelection = false,
            )
        }
        waitForIdle()
        val outer = onNodeWithText("外层列表").fetchSemanticsNode().positionInRoot.x
        val inner = onNodeWithText("内层列表").fetchSemanticsNode().positionInRoot.x
        val third = onNodeWithText("第三层列表").fetchSemanticsNode().positionInRoot.x
        assertTrue(outer <= 32f, "Outer list uses too much space: $outer")
        assertTrue(inner > outer && inner - outer <= 24f, "Nested indent: ${inner - outer}")
        assertTrue(third > inner && third - inner <= 24f, "Third-level indent: ${third - inner}")
    }

    @Test
    fun imageLeadingListKeepsMarkerAtTheImageTop() {
        for (markdown in listOf(
            "- ![列表图片](https://example.com/image.png)\n\n  图片之后的说明",
            "- ![列表图片](https://example.com/image.png) 图片之后的同段说明",
        )) {
            runDesktopComposeUiTest(width = 360, height = 400) {
                val document = MarkdownParser().parse(markdown)
                setContent {
                    Markdown(
                        document = document,
                        modifier = Modifier.fillMaxSize().background(Color.White),
                        theme = MarkdownTheme.light(),
                        imageContent = { _, imageModifier ->
                            Box(imageModifier.height(72.dp).background(Color.Gray).testTag("list-image"))
                        },
                        enableScroll = false,
                        enableSelection = false,
                    )
                }
                waitForIdle()
                val imageTop = onNodeWithTag("list-image").fetchSemanticsNode().positionInRoot.y
                val markerTop = onNodeWithText("•").fetchSemanticsNode().positionInRoot.y
                assertEquals(imageTop, markerTop, "Image-leading item must keep the marker at its start")
            }
        }
    }

    @Test
    fun orderedListSharesMarkerWidthAcrossDigitCountsAndWrappedText() = runDesktopComposeUiTest(width = 240, height = 400) {
        val first = "第一个编号跨位数测试，正文需要换行。"
        val second = "第二个编号跨位数测试，正文需要换行。"
        val document = MarkdownParser().parse("99. $first\n100. $second")
        setContent {
            Markdown(
                document = document,
                modifier = Modifier.fillMaxSize().background(Color.White),
                theme = MarkdownTheme.light().copy(bodyStyle = TextStyle(fontSize = 24.sp, lineHeight = 36.sp)),
                enableScroll = false,
                enableSelection = false,
            )
        }
        waitForIdle()
        assertEquals(
            onNodeWithText(first).fetchSemanticsNode().positionInRoot.x,
            onNodeWithText(second).fetchSemanticsNode().positionInRoot.x,
            "All ordered items must share the same text start",
        )
        for ((marker, text) in listOf("99." to first, "100." to second)) {
            assertEquals(onNodeWithText(text).firstBaseline(), onNodeWithText(marker).firstBaseline())
            val layouts = mutableListOf<TextLayoutResult>()
            onNodeWithText(marker).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            assertEquals(1, layouts.single().lineCount, "Ordered marker must not wrap")
            layouts.clear()
            onNodeWithText(text).performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
            assertTrue(layouts.single().lineCount > 1)
        }
    }

    @Test
    fun mixedLanguageTableHasOneSharedBorderPerRow() = runDesktopComposeUiTest(width = 360, height = 400) {
        val document = MarkdownParser().parse("| English | 中文标题 |\n| --- | --- |\n| Latin | 中文正文 |\n| Second | 第二行 |")
        setContent {
            Markdown(
                document = document,
                modifier = Modifier.fillMaxSize().background(Color.White),
                theme = MarkdownTheme.light().copy(
                    bodyStyle = TextStyle(fontSize = 20.sp),
                    tableBorderColor = Color.Red,
                ),
                enableScroll = false,
                enableSelection = false,
            )
        }
        waitForIdle()
        val pixels = onRoot().captureToImage().toPixelMap()
        val englishX = onNodeWithText("Latin")
            .fetchSemanticsNode()
            .positionInRoot.x
            .toInt() - 4
        val chineseX = onNodeWithText("中文正文")
            .fetchSemanticsNode()
            .positionInRoot.x
            .toInt() - 4

        fun borderRows(x: Int) = (0 until pixels.height).filter { y ->
            val color = pixels[x, y]
            color.red > 0.8f && color.green < 0.6f && color.blue < 0.6f
        }
        val englishBorders = borderRows(englishX)
        assertTrue(englishBorders.size >= 4, "Table borders were not rendered")
        assertEquals(borderRows(chineseX), englishBorders, "Cell borders must share the row height")
        val borderCount = englishBorders.zipWithNext().count { (first, second) -> second > first + 1 } + 1
        assertEquals(4, borderCount, "Three rows must have exactly four shared horizontal borders")
    }
}

private fun SemanticsNodeInteraction.firstBaseline(): Float {
    val coordinates = generateSequence(fetchSemanticsNode().layoutInfo.coordinates) { it.parentCoordinates }
        .first { it[FirstBaseline] != AlignmentLine.Unspecified }
    return coordinates.positionInRoot().y + coordinates[FirstBaseline]
}
