package com.github.zly2006.zhihu.encryption

import android.util.Base64
import java.security.MessageDigest

/**
 * ZSE v4 Encryption - Complete Kotlin implementation
 * Ported 1:1 from zse-v4.js
 *
 * This implementation produces identical output to the JavaScript version.
 */
object ZseEncryption {
    const val VERSION = "3.0"

    // S-Box for SM4 cipher (from JavaScript __ZH__.zse.zb)
    private val SBOX = intArrayOf(
        214, 144, 233, 254, 204, 225, 61, 183, 22, 182, 20, 194, 40, 251, 44, 5,
        43, 103, 154, 118, 42, 190, 4, 195, 170, 68, 19, 38, 73, 134, 6, 153,
        156, 66, 80, 244, 145, 239, 152, 122, 51, 84, 11, 67, 237, 207, 172, 98,
        228, 179, 28, 169, 201, 8, 232, 149, 128, 223, 148, 250, 117, 143, 63, 166,
        71, 7, 167, 252, 243, 115, 23, 186, 131, 89, 60, 25, 230, 133, 79, 168,
        104, 107, 129, 178, 113, 100, 218, 139, 248, 235, 15, 75, 112, 86, 157, 53,
        30, 36, 14, 94, 99, 88, 209, 162, 37, 34, 124, 59, 1, 33, 120, 135,
        212, 0, 70, 87, 159, 211, 39, 82, 76, 54, 2, 231, 160, 196, 200, 158,
        234, 191, 138, 210, 64, 199, 56, 181, 163, 247, 242, 206, 249, 97, 21, 161,
        224, 174, 93, 164, 155, 52, 26, 85, 173, 147, 50, 48, 245, 140, 177, 227,
        29, 246, 226, 46, 130, 102, 202, 96, 192, 41, 35, 171, 13, 83, 78, 111,
        213, 219, 55, 69, 222, 253, 142, 47, 3, 255, 106, 114, 109, 108, 91, 81,
        141, 27, 175, 146, 187, 221, 188, 127, 17, 217, 92, 65, 31, 16, 90, 216,
        10, 193, 49, 136, 165, 205, 123, 189, 45, 116, 208, 18, 184, 229, 180, 176,
        137, 105, 151, 74, 12, 150, 119, 126, 101, 185, 241, 9, 197, 110, 198, 132,
        24, 240, 125, 236, 58, 220, 77, 32, 121, 238, 95, 62, 215, 203, 57, 72,
    )

    // Round constants for SM4 (from JavaScript __ZH__.zse.zk)
    private val ROUND_CONSTANTS = intArrayOf(
        0x00070e15, 0x1c232a31, 0x383f464d, 0x545b6269,
        0x70777e85, 0x8c939aa1, 0xa8afb6bd, 0xc4cbd2d9,
        0xe0e7eef5, 0xfc030a11, 0x181f262d, 0x343b4249,
        0x50575e65, 0x6c737a81, 0x888f969d, 0xa4abb2b9,
        0xc0c7ced5, 0xdce3eaf1, 0xf8ff060d, 0x141b2229,
        0x30373e45, 0x4c535a61, 0x686f767d, 0x848b9299,
        0xa0a7aeb5, 0xbcc3cad1, 0xd8dfe6ed, 0xf4fb0209,
        0x10171e25, 0x2c333a41, 0x484f565d, 0x646b7279.toInt(),
    )

    /**
     * Main encryption function - matches JavaScript exports.encrypt
     *
     * @param input String to encrypt
     * @param timestamp Optional timestamp (for testing)
     * @return Base64-encoded encrypted string
     */
    fun encrypt(input: String, timestamp: Long? = null): String {
        // This implementation uses the ZseVM which executes the actual
        // bytecode-based encryption logic from the JavaScript
        val vm = ZseVM(timestamp ?: System.currentTimeMillis())
        return vm.encrypt(input)
    }

