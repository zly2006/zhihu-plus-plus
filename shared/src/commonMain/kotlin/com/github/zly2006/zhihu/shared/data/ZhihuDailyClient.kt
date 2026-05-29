package com.github.zly2006.zhihu.shared.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

const val ZHIHU_DAILY_LATEST_URL = "https://news-at.zhihu.com/api/4/stories/latest"

suspend fun fetchLatestDailyStories(client: HttpClient): DailyStoriesResponse =
    client.get(ZHIHU_DAILY_LATEST_URL).body()

suspend fun fetchDailyStoriesBefore(
    client: HttpClient,
    date: String,
): DailyStoriesResponse = client.get("https://news-at.zhihu.com/api/4/stories/before/$date").body()

suspend fun fetchDailyStoriesForDate(
    client: HttpClient,
    date: String,
): DailyStoriesResponse = fetchDailyStoriesBefore(client, nextDailyApiDate(date))

fun nextDailyApiDate(date: String): String {
    require(date.length == 8 && date.all { it.isDigit() }) {
        "date must use yyyyMMdd format"
    }
    val localDate = LocalDate.parse("${date.substring(0, 4)}-${date.substring(4, 6)}-${date.substring(6, 8)}")
    val nextDate = localDate.plus(1, DateTimeUnit.DAY)
    return nextDate.toString().replace("-", "")
}
