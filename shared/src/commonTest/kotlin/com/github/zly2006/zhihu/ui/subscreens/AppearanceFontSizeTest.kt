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

import kotlin.math.abs
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
    fun storedValuesSnapOnlyForSettingsSelection() {
        val storedValues = listOf(20, 107, 108, 125, 240)
        val snappedValues = storedValues.map { storedValue ->
            val boundedValue = storedValue.coerceIn(
                contentFontSizeLevels.first(),
                contentFontSizeLevels.last(),
            )
            contentFontSizeLevels.minBy { abs(it - boundedValue) }
        }

        assertEquals(listOf(50, 105, 110, 120, 200), snappedValues)
        assertEquals(11, contentFontSizeLevels.indexOf(105))
    }
}
