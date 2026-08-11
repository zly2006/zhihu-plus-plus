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
