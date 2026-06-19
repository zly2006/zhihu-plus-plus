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

import android.graphics.BitmapFactory
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.shared.util.raiseForStatus
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.coroutines.delay
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import java.security.MessageDigest
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.min

/**
 * 图片上传逻辑移植自 zhihu_obsidian/src/image_service.ts。
 *
 * 总体步骤：
 * 1) POST https://api.zhihu.com/images 申请 image_id + upload_token
 * 2) PUT https://zhihu-pics-upload.zhimg.com/v2-{hash} 上传到 OSS（GIF 走分片上传）
 * 3) GET https://api.zhihu.com/images/{image_id} 轮询直到 status=success，拿到 original_src / watermark_src
 */
class ZhihuImageUploader(
    private val client: HttpClient,
    private val cookies: Map<String, String>,
    private val userAgent: String,
) {
    suspend fun upload(bytes: ByteArray, mimeType: String?, fileName: String?): UploadedZhihuImage {
        val contentType = mimeType?.takeIf { it.startsWith("image/") }
            ?: guessMimeTypeFromFileName(fileName)
            ?: throw UnknownImageFormatException()
        val ext = guessExtension(contentType, fileName)
            ?: throw UnknownImageFormatException()
        val (rawWidth, rawHeight) = decodeImageSize(bytes)
        val md5Hex = md5Hex(bytes)
        return runCatching {
            val applyResponse = requestImageUpload(md5Hex)
            val imageId = applyResponse.uploadFile.imageId
            val uploadFileState = applyResponse.uploadFile.state

            if (uploadFileState == 2) {
                val uploadToken = applyResponse.uploadToken
                    ?: throw IllegalStateException(
                        "知乎返回的图片需要重新上传，但响应里缺少 uploadToken。imageId=$imageId, state=$uploadFileState",
                    )
                if (contentType.equals("image/gif", ignoreCase = true)) {
                    uploadGifMultipart(md5Hex, bytes, uploadToken)
                    notifyUploadingStatus(imageId)
                } else {
                    uploadSinglePut(md5Hex, bytes, contentType, uploadToken)
                    notifyUploadingStatus(imageId)
                }
            }

            val status = pollImageStatus(imageId)
            val original = status.originalSrc
                ?: "https://picx.zhimg.com/v2-$md5Hex"
            val watermarkSrc = status.watermarkSrc
            val watermark = status.watermarkFlag

            val originalUrl = normalizeZhihuImageUrl(original, ext)
            val watermarkUrl = watermarkSrc?.let { normalizeZhihuImageUrl(it, ext) }

            UploadedZhihuImage(
                url = originalUrl,
                originalUrl = originalUrl,
                watermark = watermark,
                watermarkUrl = watermarkUrl,
                rawWidth = rawWidth,
                rawHeight = rawHeight,
            )
        }.getOrThrow()
    }

    private suspend fun requestImageUpload(imageHash: String): ApplyImageUploadResponse {
        val body = ApplyImageUploadRequest(
            imageHash = imageHash,
            source = "article",
        )
        val response = client
            .post("https://api.zhihu.com/images") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.UserAgent, userAgent)
                header(HttpHeaders.Cookie, buildCookieHeader(cookies))
                setBody(body)
            }.raiseForStatus(dumpRequest = true)
            .body<JsonElement>()
        return AccountData.decodeJson(ApplyImageUploadResponse.serializer(), response)
    }

    private suspend fun uploadSinglePut(
        imageHash: String,
        bytes: ByteArray,
        mimeType: String,
        token: UploadToken,
    ) {
        val date = rfc1123Now()
        val ossUserAgent = OSS_USER_AGENT
        val stringToSign = buildStringToSignPut(
            mimeType = mimeType,
            date = date,
            securityToken = token.accessToken,
            ossUserAgent = ossUserAgent,
            imageHash = imageHash,
        )
        val signature = hmacSha1Base64(token.accessKey, stringToSign)
        val authorization = "OSS ${token.accessId}:$signature"

        client
            .put("https://zhihu-pics-upload.zhimg.com/v2-$imageHash") {
                contentType(ContentType.parse(mimeType))
                header("x-oss-date", date)
                header("x-oss-user-agent", ossUserAgent)
                header("x-oss-security-token", token.accessToken)
                header(HttpHeaders.Authorization, authorization)
                header(HttpHeaders.UserAgent, userAgent)
                setBody(bytes)
            }.raiseForStatus(dumpRequest = true)
    }

    private suspend fun uploadGifMultipart(
        imageHash: String,
        bytes: ByteArray,
        token: UploadToken,
    ) {
        val date = rfc1123Now()
        val ossUserAgent = OSS_USER_AGENT
        val uploadId = initMultipartUpload(imageHash, token, date, ossUserAgent)

        val partSize = 1024 * 1024
        val etags = mutableListOf<Pair<Int, String>>()
        var partNumber = 1
        var offset = 0
        while (offset < bytes.size) {
            val end = min(bytes.size, offset + partSize)
            val partBytes = bytes.copyOfRange(offset, end)
            val etag = uploadMultipartPart(
                imageHash = imageHash,
                uploadId = uploadId,
                partNumber = partNumber,
                bytes = partBytes,
                token = token,
                date = date,
                ossUserAgent = ossUserAgent,
            )
            etags.add(partNumber to etag)
            offset = end
            partNumber += 1
        }

        completeMultipartUpload(imageHash, uploadId, etags, token, date, ossUserAgent)
    }

    private suspend fun initMultipartUpload(
        imageHash: String,
        token: UploadToken,
        date: String,
        ossUserAgent: String,
    ): String {
        val stringToSign = buildOssSignatureString(
            method = "POST",
            contentType = "",
            date = date,
            securityToken = token.accessToken,
            ossUserAgent = ossUserAgent,
            resourcePath = "/zhihu-pics/v2-$imageHash",
            subResource = "uploads",
        )
        val signature = hmacSha1Base64(token.accessKey, stringToSign)
        val authorization = "OSS ${token.accessId}:$signature"
        val response = client
            .post("https://zhihu-pics-upload.zhimg.com/v2-$imageHash?uploads") {
                header("x-oss-date", date)
                header("x-oss-user-agent", ossUserAgent)
                header("x-oss-security-token", token.accessToken)
                header(HttpHeaders.Authorization, authorization)
                header(HttpHeaders.UserAgent, userAgent)
            }.raiseForStatus(dumpRequest = true)

        val xml = response.body<String>()
        return Regex("<UploadId>([^<]+)</UploadId>").find(xml)?.groupValues?.get(1)
            ?: throw IllegalStateException("无法解析 OSS UploadId: $xml")
    }

    private suspend fun uploadMultipartPart(
        imageHash: String,
        uploadId: String,
        partNumber: Int,
        bytes: ByteArray,
        token: UploadToken,
        date: String,
        ossUserAgent: String,
    ): String {
        val subResource = "partNumber=$partNumber&uploadId=$uploadId"
        val stringToSign = buildOssSignatureString(
            method = "PUT",
            contentType = "application/octet-stream",
            date = date,
            securityToken = token.accessToken,
            ossUserAgent = ossUserAgent,
            resourcePath = "/zhihu-pics/v2-$imageHash",
            subResource = subResource,
        )
        val signature = hmacSha1Base64(token.accessKey, stringToSign)
        val authorization = "OSS ${token.accessId}:$signature"

        val response = client
            .put("https://zhihu-pics-upload.zhimg.com/v2-$imageHash?$subResource") {
                contentType(ContentType.Application.OctetStream)
                header("x-oss-date", date)
                header("x-oss-user-agent", ossUserAgent)
                header("x-oss-security-token", token.accessToken)
                header(HttpHeaders.Authorization, authorization)
                header(HttpHeaders.UserAgent, userAgent)
                setBody(bytes)
            }.raiseForStatus(dumpRequest = true)

        return response.headers[HttpHeaders.ETag]
            ?: response.headers["etag"]
            ?: throw IllegalStateException("OSS 分片上传未返回 ETag")
    }

    private suspend fun completeMultipartUpload(
        imageHash: String,
        uploadId: String,
        etags: List<Pair<Int, String>>,
        token: UploadToken,
        date: String,
        ossUserAgent: String,
    ) {
        val subResource = "uploadId=$uploadId"
        val xmlBody = buildString {
            append("<CompleteMultipartUpload>")
            etags.forEach { (partNumber, etag) ->
                append("<Part>")
                append("<PartNumber>").append(partNumber).append("</PartNumber>")
                append("<ETag>").append(etag).append("</ETag>")
                append("</Part>")
            }
            append("</CompleteMultipartUpload>")
        }

        val stringToSign = buildOssSignatureString(
            method = "POST",
            contentType = "application/xml",
            date = date,
            securityToken = token.accessToken,
            ossUserAgent = ossUserAgent,
            resourcePath = "/zhihu-pics/v2-$imageHash",
            subResource = subResource,
        )
        val signature = hmacSha1Base64(token.accessKey, stringToSign)
        val authorization = "OSS ${token.accessId}:$signature"

        client
            .post("https://zhihu-pics-upload.zhimg.com/v2-$imageHash?$subResource") {
                contentType(ContentType.Application.Xml)
                header("x-oss-date", date)
                header("x-oss-user-agent", ossUserAgent)
                header("x-oss-security-token", token.accessToken)
                header(HttpHeaders.Authorization, authorization)
                header(HttpHeaders.UserAgent, userAgent)
                setBody(xmlBody)
            }.raiseForStatus(dumpRequest = true)
    }

    private suspend fun notifyUploadingStatus(imageId: String) {
        client
            .put("https://api.zhihu.com/images/$imageId/uploading_status") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.UserAgent, userAgent)
                header(HttpHeaders.Cookie, buildCookieHeader(cookies))
                setBody("""{"upload_result":"success"}""")
            }.raiseForStatus(dumpRequest = true)
    }

    private suspend fun pollImageStatus(imageId: String): ImageStatus {
        repeat(10) {
            val status = fetchImageStatus(imageId)
            if (status.status == "success") return status
            delay(2_000)
        }
        return fetchImageStatus(imageId)
    }

    private suspend fun fetchImageStatus(imageId: String): ImageStatus {
        val response = client
            .get("https://api.zhihu.com/images/$imageId") {
                header(HttpHeaders.UserAgent, userAgent)
                header(HttpHeaders.Cookie, buildCookieHeader(cookies))
            }.raiseForStatus(dumpRequest = true)
            .body<JsonElement>()
        return AccountData.decodeJson(ImageStatus.serializer(), response)
    }
}

