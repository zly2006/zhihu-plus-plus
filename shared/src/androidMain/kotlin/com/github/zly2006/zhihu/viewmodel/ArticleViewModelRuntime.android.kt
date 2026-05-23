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

package com.github.zly2006.zhihu.viewmodel

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.getContentDetail
import com.github.zly2006.zhihu.navigation.AndroidAnswerNavigatorRepository
import com.github.zly2006.zhihu.navigation.AnswerNavigatorRepository
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.shared.article.VoteUpState
import com.github.zly2006.zhihu.shared.comment.rootCommentUrl
import com.github.zly2006.zhihu.shared.data.CollectionResponse
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.util.Log
import com.github.zly2006.zhihu.ui.articleHost
import com.github.zly2006.zhihu.util.clipboardManager
import com.github.zly2006.zhihu.util.signFetchRequest
import com.github.zly2006.zhihu.viewmodel.filter.ContentOpenEventSupport
import com.github.zly2006.zhihu.viewmodel.filter.ContentOpenFrom
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.put
import java.io.File

class AndroidArticleViewModelRuntime(
    private val context: Context,
) : ArticleViewModelRuntime {
    override suspend fun getContentDetail(article: Article): DataHolder.Content? =
        DataHolder.getContentDetail(context, article)

    override suspend fun recordOpenEvent(
        destination: Article,
        questionId: Long?,
    ) {
        ContentOpenEventSupport.recordOpenEvent(
            context = context,
            destination = destination,
            questionId = questionId,
            openFrom = context.articleHost()?.consumePendingContentOpenFrom(destination)
                ?: ContentOpenFrom.UNKNOWN,
        )
    }

    override fun answerNavigatorRepository(): AnswerNavigatorRepository =
        AndroidAnswerNavigatorRepository(context)

    override suspend fun loadCollections(
        contentType: String,
        articleId: Long,
    ): CollectionResponse {
        val collectionsUrl = "https://api.zhihu.com/collections/contents/$contentType/$articleId?limit=50"
        val json = AccountData.fetchGet(context, collectionsUrl) {
            signFetchRequest()
        }!!
        return AccountData.decodeJson(json)
    }

    override suspend fun createNewCollection(
        title: String,
        description: String,
        isPublic: Boolean,
    ) {
        AccountData.fetchPost(context, "https://www.zhihu.com/api/v4/collections") {
            contentType(ContentType.Application.Json)
            setBody(
                buildJsonObject {
                    put("title", title)
                    put("description", description)
                    put("is_public", isPublic)
                },
            )
            signFetchRequest()
        }
    }

    override suspend fun voteArticle(
        article: Article,
        newState: VoteUpState,
    ): JsonObject {
        val endpoint = when (article.type) {
            ArticleType.Answer -> "https://www.zhihu.com/api/v4/answers/${article.id}/voters"
            ArticleType.Article -> "https://www.zhihu.com/api/v4/articles/${article.id}/voters"
        }
        return AccountData.fetchPost(context, endpoint) {
            when (article.type) {
                ArticleType.Answer -> setBody(mapOf("type" to newState.key))
                ArticleType.Article -> setBody(mapOf("voting" to if (newState == VoteUpState.Up) 1 else 0))
            }
            contentType(ContentType.Application.Json)
        }!!
    }

    override suspend fun fetchExportComments(
        article: Article,
        requestedCount: Int,
    ): List<DataHolder.Comment> {
        val safeRequestedCount = requestedCount.coerceAtLeast(0)
        if (safeRequestedCount == 0) return emptyList()

        val json = AccountData.fetchGet(context, article.rootCommentUrl) {
            url {
                parameters["order"] = "score"
                parameters["limit"] = safeRequestedCount.coerceAtMost(20).toString()
                parameters["include"] = "data[*].content,excerpt,headline"
            }
            signFetchRequest()
        } ?: return emptyList()

        return json["data"]
            ?.jsonArray
            ?.mapNotNull { element ->
                runCatching {
                    AccountData.decodeJson<DataHolder.Comment>(element)
                }.getOrNull()
            }?.take(safeRequestedCount)
            .orEmpty()
    }

    override fun configureSignedRequest(builder: HttpRequestBuilder) {
        builder.signFetchRequest()
    }

    override fun accountHttpClient(): HttpClient =
        AccountData.httpClient(context)

    override fun loadExportAssetText(fileName: String): String =
        context.assets.open(fileName).use { inputStream ->
            inputStream.bufferedReader().use { reader ->
                reader.readText()
            }
        }

    override fun saveHtmlToDownloads(
        displayName: String,
        htmlContent: String,
    ): String = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        saveHtmlToDownloadsWithMediaStore(displayName, htmlContent)
    } else {
        saveHtmlToLegacyDownloads(displayName, htmlContent)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveHtmlToDownloadsWithMediaStore(
        displayName: String,
        htmlContent: String,
    ): String {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, "text/html")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/Zhihu++")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }

        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            ?: throw IllegalStateException("无法创建下载文件")

        return try {
            resolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { writer ->
                writer.write(htmlContent)
            } ?: throw IllegalStateException("无法打开下载文件")

            contentValues.clear()
            contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, contentValues, null, null)
            "Zhihu++/$displayName"
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            throw e
        }
    }

    @Suppress("DEPRECATION")
    private fun saveHtmlToLegacyDownloads(
        displayName: String,
        htmlContent: String,
    ): String {
        val downloadsDir = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "Zhihu++",
        )
        if (!downloadsDir.exists() && !downloadsDir.mkdirs()) {
            throw IllegalStateException("无法创建下载目录")
        }

        val file = File(downloadsDir, displayName)
        file.writeText(htmlContent)
        return file.absolutePath
    }

    override fun saveImageToMediaStore(
        displayName: String,
        bitmap: Any,
    ) {
        val contentResolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Zhihu++")
        }

        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let { imageUri ->
            try {
                contentResolver.openOutputStream(imageUri)?.use { outputStream ->
                    (bitmap as Bitmap).compress(Bitmap.CompressFormat.PNG, 90, outputStream)
                }
            } catch (e: Exception) {
                Log.e("ArticleViewModel", "Failed to save image to MediaStore", e)
                throw e
            }
        } ?: throw Exception("Failed to create MediaStore entry")
    }

    override fun xsrfToken(): String = AccountData.data.cookies["_xsrf"] ?: ""

    override fun hasImageExportPermission(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
    } else {
        ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    override fun requestImageExportPermission() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
            )
        }
        ActivityCompat.requestPermissions(context as Activity, permissions, 1001)
    }

    override fun copyArticleMarkdownToClipboard(markdown: String) {
        val clip = ClipData.newPlainText("Zhihu Article", markdown)
        context.clipboardManager.setPrimaryClip(clip)
    }

    override fun showMessage(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    override fun showLongMessage(message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
}
