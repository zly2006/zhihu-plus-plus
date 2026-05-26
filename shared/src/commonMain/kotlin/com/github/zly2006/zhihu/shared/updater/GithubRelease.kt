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

package com.github.zly2006.zhihu.shared.updater

import com.github.zly2006.zhihu.shared.util.raiseForStatus
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.http.HttpHeaders
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

const val ZHIHU_PLUS_PLUS_GITHUB_LATEST_RELEASE_URL = "https://api.github.com/repos/zly2006/zhihu-plus-plus/releases/latest"
const val ZHIHU_PLUS_PLUS_REDEN_LATEST_RELEASE_URL = "https://redenmc.com/api/zhihu/releases/latest"
const val ZHIHU_PLUS_PLUS_GITHUB_NIGHTLY_RELEASE_URL = "https://api.github.com/repos/zly2006/zhihu-plus-plus/releases/tags/nightly"

@Serializable
data class GithubRelease(
    @SerialName("tag_name") val tagName: String = "",
    @SerialName("html_url") val htmlUrl: String? = null,
    val body: String? = null,
    val assets: List<GithubAsset> = emptyList(),
)

@Serializable
data class GithubAsset(
    val name: String,
    @SerialName("content_type") val contentType: String,
    @SerialName("browser_download_url") val browserDownloadUrl: String,
    @SerialName("cn_download_url") val cnDownloadUrl: String? = null,
)

fun extractGithubReleaseNotes(body: String): String = body
    .replace("\r\n", "\n")
    .substringAfter("## What's Changed\n")
    .substringBefore("\n**Full Changelog**:")
    .trimEnd('\n')

suspend fun fetchLatestZhihuRelease(
    client: HttpClient,
    githubToken: String?,
): GithubRelease = runCatching {
    client.get(ZHIHU_PLUS_PLUS_REDEN_LATEST_RELEASE_URL).raiseForStatus().body<GithubRelease>()
}.getOrNull() ?: client
    .get(ZHIHU_PLUS_PLUS_GITHUB_LATEST_RELEASE_URL) {
        githubToken?.let { token ->
            headers {
                append(HttpHeaders.Authorization, "Bearer $token")
            }
        }
    }.raiseForStatus()
    .body<GithubRelease>()

suspend fun fetchNightlyZhihuRelease(
    client: HttpClient,
    githubToken: String?,
): GithubRelease = client
    .get(ZHIHU_PLUS_PLUS_GITHUB_NIGHTLY_RELEASE_URL) {
        githubToken?.let { token ->
            headers {
                append(HttpHeaders.Authorization, "Bearer $token")
            }
        }
    }.raiseForStatus()
    .body<GithubRelease>()
