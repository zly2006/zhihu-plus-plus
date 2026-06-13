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

@file:Suppress("ktlint:standard:argument-list-wrapping")

package com.github.zly2006.zhihu.shared.util

import com.github.zly2006.zhihu.shared.data.ZhihuJson
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.serializer
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sin

const val ZHIHU_WEB_ZSE93 = "101_3_3.0"

fun HttpRequestBuilder.signZhihuFetchRequest(
    zse93: String = ZHIHU_WEB_ZSE93,
    dc0: String,
    body: String? = null,
) {
    val requestUrl = url.buildString()
    header("x-zse-93", zse93)
    header("x-zse-96", ZhihuFetchSignature.createZse96Header(zse93, requestUrl, dc0, body))
    header("x-requested-with", "fetch")
}

fun HttpRequestBuilder.signZhihuFetchRequest(
    cookies: Map<String, String>,
    body: String? = null,
) {
    val dc0 = cookies["d_c0"]?.takeIf { it.isNotBlank() } ?: return
    val requestBody = body ?: signedJsonBodyOrNull()
    signZhihuFetchRequest(dc0 = dc0, body = requestBody)
}

private fun HttpRequestBuilder.signedJsonBodyOrNull(): String? =
    if (contentType() == ContentType.Application.Json) {
        body as? String
            ?: bodyType?.kotlinType?.let { type ->
                ZhihuJson.json.encodeToString(serializer(type), body)
            }
    } else {
        null
    }

object ZhihuFetchSignature {
    fun createZse96Header(
        zse93: String,
        url: String,
        dc0: String,
        body: String? = null,
    ): String {
        val pathname = "/" + url.substringAfter("//").substringAfter('/')
        val signSource = listOfNotNull(
            zse93,
            pathname,
            dc0,
            body,
        ).joinToString("+")
        return "2.0_${ZseSigner.encryptZseV4(md5Hex(signSource))}"
    }

    internal fun md5Hex(input: String): String {
        val message = input.encodeToByteArray()
        val bitLength = message.size.toLong() * 8
        val paddedLength = (((message.size + 8) / 64) + 1) * 64
        val padded = ByteArray(paddedLength)
        message.copyInto(padded)
        padded[message.size] = 0x80.toByte()
        for (i in 0 until 8) {
            padded[paddedLength - 8 + i] = (bitLength ushr (8 * i)).toByte()
        }

        var a0 = 0x67452301
        var b0 = 0xefcdab89.toInt()
        var c0 = 0x98badcfe.toInt()
        var d0 = 0x10325476

        val words = IntArray(16)
        var chunkOffset = 0
        while (chunkOffset < padded.size) {
            for (i in 0 until 16) {
                val offset = chunkOffset + i * 4
                words[i] = (padded[offset].toInt() and 0xff) or
                    ((padded[offset + 1].toInt() and 0xff) shl 8) or
                    ((padded[offset + 2].toInt() and 0xff) shl 16) or
                    ((padded[offset + 3].toInt() and 0xff) shl 24)
            }

            var a = a0
            var b = b0
            var c = c0
            var d = d0

            for (i in 0 until 64) {
                val f: Int
                val g: Int
                when (i) {
                    in 0..15 -> {
                        f = (b and c) or (b.inv() and d)
                        g = i
                    }
                    in 16..31 -> {
                        f = (d and b) or (d.inv() and c)
                        g = (5 * i + 1) % 16
                    }
                    in 32..47 -> {
                        f = b xor c xor d
                        g = (3 * i + 5) % 16
                    }
                    else -> {
                        f = c xor (b or d.inv())
                        g = (7 * i) % 16
                    }
                }

                val nextD = c
                c = b
                b += (a + f + MD5_K[i] + words[g]).rotateLeft(MD5_S[i])
                a = d
                d = nextD
            }

            a0 += a
            b0 += b
            c0 += c
            d0 += d
            chunkOffset += 64
        }

        return buildString(32) {
            appendLittleEndianHex(a0)
            appendLittleEndianHex(b0)
            appendLittleEndianHex(c0)
            appendLittleEndianHex(d0)
        }
    }

    private fun StringBuilder.appendLittleEndianHex(value: Int) {
        for (i in 0 until 4) {
            val byte = (value ushr (8 * i)) and 0xff
            append(HEX[byte ushr 4])
            append(HEX[byte and 0x0f])
        }
    }

    private val HEX = "0123456789abcdef".toCharArray()

    private val MD5_S = intArrayOf(
        7,
        12,
        17,
        22,
        7,
        12,
        17,
        22,
        7,
        12,
        17,
        22,
        7,
        12,
        17,
        22,
        5,
        9,
        14,
        20,
        5,
        9,
        14,
        20,
        5,
        9,
        14,
        20,
        5,
        9,
        14,
        20,
        4,
        11,
        16,
        23,
        4,
        11,
        16,
        23,
        4,
        11,
        16,
        23,
        4,
        11,
        16,
        23,
        6,
        10,
        15,
        21,
        6,
        10,
        15,
        21,
        6,
        10,
        15,
        21,
        6,
        10,
        15,
        21,
    )

    private val MD5_K = IntArray(64) { index ->
        floor(abs(sin((index + 1).toDouble())) * 4_294_967_296.0).toLong().toInt()
    }
}
