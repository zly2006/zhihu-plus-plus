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

package com.github.zly2006.zhihu.util

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
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

    @Test
    fun answerDatesDistinguishYearsAndKeepRecentRelativeTimes() {
        val now = LocalDateTime(2026, 10, 27, 12, 0).toInstant(TimeZone.UTC).epochSeconds
        assertEquals("刚刚", formatAnswerTime(now - 30, now, TimeZone.UTC))
        assertEquals("2小时前", formatAnswerTime(now - 7_200, now, TimeZone.UTC))
        assertEquals("6天前", formatAnswerTime(now - 6 * 86_400, now, TimeZone.UTC))
        assertEquals("10-20", formatAnswerTime(now - 7 * 86_400, now, TimeZone.UTC))
        assertEquals("03-22", formatAnswerTime(LocalDateTime(2026, 3, 22, 19, 0).toInstant(TimeZone.UTC).epochSeconds, now, TimeZone.UTC))
        assertEquals("2025-03-22", formatAnswerTime(LocalDateTime(2025, 3, 22, 19, 0).toInstant(TimeZone.UTC).epochSeconds, now, TimeZone.UTC))
        val newYear = LocalDateTime(2026, 1, 1, 0, 0).toInstant(TimeZone.UTC).epochSeconds
        val oldDate = LocalDateTime(2025, 12, 20, 23, 0).toInstant(TimeZone.UTC).epochSeconds
        assertEquals("2025-12-20", formatAnswerTime(oldDate, newYear, TimeZone.UTC))
    }

    @Test
    fun fullDateTimeIncludesSecondsInSystemTimeZone() {
        val epochSeconds = LocalDateTime(1970, 1, 1, 0, 0, 0)
            .toInstant(TimeZone.currentSystemDefault())
            .epochSeconds
        assertEquals(
            "1970-01-01 00:00:00",
            formatFullDateTime(epochSeconds = epochSeconds),
        )
    }
}
