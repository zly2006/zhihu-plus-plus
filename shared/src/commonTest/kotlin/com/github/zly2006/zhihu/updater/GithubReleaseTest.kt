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

package com.github.zly2006.zhihu.updater

import com.github.zly2006.zhihu.data.ZhihuJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GithubReleaseTest {
    @Test
    fun fallsBackToQuarkLinkInOriginalBodyWithoutApk() {
        for (body in listOf(
            "夸克：https://pan.quark.cn/s/abc123\n## What's Changed\n修复问题",
            "[夸克下载](https://pan.quark.cn/s/abc123)",
            "<https://pan.quark.cn/s/abc123>",
        )) {
            val info = GithubRelease(body = body).extractAndroidDownloadInfo(true)
            assertEquals("https://pan.quark.cn/s/abc123", info.browserDownloadUrl)
            assertNull(info.cnDownloadUrl)
            assertTrue(info.opensExternally)
        }
    }

    @Test
    fun missingApkAndQuarkLinkFails() {
        assertFailsWith<IllegalStateException> {
            GithubRelease(body = "https://example.com/s/abc123").extractAndroidDownloadInfo(true)
        }
    }

    @Test
    fun apkKeepsPriorityAndVariantSelection() {
        val lite = GithubAsset("app-lite.apk", "application/vnd.android.package-archive", "https://example.com/lite.apk", "https://example.com/mirror")
        val full = lite.copy(name = "app-full.apk", browserDownloadUrl = "https://example.com/full.apk")
        val release = GithubRelease(body = "https://pan.quark.cn/s/abc123", assets = listOf(full, lite))
        assertEquals(AndroidReleaseDownloadInfo(lite.browserDownloadUrl, lite.cnDownloadUrl), release.extractAndroidDownloadInfo(true))
        assertEquals(AndroidReleaseDownloadInfo(full.browserDownloadUrl, full.cnDownloadUrl), release.extractAndroidDownloadInfo(false))
    }

    @Test
    fun decodesGithubReleasePayload() {
        val release = ZhihuJson.json.decodeFromString<GithubRelease>(
            """
            {
              "tag_name": "1.2.3",
              "body": "changes",
              "assets": [
                {
                  "name": "release-notes.txt",
                  "content_type": "text/plain",
                  "browser_download_url": "https://github.com/example/release-notes.txt",
                  "cn_download_url": "https://example.cn/release-notes.txt"
                }
              ]
            }
            """.trimIndent(),
        )

        assertEquals("1.2.3", release.tagName)
        assertEquals("changes", release.body)
        assertEquals("release-notes.txt", release.assets.single().name)
        assertEquals("text/plain", release.assets.single().contentType)
        assertEquals("https://github.com/example/release-notes.txt", release.assets.single().browserDownloadUrl)
        assertEquals("https://example.cn/release-notes.txt", release.assets.single().cnDownloadUrl)
    }

    @Test
    fun extractsReleaseNotesBetweenGithubMarkers() {
        val notes = extractGithubReleaseNotes(
            """
            # Release

            ## What's Changed
            * Added shared updater logic
            * Fixed something

            **Full Changelog**: https://github.com/example/compare
            """.trimIndent(),
        )

        assertEquals("* Added shared updater logic\n* Fixed something", notes)
    }
}
