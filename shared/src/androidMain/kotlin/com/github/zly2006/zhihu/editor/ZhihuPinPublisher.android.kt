/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
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

package com.github.zly2006.zhihu.editor

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.asApiEnvironment
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.data.ZhihuJson
import com.github.zly2006.zhihu.shared.util.raiseForStatus
import com.github.zly2006.zhihu.viewmodel.postSigned
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonElement
import java.util.UUID

@Composable
actual fun rememberZhihuPinPublisher(): ZhihuPinPublisher {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        AndroidZhihuPinPublisher(context)
    }
}

private class AndroidZhihuPinPublisher(
    private val context: Context,
) : ZhihuPinPublisher {
    override val isSupported: Boolean = true

    override suspend fun uploadImage(
        bytes: ByteArray,
        mimeType: String?,
        fileName: String?,
    ): UploadedZhihuImage =
        uploadZhihuImage(context.asApiEnvironment(), bytes, mimeType, fileName, ZhihuImageUploadSource.Pin)

    override suspend fun savePinDraft(
        title: String,
        html: String,
        textLength: Int,
        images: List<UploadedZhihuImage>,
    ) {
        val xsrf = AccountData.data.cookies["_xsrf"]
            ?: throw IllegalStateException("缺少 _xsrf Cookie，无法保存想法草稿；请先确保已登录。")

        context
            .asApiEnvironment()
            .postSigned("https://api.zhihu.com/content/drafts") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Referrer, "https://www.zhihu.com/")
                header("x-xsrftoken", xsrf)
                setBody(
                    SavePinDraftRequest(
                        data = buildPinContentPayload(
                            title = title,
                            html = html,
                            textLength = textLength,
                            images = images,
                        ),
                    ),
                )
            }.raiseForStatus(dumpRequest = true)
    }

    override suspend fun publishPin(
        title: String,
        html: String,
        textLength: Int,
        images: List<UploadedZhihuImage>,
    ): Long {
        val xsrf = AccountData.data.cookies["_xsrf"]
            ?: throw IllegalStateException("缺少 _xsrf Cookie，无法发布想法；请先确保已登录。")

        val responseElement = context
            .asApiEnvironment()
            .postSigned("https://www.zhihu.com/api/v4/content/publish") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Referrer, "https://www.zhihu.com/")
                header("x-xsrftoken", xsrf)
                setBody(
                    PublishPinRequest(
                        data = buildPinContentPayload(
                            title = title,
                            html = html,
                            textLength = textLength,
                            images = images,
                        ),
                    ),
                )
            }.raiseForStatus(dumpRequest = true)
            .body<JsonElement>()

        val response = ZhihuJson.decodeJson(DataHolder.ContentPublishResponse.serializer(), responseElement)
        if (response.message == "success") {
            val resultText = response.data?.result
                ?: throw IllegalStateException("发布成功但返回缺少 data.result: $responseElement")

            return parsePublishContentId(resultText)
                ?: throw IllegalStateException("发布成功但无法解析 publish.id")
        }

        throw IllegalStateException(
            "发布失败: ${response.message ?: "unknown"}\n$responseElement",
        )
    }

    private fun buildPinContentPayload(
        title: String,
        html: String,
        textLength: Int,
        images: List<UploadedZhihuImage>,
    ): PinContentPayload =
        PinContentPayload(
            publish = PublishTrace(traceId = "${System.currentTimeMillis()},${UUID.randomUUID()}"),
            title = title
                .takeIf { it.isNotBlank() }
                ?.let { PinContentTitle(title = it) },
            hybrid = html
                .takeIf { it.isNotBlank() }
                ?.let {
                    PinContentHybrid(
                        html = it,
                        textLength = textLength,
                    )
                },
            media = images
                .takeIf { it.isNotEmpty() }
                ?.let { uploadedImages ->
                    PinContentMedia(
                        medias = uploadedImages.map { image ->
                            PinContentMediaItem(
                                image = PinContentImage(
                                    height = image.rawHeight,
                                    width = image.rawWidth,
                                    url = image.url,
                                    originalUrl = image.originalUrl,
                                    watermark = image.watermarkMode
                                        ?: image.watermark?.let { if (it) "watermark" else "original" },
                                    watermarkUrl = image.watermarkUrl,
                                ),
                            )
                        },
                    )
                },
        )
}
