package com.github.zly2006.zhihu.markdown

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class MarkdownConversionPerformanceTest {
    @Test
    fun issue495HtmlToAstBenchmark() {
        if (System.getenv("MARKDOWN_PERFORMANCE") != "1") return
        val html = File("../app/src/androidTest/assets/issue-495-answer.html").readText()
        repeat(2) { htmlToMdAst("$html<!-- warmup-$it -->") }
        val samples = List(7) { iteration ->
            val startedAt = System.nanoTime()
            htmlToMdAst("$html<!-- sample-$iteration -->")
            (System.nanoTime() - startedAt) / 1_000_000.0
        }
        val median = samples.sorted()[samples.size / 2]
        println("MarkdownJvmHtmlConversion samplesMs=$samples medianMs=$median htmlChars=${html.length}")
        assertTrue(median < 50.0, "Issue #495 HTML conversion must remain below 50 ms on JVM: $samples")
    }
}
