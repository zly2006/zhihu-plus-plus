package com.github.zly2006.zhihu.shared.util

data class ContinuousUsageReminder(
    val elapsedForegroundMs: Long,
    val durationText: String,
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
            durationText = formatDuration(elapsedForegroundMs),
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

    private fun formatDuration(elapsedForegroundMs: Long): String {
        val totalMinutes = (elapsedForegroundMs / ONE_MINUTE_MS).coerceAtLeast(1L)
        val hours = totalMinutes / 60L
        val minutes = totalMinutes % 60L

        return when {
            hours > 0L && minutes > 0L -> "${hours}小时${minutes}分钟"
            hours > 0L -> "${hours}小时"
            else -> "${minutes}分钟"
        }
    }

    companion object {
        private const val ONE_MINUTE_MS = 60_000L

        val SUPPORTED_INTERVALS_MINUTES = setOf(15, 30, 60)

        fun normalizeIntervalMinutes(intervalMinutes: Int): Int = if (intervalMinutes in SUPPORTED_INTERVALS_MINUTES) {
            intervalMinutes
        } else {
            0
        }
    }
}
