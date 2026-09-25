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

/**
 * 增量 SHA-256，用于跨平台校验下载物摘要。
 *
 * 各平台自带的摘要实现（Android/JVM 的 MessageDigest、Apple 的 CommonCrypto）无法在 commonMain
 * 共享，而下载安装包可能有上百 MB，不能整体读进内存，因此这里按标准算法实现增量版本，
 * 并用 NIST 已知向量做回归（见 Sha256Test）。
 */
internal class Sha256 {
    private val state = intArrayOf(
        0x6a09e667.toInt(),
        0xbb67ae85.toInt(),
        0x3c6ef372.toInt(),
        0xa54ff53a.toInt(),
        0x510e527f.toInt(),
        0x9b05688c.toInt(),
        0x1f83d9ab.toInt(),
        0x5be0cd19.toInt(),
    )

    private val block = ByteArray(BLOCK_BYTES)
    private var blockLength = 0
    private var totalBytes = 0L

    /** 追加 [length] 个字节；可重复调用。 */
    fun update(bytes: ByteArray, offset: Int = 0, length: Int = bytes.size - offset) {
        var index = offset
        var remaining = length
        totalBytes += remaining
        while (remaining > 0) {
            val copied = minOf(remaining, BLOCK_BYTES - blockLength)
            bytes.copyInto(block, blockLength, index, index + copied)
            blockLength += copied
            index += copied
            remaining -= copied
            if (blockLength == BLOCK_BYTES) {
                compress(block)
                blockLength = 0
            }
        }
    }

    /** 计算摘要并返回小写十六进制字符串；调用后本实例不可继续使用。 */
    fun digestHex(): String {
        val bitLength = totalBytes * 8
        // 消息尾部补 0x80、若干 0 与 64 位大端比特长度，使补位后的长度正好对齐到块边界。
        val paddingLength = if (blockLength < BLOCK_BYTES - LENGTH_BYTES) {
            BLOCK_BYTES - LENGTH_BYTES - blockLength
        } else {
            2 * BLOCK_BYTES - LENGTH_BYTES - blockLength
        }
        val tail = ByteArray(paddingLength + LENGTH_BYTES)
        tail[0] = 0x80.toByte()
        for (i in 0 until LENGTH_BYTES) {
            tail[tail.size - 1 - i] = (bitLength ushr (8 * i)).toByte()
        }
        update(tail)
        return state.joinToString("") { word -> word.toUInt().toString(16).padStart(8, '0') }
    }

    private fun compress(chunk: ByteArray) {
        val schedule = IntArray(ROUNDS)
        for (i in 0 until BLOCK_WORDS) {
            val offset = i * 4
            schedule[i] = ((chunk[offset].toInt() and 0xff) shl 24) or
                ((chunk[offset + 1].toInt() and 0xff) shl 16) or
                ((chunk[offset + 2].toInt() and 0xff) shl 8) or
                (chunk[offset + 3].toInt() and 0xff)
        }
        for (i in BLOCK_WORDS until ROUNDS) {
            val previous = schedule[i - 15]
            val recent = schedule[i - 2]
            val s0 = previous.rotateRight(7) xor previous.rotateRight(18) xor (previous ushr 3)
            val s1 = recent.rotateRight(17) xor recent.rotateRight(19) xor (recent ushr 10)
            schedule[i] = schedule[i - 16] + s0 + schedule[i - 7] + s1
        }

        var a = state[0]
        var b = state[1]
        var c = state[2]
        var d = state[3]
        var e = state[4]
        var f = state[5]
        var g = state[6]
        var h = state[7]

        for (i in 0 until ROUNDS) {
            val s1 = e.rotateRight(6) xor e.rotateRight(11) xor e.rotateRight(25)
            val choose = (e and f) xor (e.inv() and g)
            val temp1 = h + s1 + choose + ROUND_CONSTANTS[i] + schedule[i]
            val s0 = a.rotateRight(2) xor a.rotateRight(13) xor a.rotateRight(22)
            val majority = (a and b) xor (a and c) xor (b and c)
            val temp2 = s0 + majority
            h = g
            g = f
            f = e
            e = d + temp1
            d = c
            c = b
            b = a
            a = temp1 + temp2
        }

        state[0] += a
        state[1] += b
        state[2] += c
        state[3] += d
        state[4] += e
        state[5] += f
        state[6] += g
        state[7] += h
    }
}

private const val BLOCK_BYTES = 64
private const val BLOCK_WORDS = 16
private const val ROUNDS = 64
private const val LENGTH_BYTES = 8

private val ROUND_CONSTANTS = intArrayOf(
    0x428a2f98.toInt(),
    0x71374491.toInt(),
    0xb5c0fbcf.toInt(),
    0xe9b5dba5.toInt(),
    0x3956c25b.toInt(),
    0x59f111f1.toInt(),
    0x923f82a4.toInt(),
    0xab1c5ed5.toInt(),
    0xd807aa98.toInt(),
    0x12835b01.toInt(),
    0x243185be.toInt(),
    0x550c7dc3.toInt(),
    0x72be5d74.toInt(),
    0x80deb1fe.toInt(),
    0x9bdc06a7.toInt(),
    0xc19bf174.toInt(),
    0xe49b69c1.toInt(),
    0xefbe4786.toInt(),
    0x0fc19dc6.toInt(),
    0x240ca1cc.toInt(),
    0x2de92c6f.toInt(),
    0x4a7484aa.toInt(),
    0x5cb0a9dc.toInt(),
    0x76f988da.toInt(),
    0x983e5152.toInt(),
    0xa831c66d.toInt(),
    0xb00327c8.toInt(),
    0xbf597fc7.toInt(),
    0xc6e00bf3.toInt(),
    0xd5a79147.toInt(),
    0x06ca6351.toInt(),
    0x14292967.toInt(),
    0x27b70a85.toInt(),
    0x2e1b2138.toInt(),
    0x4d2c6dfc.toInt(),
    0x53380d13.toInt(),
    0x650a7354.toInt(),
    0x766a0abb.toInt(),
    0x81c2c92e.toInt(),
    0x92722c85.toInt(),
    0xa2bfe8a1.toInt(),
    0xa81a664b.toInt(),
    0xc24b8b70.toInt(),
    0xc76c51a3.toInt(),
    0xd192e819.toInt(),
    0xd6990624.toInt(),
    0xf40e3585.toInt(),
    0x106aa070.toInt(),
    0x19a4c116.toInt(),
    0x1e376c08.toInt(),
    0x2748774c.toInt(),
    0x34b0bcb5.toInt(),
    0x391c0cb3.toInt(),
    0x4ed8aa4a.toInt(),
    0x5b9cca4f.toInt(),
    0x682e6ff3.toInt(),
    0x748f82ee.toInt(),
    0x78a5636f.toInt(),
    0x84c87814.toInt(),
    0x8cc70208.toInt(),
    0x90befffa.toInt(),
    0xa4506ceb.toInt(),
    0xbef9a3f7.toInt(),
    0xc67178f2.toInt(),
)
