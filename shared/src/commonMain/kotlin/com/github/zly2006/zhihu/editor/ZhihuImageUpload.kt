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

import com.github.zly2006.zhihu.shared.data.ZhihuJson
import com.github.zly2006.zhihu.shared.util.ZhihuFetchSignature
import com.github.zly2006.zhihu.shared.util.raiseForStatus
import com.github.zly2006.zhihu.shared.util.twoDigitString
import com.github.zly2006.zhihu.viewmodel.ZhihuApiEnvironment
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
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.math.min
import kotlin.time.Clock

/**
 * 图片上传逻辑移植自 zhihu_obsidian/src/image_service.ts。
 *
 * 总体步骤：
 * 1) POST https://api.zhihu.com/images 申请 image_id + upload_token
 * 2) PUT https://zhihu-pics-upload.zhimg.com/v2-{hash} 上传到 OSS（GIF 走分片上传）
 * 3) GET https://api.zhihu.com/images/{image_id} 轮询直到 status=success，拿到 original_src / watermark_src
 */
internal suspend fun uploadZhihuImage(
    environment: ZhihuApiEnvironment,
    bytes: ByteArray,
    mimeType: String?,
    fileName: String?,
    source: ZhihuImageUploadSource,
): UploadedZhihuImage {
    val client = environment.httpClient()
    val contentType = mimeType?.takeIf { it.startsWith("image/") }
        ?: guessMimeTypeFromFileName(fileName)
        ?: throw UnknownImageFormatException()
    val ext = guessExtension(contentType, fileName)
        ?: throw UnknownImageFormatException()
    val (rawWidth, rawHeight) = decodeImageSize(bytes)
    val md5Hex = ZhihuFetchSignature.md5Hex(bytes)
    val applyResponse = requestImageUpload(client, md5Hex, source)
    val imageId = applyResponse.uploadFile.imageId
    val uploadFileState = applyResponse.uploadFile.state

    if (uploadFileState == 2) {
        val uploadToken = applyResponse.uploadToken
            ?: throw IllegalStateException(
                "知乎返回的图片需要重新上传，但响应里缺少 uploadToken。imageId=$imageId, state=$uploadFileState",
            )
        if (contentType.equals("image/gif", ignoreCase = true)) {
            uploadGifMultipart(client, md5Hex, bytes, uploadToken)
            notifyUploadingStatus(client, imageId)
        } else {
            uploadSinglePut(client, md5Hex, bytes, contentType, uploadToken)
            notifyUploadingStatus(client, imageId)
        }
    }

    val status = pollImageStatus(client, imageId)
    val src = status.src
        ?: status.watermarkSrc
        ?: status.originalSrc
        ?: "https://picx.zhimg.com/v2-$md5Hex"
    val original = status.originalSrc ?: src
    val watermarkSrc = status.watermarkSrc
    val watermark = status.watermarkFlag

    val url = normalizeZhihuImageUrl(src, ext)
    val originalUrl = normalizeZhihuImageUrl(original, ext)
    val watermarkUrl = watermarkSrc?.let { normalizeZhihuImageUrl(it, ext) }

    return UploadedZhihuImage(
        url = url,
        originalUrl = originalUrl,
        watermark = watermark,
        watermarkMode = status.watermark,
        watermarkUrl = watermarkUrl,
        rawWidth = rawWidth,
        rawHeight = rawHeight,
        imageId = imageId,
    )
}

private suspend fun requestImageUpload(
    client: HttpClient,
    imageHash: String,
    source: ZhihuImageUploadSource,
): ApplyImageUploadResponse {
    val body = ApplyImageUploadRequest(
        imageHash = imageHash,
        source = source.apiValue,
    )
    val response = client
        .post("https://api.zhihu.com/images") {
            contentType(ContentType.Application.Json)
            setBody(body)
        }.raiseForStatus(dumpRequest = true)
        .body<JsonElement>()
    return ZhihuJson.decodeJson(ApplyImageUploadResponse.serializer(), response)
}

private suspend fun uploadSinglePut(
    client: HttpClient,
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
            setBody(bytes)
        }.raiseForStatus(dumpRequest = true)
}

