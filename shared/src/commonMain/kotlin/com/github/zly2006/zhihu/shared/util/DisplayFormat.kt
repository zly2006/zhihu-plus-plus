package com.github.zly2006.zhihu.shared.util

import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

fun formatCompactCount(count: Int): String = when {
    count >= 10_000 -> {
        if (count % 10_000 == 0) {
            "${count / 10_000} 万"
        } else {
            val roundedTenths = ((count / 10_000.0) * 10).roundToInt() / 10.0
            "${roundedTenths.formatOneDecimal()} 万"
        }
    }

    else -> count.toString()
}

fun formatDailyDate(dateString: String): String {
    if (dateString.length != 8 || dateString.any { !it.isDigit() }) {
        return dateString
    }
    return "${dateString.substring(0, 4)}年${dateString.substring(4, 6)}月${dateString.substring(6, 8)}日"
}

@OptIn(ExperimentalTime::class)
fun formatRelativeTime(
    epochSeconds: Long,
    nowEpochSeconds: Long = Clock.System.now().epochSeconds,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): String {
    val diff = nowEpochSeconds - epochSeconds

    return when {
        diff < 60 -> "刚刚"
        diff < 3_600 -> "${diff / 60}分钟前"
        diff < 86_400 -> "${diff / 3_600}小时前"
        diff < 604_800 -> "${diff / 86_400}天前"
        else -> {
            val dateTime = Instant
                .fromEpochSeconds(epochSeconds)
                .toLocalDateTime(timeZone)
            "${(dateTime.month.ordinal + 1).pad2()}-${dateTime.day.pad2()} ${dateTime.hour.pad2()}:${dateTime.minute.pad2()}"
        }
    }
}

private fun Int.pad2(): String = toString().padStart(2, '0')

private fun Double.formatOneDecimal(): String {
    val text = toString()
    val dotIndex = text.indexOf('.')
    if (dotIndex < 0) return "$text.0"
    return text.substring(0, minOf(text.length, dotIndex + 2))
}
