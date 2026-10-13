package com.hrm.markdown.renderer.inline

import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 行内内容（典型是长 LaTeX 公式）是排版的原子项：可以在它之前换行，但无法切开它。
 * 如果照原样记录宽度，整行会被撑破，超出的部分被右边界裁掉且无处可滚，用户永远读不到。
 */
@OptIn(ExperimentalTestApi::class)
class InlineFlowOverflowTest {
    @Test
    fun overWideInlineContentIsClampedToTheAvailableLineWidth() =
        runDesktopComposeUiTest(width = 400, height = 200) {
            val mathId = "math_overflow"
            var layout: InlineFlowLayout? = null
            setContent {
                val density = LocalDensity.current
                val textMeasurer = rememberTextMeasurer()
                val annotated = buildAnnotatedString {
                    append("前后")
                    appendInlinePlaceholder(mathId)
                }
                val inlineContents = mapOf(
                    mathId to InlineContentEntry(
                        alternateText = "长公式",
                        inlineTextContent = InlineTextContent(
                            placeholder = Placeholder(
                                width = 900.sp,
                                height = 40.sp,
                                placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
                            ),
                        ) {},
                    ),
                )
                layout = computeInlineFlowLayout(
                    annotated = annotated,
                    inlineContents = inlineContents,
                    style = TextStyle(fontSize = 16.sp),
                    density = density,
                    textMeasurer = textMeasurer,
                    maxWidthPx = 300f,
                    maxLines = Int.MAX_VALUE,
                )
            }
            waitForIdle()

            val computed = requireNotNull(layout)
            val mathItems = computed.lines.flatMap { it.items }
                .filterIsInstance<LineItem.InlineItem>()
                .filter { it.id == mathId }
            val math = mathItems.single()
            assertEquals(300f, math.widthPx, 0.01f, "超宽行内内容在排版时必须夹到可用行宽")
            assertTrue(
                computed.lines.all { it.lineWidthPx <= 300f },
                "任何一行的宽度都不得超出可用宽度",
            )
        }
}
