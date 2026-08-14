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

import com.fleeksoft.ksoup.Ksoup
import com.hrm.latex.parser.LatexParser
import com.hrm.latex.parser.ParseDiagnostic
import com.hrm.latex.parser.model.LatexNode
import com.hrm.markdown.parser.ast.BlockQuote
import com.hrm.markdown.parser.ast.Emphasis
import com.hrm.markdown.parser.ast.FencedCodeBlock
import com.hrm.markdown.parser.ast.Heading
import com.hrm.markdown.parser.ast.InlineMath
import com.hrm.markdown.parser.ast.Link
import com.hrm.markdown.parser.ast.ListBlock
import com.hrm.markdown.parser.ast.ListItem
import com.hrm.markdown.parser.ast.MathBlock
import com.hrm.markdown.parser.ast.Strikethrough
import com.hrm.markdown.parser.ast.StrongEmphasis
import com.hrm.markdown.parser.ast.Table
import com.hrm.markdown.parser.ast.ThematicBreak
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.security.MessageDigest
import java.text.Normalizer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ZhihuFormulaCorpusTest {
    private val json = Json { ignoreUnknownKeys = true }

    private data class AstProfile(
        var fractions: Int = 0,
        var rulelessFractions: Int = 0,
        var binomials: Int = 0,
        var roots: Int = 0,
        var indexedRoots: Int = 0,
        var superscripts: Int = 0,
        var subscripts: Int = 0,
        val delimiters: MutableList<String> = mutableListOf(),
        val tables: MutableList<String> = mutableListOf(),
        val rowGapPatterns: MutableList<String> = mutableListOf(),
        val textSegments: MutableList<String> = mutableListOf(),
        var accents: Int = 0,
        var extensibleArrows: Int = 0,
        var operators: Int = 0,
        var colors: Int = 0,
        var boxes: Int = 0,
    ) {
        fun sorted(): AstProfile =
            copy(
                delimiters = delimiters.sorted().toMutableList(),
                tables = tables.sorted().toMutableList(),
                rowGapPatterns = rowGapPatterns.sorted().toMutableList(),
                textSegments = textSegments.sorted().toMutableList(),
            )
    }

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
        val coveredStructures = mutableMapOf<String, Int>()

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
            val document = htmlToMdAst(content, noNativeBlock = true)
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

            val htmlDocument = Ksoup.parseBodyFragment(content)
            val markdownNodes = document.allNodes()
            val sourceStructures =
                mapOf(
                    "heading" to htmlDocument.select("h1, h2, h3, h4, h5, h6").size,
                    "blockquote" to htmlDocument.select("blockquote").size,
                    "code-block" to htmlDocument.select("pre").size,
                    "list" to htmlDocument.select("ul, ol").size,
                    "list-item" to htmlDocument.select("li").size,
                    "table" to htmlDocument.select("table").size,
                    "strong" to htmlDocument.select("strong, b").size,
                    "emphasis" to htmlDocument.select("em, i").size,
                    "strikethrough" to htmlDocument.select("del, s").size,
                    "link" to
                        htmlDocument
                            .select("a")
                            .count { link ->
                                link.attr("href").isNotBlank() ||
                                    link.classNames().contains("video-box")
                            },
                    "thematic-break" to htmlDocument.select("hr").size,
                )
            val actualStructures =
                mapOf(
                    "heading" to markdownNodes.count { it is Heading },
                    "blockquote" to markdownNodes.count { it is BlockQuote },
                    "code-block" to markdownNodes.count { it is FencedCodeBlock },
                    "list" to markdownNodes.count { it is ListBlock },
                    "list-item" to markdownNodes.count { it is ListItem },
                    "table" to markdownNodes.count { it is Table },
                    "strong" to markdownNodes.count { it is StrongEmphasis },
                    "emphasis" to markdownNodes.count { it is Emphasis },
                    "strikethrough" to markdownNodes.count { it is Strikethrough },
                    "link" to markdownNodes.count { it is Link },
                    "thematic-break" to markdownNodes.count { it is ThematicBreak },
                )
            sourceStructures.forEach { (name, count) ->
                coveredStructures[name] = coveredStructures.getOrDefault(name, 0) + count
                val actualCount = actualStructures.getValue(name)
                if ((name == "link" && actualCount < count) || (name != "link" && actualCount != count)) {
                    failures +=
                        "$rawPath structure=$name expected=$count " +
                        "actual=$actualCount"
                }
            }
        }

        assertTrue(
            coveredStructures.count { (_, count) -> count > 0 } >= 6,
            "Real corpus does not cover enough non-formula structures: $coveredStructures",
        )
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

    @Test
    fun current_ast_should_match_katex_semantic_profiles() {
        val formulas =
            resourceText("formulas.json")
                .let(json::parseToJsonElement)
                .jsonArray
                .map {
                    it.jsonObject
                        .getValue("latex")
                        .jsonPrimitive.content
                }
        val oracle = resourceText("katex-ast-oracle.json").let(json::parseToJsonElement).jsonObject
        val entries = oracle.getValue("entries").jsonArray

        assertEquals("0.18.1", oracle.getValue("katexVersion").jsonPrimitive.content)
        assertEquals(
            3699,
            oracle
                .getValue("parsedCount")
                .jsonPrimitive.content
                .toInt(),
        )
        assertEquals(
            4,
            oracle
                .getValue("rejectedCount")
                .jsonPrimitive.content
                .toInt(),
        )
        assertEquals(formulas.size, entries.size)

        val parser = LatexParser()
        val mismatches = mutableListOf<String>()
        val rejectionCounts = mutableMapOf<String, Int>()
        formulas.zip(entries).forEachIndexed { index, (formula, entryElement) ->
            val entry = entryElement.jsonObject
            assertEquals(
                entry.getValue("sha256").jsonPrimitive.content,
                formula.encodeToByteArray().sha256(),
                "KaTeX oracle order changed at formula[$index]",
            )
            if (entry.getValue("status").jsonPrimitive.content == "rejected") {
                val category = entry.getValue("errorCategory").jsonPrimitive.content
                rejectionCounts[category] = rejectionCounts.getOrDefault(category, 0) + 1
                return@forEachIndexed
            }

            val expected = entry.getValue("profile").jsonObject.toAstProfile()
            val actual = parser.parse(formula).toAstProfile(formula)
            if (actual != expected) {
                mismatches +=
                    "formula[$index] expected=$expected actual=$actual latex=$formula"
            }
        }

        assertEquals(mapOf("invalid-array" to 2, "malformed-dollar" to 2), rejectionCounts)
        assertTrue(mismatches.isEmpty(), mismatches.take(30).joinToString(separator = "\n"))
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

    private fun JsonObject.toAstProfile(): AstProfile =
        AstProfile(
            fractions = getValue("fractions").jsonPrimitive.content.toInt(),
            rulelessFractions = getValue("rulelessFractions").jsonPrimitive.content.toInt(),
            binomials = getValue("binomials").jsonPrimitive.content.toInt(),
            roots = getValue("roots").jsonPrimitive.content.toInt(),
            indexedRoots = getValue("indexedRoots").jsonPrimitive.content.toInt(),
            superscripts = getValue("superscripts").jsonPrimitive.content.toInt(),
            subscripts = getValue("subscripts").jsonPrimitive.content.toInt(),
            delimiters =
                getValue("delimiters")
                    .jsonArray
                    .map { it.jsonPrimitive.content }
                    .toMutableList(),
            tables =
                getValue("tables")
                    .jsonArray
                    .map { it.jsonPrimitive.content }
                    .toMutableList(),
            rowGapPatterns =
                getValue("rowGapPatterns")
                    .jsonArray
                    .map { it.jsonPrimitive.content }
                    .toMutableList(),
            textSegments =
                getValue("textSegments")
                    .jsonArray
                    .map { it.jsonPrimitive.content }
                    .toMutableList(),
            accents = getValue("accents").jsonPrimitive.content.toInt(),
            extensibleArrows = getValue("extensibleArrows").jsonPrimitive.content.toInt(),
            operators = getValue("operators").jsonPrimitive.content.toInt(),
            colors = getValue("colors").jsonPrimitive.content.toInt(),
            boxes = getValue("boxes").jsonPrimitive.content.toInt(),
        )

    private fun LatexNode.Document.toAstProfile(source: String): AstProfile {
        val profile = AstProfile()
        val pending = ArrayDeque<LatexNode>()
        pending.addAll(children)
        while (pending.isNotEmpty()) {
            val node = pending.removeFirst()
            when (node) {
                is LatexNode.Fraction -> {
                    profile.fractions++
                    if (node.style == LatexNode.Fraction.FractionStyle.RULELESS) {
                        profile.rulelessFractions++
                    }
                }
                is LatexNode.Binomial -> profile.binomials++
                is LatexNode.Root -> {
                    profile.roots++
                    if (node.index != null) {
                        profile.indexedRoots++
                    }
                }
                is LatexNode.Superscript -> profile.superscripts++
                is LatexNode.Subscript -> profile.subscripts++
                is LatexNode.BigOperator -> {
                    profile.operators++
                    if (node.superscript != null) {
                        profile.superscripts++
                    }
                    if (node.subscript != null) {
                        profile.subscripts++
                    }
                }
                is LatexNode.Prescript -> {
                    if (node.preSuperscript != null) {
                        profile.superscripts++
                    }
                    if (node.preSubscript != null) {
                        profile.subscripts++
                    }
                }
                is LatexNode.Stack -> {
                    if (!node.base.isSemanticOperator()) {
                        profile.operators++
                    }
                    if (node.above != null) {
                        profile.superscripts++
                    }
                    if (node.below != null) {
                        profile.subscripts++
                    }
                }
                is LatexNode.Delimited ->
                    profile.delimiters +=
                        "${node.left.comparableDelimiter()}|${node.right.comparableDelimiter()}"
                is LatexNode.Matrix -> {
                    profile.tables += node.rows.tableShape()
                    profile.rowGapPatterns += node.rowGaps.comparablePattern()
                    when (node.type) {
                        LatexNode.Matrix.MatrixType.PLAIN -> Unit
                        LatexNode.Matrix.MatrixType.PAREN -> profile.delimiters += "(|)"
                        LatexNode.Matrix.MatrixType.BRACKET -> profile.delimiters += "[|]"
                        LatexNode.Matrix.MatrixType.BRACE -> profile.delimiters += "{|}"
                        LatexNode.Matrix.MatrixType.VBAR -> profile.delimiters += "|||"
                        LatexNode.Matrix.MatrixType.DOUBLE_VBAR -> profile.delimiters += "‖|‖"
                    }
                }
                is LatexNode.Array -> {
                    profile.tables += node.rows.tableShape()
                    profile.rowGapPatterns += node.rowGaps.comparablePattern()
                }
                is LatexNode.Cases -> {
                    val cases =
                        node.cases.dropLastWhile { (expression, condition) ->
                            expression.isSemanticallyEmpty() && condition.isSemanticallyEmpty()
                        }
                    profile.delimiters += "{|"
                    profile.tables += "cases:${cases.size}"
                    profile.rowGapPatterns += node.rowGaps.comparablePattern()
                }
                is LatexNode.Aligned -> {
                    profile.tables += node.rows.tableShape()
                    profile.rowGapPatterns += node.rowGaps.comparablePattern()
                }
                is LatexNode.Split -> {
                    profile.tables += node.rows.tableShape()
                    profile.rowGapPatterns += node.rowGaps.comparablePattern()
                }
                is LatexNode.Eqnarray -> {
                    profile.tables += node.rows.tableShape()
                    profile.rowGapPatterns += node.rowGaps.comparablePattern()
                }
                is LatexNode.Substack ->
                    profile.tables += node.rows.tableShape()
                is LatexNode.Tabular -> {
                    profile.tables += node.rows.tableShape()
                    profile.rowGapPatterns += node.rowGaps.comparablePattern()
                }
                is LatexNode.Multline -> {
                    profile.tables += "table:${node.lines.size}x1"
                    profile.rowGapPatterns += node.rowGaps.comparablePattern()
                }
                is LatexNode.TextMode ->
                    node.text
                        .comparableText()
                        .takeIf(String::isNotEmpty)
                        ?.let(profile.textSegments::add)
                is LatexNode.Style -> {
                    val range = node.sourceRange
                    if (
                        range != null &&
                        TEXT_STYLE_COMMANDS.any { command ->
                            source.regionMatches(range.start, "\\$command", 0, command.length + 1)
                        }
                    ) {
                        node.content
                            .profileText()
                            .comparableText()
                            .takeIf(String::isNotEmpty)
                            ?.let(profile.textSegments::add)
                    }
                }
                is LatexNode.Accent ->
                    if (
                        node.accentType == LatexNode.Accent.AccentType.CANCEL ||
                        node.accentType == LatexNode.Accent.AccentType.BCANCEL ||
                        node.accentType == LatexNode.Accent.AccentType.XCANCEL
                    ) {
                        profile.boxes++
                    } else {
                        profile.accents++
                    }
                is LatexNode.ExtensibleArrow -> profile.extensibleArrows++
                is LatexNode.Operator,
                is LatexNode.OperatorName,
                is LatexNode.ModOperator,
                -> profile.operators++
                is LatexNode.Color -> profile.colors++
                is LatexNode.Boxed,
                is LatexNode.Enclose,
                is LatexNode.ColorBox,
                -> profile.boxes++
                else -> Unit
            }
            pending.addAll(node.children())
        }
        return profile.sorted()
    }

    private fun List<LatexNode.RowGap?>.comparablePattern(): String =
        joinToString(",") { gap ->
            gap?.let { "${it.number}${it.unit}" } ?: "null"
        }

    private fun LatexNode.isSemanticOperator(): Boolean =
        when (this) {
            is LatexNode.BigOperator,
            is LatexNode.Operator,
            is LatexNode.OperatorName,
            is LatexNode.ModOperator,
            -> true
            is LatexNode.Group -> children.singleSemanticNode()?.isSemanticOperator() == true
            is LatexNode.Style -> content.singleSemanticNode()?.isSemanticOperator() == true
            is LatexNode.MathStyle -> content.singleSemanticNode()?.isSemanticOperator() == true
            else -> false
        }

    private fun List<LatexNode>.singleSemanticNode(): LatexNode? =
        filterNot { node -> node.isSemanticallyEmpty() }.singleOrNull()

    private fun List<List<LatexNode>>.tableShape(): String {
        val rows = filterNot { row ->
            row.any { node -> node.containsRule() } &&
                row.all { node -> node.containsOnlyRuleOrSpace() }
        }
        return "table:${rows.size}x${rows.maxOfOrNull { it.size } ?: 0}"
    }

    private fun LatexNode.containsRule(): Boolean =
        when (this) {
            is LatexNode.HLine,
            is LatexNode.CLine,
            -> true
            is LatexNode.Group -> children.any { node -> node.containsRule() }
            else -> false
        }

    private fun LatexNode.containsOnlyRuleOrSpace(): Boolean =
        when (this) {
            is LatexNode.HLine,
            is LatexNode.CLine,
            is LatexNode.Space,
            -> true
            is LatexNode.Group -> children.all { node -> node.containsOnlyRuleOrSpace() }
            else -> false
        }

    private fun List<LatexNode>.profileText(): String =
        joinToString(separator = "") { node ->
            when (node) {
                is LatexNode.Text -> node.content
                is LatexNode.TextMode -> node.text
                is LatexNode.Symbol -> node.unicode
                else -> node.children().profileText()
            }
        }

    private fun LatexNode.isSemanticallyEmpty(): Boolean =
        when (this) {
            is LatexNode.Group -> children.all { node -> node.isSemanticallyEmpty() }
            is LatexNode.Text -> content.isBlank()
            is LatexNode.TextMode -> text.isBlank()
            is LatexNode.Style -> content.all { node -> node.isSemanticallyEmpty() }
            is LatexNode.MathStyle -> content.all { node -> node.isSemanticallyEmpty() }
            else -> false
        }

    private fun String.comparableDelimiter(): String =
        when (this) {
            "." -> ""
            "\\{" -> "{"
            "\\}" -> "}"
            "\\langle" -> "⟨"
            "\\rangle" -> "⟩"
            "lbrack" -> "\\lbrack"
            "rbrack" -> "\\rbrack"
            "\\lvert", "\\rvert" -> "|"
            "\\lVert", "\\rVert" -> "‖"
            else -> this
        }

    private fun String.comparableText(): String =
        Normalizer
            .normalize(this, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .replace(Regex("\\s+"), "")

    private companion object {
        val TEXT_STYLE_COMMANDS = setOf("textbf", "textit", "textrm", "textsf", "texttt")
    }
}
