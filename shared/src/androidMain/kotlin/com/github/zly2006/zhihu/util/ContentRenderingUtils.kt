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

import android.content.Context
import android.content.Intent
import android.os.Build
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
import com.github.zly2006.zhihu.shared.platform.androidUserMessageSink
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.readRawBytes

/**
 * 创建emoji的inline content映射
 */
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

        val contentValues = android.content.ContentValues().apply {
            put(MediaColumns.DISPLAY_NAME, fileName)
            put(MediaColumns.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES)
                put(MediaColumns.IS_PENDING, 1)
            }
        }

        val resolver = context.contentResolver
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media
                .getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val imageUri = resolver.insert(collection, contentValues)
        if (imageUri != null) {
            resolver.openOutputStream(imageUri).use { os ->
                os?.write(bytes)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaColumns.IS_PENDING, 0)
                resolver.update(imageUri, contentValues, null, null)
            }

            userMessages.showShortMessage("图片已保存到相册")
        }
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
        val shareDir = java.io.File(context.externalCacheDir, "share_images").apply { mkdirs() }
        val file = java.io.File(shareDir, "share_${System.currentTimeMillis()}.jpg")
        file.writeBytes(bytes)
        val imageUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, imageUri)
            type = "image/jpeg"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "分享图片"))
    } catch (e: Exception) {
        userMessages.showShortMessage("分享失败: ${e.message}")
    }
}

/**
 * 清空分享图片缓存目录
 */
fun clearShareImageCache(context: Context) {
    java.io.File(context.externalCacheDir, "share_images").deleteRecursively()
}
