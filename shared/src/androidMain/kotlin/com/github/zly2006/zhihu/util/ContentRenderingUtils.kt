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

package com.github.zly2006.zhihu.util

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.provider.MediaStore.MediaColumns
import androidx.compose.foundation.Image
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.unit.em
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.platform.androidUserMessageSink
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.readRawBytes
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream

fun buildArticleExportHtml(
    context: Context,
    exportData: ArticleExportData,
    extraSectionsHtml: String = "",
): String = buildArticleExportHtml(
    loadAssetText = { fileName -> loadArticleExportAssetText(context, fileName) },
    exportData = exportData,
    extraSectionsHtml = extraSectionsHtml,
)

fun buildArticleExportHtml(
    context: Context,
    content: DataHolder.Content,
    includeAppAttribution: Boolean,
    extraSectionsHtml: String = "",
): String = buildArticleExportHtml(
    loadAssetText = { fileName -> loadArticleExportAssetText(context, fileName) },
    content = content,
    includeAppAttribution = includeAppAttribution,
    extraSectionsHtml = extraSectionsHtml,
)

suspend fun buildOfflineArticleExportHtml(
    context: Context,
    exportData: ArticleExportData,
    httpClient: HttpClient,
    includeImages: Boolean = true,
    extraSectionsHtml: String = "",
): String = buildOfflineArticleExportHtml(
    loadAssetText = { fileName -> loadArticleExportAssetText(context, fileName) },
    exportData = exportData,
    httpClient = httpClient,
    includeImages = includeImages,
    extraSectionsHtml = extraSectionsHtml,
)

suspend fun buildOfflineArticleExportHtml(
    context: Context,
    content: DataHolder.Content,
    includeAppAttribution: Boolean,
    httpClient: HttpClient,
    includeImages: Boolean = true,
    extraSectionsHtml: String = "",
): String = buildOfflineArticleExportHtml(
    loadAssetText = { fileName -> loadArticleExportAssetText(context, fileName) },
    content = content,
    includeAppAttribution = includeAppAttribution,
    httpClient = httpClient,
    includeImages = includeImages,
    extraSectionsHtml = extraSectionsHtml,
)

private fun loadArticleExportAssetText(
    context: Context,
    fileName: String,
): String = context.assets.open(fileName).use { inputStream ->
    inputStream.bufferedReader().use { reader ->
        reader.readText()
    }
}

fun saveBitmapToGallery(
    context: Context,
    displayName: String,
    bitmap: Bitmap,
) {
    saveImageToMediaStore(
        context = context,
        displayName = displayName,
        mimeType = "image/jpeg",
        relativePath = Environment.DIRECTORY_PICTURES + "/Zhihu++",
    ) { outputStream ->
        if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)) {
            throw IllegalStateException("Failed to encode image")
        }
    }
}

suspend fun shareBitmap(
    context: Context,
    displayName: String,
    bitmap: Bitmap,
) {
    val file = withContext(Dispatchers.IO) {
        java.io.File(shareImageCacheDir(context), displayName.replace('/', '_').replace('\\', '_')).also { targetFile ->
            targetFile.outputStream().use { outputStream ->
                if (!bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)) {
                    throw IllegalStateException("Failed to encode image")
                }
            }
        }
    }
    withContext(Dispatchers.Main) {
        shareImageFile(context, file, displayName)
    }
}

private fun saveDownloadedImageToGallery(
    context: Context,
    imageUrl: String,
    contentTypeHeader: String?,
    displayName: String,
    bytes: ByteArray,
) {
    saveImageToMediaStore(
        context = context,
        displayName = displayName,
        mimeType = resolveArticleExportImageMimeType(
            contentTypeHeader = contentTypeHeader,
            imageUrl = imageUrl,
            imageBytes = bytes,
        ),
        relativePath = Environment.DIRECTORY_PICTURES,
    ) { outputStream ->
        outputStream.write(bytes)
    }
}

