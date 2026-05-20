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
