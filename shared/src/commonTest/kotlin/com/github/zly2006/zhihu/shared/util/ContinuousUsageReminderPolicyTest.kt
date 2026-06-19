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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ContinuousUsageReminderPolicyTest {
    @Test
    fun disabledIntervalDoesNotEmitReminder() {
        val policy = ContinuousUsageReminderPolicy(intervalMinutes = 7)

        assertFalse(policy.isEnabled)
        assertNull(policy.consumeReminder(elapsedForegroundMs = 60_000L * 60L))
    }

    @Test
    fun emitsOncePerElapsedIntervalBucket() {
        val policy = ContinuousUsageReminderPolicy(intervalMinutes = 15)

        assertNull(policy.consumeReminder(elapsedForegroundMs = 14 * 60_000L))

        val first = policy.consumeReminder(elapsedForegroundMs = 15 * 60_000L)
        assertNotNull(first)
        assertEquals("15分钟", first.durationText)
        assertNull(policy.consumeReminder(elapsedForegroundMs = 16 * 60_000L))

        val second = policy.consumeReminder(elapsedForegroundMs = 30 * 60_000L)
        assertNotNull(second)
        assertEquals("30分钟", second.durationText)
    }

    @Test
    fun updateIntervalStartsFromCurrentElapsedBucket() {
        val policy = ContinuousUsageReminderPolicy(intervalMinutes = 15)

        policy.updateInterval(intervalMinutes = 30, elapsedForegroundMs = 65 * 60_000L)

        assertTrue(policy.isEnabled)
        assertNull(policy.consumeReminder(elapsedForegroundMs = 80 * 60_000L))
        assertEquals("1小时30分钟", policy.consumeReminder(elapsedForegroundMs = 90 * 60_000L)?.durationText)
    }
}
