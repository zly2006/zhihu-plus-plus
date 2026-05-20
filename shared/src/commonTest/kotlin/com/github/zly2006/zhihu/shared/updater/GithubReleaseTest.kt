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
                  "name": "app.apk",
                  "content_type": "application/vnd.android.package-archive",
                  "browser_download_url": "https://github.com/example/app.apk",
                  "cn_download_url": "https://example.cn/app.apk"
                }
              ]
            }
            """.trimIndent(),
        )

        assertEquals("1.2.3", release.tagName)
        assertEquals("changes", release.body)
        assertEquals("app.apk", release.assets.single().name)
        assertEquals("application/vnd.android.package-archive", release.assets.single().contentType)
        assertEquals("https://github.com/example/app.apk", release.assets.single().browserDownloadUrl)
        assertEquals("https://example.cn/app.apk", release.assets.single().cnDownloadUrl)
    }
}
