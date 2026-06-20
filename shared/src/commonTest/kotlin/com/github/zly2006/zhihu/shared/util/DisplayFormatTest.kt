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

package com.github.zly2006.zhihu.shared.util

import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals

class DisplayFormatTest {
    @Test
    fun compactCountFormatsWanUnits() {
        assertEquals("9999", formatCompactCount(9_999))
        assertEquals("1 万", formatCompactCount(10_000))
        assertEquals("1.2 万", formatCompactCount(12_345))
    }

    @Test
    fun dailyDateFormatsKnownZhihuDateShape() {
        assertEquals("2026年05月21日", formatDailyDate("20260521"))
        assertEquals("2026-05-21", formatDailyDate("2026-05-21"))
    }

    @Test
    fun relativeTimeFormatsRecentAndAbsoluteValues() {
        assertEquals("刚刚", formatRelativeTime(epochSeconds = 100, nowEpochSeconds = 150))
        assertEquals("2分钟前", formatRelativeTime(epochSeconds = 100, nowEpochSeconds = 220))
        assertEquals("3小时前", formatRelativeTime(epochSeconds = 100, nowEpochSeconds = 11_000))
        assertEquals("2天前", formatRelativeTime(epochSeconds = 100, nowEpochSeconds = 172_900))
        assertEquals(
            "01-01 00:00",
            formatRelativeTime(
                epochSeconds = 0,
                nowEpochSeconds = 604_800,
                timeZone = TimeZone.UTC,
            ),
        )
    }
}