private suspend fun uploadGifMultipart(
    client: HttpClient,
    imageHash: String,
    bytes: ByteArray,
    token: UploadToken,
) {
    val date = rfc1123Now()
    val ossUserAgent = OSS_USER_AGENT
    val uploadId = initMultipartUpload(client, imageHash, token, date, ossUserAgent)

    val partSize = 1024 * 1024
    val etags = mutableListOf<Pair<Int, String>>()
    var partNumber = 1
    var offset = 0
    while (offset < bytes.size) {
        val end = min(bytes.size, offset + partSize)
        val partBytes = bytes.copyOfRange(offset, end)
        val etag = uploadMultipartPart(
            client = client,
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

    completeMultipartUpload(client, imageHash, uploadId, etags, token, date, ossUserAgent)
}

private suspend fun initMultipartUpload(
    client: HttpClient,
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
        }.raiseForStatus(dumpRequest = true)

    val xml = response.body<String>()
    return Regex("<UploadId>([^<]+)</UploadId>").find(xml)?.groupValues?.get(1)
        ?: throw IllegalStateException("无法解析 OSS UploadId: $xml")
}

private suspend fun uploadMultipartPart(
    client: HttpClient,
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
            setBody(bytes)
        }.raiseForStatus(dumpRequest = true)

    return response.headers[HttpHeaders.ETag]
        ?: response.headers["etag"]
        ?: throw IllegalStateException("OSS 分片上传未返回 ETag")
}

private suspend fun completeMultipartUpload(
    client: HttpClient,
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
            setBody(xmlBody)
        }.raiseForStatus(dumpRequest = true)
}

private suspend fun notifyUploadingStatus(
    client: HttpClient,
    imageId: String,
) {
    client
        .put("https://api.zhihu.com/images/$imageId/uploading_status") {
            contentType(ContentType.Application.Json)
            setBody("""{"upload_result":"success"}""")
        }.raiseForStatus(dumpRequest = true)
}

private suspend fun pollImageStatus(
    client: HttpClient,
    imageId: String,
): ImageStatus {
    repeat(10) {
        val status = fetchImageStatus(client, imageId)
        if (status.status == "success") return status
        delay(2_000)
    }
    return fetchImageStatus(client, imageId)
}

