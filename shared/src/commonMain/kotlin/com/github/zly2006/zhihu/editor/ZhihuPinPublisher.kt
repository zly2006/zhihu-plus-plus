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

import androidx.compose.runtime.Composable
import com.fleeksoft.ksoup.Ksoup
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.data.ZhihuJson
import com.github.zly2006.zhihu.shared.util.raiseForStatus
import com.github.zly2006.zhihu.viewmodel.ZhihuApiEnvironment
import com.github.zly2006.zhihu.viewmodel.postSigned
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * 将“发布知乎想法”封装为平台能力。
 *
 * 想法的标题、正文和图片 payload 与回答发布完全不同，不能与回答编辑流程共用一个发布器。
 */
interface ZhihuPinPublisher {
    val isSupported: Boolean

    /**
     * 上传图片到知乎图床。想法图片只进入 media.medias，不插入 Markdown 正文。
     */
    suspend fun uploadImage(
        bytes: ByteArray,
        mimeType: String?,
        fileName: String?,
    ): UploadedZhihuImage

    /**
     * 保存想法草稿。
     *
     * 对应端点：POST https://api.zhihu.com/content/drafts，action=pin。
     */
    suspend fun savePinDraft(
        title: String,
        html: String,
        textLength: Int,
        images: List<UploadedZhihuImage>,
    )

    /**
     * 发布一条新的想法。
     *
     * 返回值：发布成功后的 pinId。
     *
     * 对应端点：POST /api/v4/content/publish，action=pin。
     */
    suspend fun publishPin(
        title: String,
        html: String,
        textLength: Int,
        images: List<UploadedZhihuImage>,
    ): Long
}

@Composable
expect fun rememberZhihuPinPublisher(): ZhihuPinPublisher

internal class ZhihuApiPinPublisher(
    private val environment: ZhihuApiEnvironment,
) : ZhihuPinPublisher {
    override val isSupported: Boolean = true

    override suspend fun uploadImage(
        bytes: ByteArray,
        mimeType: String?,
        fileName: String?,
    ): UploadedZhihuImage =
        uploadZhihuImage(environment, bytes, mimeType, fileName, ZhihuImageUploadSource.Pin)

    override suspend fun savePinDraft(
        title: String,
        html: String,
        textLength: Int,
        images: List<UploadedZhihuImage>,
    ) {
        val xsrf = environment.authenticatedCookies()["_xsrf"]
            ?: throw IllegalStateException("缺少 _xsrf Cookie，无法保存想法草稿；请先确保已登录。")

        environment
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
        val xsrf = environment.authenticatedCookies()["_xsrf"]
            ?: throw IllegalStateException("缺少 _xsrf Cookie，无法发布想法；请先确保已登录。")

        val responseElement = environment
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
            publish = PublishTrace(traceId = newPublishTraceId()),
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

internal object UnsupportedZhihuPinPublisher : ZhihuPinPublisher {
    override val isSupported: Boolean = false

    override suspend fun uploadImage(
        bytes: ByteArray,
        mimeType: String?,
        fileName: String?,
    ): UploadedZhihuImage = throw UnsupportedOperationException("当前平台暂不支持上传图片")

    override suspend fun savePinDraft(
        title: String,
        html: String,
        textLength: Int,
        images: List<UploadedZhihuImage>,
    ): Unit = throw UnsupportedOperationException("当前平台暂不支持发布知乎想法")

    override suspend fun publishPin(
        title: String,
        html: String,
        textLength: Int,
        images: List<UploadedZhihuImage>,
    ): Long = throw UnsupportedOperationException("当前平台暂不支持发布知乎想法")
}

internal fun calculatePinHtmlTextLength(html: String): Int =
    Ksoup
        .parseBodyFragment(html)
        .body()
        .text()
        .length

@Serializable
data class SavePinDraftRequest(
    val action: String = "pin",
    val data: PinContentPayload,
)

@Serializable
data class PublishPinRequest(
    val action: String = "pin",
    val data: PinContentPayload,
)

@Serializable
data class PinContentPayload(
    val publish: PublishTrace,
    val commentsPermission: PublishCommentsPermission = PublishCommentsPermission(),
    @SerialName("extra_info")
    val extraInfo: PinContentExtraInfo = PinContentExtraInfo(),
    val draft: PinContentDraft = PinContentDraft(),
    @OptIn(ExperimentalSerializationApi::class)
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val title: PinContentTitle? = null,
    @OptIn(ExperimentalSerializationApi::class)
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val hybrid: PinContentHybrid? = null,
    @OptIn(ExperimentalSerializationApi::class)
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val media: PinContentMedia? = null,
    @OptIn(ExperimentalSerializationApi::class)
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val topic: PinContentTopic? = null,
)

@Serializable
data class PinContentExtraInfo(
    @SerialName("view_permission")
    val viewPermission: String = "all",
    val publisher: String = "pc",
)

@Serializable
data class PinContentDraft(
    val disabled: Int = 1,
)

@Serializable
data class PinContentTitle(
    val title: String,
)

@Serializable
data class PinContentHybrid(
    val html: String,
    val textLength: Int,
)

@Serializable
data class PinContentMedia(
    val medias: List<PinContentMediaItem>,
)

@Serializable
data class PinContentMediaItem(
    val image: PinContentImage,
)

@Serializable
data class PinContentImage(
    val height: Int,
    val width: Int,
    val url: String,
    val originalUrl: String,
    @OptIn(ExperimentalSerializationApi::class)
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val watermark: String? = null,
    @OptIn(ExperimentalSerializationApi::class)
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val watermarkUrl: String? = null,
)

@Serializable
data class PinContentTopic(
    val topics: List<PinContentTopicItem> = emptyList(),
)

@Serializable
data class PinContentTopicItem(
    @SerialName("topic_id")
    val topicId: String,
    @SerialName("topic_name")
    val topicName: String,
)
