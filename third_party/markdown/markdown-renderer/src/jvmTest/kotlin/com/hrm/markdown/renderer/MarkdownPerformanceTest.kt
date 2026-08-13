package com.hrm.markdown.renderer

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import com.hrm.markdown.parser.MarkdownParser
import com.hrm.latex.renderer.Latex
import com.hrm.latex.renderer.LatexRenderCache
import com.hrm.latex.renderer.LocalLatexRenderCache
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@OptIn(ExperimentalTestApi::class)
class MarkdownPerformanceTest {
    @Test
    fun markdownPreviewIdentityStressRendersAcrossRepeatedDocumentChanges() =
        runDesktopComposeUiTest(width = 412, height = 892) {
            fun stressMarkdown(round: Int) = buildString {
                appendLine("回答预览正文 $round")
                appendLine()
                repeat(40) { index ->
                    appendLine("[引用 $index][ref-$round-$index]")
                    appendLine("[ref-$round-$index]: https://example.com/$round/$index")
                    appendLine("${'$'}${'$'}x_$index + y_$round${'$'}${'$'} 同行尾随正文 $index")
                    appendLine()
                }
            }
            var markdown by mutableStateOf(stressMarkdown(0))

            setContent {
                Markdown(
                    markdown = markdown,
                    modifier = Modifier.fillMaxSize(),
                    enableScroll = true,
                    enableSelection = true,
                )
            }

            repeat(8) { round ->
                runOnUiThread { markdown = stressMarkdown(round) }
                waitForRenderedMarker("回答预览正文 $round")
                waitForIdle()
                onNode(hasText("回答预览正文 $round", substring = true), useUnmergedTree = true)
                    .assertIsDisplayed()
            }
        }

    @Test
    fun preparedLatexDrawsAndSurvivesCompositionDisposal() = runDesktopComposeUiTest(width = 412, height = 892) {
        val cache = LatexRenderCache()
        var showFormula by mutableStateOf(true)
        setContent {
            CompositionLocalProvider(LocalLatexRenderCache provides cache) {
                if (showFormula) {
                    Latex("\\frac{a+b}{c+d}+\\sum_{i=1}^{n}x_i^2")
                }
            }
        }

        waitUntil("LaTeX preparation did not complete", timeoutMillis = 10_000) { cache.size == 1 }
        waitForIdle()
        val pixels = onRoot().captureToImage().toPixelMap()
        assertTrue(
            (0 until pixels.height).any { y ->
                (0 until pixels.width).any { x -> pixels[x, y] != Color.Transparent }
            },
            "Prepared formula did not draw visible pixels",
        )

        val revisionAfterPreparation = cache.revision
        runOnUiThread { showFormula = false }
        waitForIdle()
        runOnUiThread { showFormula = true }
        waitForIdle()
        onRoot().captureToImage()
        kotlin.test.assertEquals(revisionAfterPreparation, cache.revision)
    }

    @Test
    fun inlineLatexReplacesEstimateWithPreparedDimensionsAndDraws() =
        runDesktopComposeUiTest(width = 412, height = 240) {
            val cache = LatexRenderCache()
            val mathColor = Color(0xFFFF00FF)
            setContent {
                CompositionLocalProvider(LocalLatexRenderCache provides cache) {
                    Markdown(
                        markdown = "before ${'$'}`\\frac{\\sqrt{x^2+y^2}}{\\sum_{i=1}^{n} i^2}`${'$'} after",
                        modifier = Modifier.fillMaxSize(),
                        theme = MarkdownTheme.light().copy(mathColor = mathColor),
                        enableScroll = false,
                    )
                }
            }

            waitUntil("Inline LaTeX preparation did not complete", timeoutMillis = 10_000) { cache.size == 1 }
            waitForIdle()
            val pixels = onRoot().captureToImage().toPixelMap()
            val formulaPixels = buildList {
                for (y in 0 until pixels.height) {
                    for (x in 0 until pixels.width) {
                        val color = pixels[x, y]
                        if (color.red > 0.8f && color.blue > 0.8f && color.green < 0.2f && color.alpha > 0.5f) {
                            add(x to y)
                        }
                    }
                }
            }
            assertTrue(formulaPixels.size > 20, "Prepared inline formula did not draw in the requested color")
            val formulaHeight = formulaPixels.maxOf { it.second } - formulaPixels.minOf { it.second } + 1
            assertTrue(formulaHeight >= 20, "Tall inline formula appears clipped: height=$formulaHeight")
        }

