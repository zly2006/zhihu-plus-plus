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
import com.github.zly2006.zhihu.viewmodel.AndroidPreparedExportWebView
import com.github.zly2006.zhihu.viewmodel.SharedAndroidPaginationEnvironment
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.roundToInt

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
                assertTrue(context.savedDownloadSize(htmlDisplayName) > 0L)
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
                assertTrue(File(savedLocation).length() > 0L)
            }
        } finally {
            context.deleteSavedDownload(htmlDisplayName)
        }
    }

    @Test
    fun androidEnvironmentBuildsOfflineArticleExportHtml() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val environment = SharedAndroidPaginationEnvironment(context, allowGuestAccess = true)

        val html = environment.buildOfflineArticleExportHtml(
            content = sampleArticleContent(),
            includeAppAttribution = true,
            httpClient = environment.httpClient(),
        )

        assertTrue(html.contains("导出环境回归"))
        assertTrue(html.length > 1_000)
    }

    @Test
    fun articleImageRendererUsesStableContentHeightBeforeFullLayout() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val environment = SharedAndroidPaginationEnvironment(context, allowGuestAccess = true)
        val renderer = environment.articleImageExportRenderer { fileName ->
            context.assets
                .open(fileName)
                .bufferedReader()
                .use { it.readText() }
        }
        val prepared = renderer.prepareExportWebView(
            htmlContent = environment.buildArticleExportHtml(
                content = sampleLongAnswerContent(),
                includeAppAttribution = true,
                extraSectionsHtml = "",
            ),
            timeoutMs = 15_000,
        ) as AndroidPreparedExportWebView
        val bitmap = renderer.captureExportBitmap(prepared) as Bitmap
        try {
            assertTrue(prepared.contentHeightPx < 60_000)
            assertEquals(prepared.expectedBitmapWidthPx(context), bitmap.width)
            assertEquals(prepared.expectedBitmapHeightPx(context), bitmap.height)
            assertTrue(bitmap.hasVisibleContentPixels())
            ByteArrayOutputStream().use { output ->
                assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG, 70, output))
                assertTrue(output.size() > 0)
            }
        } finally {
            bitmap.recycle()
            renderer.destroyExportWebView(prepared)
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

    private fun Context.savedDownloadSize(displayName: String): Long =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentResolver.querySavedDownloadSize(displayName) ?: -1L
        } else {
            File(
                File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "Zhihu++",
                ),
                displayName,
            ).length()
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

    private fun ContentResolver.querySavedDownloadSize(displayName: String): Long? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            query(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.MediaColumns.SIZE),
                "${MediaStore.MediaColumns.DISPLAY_NAME}=?",
                arrayOf(displayName),
                null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getLong(0) else null
            }
        } else {
            null
        }

    private fun AndroidPreparedExportWebView.expectedBitmapWidthPx(context: Context): Int =
        (viewportWidthPx * context.exportScale()).roundToInt().coerceAtLeast(1)

    private fun AndroidPreparedExportWebView.expectedBitmapHeightPx(context: Context): Int =
        (contentHeightPx * context.exportScale()).roundToInt().coerceAtLeast(1)

    private fun Context.exportScale(): Float =
        ARTICLE_EXPORT_DPI_FOR_TEST / resources.displayMetrics.densityDpi
            .coerceAtLeast(1)
            .toFloat()

    private fun Bitmap.hasVisibleContentPixels(): Boolean {
        val row = IntArray(width)
        var visiblePixelCount = 0
        val requiredVisiblePixels = 100
        for (y in 0 until height) {
            getPixels(row, 0, width, 0, y, width, 1)
            for (pixel in row) {
                val alpha = pixel ushr 24
                val red = pixel shr 16 and 0xff
                val green = pixel shr 8 and 0xff
                val blue = pixel and 0xff
                if (alpha > 0 && (red < 245 || green < 245 || blue < 245)) {
                    visiblePixelCount++
                    if (visiblePixelCount >= requiredVisiblePixels) {
                        return true
                    }
                }
            }
        }
        return false
    }

    private companion object {
        const val ARTICLE_EXPORT_DPI_FOR_TEST = 200f
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

    private fun sampleLongAnswerContent(): DataHolder.Answer = DataHolder.Answer(
        id = 3309625617L,
        answerType = "normal",
        author = DataHolder.Author(
            avatarUrl = "",
            gender = 0,
            headline = "导出测试答主简介",
            id = "answer-export-author",
            isAdvertiser = false,
            isOrg = false,
            name = "导出测试答主",
            type = "people",
            url = "https://www.zhihu.com/people/answer-export-author",
            urlToken = "answer-export-author",
            userType = "people",
        ),
        canComment = DataHolder.CanComment(status = true, reason = ""),
        commentCount = 4175,
        content = (1..24).joinToString("") { index ->
            "<p data-pid=\"p$index\">第 $index 段用于验证 WebView 导出高度稳定性的正文。</p>"
        },
        excerpt = "导出测试回答摘要",
        type = "answer",
        createdTime = 1_710_000_000L,
        updatedTime = 1_710_000_600L,
        url = "https://www.zhihu.com/question/631983014/answer/3309625617",
        thanksCount = 0,
        voteupCount = 12,
        question = DataHolder.AnswerModelQuestion(
            created = 1_710_000_000L,
            id = 631983014L,
            questionType = "normal",
            title = "导出高度稳定性测试问题",
            type = "question",
            updatedTime = 1_710_000_600L,
            url = "https://www.zhihu.com/question/631983014",
        ),
    )
}