private fun saveImageToMediaStore(
    context: Context,
    displayName: String,
    mimeType: String,
    relativePath: String,
    writeImage: (OutputStream) -> Unit,
) {
    val contentValues = ContentValues().apply {
        put(MediaColumns.DISPLAY_NAME, displayName)
        put(MediaColumns.MIME_TYPE, mimeType)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            put(MediaColumns.RELATIVE_PATH, relativePath)
            put(MediaColumns.IS_PENDING, 1)
        }
    }

    val resolver = context.contentResolver
    val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
    } else {
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    }
    val imageUri = resolver.insert(collection, contentValues)
        ?: throw IllegalStateException("Failed to create MediaStore entry")

    try {
        resolver.openOutputStream(imageUri)?.use(writeImage)
            ?: throw IllegalStateException("Failed to open MediaStore output stream")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.clear()
            contentValues.put(MediaColumns.IS_PENDING, 0)
            resolver.update(imageUri, contentValues, null, null)
        }
    } catch (e: Exception) {
        resolver.delete(imageUri, null, null)
        throw e
    }
}

fun createEmojiInlineContent(emojiKeys: Set<String>): Map<String, InlineTextContent> {
    return emojiKeys
        .filter { it.startsWith("emoji_") }
        .mapNotNull { emojiKey ->
            val fileName = emojiKey.removePrefix("emoji_")
            val path = EmojiManager.getEmojiPathByFileName(fileName) ?: return@mapNotNull null

            emojiKey to InlineTextContent(
                placeholder = Placeholder(
                    width = 1.3.em,
                    height = 1.3.em,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
                ),
            ) {
                val bitmap = android.graphics.BitmapFactory.decodeFile(path)
                if (bitmap != null) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = emojiKey,
                        modifier = Modifier,
                    )
                }
            }
        }.toMap()
}

/**
 * 保存图片到相册
 */
suspend fun saveImageToGallery(
    context: Context,
    httpClient: HttpClient,
    imageUrl: String,
) {
    val userMessages = androidUserMessageSink(context)
    try {
        val response = httpClient.get(imageUrl)
        val bytes = response.readRawBytes()
        val fileName = imageUrl.toUri().lastPathSegment ?: "downloaded_image.jpg"
        saveDownloadedImageToGallery(
            context = context,
            imageUrl = imageUrl,
            contentTypeHeader = response.headers[HttpHeaders.ContentType],
            displayName = fileName,
            bytes = bytes,
        )
        userMessages.showShortMessage("图片已保存到相册")
    } catch (e: Exception) {
        userMessages.showShortMessage("保存失败: ${e.message}")
    }
}

/**
 * 分享图片
 * 图片临时保存到 externalCacheDir/share_images/，应用启动时自动清空
 */
suspend fun shareImage(
    context: Context,
    httpClient: HttpClient,
    imageUrl: String,
) {
    val userMessages = androidUserMessageSink(context)
    try {
        val response = httpClient.get(imageUrl)
        val bytes = response.readRawBytes()
        val file = java.io.File(shareImageCacheDir(context), "share_${System.currentTimeMillis()}.jpg")
        file.writeBytes(bytes)
        shareImageFile(context, file, file.name)
    } catch (e: Exception) {
        userMessages.showShortMessage("分享失败: ${e.message}")
    }
}

private fun shareImageCacheDir(context: Context): java.io.File =
    java.io.File(context.externalCacheDir ?: context.cacheDir, "share_images").apply {
        if (!exists() && !mkdirs()) {
            throw IllegalStateException("无法创建分享缓存目录")
        }
    }

private fun shareImageFile(
    context: Context,
    file: java.io.File,
    title: String,
) {
    val imageUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    val shareIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_STREAM, imageUri)
        putExtra(Intent.EXTRA_TITLE, title)
        clipData = android.content.ClipData.newUri(context.contentResolver, title, imageUri)
        type = "image/jpeg"
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    val chooserIntent = Intent.createChooser(shareIntent, "分享图片")
    if (context !is android.app.Activity) {
        chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(chooserIntent)
}

/**
 * 清空分享图片缓存目录
 */
fun clearShareImageCache(context: Context) {
    java.io.File(context.externalCacheDir ?: context.cacheDir, "share_images").deleteRecursively()
}
