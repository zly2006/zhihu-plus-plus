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