private suspend fun fetchImageStatus(
    client: HttpClient,
    imageId: String,
): ImageStatus {
    val response = client
        .get("https://api.zhihu.com/images/$imageId")
        .raiseForStatus(dumpRequest = true)
        .body<JsonElement>()
    return ZhihuJson.decodeJson(ImageStatus.serializer(), response)
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
    val src: String? = null,
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

private const val HMAC_SHA1_BLOCK_SIZE = 64
private val RFC1123_DAY_NAMES = arrayOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
private val RFC1123_MONTH_NAMES = arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

private fun decodeImageSize(bytes: ByteArray): Pair<Int, Int> =
    decodePngSize(bytes)
        ?: decodeJpegSize(bytes)
        ?: decodeGifSize(bytes)
        ?: decodeWebpSize(bytes)
        ?: (0 to 0)

private fun decodePngSize(bytes: ByteArray): Pair<Int, Int>? {
    if (bytes.size < 24 ||
        bytes[0] != 0x89.toByte() ||
        bytes[1] != 'P'.code.toByte() ||
        bytes[2] != 'N'.code.toByte() ||
        bytes[3] != 'G'.code.toByte()
    ) {
        return null
    }
    return readUInt32BigEndian(bytes, 16) to readUInt32BigEndian(bytes, 20)
}

private fun decodeJpegSize(bytes: ByteArray): Pair<Int, Int>? {
    if (bytes.size < 4 || bytes[0] != 0xff.toByte() || bytes[1] != 0xd8.toByte()) {
        return null
    }

    var offset = 2
    while (offset + 3 < bytes.size) {
        if (bytes[offset] != 0xff.toByte()) {
            offset += 1
            continue
        }
        while (offset < bytes.size && bytes[offset] == 0xff.toByte()) {
            offset += 1
        }
        if (offset >= bytes.size) return null

        val marker = bytes[offset].toInt() and 0xff
        offset += 1
        if (marker == 0xd9 || marker == 0xda) return null
        if (marker == 0x01 || marker in 0xd0..0xd7) continue
        if (offset + 1 >= bytes.size) return null

        val segmentLength = readUInt16BigEndian(bytes, offset)
        if (segmentLength < 2 || offset + segmentLength > bytes.size) return null
        if (isJpegStartOfFrame(marker)) {
            if (offset + 6 >= bytes.size) return null
            val height = readUInt16BigEndian(bytes, offset + 3)
            val width = readUInt16BigEndian(bytes, offset + 5)
            return width to height
        }
        offset += segmentLength
    }
    return null
}

private fun decodeGifSize(bytes: ByteArray): Pair<Int, Int>? {
    if (bytes.size < 10 ||
        bytes[0] != 'G'.code.toByte() ||
        bytes[1] != 'I'.code.toByte() ||
        bytes[2] != 'F'.code.toByte()
    ) {
        return null
    }
    return readUInt16LittleEndian(bytes, 6) to readUInt16LittleEndian(bytes, 8)
}

private fun decodeWebpSize(bytes: ByteArray): Pair<Int, Int>? {
    if (bytes.size < 20 ||
        !bytes.hasAscii(0, "RIFF") ||
        !bytes.hasAscii(8, "WEBP")
    ) {
        return null
    }

    var offset = 12
    while (offset + 8 <= bytes.size) {
        val chunkType = bytes.ascii(offset, 4)
        val chunkSize = readUInt32LittleEndian(bytes, offset + 4)
        val dataOffset = offset + 8
        if (chunkSize < 0 || dataOffset + chunkSize > bytes.size) return null

        when (chunkType) {
            "VP8X" -> {
                if (chunkSize >= 10) {
                    val width = readUInt24LittleEndian(bytes, dataOffset + 4) + 1
                    val height = readUInt24LittleEndian(bytes, dataOffset + 7) + 1
                    return width to height
                }
            }
            "VP8L" -> {
                if (chunkSize >= 5 && bytes[dataOffset] == 0x2f.toByte()) {
                    val bits = readUInt32LittleEndian(bytes, dataOffset + 1)
                    val width = (bits and 0x3fff) + 1
                    val height = ((bits ushr 14) and 0x3fff) + 1
                    return width to height
                }
            }
            "VP8 " -> {
                if (chunkSize >= 10 &&
                    bytes[dataOffset + 3] == 0x9d.toByte() &&
                    bytes[dataOffset + 4] == 0x01.toByte() &&
                    bytes[dataOffset + 5] == 0x2a.toByte()
                ) {
                    val width = readUInt16LittleEndian(bytes, dataOffset + 6) and 0x3fff
                    val height = readUInt16LittleEndian(bytes, dataOffset + 8) and 0x3fff
                    return width to height
                }
            }
        }

        offset = dataOffset + chunkSize + (chunkSize and 1)
    }
    return null
}

private fun rfc1123Now(): String {
    val now = Clock.System.now().toLocalDateTime(TimeZone.UTC)
    val day = RFC1123_DAY_NAMES[now.dayOfWeek.ordinal]
    val month = RFC1123_MONTH_NAMES[now.month.ordinal]
    return "$day, ${now.day.twoDigitString()} $month ${now.year} " +
        "${now.hour.twoDigitString()}:${now.minute.twoDigitString()}:${now.second.twoDigitString()} GMT"
}

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

@OptIn(ExperimentalEncodingApi::class)
private fun hmacSha1Base64(secret: String, message: String): String {
    val rawKey = secret.encodeToByteArray()
    val key = if (rawKey.size > HMAC_SHA1_BLOCK_SIZE) sha1(rawKey) else rawKey
    val keyBlock = ByteArray(HMAC_SHA1_BLOCK_SIZE)
    key.copyInto(keyBlock)

    val outerPad = ByteArray(HMAC_SHA1_BLOCK_SIZE)
    val innerPad = ByteArray(HMAC_SHA1_BLOCK_SIZE)
    for (index in keyBlock.indices) {
        outerPad[index] = (keyBlock[index].toInt() xor 0x5c).toByte()
        innerPad[index] = (keyBlock[index].toInt() xor 0x36).toByte()
    }

    return Base64.Default.encode(sha1(outerPad + sha1(innerPad + message.encodeToByteArray())))
}

private fun sha1(input: ByteArray): ByteArray {
    val bitLength = input.size.toLong() * 8
    val paddedLength = (((input.size + 8) / 64) + 1) * 64
    val padded = ByteArray(paddedLength)
    input.copyInto(padded)
    padded[input.size] = 0x80.toByte()
    for (index in 0 until 8) {
        padded[paddedLength - 1 - index] = (bitLength ushr (index * 8)).toByte()
    }

    var h0 = 0x67452301
    var h1 = 0xefcdab89.toInt()
    var h2 = 0x98badcfe.toInt()
    var h3 = 0x10325476
    var h4 = 0xc3d2e1f0.toInt()
    val words = IntArray(80)

    var chunkOffset = 0
    while (chunkOffset < padded.size) {
        for (index in 0 until 16) {
            words[index] = readUInt32BigEndian(padded, chunkOffset + index * 4)
        }
        for (index in 16 until 80) {
            words[index] = (words[index - 3] xor words[index - 8] xor words[index - 14] xor words[index - 16]).rotateLeft(1)
        }

        var a = h0
        var b = h1
        var c = h2
        var d = h3
        var e = h4

        for (index in 0 until 80) {
            val (f, k) = when (index) {
                in 0..19 -> ((b and c) or (b.inv() and d)) to 0x5a827999
                in 20..39 -> (b xor c xor d) to 0x6ed9eba1
                in 40..59 -> ((b and c) or (b and d) or (c and d)) to 0x8f1bbcdc.toInt()
                else -> (b xor c xor d) to 0xca62c1d6.toInt()
            }
            val temp = a.rotateLeft(5) + f + e + k + words[index]
            e = d
            d = c
            c = b.rotateLeft(30)
            b = a
            a = temp
        }

        h0 += a
        h1 += b
        h2 += c
        h3 += d
        h4 += e
        chunkOffset += 64
    }

    return ByteArray(20).apply {
        writeIntBigEndian(0, h0)
        writeIntBigEndian(4, h1)
        writeIntBigEndian(8, h2)
        writeIntBigEndian(12, h3)
        writeIntBigEndian(16, h4)
    }
}

private fun isJpegStartOfFrame(marker: Int): Boolean =
    marker in 0xc0..0xcf && marker != 0xc4 && marker != 0xc8 && marker != 0xcc

private fun readUInt16BigEndian(bytes: ByteArray, offset: Int): Int =
    ((bytes[offset].toInt() and 0xff) shl 8) or
        (bytes[offset + 1].toInt() and 0xff)

private fun readUInt16LittleEndian(bytes: ByteArray, offset: Int): Int =
    (bytes[offset].toInt() and 0xff) or
        ((bytes[offset + 1].toInt() and 0xff) shl 8)

private fun readUInt24LittleEndian(bytes: ByteArray, offset: Int): Int =
    (bytes[offset].toInt() and 0xff) or
        ((bytes[offset + 1].toInt() and 0xff) shl 8) or
        ((bytes[offset + 2].toInt() and 0xff) shl 16)

private fun readUInt32BigEndian(bytes: ByteArray, offset: Int): Int =
    ((bytes[offset].toInt() and 0xff) shl 24) or
        ((bytes[offset + 1].toInt() and 0xff) shl 16) or
        ((bytes[offset + 2].toInt() and 0xff) shl 8) or
        (bytes[offset + 3].toInt() and 0xff)

private fun readUInt32LittleEndian(bytes: ByteArray, offset: Int): Int =
    (bytes[offset].toInt() and 0xff) or
        ((bytes[offset + 1].toInt() and 0xff) shl 8) or
        ((bytes[offset + 2].toInt() and 0xff) shl 16) or
        ((bytes[offset + 3].toInt() and 0xff) shl 24)

private fun ByteArray.writeIntBigEndian(offset: Int, value: Int) {
    this[offset] = (value ushr 24).toByte()
    this[offset + 1] = (value ushr 16).toByte()
    this[offset + 2] = (value ushr 8).toByte()
    this[offset + 3] = value.toByte()
}

private fun ByteArray.hasAscii(offset: Int, value: String): Boolean {
    if (offset + value.length > size) return false
    return value.indices.all { index -> this[offset + index] == value[index].code.toByte() }
}

private fun ByteArray.ascii(offset: Int, length: Int): String =
    CharArray(length) { index -> this[offset + index].toInt().toChar() }.concatToString()

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
