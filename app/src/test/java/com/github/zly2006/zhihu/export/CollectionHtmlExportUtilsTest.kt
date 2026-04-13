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

package com.github.zly2006.zhihu.export

import com.github.zly2006.zhihu.util.CollectionHtmlZipExportProgress
import com.github.zly2006.zhihu.util.ResolvedCollectionHtmlExportItem
import com.github.zly2006.zhihu.util.buildCollectionExportZipFileName
import com.github.zly2006.zhihu.util.exportCollectionItemsToZip
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.util.zip.ZipFile

class CollectionHtmlExportUtilsTest {
    @Test
    fun exportCollectionItemsToZipStagesHtmlFilesAndSkipsUnsupportedItems() = runBlocking {
        val cacheDir = Files.createTempDirectory("collection-export-cache").toFile()
        val outputDir = Files.createTempDirectory("collection-export-output").toFile()

        try {
            val progressUpdates = mutableListOf<CollectionHtmlZipExportProgress>()
            val result = exportCollectionItemsToZip(
                collectionTitle = "我的收藏夹",
                items = listOf(
                    FakeExportItem(
                        title = "已导出回答",
                        resolved = ResolvedCollectionHtmlExportItem(
                            htmlFileName = "zhihu++_answer_1.html",
                            htmlContent = "<html><body>answer</body></html>",
                        ),
                    ),
                    FakeExportItem(
                        title = "跳过视频",
                        resolved = null,
                    ),
                    FakeExportItem(
                        title = "已导出文章",
                        resolved = ResolvedCollectionHtmlExportItem(
                            htmlFileName = "zhihu++_article_2.html",
                            htmlContent = "<html><body>article</body></html>",
                        ),
                    ),
                ),
                cacheDir = cacheDir,
                outputDir = outputDir,
                timestampMillis = 1_774_519_200_000,
                displayTitle = { it.title },
                resolveItem = { it.resolved },
                onProgress = { progressUpdates += it },
            )

            assertEquals(3, result.totalCount)
            assertEquals(2, result.successCount)
            assertEquals(1, result.skippedCount)
            assertEquals(0, result.failedCount)
            assertEquals(
                buildCollectionExportZipFileName(
                    collectionTitle = "我的收藏夹",
                    timestampMillis = 1_774_519_200_000,
                ),
                result.zipFile?.name,
            )
            assertTrue(result.stagingDir.exists())
            assertTrue(result.stagingDir.resolve("zhihu++_answer_1.html").exists())
            assertTrue(result.stagingDir.resolve("zhihu++_article_2.html").exists())
            assertNotNull(result.zipFile)

            ZipFile(result.zipFile!!).use { zipFile ->
                assertNotNull(zipFile.getEntry("zhihu++_answer_1.html"))
                assertNotNull(zipFile.getEntry("zhihu++_article_2.html"))
            }

            assertTrue(progressUpdates.isNotEmpty())
            assertEquals(3, progressUpdates.last().processedCount)
            assertEquals(2, progressUpdates.last().successCount)
            assertEquals(1, progressUpdates.last().skippedCount)
            assertEquals(0, progressUpdates.last().failedCount)
            assertEquals("已导出文章", progressUpdates.last().currentTitle)
        } finally {
            cacheDir.deleteRecursively()
            outputDir.deleteRecursively()
        }
    }

    @Test
    fun exportCollectionItemsToZipSkipsZipCreationWhenNothingIsExported() = runBlocking {
        val cacheDir = Files.createTempDirectory("collection-export-cache-empty").toFile()
        val outputDir = Files.createTempDirectory("collection-export-output-empty").toFile()

        try {
            val result = exportCollectionItemsToZip(
                collectionTitle = "全是视频",
                items = listOf(
                    FakeExportItem(title = "视频 1", resolved = null),
                    FakeExportItem(title = "问题 2", resolved = null),
                ),
                cacheDir = cacheDir,
                outputDir = outputDir,
                timestampMillis = 1_774_519_200_000,
                displayTitle = { it.title },
                resolveItem = { it.resolved },
            )

            assertEquals(2, result.totalCount)
            assertEquals(0, result.successCount)
            assertEquals(2, result.skippedCount)
            assertEquals(0, result.failedCount)
            assertNull(result.zipFile)
        } finally {
            cacheDir.deleteRecursively()
            outputDir.deleteRecursively()
        }
    }

    private data class FakeExportItem(
        val title: String,
        val resolved: ResolvedCollectionHtmlExportItem?,
    )
}
