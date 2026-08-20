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

package com.github.zly2006.zhihu.ui.subscreens

import com.github.zly2006.zhihu.platform.SettingsStore
import kotlin.test.Test
import kotlin.test.assertEquals

class AppearanceFontSizeTest {
    @Test
    fun fontSizeLevelsAreFineNearDefaultAndCoarserAtLargeSizes() {
        assertEquals(
            (50..120 step 5).toList() + (130..200 step 10).toList(),
            contentFontSizeLevels,
        )
    }

    @Test
    fun fontSizeSnapsToNearestLevel() {
        assertEquals(50, normalizedContentFontSize(20))
        assertEquals(105, normalizedContentFontSize(107))
        assertEquals(110, normalizedContentFontSize(108))
        assertEquals(120, normalizedContentFontSize(125))
        assertEquals(200, normalizedContentFontSize(240))
    }

    @Test
    fun readingFontSizeNormalizesAndPersistsStoredValue() {
        var storedFontSize = 107
        val settings = SettingsStore(
            getBoolean = { _, defaultValue -> defaultValue },
            putBoolean = { _, _ -> },
            getString = { _, defaultValue -> defaultValue },
            putString = { _, _ -> },
            getStringOrNull = { _ -> null },
            putStringSet = { _, _ -> },
            getStringSet = { _, defaultValue -> defaultValue },
            getInt = { key, defaultValue -> if (key == PREF_FONT_SIZE) storedFontSize else defaultValue },
            putInt = { key, value -> if (key == PREF_FONT_SIZE) storedFontSize = value },
            getLong = { _, defaultValue -> defaultValue },
            putLong = { _, _ -> },
            getFloat = { _, defaultValue -> defaultValue },
            putFloat = { _, _ -> },
            remove = { _ -> },
        )

        assertEquals(105, settings.contentFontSize())
        assertEquals(105, storedFontSize)
    }
}
