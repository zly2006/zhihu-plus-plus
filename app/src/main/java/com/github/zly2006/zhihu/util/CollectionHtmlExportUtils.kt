package com.github.zly2006.zhihu.util

import kotlinx.coroutines.CancellationException
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

data class ResolvedCollectionHtmlExportItem(
    val htmlFileName: String,
    val htmlContent: String,
)

data class CollectionHtmlZipExportProgress(
    val totalCount: Int,
    val processedCount: Int,
    val successCount: Int,
    val skippedCount: Int,
    val failedCount: Int,
    val currentTitle: String = "",
)

data class CollectionHtmlZipExportResult(
    val totalCount: Int,
    val successCount: Int,
    val skippedCount: Int,
    val failedCount: Int,
    val stagingDir: File,
    val zipFile: File?,
)

fun buildCollectionExportZipFileName(
    collectionTitle: String,
    timestampMillis: Long = System.currentTimeMillis(),
): String {
    val safeTitle = sanitizeArticleExportFileNamePart(collectionTitle).ifBlank { "收藏夹" }
    val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date(timestampMillis))
    return "zhihu++_${safeTitle}_$timestamp.zip"
}

suspend fun <T> exportCollectionItemsToZip(
    collectionTitle: String,
    items: List<T>,
    cacheDir: File,
    outputDir: File,
    timestampMillis: Long = System.currentTimeMillis(),
    displayTitle: (T) -> String,
    resolveItem: suspend (T) -> ResolvedCollectionHtmlExportItem?,
    onProgress: suspend (CollectionHtmlZipExportProgress) -> Unit = {},
): CollectionHtmlZipExportResult {
    val stagingDir = File(
        cacheDir,
        "collection_html_export_${sanitizeArticleExportFileNamePart(collectionTitle).ifBlank { "collection" }}_$timestampMillis",
    )
    if (stagingDir.exists()) {
        stagingDir.deleteRecursively()
    }
    if (!stagingDir.mkdirs()) {
        throw IllegalStateException("无法创建导出缓存目录")
    }

    val totalCount = items.size
    var processedCount = 0
    var successCount = 0
    var skippedCount = 0
    var failedCount = 0
    var currentTitle = ""

    suspend fun emitProgress() {
        onProgress(
            CollectionHtmlZipExportProgress(
                totalCount = totalCount,
                processedCount = processedCount,
                successCount = successCount,
                skippedCount = skippedCount,
                failedCount = failedCount,
                currentTitle = currentTitle,
            ),
        )
    }

    emitProgress()

    items.forEach { item ->
        currentTitle = displayTitle(item)
        try {
            val resolvedItem = resolveItem(item)
            if (resolvedItem == null) {
                skippedCount++
            } else {
                val htmlFile = File(stagingDir, resolvedItem.htmlFileName)
                htmlFile.parentFile?.mkdirs()
                htmlFile.writeText(resolvedItem.htmlContent)
                successCount++
            }
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            failedCount++
        } finally {
            processedCount++
            emitProgress()
        }
    }

    val zipFile = if (successCount > 0) {
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw IllegalStateException("无法创建导出 ZIP 目录")
        }

        File(outputDir, buildCollectionExportZipFileName(collectionTitle, timestampMillis)).also { file ->
            if (file.exists()) {
                file.delete()
            }
            zipDirectoryContents(stagingDir, file)
        }
    } else {
        null
    }

    return CollectionHtmlZipExportResult(
        totalCount = totalCount,
        successCount = successCount,
        skippedCount = skippedCount,
        failedCount = failedCount,
        stagingDir = stagingDir,
        zipFile = zipFile,
    )
}

private fun zipDirectoryContents(
    sourceDir: File,
    zipFile: File,
) {
    ZipOutputStream(FileOutputStream(zipFile).buffered()).use { outputStream ->
        sourceDir
            .listFiles()
            ?.sortedBy { it.name }
            ?.forEach { file ->
                addFileToZip(
                    file = file,
                    entryPrefix = "",
                    outputStream = outputStream,
                )
            }
    }
}

private fun addFileToZip(
    file: File,
    entryPrefix: String,
    outputStream: ZipOutputStream,
) {
    if (file.isDirectory) {
        val nextPrefix = if (entryPrefix.isBlank()) file.name else "$entryPrefix/${file.name}"
        file
            .listFiles()
            ?.sortedBy { it.name }
            ?.forEach { child ->
                addFileToZip(
                    file = child,
                    entryPrefix = nextPrefix,
                    outputStream = outputStream,
                )
            }
        return
    }

    val entryName = if (entryPrefix.isBlank()) file.name else "$entryPrefix/${file.name}"
    outputStream.putNextEntry(ZipEntry(entryName))
    file.inputStream().buffered().use { inputStream ->
        inputStream.copyTo(outputStream)
    }
    outputStream.closeEntry()
}
