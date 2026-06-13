/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
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
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.roundToInt
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class HttpStatusException(
    val status: HttpStatusCode,
    val requestUrl: Url,
    val bodyText: String,
) : Exception() {
    override val message: String
        get() = "HTTP error: ${status.value} ${status.description} on $requestUrl: \n $bodyText"

    var dumpedCurlRequest: String? = null

    constructor(
        status: HttpStatusCode,
        requestUrl: Url,
        bodyText: String,
        dumpedCurlRequest: String?,
    ) : this(status, requestUrl, bodyText) {
        this.dumpedCurlRequest = dumpedCurlRequest
    }
}

suspend fun HttpResponse.raiseForStatus(dumpRequest: Boolean = false): HttpResponse {
    if (status.value >= 400) {
        throw HttpStatusException(
            status = status,
            requestUrl = request.url,
            bodyText = bodyAsText(),
            dumpedCurlRequest = if (dumpRequest) dumpCurlRequest() else null,
        )
    }
    return this
}

fun HttpResponse.dumpCurlRequest(): String {
    val sb = StringBuilder()
    sb.append("curl -X ${request.method.value} '${request.url}' ")
    request.headers.forEach { key, values ->
        values.forEach { value ->
            sb.append("\\\n  -H '$key: $value' ")
        }
    }
    return sb.toString()
}

fun formatCompactCount(count: Int): String = when {
    count >= 10_000 -> {
        if (count % 10_000 == 0) {
            "${count / 10_000} 万"
        } else {
            val roundedTenths = ((count / 10_000.0) * 10).roundToInt() / 10.0
            "$roundedTenths 万"
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
            "${(dateTime.month.ordinal + 1).twoDigitString()}-${dateTime.day.twoDigitString()} ${dateTime.hour.twoDigitString()}:${dateTime.minute.twoDigitString()}"
        }
    }
}

internal fun Int.twoDigitString(): String = toString().padStart(2, '0')

fun extractImageUrl(attribute: (String) -> String): String? =
    attribute("data-original-token")
        .takeIf { it.startsWith("v2-") }
        ?.let { "https://pic1.zhimg.com/$it" }
        ?: attribute("data-original").takeIf { it.isNotBlank() }
        ?: attribute("data-default-watermark-src").takeIf { it.isNotBlank() }
        ?: attribute("data-actualsrc").takeIf { it.isNotBlank() }
        ?: attribute("data-thumbnail").takeIf { it.isNotBlank() }
        ?: attribute("src").takeIf { it.isNotBlank() }
