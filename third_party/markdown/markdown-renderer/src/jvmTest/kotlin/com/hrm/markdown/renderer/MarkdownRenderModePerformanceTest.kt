package com.hrm.markdown.renderer

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.v2.runDesktopComposeUiTest
import java.io.File
import kotlin.test.Test
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

@OptIn(ExperimentalTestApi::class)
class MarkdownRenderModePerformanceTest {
    @Test
    fun benchmarkDeferredRenderModes() = runDesktopComposeUiTest(width = 412, height = 892) {
        if (System.getenv("MARKDOWN_PERFORMANCE") != "1") return@runDesktopComposeUiTest
        var markdown by mutableStateOf("预热")
        var mode by mutableStateOf(CurrentRenderMode.InternalLazy)
        var compositionRevision by mutableIntStateOf(0)
        setContent {
            key(compositionRevision) {
                when (mode) {
                    CurrentRenderMode.SelectableDeferred -> Markdown(
                        markdown = markdown,
                        modifier = Modifier.fillMaxSize(),
                        enableScroll = true,
                        enableSelection = true,
                    )

                    CurrentRenderMode.InternalLazy -> Markdown(
                        markdown = markdown,
                        modifier = Modifier.fillMaxSize(),
                        enableScroll = true,
                        enableSelection = false,
                    )

                }
            }
        }
        waitForIdle()
        onRoot().captureToImage()

        val scenarios = renderModeScenarios()
        for (candidate in CurrentRenderMode.entries) {
            repeat(2) { warmup ->
                scenarios.forEach { scenario ->
                    runOnUiThread {
                        compositionRevision++
                        mode = candidate
                        markdown = "${scenario.name} warmup $warmup\n\n${scenario.markdown}"
                    }
                    waitForRenderedMarker("${scenario.name} warmup $warmup")
                    waitForIdle()
                    onRoot().captureToImage()
                }
            }
            scenarios.forEach { scenario ->
                val samples = List(7) { iteration ->
                    val startedAt = System.nanoTime()
                    runOnUiThread {
                        compositionRevision++
                        mode = candidate
                        markdown = "${scenario.name} sample $iteration\n\n${scenario.markdown}"
                    }
                    waitForRenderedMarker("${scenario.name} sample $iteration")
                    waitForIdle()
                    onRoot().captureToImage()
                    (System.nanoTime() - startedAt) / 1_000_000.0
                }
                println("CurrentMarkdownMode lifecycle=Recreated mode=$candidate scenario=${scenario.name} total=${samples.modeSummary()}")
            }
        }
    }

    @Test
    fun benchmarkLongFormulaScrollInDeferredModes() = runDesktopComposeUiTest(width = 412, height = 892) {
        if (System.getenv("MARKDOWN_PERFORMANCE") != "1") return@runDesktopComposeUiTest
        val markdown = renderModeFormulaArticle()
        var mode by mutableStateOf(CurrentRenderMode.InternalLazy)
        setContent {
            when (mode) {
                CurrentRenderMode.SelectableDeferred -> Markdown(
                    markdown,
                    Modifier.fillMaxSize(),
                    enableScroll = true,
                    enableSelection = true,
                )
                CurrentRenderMode.InternalLazy -> Markdown(
                    markdown,
                    Modifier.fillMaxSize(),
                    enableScroll = true,
                    enableSelection = false,
                )
            }
        }
        for (candidate in CurrentRenderMode.entries) {
            runOnUiThread { mode = candidate }
            waitForRenderedMarker("项目真实长公式压力文章")
            waitForIdle()
            onRoot().captureToImage()
            val scroll = onAllNodes(hasScrollAction())[0]
            val forward = List(40) {
                val startedAt = System.nanoTime()
                scroll.performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, 700f) }
                waitForIdle()
                onRoot().captureToImage()
                (System.nanoTime() - startedAt) / 1_000_000.0
            }
            val backward = List(40) {
                val startedAt = System.nanoTime()
                scroll.performSemanticsAction(SemanticsActions.ScrollBy) { it(0f, -700f) }
                waitForIdle()
                onRoot().captureToImage()
                (System.nanoTime() - startedAt) / 1_000_000.0
            }
            println(
                "CurrentFormulaScroll mode=$candidate forward=${forward.modeSummary()} backward=${backward.modeSummary()}",
            )
        }
    }

    private suspend fun androidx.compose.ui.test.ComposeUiTest.waitForRenderedMarker(marker: String) {
        waitUntil("Markdown did not render marker: $marker", timeoutMillis = 30_000) {
            onAllNodes(hasText(marker, substring = true), useUnmergedTree = true)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }
    }
}

