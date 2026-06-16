/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
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

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
    Quote(">"),
    Divider("HR"),
    CodeBlock("```"),
    InlineCode("</>"),
    Math("∑"),
    Link("Link"),
}

@Composable
fun MarkdownShortcutToolbar(
    onApplyShortcut: (MarkdownShortcut) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .testTag("markdown_shortcut_toolbar"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 6.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            MarkdownShortcut.entries.forEach { shortcut ->
                ShortcutButton(
                    label = shortcut.label,
                    enabled = enabled,
                    onClick = { onApplyShortcut(shortcut) },
                )
            }
            Spacer(Modifier.width(2.dp))
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
        MarkdownShortcut.H1 -> insertPrefixAtLineStart("# ")
        MarkdownShortcut.H2 -> insertPrefixAtLineStart("## ")
        MarkdownShortcut.Bold -> wrapSelection(prefix = "**", suffix = "**", placeholder = "加粗")
        MarkdownShortcut.Italic -> wrapSelection(prefix = "*", suffix = "*", placeholder = "斜体")
        MarkdownShortcut.Quote -> insertPrefixAtLineStart("> ")
        MarkdownShortcut.Divider -> insertBlock("---")
        MarkdownShortcut.CodeBlock -> insertCodeBlock()
        MarkdownShortcut.InlineCode -> wrapSelection(prefix = "`", suffix = "`", placeholder = "code")
        MarkdownShortcut.Math -> wrapSelection(prefix = "$", suffix = "$", placeholder = "x")
        MarkdownShortcut.Link -> insertLink()
    }

fun TextFieldValue.insertTextAtSelection(insert: String): TextFieldValue =
    replaceSelection(insert = insert, cursorOffsetInInsert = insert.length)

private fun TextFieldValue.replaceSelection(
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

private fun TextFieldValue.insertPrefixAtLineStart(prefix: String): TextFieldValue {
    val cursor = selection.min
    val lineStart = text.lastIndexOf('\n', startIndex = (cursor - 1).coerceAtLeast(0)).let { idx ->
        if (idx < 0) 0 else idx + 1
    }
    val newText = buildString {
        append(text.substring(0, lineStart))
        append(prefix)
        append(text.substring(lineStart))
    }
    val delta = prefix.length
    val newSelection =
        if (selection.min == selection.max) {
            val newCursor = (cursor + delta).coerceIn(0, newText.length)
            TextRange(newCursor, newCursor)
        } else {
            TextRange(selection.min + delta, selection.max + delta)
        }
    return TextFieldValue(
        text = newText,
        selection = newSelection,
    )
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
    val cursor = selection.min
    val before = text.getOrNull(cursor - 1)
    val after = text.getOrNull(cursor)
    val prefix = if (cursor == 0 || before == '\n') "" else "\n"
    val suffix = if (after == null || after == '\n') "" else "\n"
    val insert = prefix + "```\n\n```\n" + suffix
    val cursorOffset = prefix.length + "```\n".length
    return replaceSelection(insert = insert, cursorOffsetInInsert = cursorOffset)
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
