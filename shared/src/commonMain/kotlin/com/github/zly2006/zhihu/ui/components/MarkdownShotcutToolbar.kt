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

package com.github.zly2006.zhihu.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FirstPage
import androidx.compose.material.icons.filled.LastPage
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class MarkdownShortcut(
    val label: String,
) {
    H1("H1"),
    H2("H2"),
    Bold("B"),
    Italic("I"),
    Link("Link"),
    Quote(">"),
    InlineCode("</>"),
    CodeBlock("```"),
    Divider("HR"),
    Math("∑"),
}

@Composable
fun MarkdownShortcutToolbar(
    onApplyShortcut: (MarkdownShortcut) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    var expanded by rememberSaveable { mutableStateOf(true) }
    val opacityFraction = fabOpacityPercent.intValue / 100f
    val containerColor = FloatingActionButtonDefaults.containerColor.copy(alpha = opacityFraction)
    val contentColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = opacityFraction)
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .testTag("markdown_shortcut_toolbar"),
        horizontalArrangement = Arrangement.End,
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = containerColor,
            contentColor = contentColor,
        ) {
            Row(
                modifier = Modifier.padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AnimatedVisibility(
                    visible = expanded,
                    modifier = Modifier.weight(1f, fill = false),
                    enter = expandHorizontally(expandFrom = Alignment.End) + fadeIn(),
                    exit = shrinkHorizontally(shrinkTowards = Alignment.End) + fadeOut(),
                ) {
                    Row(
                        modifier =
                            Modifier
                                .horizontalScroll(rememberScrollState())
                                .padding(start = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        MarkdownShortcut.entries.forEach { shortcut ->
                            ShortcutButton(
                                label = shortcut.label,
                                enabled = enabled,
                                onClick = { onApplyShortcut(shortcut) },
                            )
                        }
                    }
                }
                if (expanded) {
                    VerticalDivider(
                        modifier = Modifier.height(24.dp),
                        color = contentColor.copy(alpha = 0.3f),
                    )
                }
                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Filled.LastPage else Icons.Filled.FirstPage,
                        contentDescription = if (expanded) "收起工具栏" else "展开工具栏",
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ShortcutButton(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(40.dp),
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
    }
}

fun TextFieldValue.applyMarkdownShortcut(shortcut: MarkdownShortcut): TextFieldValue =
    when (shortcut) {
        MarkdownShortcut.H1 -> toggleHeading(targetPrefix = "# ", otherPrefix = "## ")
        MarkdownShortcut.H2 -> toggleHeading(targetPrefix = "## ", otherPrefix = "# ")
        MarkdownShortcut.Bold -> toggleWrapSelection(prefix = "**", suffix = "**", placeholder = "加粗")
        MarkdownShortcut.Italic -> toggleWrapSelection(prefix = "*", suffix = "*", placeholder = "斜体")
        MarkdownShortcut.Quote -> toggleQuoteAtLineStart()
        MarkdownShortcut.Divider -> insertBlock("---")
        MarkdownShortcut.CodeBlock -> toggleCodeBlock()
        MarkdownShortcut.InlineCode -> toggleWrapSelection(prefix = "`", suffix = "`", placeholder = "code")
        MarkdownShortcut.Math -> toggleWrapSelection(prefix = "$", suffix = "$", placeholder = "x")
        MarkdownShortcut.Link -> insertLink()
    }

internal fun TextFieldValue.replaceSelection(
    insert: String,
    cursorOffsetInInsert: Int,
): TextFieldValue {
    val start = selection.min
    val end = selection.max
    val newText = buildString {
        append(text.substring(0, start))
        append(insert)
        append(text.substring(end))
    }
    val cursor = (start + cursorOffsetInInsert).coerceIn(0, newText.length)
    return TextFieldValue(
        text = newText,
        selection = TextRange(cursor, cursor),
    )
}

private fun TextFieldValue.wrapSelection(
    prefix: String,
    suffix: String,
    placeholder: String,
): TextFieldValue {
    val start = selection.min
    val end = selection.max
    val selected = text.substring(start, end)
    val core = selected.ifEmpty { placeholder }
    val insert = prefix + core + suffix
    val cursorOffset =
        if (selected.isEmpty()) {
            prefix.length
        } else {
            insert.length
        }
    return replaceSelection(insert = insert, cursorOffsetInInsert = cursorOffset)
}

private fun TextFieldValue.toggleHeading(
    targetPrefix: String,
    otherPrefix: String,
): TextFieldValue {
    val cursor = selection.min
    val lineStart = text.lineStartOf(cursor)
    return when {
        text.startsWith(targetPrefix, startIndex = lineStart) -> {
            replaceAt(
                replaceStart = lineStart,
                replaceEndExclusive = lineStart + targetPrefix.length,
                replaceWith = "",
            )
        }
        text.startsWith(otherPrefix, startIndex = lineStart) -> {
            replaceAt(
                replaceStart = lineStart,
                replaceEndExclusive = lineStart + otherPrefix.length,
                replaceWith = targetPrefix,
            )
        }
        else -> {
            replaceAt(
                replaceStart = lineStart,
                replaceEndExclusive = lineStart,
                replaceWith = targetPrefix,
            )
        }
    }
}

private fun TextFieldValue.toggleQuoteAtLineStart(): TextFieldValue {
    val cursor = selection.min
    val lineStart = text.lineStartOf(cursor)
    val lineEnd = text.lineEndOf(cursor)
    var idx = lineStart
    while (idx + 2 <= lineEnd && text.startsWith("> ", startIndex = idx)) {
        idx += 2
    }
    return if (idx > lineStart) {
        replaceAt(
            replaceStart = lineStart,
            replaceEndExclusive = idx,
            replaceWith = "",
        )
    } else {
        replaceAt(
            replaceStart = lineStart,
            replaceEndExclusive = lineStart,
            replaceWith = "> ",
        )
    }
}

private fun TextFieldValue.toggleWrapSelection(
    prefix: String,
    suffix: String,
    placeholder: String,
): TextFieldValue {
    fun isSingleMarker(marker: Char): Boolean =
        prefix.length == 1 && suffix.length == 1 && prefix[0] == marker && suffix[0] == marker

    fun isSingleMarkerBoundary(index: Int, marker: Char): Boolean {
        val prev = text.getOrNull(index - 1)
        val next = text.getOrNull(index + 1)
        if (prev == marker || next == marker) return false
        return true
    }

    val start = selection.min
    val end = selection.max
    if (start != end) {
        val canUnwrapAroundSelection =
            start >= prefix.length &&
                end + suffix.length <= text.length &&
                text.regionMatches(start - prefix.length, prefix, 0, prefix.length) &&
                text.regionMatches(end, suffix, 0, suffix.length) &&
                when {
                    isSingleMarker('*') && prefix == "*" -> isSingleMarkerBoundary(start - 1, '*') && isSingleMarkerBoundary(end, '*')
                    isSingleMarker('$') && prefix == "$" -> isSingleMarkerBoundary(start - 1, '$') && isSingleMarkerBoundary(end, '$')
                    else -> true
                }

        if (canUnwrapAroundSelection) {
            val removePrefixStart = start - prefix.length
            val removePrefixEndExclusive = start
            val removeSuffixStart = end
            val removeSuffixEndExclusive = end + suffix.length
            return removeRanges(
                ranges =
                    listOf(
                        removeSuffixStart to removeSuffixEndExclusive,
                        removePrefixStart to removePrefixEndExclusive,
                    ),
                newSelection = TextRange(removePrefixStart, removeSuffixStart - prefix.length),
            )
        }

        val selected = text.substring(start, end)
        val canUnwrapInsideSelection =
            selected.startsWith(prefix) &&
                selected.endsWith(suffix) &&
                selected.length >= prefix.length + suffix.length &&
                when {
                    isSingleMarker('*') && prefix == "*" -> isSingleMarkerBoundary(start, '*') && isSingleMarkerBoundary(end - 1, '*')
                    isSingleMarker('$') && prefix == "$" -> isSingleMarkerBoundary(start, '$') && isSingleMarkerBoundary(end - 1, '$')
                    else -> true
                }

        if (canUnwrapInsideSelection) {
            val core = selected.substring(prefix.length, selected.length - suffix.length)
            val newText = text.replaceRange(start, end, core)
            return TextFieldValue(
                text = newText,
                selection = TextRange(start, start + core.length),
            )
        }

        val insert = prefix + selected + suffix
        return replaceSelection(insert = insert, cursorOffsetInInsert = insert.length)
    }

    val cursor = selection.min
    val lineStart = text.lineStartOf(cursor)
    val lineEnd = text.lineEndOf(cursor)
    val prefixStart = text.lastIndexOf(prefix, startIndex = (cursor - 1).coerceAtLeast(lineStart))
    if (prefixStart >= lineStart && prefixStart + prefix.length <= cursor) {
        val suffixStart = text.indexOf(suffix, startIndex = cursor)
        if (suffixStart >= 0 && suffixStart + suffix.length <= lineEnd) {
            val canUnwrapAtCursor =
                when {
                    isSingleMarker('*') && prefix == "*" -> isSingleMarkerBoundary(prefixStart, '*') && isSingleMarkerBoundary(suffixStart, '*')
                    isSingleMarker('$') && prefix == "$" -> isSingleMarkerBoundary(prefixStart, '$') && isSingleMarkerBoundary(suffixStart, '$')
                    else -> true
                }
            if (canUnwrapAtCursor) {
                val removePrefixStart = prefixStart
                val removePrefixEndExclusive = prefixStart + prefix.length
                val removeSuffixStart = suffixStart
                val removeSuffixEndExclusive = suffixStart + suffix.length
                val newCursor = (cursor - prefix.length).coerceAtLeast(0)
                return removeRanges(
                    ranges =
                        listOf(
                            removeSuffixStart to removeSuffixEndExclusive,
                            removePrefixStart to removePrefixEndExclusive,
                        ),
                    newSelection = TextRange(newCursor, newCursor),
                )
            }
        }
    }

    text
        .findWordRangeAtCursor(cursor)
        ?.takeIf { it.min != it.max }
        ?.let { wordRange ->
            val wordStart = wordRange.min
            val wordEnd = wordRange.max
            val cursorOffsetInWord = (cursor - wordStart).coerceIn(0, wordEnd - wordStart)
            val word = text.substring(wordStart, wordEnd)
            val insert = prefix + word + suffix
            val newText = text.replaceRange(wordStart, wordEnd, insert)
            val newCursor = (wordStart + prefix.length + cursorOffsetInWord).coerceIn(0, newText.length)
            return TextFieldValue(
                text = newText,
                selection = TextRange(newCursor, newCursor),
            )
        }

    return wrapSelection(prefix = prefix, suffix = suffix, placeholder = placeholder)
}

private fun TextFieldValue.insertBlock(block: String): TextFieldValue {
    val cursor = selection.min
    val before = text.getOrNull(cursor - 1)
    val after = text.getOrNull(cursor)
    val prefix = if (cursor == 0 || before == '\n') "" else "\n"
    val suffix = if (after == null || after == '\n') "" else "\n"
    val insert = prefix + block + "\n" + suffix
    val cursorOffset = insert.length
    return replaceSelection(insert = insert, cursorOffsetInInsert = cursorOffset)
}

private fun TextFieldValue.insertCodeBlock(): TextFieldValue {
    val start = selection.min
    val end = selection.max
    val hasSelection = start != end
    val endForLine =
        if (hasSelection && end > 0 && text.getOrNull(end - 1) == '\n') {
            end - 1
        } else {
            end
        }
    val startLineStart = text.lineStartOf(start)
    val endLineStart = text.lineStartOf(endForLine)
    val wrapEndExclusive = text.lineEndExclusiveOf(endLineStart)
    val content = text.substring(startLineStart, wrapEndExclusive)
    val open = "```\n"
    val close = "```\n"
    val insert = open + (if (content.endsWith('\n')) content else "$content\n") + close
    val newText = text.replaceRange(startLineStart, wrapEndExclusive, insert)

    val shift = open.length
    val newSelection =
        TextRange(
            (startLineStart + shift + (selection.start - startLineStart)).coerceIn(0, newText.length),
            (startLineStart + shift + (selection.end - startLineStart)).coerceIn(0, newText.length),
        )
    return TextFieldValue(
        text = newText,
        selection = newSelection,
    )
}

private fun TextFieldValue.toggleCodeBlock(): TextFieldValue {
    val cursor = selection.min
    val startFenceLineStart = text.findPreviousFenceLineStart(cursor) ?: return insertCodeBlock()
    val startFenceLineEndExclusive = text.lineEndExclusiveOf(startFenceLineStart)
    val endFenceLineStart = text.findNextFenceLineStart(startFenceLineEndExclusive) ?: return insertCodeBlock()
    val endFenceLineEndExclusive = text.lineEndExclusiveOf(endFenceLineStart)

    val isCursorInside =
        cursor >= startFenceLineEndExclusive &&
            cursor <= endFenceLineStart

    if (!isCursorInside) return insertCodeBlock()

    val startFenceLen = startFenceLineEndExclusive - startFenceLineStart
    val desiredSelection =
        if (selection.min == selection.max) {
            val newCursor = (cursor - startFenceLen).coerceAtLeast(0)
            TextRange(newCursor, newCursor)
        } else {
            null
        }
    return removeRanges(
        ranges =
            listOf(
                endFenceLineStart to endFenceLineEndExclusive,
                startFenceLineStart to startFenceLineEndExclusive,
            ),
        newSelection = desiredSelection,
    )
}

private fun TextFieldValue.insertLink(): TextFieldValue {
    val start = selection.min
    val end = selection.max
    val selected = text.substring(start, end)
    val linkText = selected.ifEmpty { "链接文字" }
    val insert = "[$linkText](https://)"
    val cursorOffset =
        if (selected.isEmpty()) {
            1
        } else {
            insert.indexOf("https://")
        }
    return replaceSelection(insert = insert, cursorOffsetInInsert = cursorOffset)
}

private fun TextFieldValue.replaceAt(
    replaceStart: Int,
    replaceEndExclusive: Int,
    replaceWith: String,
): TextFieldValue {
    val start = replaceStart.coerceIn(0, text.length)
    val endExclusive = replaceEndExclusive.coerceIn(start, text.length)
    val newText = text.replaceRange(start, endExclusive, replaceWith)
    val delta = replaceWith.length - (endExclusive - start)

    fun map(pos: Int): Int {
        val p = pos.coerceIn(0, text.length)
        if (start == endExclusive) {
            return if (p < start) {
                p
            } else {
                (p + replaceWith.length).coerceIn(0, newText.length)
            }
        }
        return when {
            p < start -> p
            p < endExclusive -> start + replaceWith.length
            else -> (p + delta).coerceIn(0, newText.length)
        }
    }

    val newSelection =
        TextRange(
            map(selection.start),
            map(selection.end),
        )
    return TextFieldValue(
        text = newText,
        selection = newSelection,
    )
}

private fun TextFieldValue.removeRanges(
    ranges: List<Pair<Int, Int>>,
    newSelection: TextRange? = null,
): TextFieldValue {
    var newText = text
    var newStart = selection.start
    var newEnd = selection.end

    fun map(pos: Int, start: Int, endExclusive: Int): Int =
        when {
            pos <= start -> pos
            pos < endExclusive -> start
            else -> pos - (endExclusive - start)
        }

    ranges
        .sortedByDescending { it.first }
        .forEach { (rawStart, rawEndExclusive) ->
            val start = rawStart.coerceIn(0, newText.length)
            val endExclusive = rawEndExclusive.coerceIn(start, newText.length)
            if (endExclusive == start) return@forEach
            newText = newText.removeRange(start, endExclusive)
            newStart = map(newStart, start, endExclusive).coerceIn(0, newText.length)
            newEnd = map(newEnd, start, endExclusive).coerceIn(0, newText.length)
        }

    val finalSelection = newSelection ?: TextRange(newStart, newEnd)
    return TextFieldValue(
        text = newText,
        selection =
            TextRange(
                finalSelection.start.coerceIn(0, newText.length),
                finalSelection.end.coerceIn(0, newText.length),
            ),
    )
}

private fun String.lineStartOf(index: Int): Int {
    val cursor = index.coerceIn(0, length)
    val idx = lastIndexOf('\n', startIndex = (cursor - 1).coerceAtLeast(0))
    return if (idx < 0) 0 else idx + 1
}

private fun String.lineEndOf(index: Int): Int {
    val cursor = index.coerceIn(0, length)
    val idx = indexOf('\n', startIndex = cursor)
    return if (idx < 0) length else idx
}

private fun String.lineEndExclusiveOf(lineStart: Int): Int {
    val start = lineStart.coerceIn(0, length)
    val idx = indexOf('\n', startIndex = start)
    return if (idx < 0) length else idx + 1
}

private fun String.isFenceLineStart(lineStart: Int): Boolean {
    if (lineStart < 0 || lineStart > length) return false
    if (!startsWith("```", startIndex = lineStart)) return false
    val after = getOrNull(lineStart + 3) ?: return true
    return after == '\n'
}

private fun String.findPreviousFenceLineStart(fromIndex: Int): Int? {
    var lineStart = lineStartOf(fromIndex)
    while (true) {
        if (isFenceLineStart(lineStart)) return lineStart
        if (lineStart == 0) return null
        lineStart = lineStartOf(lineStart - 1)
    }
}

private fun String.findNextFenceLineStart(fromIndex: Int): Int? {
    var index = fromIndex.coerceIn(0, length)
    while (index < length) {
        val lineStart = lineStartOf(index)
        if (lineStart >= index && isFenceLineStart(lineStart)) return lineStart
        val next = lineEndExclusiveOf(lineStart)
        if (next <= index) return null
        index = next
    }
    return null
}

private fun String.findWordRangeAtCursor(cursor: Int): TextRange? {
    fun isCjkLike(codePoint: Int): Boolean =
        codePoint in 0x3400..0x4DBF ||
            codePoint in 0x4E00..0x9FFF ||
            codePoint in 0xF900..0xFAFF ||
            codePoint in 0x2F800..0x2FA1F ||
            codePoint in 0x3040..0x309F ||
            codePoint in 0x30A0..0x30FF ||
            codePoint in 0xAC00..0xD7AF ||
            codePoint in 0x20000..0x2A6DF ||
            codePoint in 0x2A700..0x2B73F ||
            codePoint in 0x2B740..0x2B81F ||
            codePoint in 0x2B820..0x2CEAF ||
            codePoint in 0x2CEB0..0x2EBEF

    fun classify(c: Char): WordCharClass =
        when {
            c == '_' -> WordCharClass.Latin
            c.isLetterOrDigit() -> if (isCjkLike(c.code)) WordCharClass.Cjk else WordCharClass.Latin
            else -> WordCharClass.Other
        }

    val index = cursor.coerceIn(0, length)
    val beforeClass = getOrNull(index - 1)?.let(::classify) ?: WordCharClass.Other
    val afterClass = getOrNull(index)?.let(::classify) ?: WordCharClass.Other
    val baseClass =
        when {
            afterClass != WordCharClass.Other -> afterClass
            beforeClass != WordCharClass.Other -> beforeClass
            else -> WordCharClass.Other
        }
    if (baseClass == WordCharClass.Other) return null
    val baseIndex = if (afterClass != WordCharClass.Other) index else (index - 1).coerceAtLeast(0)

    var start = baseIndex
    while (start > 0 && classify(this[start - 1]) == baseClass) {
        start--
    }
    var endExclusive = baseIndex + 1
    while (endExclusive < length && classify(this[endExclusive]) == baseClass) {
        endExclusive++
    }
    return TextRange(start, endExclusive)
}

private enum class WordCharClass {
    Latin,
    Cjk,
    Other,
}
