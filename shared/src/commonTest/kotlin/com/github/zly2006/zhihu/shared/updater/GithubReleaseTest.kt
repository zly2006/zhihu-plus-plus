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

package com.github.zly2006.zhihu.shared.updater

import com.github.zly2006.zhihu.shared.data.ZhihuJson
import kotlin.test.Test
import kotlin.test.assertEquals

class GithubReleaseTest {
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
