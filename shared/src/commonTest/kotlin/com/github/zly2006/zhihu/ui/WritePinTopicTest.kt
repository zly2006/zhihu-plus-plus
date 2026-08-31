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

package com.github.zly2006.zhihu.ui

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.github.zly2006.zhihu.editor.PinContentTopicItem
import com.github.zly2006.zhihu.editor.PinContentTopicMarker
import com.github.zly2006.zhihu.editor.PinTopicSuggestion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WritePinTopicTest {
    @Test
    fun ignoresTopicQueryWhenCursorIsAtTextStart() {
        assertNull(activePinTopicQuery(TextFieldValue("#机器学习", selection = TextRange.Zero)))
        assertNull(activePinTopicQuery(TextFieldValue("正文 #机器学习", selection = TextRange.Zero)))
        assertNull(activePinTopicQuery(TextFieldValue("", selection = TextRange.Zero)))
    }

    @Test
    fun findsInlineTopicQueryIncludingSpaces() {
        val value = TextFieldValue("正文 #机器 学习", selection = TextRange("正文 #机器 学习".length))
        assertEquals("机器 学习", activePinTopicQuery(value)?.query)
    }

    @Test
    fun doesNotReopenSuggestionForSelectedInlineTopic() {
        val value = TextFieldValue("#编程 ", selection = TextRange(4))
        assertNull(activePinTopicQuery(value, listOf(PinContentTopicMarker(PinContentTopicItem("1354", "编程"), 0, 3))))
        assertNull(
            activePinTopicQuery(
                value.copy(selection = TextRange(2)),
                listOf(PinContentTopicMarker(PinContentTopicItem("1354", "编程"), 0, 3)),
            ),
        )
    }

    @Test
    fun insertsSuggestedTopicAtCursorWithoutExternalChip() {
        val value = TextFieldValue("正文 #编", selection = TextRange(5))
        val query = activePinTopicQuery(value)!!
        val result = insertPinTopic(
            value,
            query,
            PinTopicSuggestion("19554298", "编程", 1354, "402 万讨论"),
        )
        assertEquals("正文 #编程 ", result.text)
        assertEquals(result.text.length, result.selection.start)
    }

    @Test
    fun keepsMarkerOnOutsideEditAndRemovesItWhenEntityIsEdited() {
        val marker = PinContentTopicMarker(PinContentTopicItem("1354", "编程"), 3, 6)
        assertEquals(
            marker.copy(start = 5, endExclusive = 8),
            updatePinTopicMarkers("正文 #编程 ", "前缀正文 #编程 ", listOf(marker)).single(),
        )
        assertEquals(
            emptyList(),
            updatePinTopicMarkers("正文 #编程 ", "正文 #编 ", listOf(marker)),
        )
    }

    @Test
    fun shiftsExistingMarkerWhenAnotherTopicIsInsertedBeforeIt() {
        val existing = PinContentTopicMarker(PinContentTopicItem("19554298", "编程"), 6, 9)
        val oldValue = TextFieldValue("#机 正文 #编程 ", selection = TextRange(2))
        val query = activePinTopicQuery(oldValue)!!
        val newValue = insertPinTopic(oldValue, query, PinTopicSuggestion("1", "机器学习", 2, ""))

        assertEquals(
            existing.copy(start = 10, endExclusive = 13),
            updatePinTopicMarkers(oldValue.text, newValue.text, listOf(existing)).single(),
        )
    }
}
