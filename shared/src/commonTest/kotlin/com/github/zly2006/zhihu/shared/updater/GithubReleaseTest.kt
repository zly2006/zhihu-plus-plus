package com.github.zly2006.zhihu.shared.updater

import com.github.zly2006.zhihu.shared.data.ZhihuJson
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

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

    @Test
    fun selectsLiteAndFullApkAssets() {
        val lite = GithubAsset(
            name = "app-lite-debug.apk",
            contentType = ANDROID_APK_CONTENT_TYPE,
            browserDownloadUrl = "https://github.com/example/lite.apk",
        )
        val full = GithubAsset(
            name = "app-full-debug.apk",
            contentType = ANDROID_APK_CONTENT_TYPE,
            browserDownloadUrl = "https://github.com/example/full.apk",
        )

        assertEquals(lite, selectGithubApkAsset(listOf(full, lite), isLiteVariant = true))
        assertEquals(full, selectGithubApkAsset(listOf(lite, full), isLiteVariant = false))
    }

    @Test
    fun extractsDownloadInfoFromApkAssets() {
        val release = GithubRelease(
            assets = listOf(
                GithubAsset(
                    name = "notes.txt",
                    contentType = "text/plain",
                    browserDownloadUrl = "https://github.com/example/notes.txt",
                ),
                GithubAsset(
                    name = "app-lite-debug.apk",
                    contentType = ANDROID_APK_CONTENT_TYPE,
                    browserDownloadUrl = "https://github.com/example/lite.apk",
                    cnDownloadUrl = "https://example.cn/lite.apk",
                ),
            ),
        )

        val downloadInfo = release.extractGithubDownloadInfo(isLiteVariant = true)

        assertEquals("https://github.com/example/lite.apk", downloadInfo.browserDownloadUrl)
        assertEquals("https://example.cn/lite.apk", downloadInfo.cnDownloadUrl)
    }

    @Test
    fun returnsNullWhenVariantSpecificApkIsMissing() {
        val asset = GithubAsset(
            name = "app-release.apk",
            contentType = ANDROID_APK_CONTENT_TYPE,
            browserDownloadUrl = "https://github.com/example/app.apk",
        )

        assertNull(selectGithubApkAsset(listOf(asset), isLiteVariant = true))
    }
}
