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

package com.github.zly2006.zhihu.util

actual fun hmacSha1Hex(key: String, message: String): String {
    val blockSize = 64
    val originalKey = key.encodeToByteArray()
    val normalizedKey = (if (originalKey.size > blockSize) sha1(originalKey) else originalKey)
        .copyOf(blockSize)
    val innerKey = ByteArray(blockSize) { index -> (normalizedKey[index].toInt() xor 0x36).toByte() }
    val outerKey = ByteArray(blockSize) { index -> (normalizedKey[index].toInt() xor 0x5c).toByte() }
    val innerHash = sha1(innerKey + message.encodeToByteArray())
    return sha1(outerKey + innerHash).joinToString("") { byte ->
        (byte.toInt() and 0xff).toString(16).padStart(2, '0')
    }
}

private fun sha1(input: ByteArray): ByteArray {
    val bitLength = input.size.toLong() * 8
    val paddedSize = ((input.size + 9 + 63) / 64) * 64
    val padded = ByteArray(paddedSize)
    input.copyInto(padded)
    padded[input.size] = 0x80.toByte()
    for (index in 0 until 8) {
        padded[padded.lastIndex - index] = (bitLength ushr (index * 8)).toByte()
    }

    var h0 = 0x67452301
    var h1 = 0xEFCDAB89.toInt()
    var h2 = 0x98BADCFE.toInt()
    var h3 = 0x10325476
    var h4 = 0xC3D2E1F0.toInt()
    val words = IntArray(80)

    for (chunkOffset in padded.indices step 64) {
        for (index in 0 until 16) {
            val offset = chunkOffset + index * 4
            words[index] =
                ((padded[offset].toInt() and 0xff) shl 24) or
                ((padded[offset + 1].toInt() and 0xff) shl 16) or
                ((padded[offset + 2].toInt() and 0xff) shl 8) or
                (padded[offset + 3].toInt() and 0xff)
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
            val (function, constant) = when (index) {
                in 0..19 -> ((b and c) or (b.inv() and d)) to 0x5A827999
                in 20..39 -> (b xor c xor d) to 0x6ED9EBA1
                in 40..59 -> ((b and c) or (b and d) or (c and d)) to 0x8F1BBCDC.toInt()
                else -> (b xor c xor d) to 0xCA62C1D6.toInt()
            }
            val temporary = a.rotateLeft(5) + function + e + constant + words[index]
            e = d
            d = c
            c = b.rotateLeft(30)
            b = a
            a = temporary
        }

        h0 += a
        h1 += b
        h2 += c
        h3 += d
        h4 += e
    }

    return ByteArray(20).also { output ->
        intArrayOf(h0, h1, h2, h3, h4).forEachIndexed { wordIndex, word ->
            val offset = wordIndex * 4
            output[offset] = (word ushr 24).toByte()
            output[offset + 1] = (word ushr 16).toByte()
            output[offset + 2] = (word ushr 8).toByte()
            output[offset + 3] = word.toByte()
        }
    }
}