    @Test
    fun lightweightMarkdownLinkStillHandlesTouch() = runDesktopComposeUiTest(width = 412, height = 892) {
        var clickedUrl: String? = null
        setContent {
            Markdown(
                document = MarkdownParser().parse("[可点击链接](https://example.com/performance)"),
                modifier = Modifier.fillMaxSize(),
                onLinkClick = { clickedUrl = it },
            )
        }

        onNode(hasText("可点击链接"), useUnmergedTree = true).performTouchInput { click() }
        waitForIdle()

        kotlin.test.assertEquals("https://example.com/performance", clickedUrl)
    }

    @Test
    fun inlineFormulaParagraphLinkStillHandlesTouch() = runDesktopComposeUiTest(width = 412, height = 892) {
        var clickedUrl: String? = null
        setContent {
            Markdown(
                markdown = "公式 ${'$'}`x^2`${'$'} 与 [混合段落链接](https://example.com/mixed)",
                modifier = Modifier.fillMaxSize(),
                onLinkClick = { clickedUrl = it },
            )
        }

        waitUntil("Mixed paragraph link was not rendered", timeoutMillis = 10_000) {
            onAllNodes(hasText("混合段落链接", substring = true), useUnmergedTree = true)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }
        onNode(hasText("混合段落链接", substring = true), useUnmergedTree = true).performTouchInput { click() }
        waitForIdle()

        kotlin.test.assertEquals("https://example.com/mixed", clickedUrl)
    }

