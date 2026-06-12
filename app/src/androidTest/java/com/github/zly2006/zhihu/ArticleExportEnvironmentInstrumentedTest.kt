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

package com.github.zly2006.zhihu

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.viewmodel.SharedAndroidPaginationEnvironment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class ArticleExportEnvironmentInstrumentedTest {
    @Test
    fun androidEnvironmentProvidesArticleImageExportPlatformCapabilities() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val environment = SharedAndroidPaginationEnvironment(context, allowGuestAccess = true)

        assertNotNull(environment.articleImageExportRenderer { "" })

        val html = environment.buildArticleExportHtml(
            content = sampleArticleContent(),
            includeAppAttribution = true,
            extraSectionsHtml = "",
        )
        assertTrue(html.contains("导出环境回归"))

        val bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)
        val displayName = "zhihu-export-regression-${System.currentTimeMillis()}.jpg"
        try {
            environment.saveImageToMediaStore(displayName, bitmap)
            assertTrue(context.savedImageExists(displayName))
        } finally {
            bitmap.recycle()
            context.deleteSavedImage(displayName)
        }

        val htmlDisplayName = "zhihu-html-export-regression-${System.currentTimeMillis()}.html"
        try {
            val savedLocation = environment.saveHtmlToDownloads(htmlDisplayName, "<html><body>正文</body></html>")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                assertEquals("Zhihu++/$htmlDisplayName", savedLocation)
                assertTrue(context.savedDownloadExists(htmlDisplayName))
            } else {
                assertEquals(
                    File(
                        File(
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                            "Zhihu++",
                        ),
                        htmlDisplayName,
                    ).absolutePath,
                    savedLocation,
                )
                assertTrue(File(savedLocation).exists())
            }
        } finally {
            context.deleteSavedDownload(htmlDisplayName)
        }
    }

    private fun Context.savedImageExists(displayName: String): Boolean =
        contentResolver.querySavedImage(displayName) { count > 0 } == true

    private fun Context.deleteSavedImage(displayName: String) {
        contentResolver.delete(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            "${MediaStore.MediaColumns.DISPLAY_NAME}=?",
            arrayOf(displayName),
        )
    }

    private fun Context.savedDownloadExists(displayName: String): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentResolver.querySavedDownload(displayName) { count > 0 } == true
        } else {
            File(
                File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "Zhihu++",
                ),
                displayName,
            ).exists()
        }

    private fun Context.deleteSavedDownload(displayName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentResolver.delete(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                "${MediaStore.MediaColumns.DISPLAY_NAME}=?",
                arrayOf(displayName),
            )
        } else {
            File(
                File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "Zhihu++",
                ),
                displayName,
            ).delete()
        }
    }

    private fun <T> ContentResolver.querySavedImage(
        displayName: String,
        block: android.database.Cursor.() -> T,
    ): T? = query(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        arrayOf(MediaStore.MediaColumns._ID),
        "${MediaStore.MediaColumns.DISPLAY_NAME}=?",
        arrayOf(displayName),
        null,
    )?.use(block)

    private fun <T> ContentResolver.querySavedDownload(
        displayName: String,
        block: android.database.Cursor.() -> T,
    ): T? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        query(
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.MediaColumns._ID),
            "${MediaStore.MediaColumns.DISPLAY_NAME}=?",
            arrayOf(displayName),
            null,
        )?.use(block)
    } else {
        null
    }

    private fun sampleArticleContent(): DataHolder.Article = DataHolder.Article(
        id = 1001L,
        author = DataHolder.Author(
            avatarUrl = "",
            gender = 0,
            headline = "导出测试作者简介",
            id = "export-author",
            isAdvertiser = false,
            isOrg = false,
            name = "导出测试作者",
            type = "people",
            url = "https://www.zhihu.com/people/export-author",
            urlToken = "export-author",
            userType = "people",
        ),
        canComment = DataHolder.CanComment(status = true, reason = ""),
        title = "导出环境回归",
        content = "<p>正文</p>",
        excerpt = "正文",
        type = "article",
        created = 1_710_000_000L,
        updated = 1_710_000_600L,
        url = "https://zhuanlan.zhihu.com/p/1001",
        voteupCount = 12,
    )
}
