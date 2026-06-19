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

import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import com.github.zly2006.zhihu.shared.platform.androidSettingsStore
import com.github.zly2006.zhihu.shared.util.ContinuousUsageReminderPolicy
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class ContinuousUsageReminderManager(
    private val activity: ComponentActivity,
) {
    private val settingsStore by lazy { androidSettingsStore(activity) }

    private var policy = ContinuousUsageReminderPolicy(loadIntervalMinutes())
    private var sessionAccumulatedForegroundMs =
        settingsStore.getLong(KEY_SESSION_ACCUMULATED_FOREGROUND_MS, 0L).coerceAtLeast(0L)
    private var foregroundStartElapsedMs: Long? = null
    private var checkJob: Job? = null
    private var reminderDialog: AlertDialog? = null

    fun onAppForeground() {
        if (foregroundStartElapsedMs == null) {
            restoreSessionForForegroundStart()
            foregroundStartElapsedMs = SystemClock.elapsedRealtime()
        }
        val elapsedForegroundMs = currentElapsedForegroundMs()
        syncIntervalWithPreferences(elapsedForegroundMs, forceUpdatePolicyBucket = true)
        ensureCheckLoop()
        checkAndShowReminder()
    }

    fun onAppBackground() {
        checkJob?.cancel()
        checkJob = null

        foregroundStartElapsedMs?.let { startElapsed ->
            val foregroundDuration = SystemClock.elapsedRealtime() - startElapsed
            sessionAccumulatedForegroundMs += foregroundDuration
            sessionAccumulatedForegroundMs = sessionAccumulatedForegroundMs.coerceAtLeast(0L)
            settingsStore.putLong(KEY_SESSION_ACCUMULATED_FOREGROUND_MS, sessionAccumulatedForegroundMs)
            settingsStore.putLong(KEY_SESSION_LAST_BACKGROUND_WALL_CLOCK_MS, System.currentTimeMillis())
        }
        foregroundStartElapsedMs = null

        reminderDialog?.dismiss()
        reminderDialog = null
    }

    fun onDestroy() {
        checkJob?.cancel()
        checkJob = null
        reminderDialog?.dismiss()
        reminderDialog = null
    }

    private fun ensureCheckLoop() {
        if (checkJob?.isActive == true) return
        checkJob = activity.lifecycleScope.launch {
            while (isActive) {
                delay(CHECK_INTERVAL_MS)
                checkAndShowReminder()
            }
        }
    }

    private fun checkAndShowReminder() {
        val elapsedForegroundMs = currentElapsedForegroundMs()
        if (elapsedForegroundMs <= 0L) return

        syncIntervalWithPreferences(elapsedForegroundMs)

        val reminder = policy.consumeReminder(elapsedForegroundMs) ?: return
        if (activity.isFinishing || activity.isDestroyed) return

        if (reminderDialog?.isShowing == true) return
        reminderDialog = AlertDialog
            .Builder(activity)
            .setTitle("连续浏览提醒")
            .setMessage("你已经连续浏览知乎 ${reminder.durationText}\n\n休息一下吧")
            .setPositiveButton("知道了", null)
            .show()
    }

    private fun restoreSessionForForegroundStart() {
        val now = System.currentTimeMillis()
        val lastBackgroundWallClockMs = settingsStore.getLong(KEY_SESSION_LAST_BACKGROUND_WALL_CLOCK_MS, 0L)
        val shouldContinueSession = shouldContinueSession(lastBackgroundWallClockMs, now)

        if (shouldContinueSession) {
            sessionAccumulatedForegroundMs =
                settingsStore.getLong(KEY_SESSION_ACCUMULATED_FOREGROUND_MS, 0L).coerceAtLeast(0L)
        } else {
            sessionAccumulatedForegroundMs = 0L
            policy.resetSession()
            settingsStore.putLong(KEY_SESSION_ACCUMULATED_FOREGROUND_MS, 0L)
        }

        settingsStore.putLong(KEY_SESSION_LAST_BACKGROUND_WALL_CLOCK_MS, 0L)
    }

    fun currentElapsedForegroundMs(): Long {
        val startElapsed = foregroundStartElapsedMs ?: return sessionAccumulatedForegroundMs
        return sessionAccumulatedForegroundMs + (SystemClock.elapsedRealtime() - startElapsed)
    }

    private fun syncIntervalWithPreferences(
        elapsedForegroundMs: Long = 0L,
        forceUpdatePolicyBucket: Boolean = false,
    ) {
        val interval = loadIntervalMinutes()
        if (!forceUpdatePolicyBucket && interval == policy.intervalMinutes) return
        policy.updateInterval(intervalMinutes = interval, elapsedForegroundMs = elapsedForegroundMs)
    }

    private fun loadIntervalMinutes(): Int {
        val storedInterval = settingsStore.getInt(KEY_CONTINUOUS_USAGE_REMINDER_INTERVAL_MINUTES, 0)
        return ContinuousUsageReminderPolicy.normalizeIntervalMinutes(storedInterval)
    }

    companion object {
        const val KEY_CONTINUOUS_USAGE_REMINDER_INTERVAL_MINUTES = "continuousUsageReminderIntervalMinutes"
        private const val KEY_SESSION_ACCUMULATED_FOREGROUND_MS = "continuousUsageReminderSessionAccumulatedMs"
        private const val KEY_SESSION_LAST_BACKGROUND_WALL_CLOCK_MS = "continuousUsageReminderLastBackgroundWallClockMs"
        private const val CHECK_INTERVAL_MS = 10_000L
        private const val CONTINUITY_GRACE_MS = 5 * 60_000L

        internal fun shouldContinueSession(lastBackgroundWallClockMs: Long, nowWallClockMs: Long): Boolean {
            if (lastBackgroundWallClockMs <= 0L) return false
            if (nowWallClockMs < lastBackgroundWallClockMs) return false
            return nowWallClockMs - lastBackgroundWallClockMs <= CONTINUITY_GRACE_MS
        }
    }
}