private enum class CurrentRenderMode { SelectableDeferred, InternalLazy }
private data class RenderModeScenario(val name: String, val markdown: String)

private fun List<Double>.modeSummary(): String {
    val sorted = sorted()
    fun percentile(fraction: Double) = sorted[((lastIndex * fraction).toInt()).coerceIn(indices)]
    return "samplesMs=$this medianMs=${percentile(0.5)} p90Ms=${percentile(0.9)} maxMs=${sorted.last()}"
}

private fun renderModeScenarios(): List<RenderModeScenario> = listOf(
    RenderModeScenario("short-prose", "一段普通正文，用于覆盖最常见的短回答。"),
    RenderModeScenario("plain-paragraphs", (1..30).joinToString("\n\n") {
        "第 $it 段普通正文，包含用于对齐长度的内容以及 English words。"
    }),
    RenderModeScenario("formatted-prose", (1..30).joinToString("\n\n") {
        "第 $it 段包含 **加粗**、*斜体*、~~删除线~~ 和 [链接](https://example.com/$it)。"
    }),
    RenderModeScenario("long-prose", (1..400).joinToString("\n\n") {
        "第 $it 段长正文用于覆盖超长回答，包含中文、English words and numbers 1234567890。"
    }),
    RenderModeScenario("headings-and-quotes", (1..80).joinToString("\n\n") { "## 标题 $it\n\n> 第 $it 条引用正文" }),
    RenderModeScenario("lists", (1..300).joinToString("\n") { "- 列表项 $it，带有 **强调内容**" }),
    RenderModeScenario("code-blocks", (1..50).joinToString("\n\n") {
        "```kotlin\nval item$it = List(20) { value -> value * $it }\nprintln(item$it.sum())\n```"
    }),
    RenderModeScenario("inline-math", (1..150).joinToString("\n\n") {
        "第 $it 个公式为 ${'$'}`x_$it = \\frac{a_$it+b_$it}{c_$it}`${'$'}，并保留周围正文。"
    }),
    RenderModeScenario("block-math", (1..80).joinToString("\n\n") {
        "${'$'}${'$'}\\sum_{i=1}^{n} \\frac{x_i^{$it}}{1+x_i^2}${'$'}${'$'}"
    }),
    RenderModeScenario("tables", buildString {
        appendLine("| 序号 | 名称 | 描述 |")
        appendLine("| ---: | :--- | :--- |")
        repeat(200) { appendLine("| $it | 项目 $it | 第 $it 行表格内容 |") }
    }),
    RenderModeScenario("mixed-document", (1..100).joinToString("\n\n") {
        "## 小节 $it\n\n正文 **$it** 与行内公式 ${'$'}`x_$it^2`${'$'}。\n\n- 要点 A\n- 要点 B\n\n> 引用 $it"
    }),
    RenderModeScenario("real-long-formula-stress", renderModeFormulaArticle()),
)

private fun renderModeFormulaArticle(): String {
    val corpus = File("../../../shared/src/jvmTest/resources/zhihu-formula-corpus/formulas.json")
    val formulas = Json.parseToJsonElement(corpus.readText()).jsonArray
        .map { it.jsonObject.getValue("latex").jsonPrimitive.content }
        .distinct().sortedByDescending(String::length).take(80)
    return buildString {
        appendLine("# 项目真实长公式压力文章")
        formulas.forEachIndexed { index, latex ->
            appendLine("\n## 长公式 ${index + 1}（${latex.length} 字符）\n\n${'$'}${'$'}\n$latex\n${'$'}${'$'}\n")
        }
    }
}
