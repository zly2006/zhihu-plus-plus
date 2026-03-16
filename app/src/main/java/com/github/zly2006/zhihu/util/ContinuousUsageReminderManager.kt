package com.github.zly2006.zhihu.util

import android.content.Context
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import androidx.lifecycle.lifecycleScope
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

class ContinuousUsageReminderManager(
    private val activity: ComponentActivity,
) {
    private val preferences by lazy {
        activity.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
    }

    private var policy = ContinuousUsageReminderPolicy(loadIntervalMinutes())
    private var sessionAccumulatedForegroundMs =
        preferences.getLong(KEY_SESSION_ACCUMULATED_FOREGROUND_MS, 0L).coerceAtLeast(0L)
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
            preferences.edit {
                putLong(KEY_SESSION_ACCUMULATED_FOREGROUND_MS, sessionAccumulatedForegroundMs)
                putLong(KEY_SESSION_LAST_BACKGROUND_WALL_CLOCK_MS, System.currentTimeMillis())
            }
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
            .setMessage("你已经看了${reminder.hoursText}个小时知乎了\n\n休息一下吧")
            .setPositiveButton("知道了", null)
            .show()
    }

    private fun restoreSessionForForegroundStart() {
        val now = System.currentTimeMillis()
        val lastBackgroundWallClockMs = preferences.getLong(KEY_SESSION_LAST_BACKGROUND_WALL_CLOCK_MS, 0L)
        val shouldContinueSession = shouldContinueSession(lastBackgroundWallClockMs, now)

        if (shouldContinueSession) {
            sessionAccumulatedForegroundMs =
                preferences.getLong(KEY_SESSION_ACCUMULATED_FOREGROUND_MS, 0L).coerceAtLeast(0L)
        } else {
            sessionAccumulatedForegroundMs = 0L
            policy.resetSession()
            preferences.edit { putLong(KEY_SESSION_ACCUMULATED_FOREGROUND_MS, 0L) }
        }

        preferences.edit { putLong(KEY_SESSION_LAST_BACKGROUND_WALL_CLOCK_MS, 0L) }
    }

    private fun currentElapsedForegroundMs(): Long {
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
        val storedInterval = preferences.getInt(KEY_CONTINUOUS_USAGE_REMINDER_INTERVAL_MINUTES, 0)
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

data class ContinuousUsageReminder(
    val elapsedForegroundMs: Long,
    val hoursText: String,
)

class ContinuousUsageReminderPolicy(
    intervalMinutes: Int,
) {
    var intervalMinutes: Int = normalizeIntervalMinutes(intervalMinutes)
        private set

    val isEnabled: Boolean
        get() = intervalMinutes > 0

    private var lastReminderBucket: Int = 0

    fun consumeReminder(elapsedForegroundMs: Long): ContinuousUsageReminder? {
        if (!isEnabled || elapsedForegroundMs <= 0L) return null

        val intervalMs = intervalMinutes * ONE_MINUTE_MS
        val currentBucket = (elapsedForegroundMs / intervalMs).toInt()
        if (currentBucket <= lastReminderBucket) return null

        lastReminderBucket = currentBucket
        return ContinuousUsageReminder(
            elapsedForegroundMs = elapsedForegroundMs,
            hoursText = formatHours(elapsedForegroundMs.toDouble() / ONE_HOUR_MS),
        )
    }

    fun updateInterval(intervalMinutes: Int, elapsedForegroundMs: Long) {
        this.intervalMinutes = normalizeIntervalMinutes(intervalMinutes)
        if (!isEnabled) {
            lastReminderBucket = 0
            return
        }

        val intervalMs = this.intervalMinutes * ONE_MINUTE_MS
        lastReminderBucket = (elapsedForegroundMs / intervalMs).toInt()
    }

    fun resetSession() {
        lastReminderBucket = 0
    }

    private fun formatHours(hours: Double): String {
        val rounded = (hours * 100).roundToInt() / 100.0
        val integerPart = rounded.toInt()
        if (rounded == integerPart.toDouble()) {
            return integerPart.toString()
        }
        return rounded
            .toString()
            .trimEnd('0')
            .trimEnd('.')
    }

    companion object {
        private const val ONE_MINUTE_MS = 60_000L
        private const val ONE_HOUR_MS = 3_600_000.0

        val SUPPORTED_INTERVALS_MINUTES = setOf(15, 30, 60)

        fun normalizeIntervalMinutes(intervalMinutes: Int): Int = if (intervalMinutes in SUPPORTED_INTERVALS_MINUTES) {
            intervalMinutes
        } else {
            0
        }
    }
}