    @Test
    fun recursivelyDeferredListMaterializesItsLastItem() = runDesktopComposeUiTest(width = 412, height = 892) {
        val lastItem = "nested-list-item-299"
        val markdown = (0..299).joinToString("\n") { "- nested-list-item-$it" }
        setContent {
            Markdown(
                markdown = markdown,
                modifier = Modifier.fillMaxSize(),
                enableScroll = true,
                enableSelection = true,
            )
        }

        waitUntil("Deferred list never exposed a scroll container", timeoutMillis = 10_000) {
            onAllNodes(hasScrollAction())
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .size == 1
        }
        val scroll = onNode(hasScrollAction())
        repeat(120) {
            val reached = onAllNodes(hasText(lastItem), useUnmergedTree = true)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
            if (reached) return@runDesktopComposeUiTest
            scroll.performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, 700f) }
            waitForIdle()
        }
        assertTrue(false, "Recursively deferred list never materialized its last item")
    }

    @Test
    fun recursivelyDeferredTableMaterializesItsLastRow() = runDesktopComposeUiTest(width = 412, height = 892) {
        val lastCell = "deferred-table-row-199"
        val markdown = buildString {
            appendLine("| 序号 | 内容 |")
            appendLine("| ---: | :--- |")
            repeat(200) { index -> appendLine("| $index | deferred-table-row-$index |") }
        }
        setContent {
            Markdown(
                markdown = markdown,
                modifier = Modifier.fillMaxSize(),
                enableScroll = true,
                enableSelection = true,
            )
        }

        val verticalScrollMatcher = SemanticsMatcher("has vertical scroll axis") { node ->
            node.config.contains(SemanticsProperties.VerticalScrollAxisRange)
        }
        waitUntil("Deferred table never exposed a scroll container", timeoutMillis = 10_000) {
            onAllNodes(verticalScrollMatcher)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }
        val verticalScroll = onNode(verticalScrollMatcher)
        repeat(120) {
            val reached = onAllNodes(hasText(lastCell), useUnmergedTree = true)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
            if (reached) return@runDesktopComposeUiTest
            verticalScroll.performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, 700f) }
            waitForIdle()
        }
        assertTrue(false, "Recursively deferred table never materialized its last row")
    }

    @Test
    fun footnoteNavigatesToDeferredDefinitionAndBackInOuterScroll() =
        runDesktopComposeUiTest(width = 412, height = 892) {
            val markdown = buildString {
                appendLine("正文开头脚注[^note]")
                appendLine()
                repeat(60) { index ->
                    appendLine("填充段落 $index：用于确保脚注定义位于当前屏幕之外。")
                    appendLine()
                }
                appendLine("[^note]: 脚注内容")
            }
            setContent {
                val scrollState = rememberScrollState()
                Column(Modifier.fillMaxSize().verticalScroll(scrollState)) {
                    Markdown(
                        markdown = markdown,
                        scrollState = scrollState,
                        enableScroll = false,
                    )
                }
            }

            val footnoteReferenceMatcher = hasText("[1]") and hasClickAction()
            waitUntil("Footnote reference was not rendered", timeoutMillis = 10_000) {
                onAllNodes(footnoteReferenceMatcher, useUnmergedTree = true)
                    .fetchSemanticsNodes(atLeastOneRootRequired = false)
                    .size == 1
            }
            val footnoteReference = onNode(footnoteReferenceMatcher, useUnmergedTree = true)
            footnoteReference.assertIsDisplayed().performClick()
            waitUntil("Footnote definition was not brought into view", timeoutMillis = 10_000) {
                runCatching {
                    onNode(hasText("脚注内容"), useUnmergedTree = true).assertIsDisplayed()
                }.isSuccess
            }
            onNode(hasText("↩"), useUnmergedTree = true).assertIsDisplayed().performClick()
            waitUntil("Footnote reference was not brought back into view", timeoutMillis = 10_000) {
                runCatching {
                    onNode(hasText("正文开头脚注", substring = true), useUnmergedTree = true).assertIsDisplayed()
                }.isSuccess
            }
        }

    @Test
    fun benchmarkRealLayoutAndDrawScenarios() = runDesktopComposeUiTest(width = 412, height = 892) {
        if (!markdownPerformanceEnabled()) return@runDesktopComposeUiTest
        var markdown by mutableStateOf("预热")
        setContent {
            Markdown(
                markdown = markdown,
                modifier = Modifier.fillMaxSize(),
                enableScroll = true,
                enableSelection = true,
            )
        }
        waitForIdle()
        onRoot().captureToImage()

        val scenarios = markdownPerformanceScenarios()
        repeat(2) { warmup ->
            scenarios.forEach { scenario ->
                val marker = "${scenario.name} warmup $warmup"
                runOnUiThread { markdown = "$marker\n\n${scenario.markdown}" }
                waitForRenderedMarker(marker)
                waitForIdle()
                onRoot().captureToImage()
            }
        }

        val samplesByScenario = scenarios.associate { scenario ->
            val samples = List(7) { iteration ->
                val marker = "${scenario.name} sample $iteration"
                val startedAt = System.nanoTime()
                runOnUiThread { markdown = "$marker\n\n${scenario.markdown}" }
                waitForRenderedMarker(marker)
                waitForIdle()
                val elapsedMs = (System.nanoTime() - startedAt) / 1_000_000.0
                onRoot().captureToImage()
                elapsedMs
            }
            val median = samples.sorted()[samples.size / 2]
            println("MarkdownJvmBenchmark ${scenario.name} samplesMs=$samples medianMs=$median")
            scenario.name to samples
        }
        val medians = samplesByScenario.mapValues { (_, samples) -> samples.sorted()[samples.size / 2] }
        val allSamples = samplesByScenario.values.flatten()
        println("MarkdownJvmBenchmark mediansMs=$medians")
        assertTrue(
            allSamples.all { it < 100.0 },
            "Every steady-state JVM sample must remain below 100 ms: $samplesByScenario",
        )
        assertTrue(
            allSamples.count { it < 50.0 } >= allSamples.size * 0.7,
            "At least 70% of steady-state JVM samples must remain below 50 ms: $samplesByScenario",
        )
        assertTrue(medians.values.all { it < 30.0 }, "All steady-state JVM scenarios must remain below 30 ms: $medians")
    }

    @Test
    fun benchmarkParseAndHeightEstimateStages() {
        if (!markdownPerformanceEnabled()) return
        val theme = MarkdownTheme.light()
        markdownPerformanceScenarios().forEach { scenario ->
            val parser = MarkdownParser()
            repeat(2) { parser.parse(scenario.markdown) }
            val parseSamples = List(7) {
                val startedAt = System.nanoTime()
                parser.parse(scenario.markdown)
                (System.nanoTime() - startedAt) / 1_000_000.0
            }
            val document = parser.parse(scenario.markdown)
            val estimateSamples = List(7) {
                val startedAt = System.nanoTime()
                document.children.forEach { estimateMarkdownBlockHeightDp(it, 412f, theme) }
                (System.nanoTime() - startedAt) / 1_000_000.0
            }
            println(
                "MarkdownJvmStages ${scenario.name} parse=${parseSamples.summary()} " +
                    "heightEstimate=${estimateSamples.summary()}",
            )
        }
    }

    @Test
    fun benchmarkScrollingThroughRealLongFormulaArticle() = runDesktopComposeUiTest(width = 412, height = 892) {
        if (!markdownPerformanceEnabled()) return@runDesktopComposeUiTest
        val cache = LatexRenderCache()
        setContent {
            CompositionLocalProvider(LocalLatexRenderCache provides cache) {
                Markdown(
                    markdown = realLongFormulaStressArticle(),
                    modifier = Modifier.fillMaxSize(),
                    enableScroll = true,
                    enableSelection = true,
                )
            }
        }
        waitUntil("Formula stress article did not create its scroll container", timeoutMillis = 10_000) {
            onAllNodes(hasScrollAction())
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }
        waitForIdle()
        onRoot().captureToImage()

        val scrollContainer = onAllNodes(hasScrollAction())[0]
        val forwardSamples = List(40) {
            val startedAt = System.nanoTime()
            scrollContainer.performSemanticsAction(SemanticsActions.ScrollBy) { scrollBy ->
                scrollBy(0f, 700f)
            }
            waitForIdle()
            val elapsedMs = (System.nanoTime() - startedAt) / 1_000_000.0
            // Keep pixel capture as a draw-completeness assertion, but exclude Skia's bitmap
            // readback because a real scroll frame presents the rendered surface without copying
            // every pixel back to the CPU.
            onRoot().captureToImage()
            elapsedMs
        }
        val backwardSamples = List(40) {
            val startedAt = System.nanoTime()
            scrollContainer.performSemanticsAction(SemanticsActions.ScrollBy) { scrollBy ->
                scrollBy(0f, -700f)
            }
            waitForIdle()
            val elapsedMs = (System.nanoTime() - startedAt) / 1_000_000.0
            onRoot().captureToImage()
            elapsedMs
        }
        println(
            "MarkdownJvmFormulaScroll forward=${forwardSamples.summary()} " +
                "backward=${backwardSamples.summary()} preparedFormulaCount=${cache.size}",
        )
        assertTrue(cache.size >= 20, "Stress scroll did not prepare enough real formulas: ${cache.size}")
        assertTrue(
            forwardSamples.all { it < 100.0 } && backwardSamples.all { it < 100.0 },
            "Every prepared-formula scroll step must remain below 100 ms: forward=$forwardSamples backward=$backwardSamples",
        )
        assertTrue(
            forwardSamples.count { it < 50.0 } >= forwardSamples.size * 0.7 &&
                backwardSamples.count { it < 50.0 } >= backwardSamples.size * 0.7,
            "At least 70% of scroll steps in each direction must remain below 50 ms: " +
                "forward=$forwardSamples backward=$backwardSamples",
        )
    }

    @Test
    fun benchmarkUiResponseWhileLongFormulasPrepare() = runDesktopComposeUiTest(width = 412, height = 892) {
        if (!markdownPerformanceEnabled()) return@runDesktopComposeUiTest
        val cache = LatexRenderCache()
        val scrollState = ScrollState(0)
        var frameMarker by mutableIntStateOf(0)
        setContent {
            CompositionLocalProvider(LocalLatexRenderCache provides cache) {
                Box {
                    Markdown(
                        markdown = realLongFormulaStressArticle(),
                        modifier = Modifier.fillMaxSize(),
                        scrollState = scrollState,
                        enableScroll = true,
                        enableSelection = true,
                    )
                    BasicText("formula-response-$frameMarker")
                }
            }
        }
        waitForIdle()

        val coldStartedAt = System.nanoTime()
        runOnUiThread {
            scrollState.dispatchRawDelta(700f)
            frameMarker = -1
        }
        waitUntil("UI warmup frame did not render", timeoutMillis = 2_000) {
            onAllNodes(hasText("formula-response--1"), useUnmergedTree = true)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }
        runOnUiThread {
            scrollState.dispatchRawDelta(-700f)
            frameMarker = -2
        }
        waitUntil("UI return warmup frame did not render", timeoutMillis = 2_000) {
            onAllNodes(hasText("formula-response--2"), useUnmergedTree = true)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }
        val coldResponseMs = (System.nanoTime() - coldStartedAt) / 1_000_000.0

        val samples = List(40) { iteration ->
            val expectedMarker = iteration + 1
            val startedAt = System.nanoTime()
            runOnUiThread {
                scrollState.dispatchRawDelta(700f)
                frameMarker = expectedMarker
            }
            waitUntil("UI did not present frame $expectedMarker", timeoutMillis = 2_000) {
                onAllNodes(hasText("formula-response-$expectedMarker"), useUnmergedTree = true)
                    .fetchSemanticsNodes(atLeastOneRootRequired = false)
                    .isNotEmpty()
            }
            (System.nanoTime() - startedAt) / 1_000_000.0
        }
        println("MarkdownJvmFormulaUiResponse coldWarmupMs=$coldResponseMs ${samples.summary()}")
        assertTrue(samples.all { it < 100.0 }, "Formula scrolling blocked UI response for 100 ms: $samples")
        assertTrue(
            samples.count { it < 50.0 } >= samples.size * 0.7,
            "At least 70% of formula-scroll UI responses must remain below 50 ms: $samples",
        )

        waitForIdle()
        onRoot().captureToImage()
        assertTrue(cache.size >= 20, "Responsive scrolling did not eventually prepare real formulas: ${cache.size}")
    }

    private suspend fun androidx.compose.ui.test.ComposeUiTest.waitForRenderedMarker(marker: String) {
        waitUntil("Markdown did not render marker: $marker", timeoutMillis = 10_000) {
            onAllNodes(hasText(marker), useUnmergedTree = true)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }
    }
}

