package com.github.zly2006.zhihu.shared.updater

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

const val ZHIHU_PLUS_PLUS_GITHUB_LATEST_RELEASE_URL = "https://api.github.com/repos/zly2006/zhihu-plus-plus/releases/latest"
const val ZHIHU_PLUS_PLUS_REDEN_LATEST_RELEASE_URL = "https://redenmc.com/api/zhihu/releases/latest"
const val ZHIHU_PLUS_PLUS_GITHUB_NIGHTLY_RELEASE_URL = "https://api.github.com/repos/zly2006/zhihu-plus-plus/releases/tags/nightly"

@Serializable
data class GithubRelease(
    @SerialName("tag_name") val tagName: String = "",
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

data class GithubDownloadInfo(
    val browserDownloadUrl: String,
    val cnDownloadUrl: String? = null,
)

fun extractGithubReleaseNotes(body: String): String = body
    .replace("\r\n", "\n")
    .substringAfter("## What's Changed\n")
    .substringBefore("\n**Full Changelog**:")
    .trimEnd('\n')