@Serializable
private data class ApplyImageUploadRequest(
    @SerialName("image_hash")
    val imageHash: String,
    val source: String,
)

@Serializable
private data class ApplyImageUploadResponse(
    val uploadFile: UploadFile,
    val uploadToken: UploadToken? = null,
)

@Serializable
private data class UploadFile(
    val imageId: String,
    val state: Int,
)

@Serializable
private data class UploadToken(
    val accessId: String,
    val accessKey: String,
    val accessToken: String,
)

@Serializable
private data class ImageStatus(
    val status: String? = null,
    val originalSrc: String? = null,
    val watermark: String? = null,
    val watermarkSrc: String? = null,
) {
    val watermarkFlag: Boolean?
        get() = when (watermark?.lowercase()) {
            "watermark", "true", "1" -> true
            "original", "false", "0" -> false
            else -> null
        }
}

private const val OSS_USER_AGENT =
    "aliyun-sdk-js/6.8.0 Chrome 99.0.4844.84 on Windows 10 64-bit"

private fun buildCookieHeader(cookies: Map<String, String>): String {
    val cookieNames = listOf(
        "_zap",
        "_xsrf",
        "BEC",
        "d_c0",
        "captcha_session_v2",
        "z_c0",
        "q_c1",
    )
    return cookieNames
        .mapNotNull { key ->
            cookies[key]?.takeIf { it.isNotBlank() }?.let { value -> "$key=$value" }
        }.joinToString("; ")
}

