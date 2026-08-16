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

import com.hrm.latex.parser.LatexParser
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import kotlin.test.Test

class LatexParserPerformanceTest {
    @Test
    fun parseCompleteZhihuFormulaCorpus() {
        if (System.getenv("MARKDOWN_PERFORMANCE") != "1") return
        val formulas =
            Json
                .parseToJsonElement(
                    File("src/jvmTest/resources/zhihu-formula-corpus/formulas.json").readText(),
                ).jsonArray
                .map {
                    it.jsonObject
                        .getValue("latex")
                        .jsonPrimitive.content
                }
        val parser = LatexParser()
        repeat(2) { formulas.forEach(parser::parseWithDiagnostics) }
        val samples = List(7) {
            val startedAt = System.nanoTime()
            formulas.forEach(parser::parseWithDiagnostics)
            (System.nanoTime() - startedAt) / 1_000_000.0
        }
        val sorted = samples.sorted()
        println(
            "CurrentLatexParser formulas=${formulas.size} samplesMs=$samples " +
                "medianMs=${sorted[sorted.size / 2]} perFormulaMicros=${sorted[sorted.size / 2] * 1000 / formulas.size}",
        )
    }
}
