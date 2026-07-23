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
import com.hrm.latex.parser.ParseDiagnostic
import com.hrm.latex.parser.model.LatexNode
import com.hrm.markdown.parser.ast.InlineMath
import com.hrm.markdown.parser.ast.MathBlock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.security.MessageDigest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ZhihuFormulaCorpusTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun raw_zhihu_content_should_build_markdown_ast_without_losing_equations() {
        if (javaClass.getResource("/zhihu-formula-corpus/raw") == null) {
            return
        }
        val manifest = resourceText("manifest.json").let(json::parseToJsonElement).jsonObject
        val formulaIndex = resourceText("formulas.json").let(json::parseToJsonElement).jsonArray
        assertTrue(manifest.getValue("authors").jsonArray.size >= 10)
        assertEquals(
            manifest
                .getValue("unique_formula_count")
                .jsonPrimitive.content
                .toInt(),
            formulaIndex.size,
        )
        val formulasByContent = mutableMapOf<String, MutableList<String>>()
        formulaIndex.forEach { formulaElement ->
            val formula = formulaElement.jsonObject
            val latex = formula.getValue("latex").jsonPrimitive.content
            formula.getValue("sources").jsonArray.forEach { sourceElement ->
                val source = sourceElement.jsonObject
                val key =
                    "${source.getValue("content_type").jsonPrimitive.content}-" +
                        source.getValue("content_id").jsonPrimitive.content
                formulasByContent.getOrPut(key, ::mutableListOf) += latex
            }
        }
        assertEquals(
            manifest
                .getValue("formula_occurrences")
                .jsonPrimitive.content
                .toInt(),
            formulasByContent.values.sumOf { it.size },
        )
        val failures = mutableListOf<String>()

        manifest.getValue("contents").jsonArray.forEach { itemElement ->
            val item = itemElement.jsonObject
            val contentKey =
                "${item.getValue("type").jsonPrimitive.content}-" +
                    item.getValue("id").jsonPrimitive.content
            val rawPath = item.getValue("raw_path").jsonPrimitive.content
            val raw = resourceBytes(rawPath)
            val expectedSha256 = item.getValue("raw_sha256").jsonPrimitive.content
            val content = json
                .parseToJsonElement(raw.decodeToString())
                .jsonObject
                .getValue("content")
                .jsonPrimitive
                .content

            assertEquals(expectedSha256, raw.sha256(), "Raw fixture changed: $rawPath")
            assertEquals(
                item.getValue("content_sha256").jsonPrimitive.content,
                content.encodeToByteArray().sha256(),
                "Raw content changed: $rawPath",
            )
            val document = htmlToMdAst(content)
            val expectedFormulas = formulasByContent.getValue(contentKey)
            assertEquals(
                item
                    .getValue("formula_count")
                    .jsonPrimitive.content
                    .toInt(),
                expectedFormulas.size,
                "Formula index changed: $rawPath",
            )
            val actualFormulas =
                document
                    .allNodes()
                    .asSequence()
                    .mapNotNull {
                        when (it) {
                            is InlineMath -> it.literal
                            is MathBlock -> it.literal
                            else -> null
                        }
                    }.toList()
            val expectedCounts = expectedFormulas.groupingBy { it }.eachCount()
            val actualCounts = actualFormulas.groupingBy { it }.eachCount()
            if (actualCounts != expectedCounts) {
                val missing = expectedCounts
                    .filter { (formula, count) -> actualCounts.getOrDefault(formula, 0) < count }
                    .keys
                    .firstOrNull()
                    .orEmpty()
                failures +=
                    "$rawPath expected=${expectedFormulas.size} actual=${actualFormulas.size}" +
                    " missing=$missing"
            }
        }

        assertTrue(failures.isEmpty(), failures.joinToString(separator = "\n"))
    }

    @Test
    fun formula_index_should_be_self_consistent() {
        val manifest = resourceText("manifest.json").let(json::parseToJsonElement).jsonObject
        val contents = manifest.getValue("contents").jsonArray.map { it.jsonObject }
        val formulas = resourceText("formulas.json").let(json::parseToJsonElement).jsonArray.map { it.jsonObject }
        val contentKeys = contents.map {
            "${it.getValue("type").jsonPrimitive.content}-${it.getValue("id").jsonPrimitive.content}"
        }
        val rawPaths = contents.map { it.getValue("raw_path").jsonPrimitive.content }
        val latex = formulas.map { it.getValue("latex").jsonPrimitive.content }
        val sourceKeys = formulas.flatMap { formula ->
            formula.getValue("sources").jsonArray.map { sourceElement ->
                val source = sourceElement.jsonObject
                "${source.getValue("content_type").jsonPrimitive.content}-" +
                    source.getValue("content_id").jsonPrimitive.content
            }
        }

        assertEquals(contentKeys.size, contentKeys.toSet().size, "Duplicate content key")
        assertEquals(rawPaths.size, rawPaths.toSet().size, "Duplicate raw path")
        assertEquals(latex.size, latex.toSet().size, "Duplicate formula")
        assertTrue(sourceKeys.all(contentKeys.toSet()::contains), "Formula source missing from manifest")
        assertEquals(
            manifest
                .getValue("formula_occurrences")
                .jsonPrimitive.content
                .toInt(),
            sourceKeys.size,
        )
        assertEquals(
            sourceKeys.size,
            contents.sumOf {
                it
                    .getValue("formula_count")
                    .jsonPrimitive.content
                    .toInt()
            },
        )
    }

    @Test
    fun extracted_formulas_should_parse_with_the_current_latex_library() {
        val parser = LatexParser()
        val formulas = resourceText("formulas.json")
            .let(json::parseToJsonElement)
            .jsonArray
            .map {
                it.jsonObject
                    .getValue("latex")
                    .jsonPrimitive.content
            }
        val failures = mutableListOf<String>()
        val unknownCommands = mutableMapOf<String, MutableList<String>>()
        var acceptedTrailingBackslashes = 0

        formulas.forEachIndexed { index, formula ->
            try {
                val result = parser.parseWithDiagnostics(formula)
                if (result.document.children.isEmpty()) {
                    failures += "formula[$index] produced an empty AST: $formula"
                }
                val pending = ArrayDeque<LatexNode>()
                pending.addAll(result.document.children)
                while (pending.isNotEmpty()) {
                    val node = pending.removeFirst()
                    if (node is LatexNode.Command) {
                        unknownCommands.getOrPut(node.name, ::mutableListOf) += formula
                    }
                    pending.addAll(node.children())
                }
                val trailingBackslashCount = formula.length - formula.trimEnd('\\').length
                result.errors.forEach { error ->
                    if (
                        trailingBackslashCount % 2 == 1 &&
                        error.category == ParseDiagnostic.Category.UNKNOWN_COMMAND &&
                        error.message == "Trailing backslash does not form a control sequence"
                    ) {
                        acceptedTrailingBackslashes++
                    } else {
                        failures +=
                            "formula[$index] unexpected ${error.category} error: " +
                            "${error.message}: $formula"
                    }
                }
            } catch (error: Exception) {
                failures += "formula[$index] ${error::class.simpleName}: $formula"
            }
        }

        assertTrue(failures.isEmpty(), failures.take(20).joinToString(separator = "\n"))
        assertEquals(211, acceptedTrailingBackslashes, "Known malformed trailing backslashes changed")
        assertTrue(
            unknownCommands.isEmpty(),
            unknownCommands.entries
                .sortedByDescending { it.value.size }
                .take(30)
                .joinToString(separator = "\n") { (command, samples) ->
                    "\\$command count=${samples.size} sample=${samples.first()}"
                },
        )
    }

    private fun resourceText(path: String): String = resourceBytes(path).decodeToString()

    private fun resourceBytes(path: String): ByteArray =
        checkNotNull(javaClass.getResourceAsStream("/zhihu-formula-corpus/$path")) {
            "Missing corpus resource: $path"
        }.use { it.readBytes() }

    private fun ByteArray.sha256(): String =
        MessageDigest
            .getInstance("SHA-256")
            .digest(this)
            .joinToString("") { byte -> "%02x".format(byte) }
}
