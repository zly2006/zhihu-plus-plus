@file:Suppress("ktlint:standard:argument-list-wrapping")

package com.github.zly2006.zhihu.shared.util

import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sin

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
                b += Integer.rotateLeft(a + f + MD5_K[i] + words[g], MD5_S[i])
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
