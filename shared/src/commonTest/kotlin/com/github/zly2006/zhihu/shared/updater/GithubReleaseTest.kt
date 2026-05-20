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
