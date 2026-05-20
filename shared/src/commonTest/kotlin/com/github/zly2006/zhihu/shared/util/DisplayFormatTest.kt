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