private fun md5Hex(bytes: ByteArray): String =
    MessageDigest
        .getInstance("MD5")
        .digest(bytes)
        .joinToString("") { b -> "%02x".format(b) }

private fun decodeImageSize(bytes: ByteArray): Pair<Int, Int> {
    val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
    return (options.outWidth.takeIf { it > 0 } ?: 0) to (options.outHeight.takeIf { it > 0 } ?: 0)
}

private fun rfc1123Now(): String =
    DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now(ZoneOffset.UTC))

private fun buildStringToSignPut(
    mimeType: String,
    date: String,
    securityToken: String,
    ossUserAgent: String,
    imageHash: String,
): String = buildString {
    append("PUT\n")
    append("\n")
    append(mimeType).append('\n')
    append(date).append('\n')
    append("x-oss-date:").append(date).append('\n')
    append("x-oss-security-token:").append(securityToken).append('\n')
    append("x-oss-user-agent:").append(ossUserAgent).append('\n')
    append("/zhihu-pics/v2-").append(imageHash)
}

private fun buildOssSignatureString(
    method: String,
    contentType: String,
    date: String,
    securityToken: String,
    ossUserAgent: String,
    resourcePath: String,
    subResource: String?,
): String {
    val canonicalHeaders = buildString {
        append("x-oss-date:").append(date).append('\n')
        append("x-oss-security-token:").append(securityToken).append('\n')
        append("x-oss-user-agent:").append(ossUserAgent).append('\n')
    }
    val canonicalResource = if (subResource.isNullOrBlank()) {
        resourcePath
    } else {
        "$resourcePath?$subResource"
    }
    return buildString {
        append(method).append('\n')
        append('\n')
        append(contentType).append('\n')
        append(date).append('\n')
        append(canonicalHeaders)
        append(canonicalResource)
    }
}

