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

package com.github.zly2006.zhihu.updater

const val ZHIHU_PLUS_PLUS_GITHUB_LATEST_RELEASE_URL =
    com.github.zly2006.zhihu.shared.updater.ZHIHU_PLUS_PLUS_GITHUB_LATEST_RELEASE_URL
const val ZHIHU_PLUS_PLUS_REDEN_LATEST_RELEASE_URL =
    com.github.zly2006.zhihu.shared.updater.ZHIHU_PLUS_PLUS_REDEN_LATEST_RELEASE_URL
const val ZHIHU_PLUS_PLUS_GITHUB_NIGHTLY_RELEASE_URL =
    com.github.zly2006.zhihu.shared.updater.ZHIHU_PLUS_PLUS_GITHUB_NIGHTLY_RELEASE_URL

typealias GithubRelease = com.github.zly2006.zhihu.shared.updater.GithubRelease
typealias GithubAsset = com.github.zly2006.zhihu.shared.updater.GithubAsset

fun extractGithubReleaseNotes(body: String): String =
    com.github.zly2006.zhihu.shared.updater
        .extractGithubReleaseNotes(body)