private fun markdownPerformanceEnabled(): Boolean = System.getenv("MARKDOWN_PERFORMANCE") == "1"

private fun List<Double>.summary(): String {
    val sorted = sorted()
    fun percentile(fraction: Double) = sorted[((lastIndex * fraction).toInt()).coerceIn(indices)]
    return "samplesMs=$this medianMs=${percentile(0.5)} p90Ms=${percentile(0.9)} maxMs=${sorted.last()}"
}

private data class MarkdownPerformanceScenario(
    val name: String,
    val markdown: String,
)

private fun markdownPerformanceScenarios(): List<MarkdownPerformanceScenario> = listOf(
    MarkdownPerformanceScenario("short-prose", "一段普通正文，用于覆盖最常见的短回答。"),
    repeatedParagraphScenario("plain-paragraphs") { index ->
        "第 $index 段普通正文，包含用于对齐长度的内容以及 English words。"
    },
    repeatedParagraphScenario("bold-paragraphs") { index ->
        "第 $index 段包含 **加粗内容** 以及用于对齐长度的普通正文。"
    },
    repeatedParagraphScenario("italic-paragraphs") { index ->
        "第 $index 段包含 *斜体内容* 以及用于对齐长度的普通正文。"
    },
    repeatedParagraphScenario("strike-paragraphs") { index ->
        "第 $index 段包含 ~~删除线内容~~ 以及用于对齐长度的普通正文。"
    },
    repeatedParagraphScenario("link-paragraphs") { index ->
        "第 $index 段包含 [链接内容](https://example.com/$index) 以及用于对齐长度的普通正文。"
    },
    MarkdownPerformanceScenario(
        "formatted-prose",
        (1..30).joinToString("\n\n") { index ->
            "第 $index 段包含 **加粗**、*斜体*、~~删除线~~ 和 [链接](https://example.com/$index)。"
        },
    ),
    MarkdownPerformanceScenario(
        "long-prose",
        (1..400).joinToString("\n\n") { index ->
            "第 $index 段长正文用于覆盖超长回答，包含中文、English words and numbers 1234567890。"
        },
    ),
    MarkdownPerformanceScenario(
        "headings-and-quotes",
        (1..80).joinToString("\n\n") { index -> "## 标题 $index\n\n> 第 $index 条引用正文" },
    ),
    MarkdownPerformanceScenario(
        "lists",
        (1..300).joinToString("\n") { index -> "- 列表项 $index，带有 **强调内容**" },
    ),
    MarkdownPerformanceScenario(
        "code-blocks",
        (1..50).joinToString("\n\n") { index ->
            "```kotlin\nval item$index = List(20) { it * $index }\nprintln(item$index.sum())\n```"
        },
    ),
    MarkdownPerformanceScenario(
        "inline-math",
        (1..150).joinToString("\n\n") { index ->
            "第 $index 个公式为 ${'$'}`x_$index = \\frac{a_$index+b_$index}{c_$index}`${'$'}，并保留周围正文。"
        },
    ),
    MarkdownPerformanceScenario(
        "block-math",
        (1..80).joinToString("\n\n") { index ->
            "${'$'}${'$'}\\sum_{i=1}^{n} \\frac{x_i^{$index}}{1+x_i^2}${'$'}${'$'}"
        },
    ),
    MarkdownPerformanceScenario(
        "tables",
        buildString {
            appendLine("| 序号 | 名称 | 描述 |")
            appendLine("| ---: | :--- | :--- |")
            repeat(200) { index -> appendLine("| $index | 项目 $index | 第 $index 行表格内容 |") }
        },
    ),
    MarkdownPerformanceScenario(
        "mixed-document",
        (1..100).joinToString("\n\n") { index ->
            "## 小节 $index\n\n正文 **$index** 与行内公式 ${'$'}`x_$index^2`${'$'}。\n\n" +
                "- 要点 A\n- 要点 B\n\n> 引用 $index"
        },
    ),
    MarkdownPerformanceScenario(
        name = "real-long-formula-stress",
        markdown = realLongFormulaStressArticle(),
    ),
)

private fun repeatedParagraphScenario(
    name: String,
    paragraph: (Int) -> String,
): MarkdownPerformanceScenario = MarkdownPerformanceScenario(
    name = name,
    markdown = (1..30).joinToString("\n\n", transform = paragraph),
)

private fun realLongFormulaStressArticle(): String {
    val corpus = File("../../../shared/src/jvmTest/resources/zhihu-formula-corpus/formulas.json")
    check(corpus.isFile) { "Missing project formula corpus: ${corpus.absolutePath}" }
    val formulas = Json.parseToJsonElement(corpus.readText())
        .jsonArray
        .map { it.jsonObject.getValue("latex").jsonPrimitive.content }
        .distinct()
        .sortedByDescending(String::length)
        .take(80)
    return buildString {
        appendLine("# 项目真实长公式压力文章")
        appendLine()
        formulas.forEachIndexed { index, latex ->
            appendLine("## 长公式 ${index + 1}（${latex.length} 字符）")
            appendLine()
            appendLine("${'$'}${'$'}")
            appendLine(latex)
            appendLine("${'$'}${'$'}")
            appendLine()
        }
    }
}