private fun hmacSha1Base64(secret: String, message: String): String {
    val mac = Mac.getInstance("HmacSHA1")
    mac.init(SecretKeySpec(secret.toByteArray(Charsets.UTF_8), "HmacSHA1"))
    val raw = mac.doFinal(message.toByteArray(Charsets.UTF_8))
    return android.util.Base64.encodeToString(raw, android.util.Base64.NO_WRAP)
}

private fun guessMimeTypeFromFileName(fileName: String?): String? = when {
    fileName == null -> null
    fileName.endsWith(".png", ignoreCase = true) -> "image/png"
    fileName.endsWith(".jpg", ignoreCase = true) || fileName.endsWith(".jpeg", ignoreCase = true) -> "image/jpeg"
    fileName.endsWith(".gif", ignoreCase = true) -> "image/gif"
    fileName.endsWith(".webp", ignoreCase = true) -> "image/webp"
    else -> null
}

private fun guessExtension(mimeType: String, fileName: String?): String? {
    val extFromName = fileName
        ?.substringAfterLast('.', "")
        ?.takeIf { it.isNotBlank() }
        ?.lowercase()
    return when (extFromName) {
        "png" -> "png"
        "jpg", "jpeg" -> "jpg"
        "gif" -> "gif"
        "webp" -> "webp"
        null -> when {
            mimeType.equals("image/png", ignoreCase = true) -> "png"
            mimeType.equals("image/jpeg", ignoreCase = true) -> "jpg"
            mimeType.equals("image/gif", ignoreCase = true) -> "gif"
            mimeType.equals("image/webp", ignoreCase = true) -> "webp"
            else -> null
        }

        else -> null
    }
}

private fun normalizeZhihuImageUrl(rawUrl: String, ext: String): String {
    val trimmed = rawUrl.trimEnd('.')
    val base = trimmed.substringBefore('?')
    val query = trimmed.substringAfter('?', missingDelimiterValue = "")

    val baseWithExt = run {
        val lastSegment = base.substringAfterLast('/')
        if ('.' in lastSegment) base else "$base.$ext"
    }

    return if (query.isBlank()) {
        baseWithExt
    } else {
        "$baseWithExt?$query"
    }
}