    /**
     * Encrypt with "2.0_" prefix (matching zse96 format)
     */
    fun encryptWithPrefix(input: String, timestamp: Long? = null): String {
        return "2.0_" + encrypt(input, timestamp)
    }

    // Internal helper functions matching JavaScript implementation

    internal fun encodeURIComponent(str: String): String {
        return java.net.URLEncoder.encode(str, "UTF-8")
            .replace("+", "%20")
            .replace("%21", "!")
            .replace("%27", "'")
            .replace("%28", "(")
            .replace("%29", ")")
            .replace("%7E", "~")
    }

    internal fun writeInt32BE(value: Int, data: ByteArray, offset: Int) {
        data[offset] = (value ushr 24).toByte()
        data[offset + 1] = (value ushr 16).toByte()
        data[offset + 2] = (value ushr 8).toByte()
        data[offset + 3] = value.toByte()
    }

    internal fun readInt32BE(data: ByteArray, offset: Int): Int {
        return ((data[offset].toInt() and 0xFF) shl 24) or
            ((data[offset + 1].toInt() and 0xFF) shl 16) or
            ((data[offset + 2].toInt() and 0xFF) shl 8) or
            (data[offset + 3].toInt() and 0xFF)
    }

    internal fun rotateLeft(value: Int, bits: Int): Int {
        return (value shl bits) or (value ushr (32 - bits))
    }

    /**
     * SM4 tau transformation (S-box + linear transformation)
     * Matches JavaScript G function
     */
    internal fun tau(value: Int): Int {
        val temp = ByteArray(4)
        val result = ByteArray(4)

        writeInt32BE(value, temp, 0)
        result[0] = SBOX[temp[0].toInt() and 0xFF].toByte()
        result[1] = SBOX[temp[1].toInt() and 0xFF].toByte()
        result[2] = SBOX[temp[2].toInt() and 0xFF].toByte()
        result[3] = SBOX[temp[3].toInt() and 0xFF].toByte()

        val ti = readInt32BE(result, 0)
        return ti xor rotateLeft(ti, 2) xor rotateLeft(ti, 10) xor
            rotateLeft(ti, 18) xor rotateLeft(ti, 24)
    }

    /**
     * SM4 round function - matches JavaScript __g.r
     */
    internal fun sm4Round(input: ByteArray): ByteArray {
        val output = ByteArray(16)
        val state = IntArray(36)

        state[0] = readInt32BE(input, 0)
        state[1] = readInt32BE(input, 4)
        state[2] = readInt32BE(input, 8)
        state[3] = readInt32BE(input, 12)

        for (i in 0 until 32) {
            val ta = tau(state[i + 1] xor state[i + 2] xor state[i + 3] xor ROUND_CONSTANTS[i])
            state[i + 4] = state[i] xor ta
        }

        writeInt32BE(state[35], output, 0)
        writeInt32BE(state[34], output, 4)
        writeInt32BE(state[33], output, 8)
        writeInt32BE(state[32], output, 12)

        return output
    }

    /**
     * XOR-based block encryption - matches JavaScript __g.x
     */
    internal fun xorEncrypt(input: ByteArray, key: ByteArray): ByteArray {
        val result = mutableListOf<Byte>()
        var keyState = key.copyOf()
        var blockIndex = 0

        while (blockIndex * 16 < input.size) {
            val remaining = input.size - blockIndex * 16
            val blockSize = minOf(16, remaining)

            val block = input.copyOfRange(blockIndex * 16, blockIndex * 16 + blockSize)
            val xorBlock = ByteArray(16)

            // XOR with key
            for (i in 0 until 16) {
                xorBlock[i] = if (i < block.size) {
                    (block[i].toInt() xor keyState[i].toInt()).toByte()
                } else {
                    keyState[i]
                }
            }

            // SM4 round
            keyState = sm4Round(xorBlock)
            result.addAll(keyState.toList())

            blockIndex++
        }

        return result.toByteArray()
    }
}
