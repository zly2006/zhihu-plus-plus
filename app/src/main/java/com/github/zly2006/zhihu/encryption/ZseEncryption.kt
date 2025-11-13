package com.github.zly2006.zhihu.encryption

import android.util.Base64
import kotlin.experimental.and
import kotlin.experimental.or
import kotlin.experimental.xor

/**
 * Kotlin implementation of ZSE v4 encryption algorithm
 * Ported from zse-v4.js
 * 
 * This is a complex encryption algorithm that uses:
 * - SM4 block cipher
 * - Custom virtual machine for bytecode execution
 * - XOR operations and transformations
 */
object ZseEncryption {
    
    const val VERSION = "3.0"
    
    // SM4 S-Box (substitution box used in SM4 encryption)
    private val SM4_SBOX = intArrayOf(
        0xd6, 0x90, 0xe9, 0xfe, 0xcc, 0xe1, 0x3d, 0xb7, 0x16, 0xb6, 0x14, 0xc2, 0x28, 0xfb, 0x2c, 0x05,
        0x2b, 0x67, 0x9a, 0x76, 0x2a, 0xbe, 0x04, 0xc3, 0xaa, 0x44, 0x13, 0x26, 0x49, 0x86, 0x06, 0x99,
        0x9c, 0x42, 0x50, 0xf4, 0x91, 0xef, 0x98, 0x7a, 0x33, 0x54, 0x0b, 0x43, 0xed, 0xcf, 0xac, 0x62,
        0xe4, 0xb3, 0x1c, 0xa9, 0xc9, 0x08, 0xe8, 0x95, 0x80, 0xdf, 0x94, 0xfa, 0x75, 0x8f, 0x3f, 0xa6,
        0x47, 0x07, 0xa7, 0xfc, 0xf3, 0x73, 0x17, 0xba, 0x83, 0x59, 0x3c, 0x19, 0xe6, 0x85, 0x4f, 0xa8,
        0x68, 0x6b, 0x81, 0xb2, 0x71, 0x64, 0xda, 0x8b, 0xf8, 0xeb, 0x0f, 0x4b, 0x70, 0x56, 0x9d, 0x35,
        0x1e, 0x24, 0x0e, 0x5e, 0x63, 0x58, 0xd1, 0xa2, 0x25, 0x22, 0x7c, 0x3b, 0x01, 0x21, 0x78, 0x87,
        0xd4, 0x00, 0x46, 0x57, 0x9f, 0xd3, 0x27, 0x52, 0x4c, 0x36, 0x02, 0xe7, 0xa0, 0xc4, 0xc8, 0x9e,
        0xea, 0xbf, 0x8a, 0xd2, 0x40, 0xc7, 0x38, 0xb5, 0xa3, 0xf7, 0xf2, 0xce, 0xf9, 0x61, 0x15, 0xa1,
        0xe0, 0xae, 0x5d, 0xa4, 0x9b, 0x34, 0x1a, 0x55, 0xad, 0x93, 0x32, 0x30, 0xf5, 0x8c, 0xb1, 0xe3,
        0x1d, 0xf6, 0xe2, 0x2e, 0x82, 0x66, 0xca, 0x60, 0xc0, 0x29, 0x23, 0xab, 0x0d, 0x53, 0x4e, 0x6f,
        0xd5, 0xdb, 0x37, 0x45, 0xde, 0xfd, 0x8e, 0x2f, 0x03, 0xff, 0x6a, 0x72, 0x6d, 0x6c, 0x5b, 0x51,
        0x8d, 0x1b, 0xaf, 0x92, 0xbb, 0xdd, 0xbc, 0x7f, 0x11, 0xd9, 0x5c, 0x41, 0x1f, 0x10, 0x5a, 0xd8,
        0x0a, 0xc1, 0x31, 0x88, 0xa5, 0xcd, 0x7b, 0xbd, 0x2d, 0x74, 0xd0, 0x12, 0xb8, 0xe5, 0xb4, 0xb0,
        0x89, 0x69, 0x97, 0x4a, 0x0c, 0x96, 0x77, 0x7e, 0x65, 0xb9, 0xf1, 0x09, 0xc5, 0x6e, 0xc6, 0x84,
        0x18, 0xf0, 0x7d, 0xec, 0x3a, 0xdc, 0x4d, 0x20, 0x79, 0xee, 0x5f, 0x3e, 0xd7, 0xcb, 0x39, 0x48
    )
    
    // SM4 encryption round constants
    private val SM4_CK = intArrayOf(
        0x00070e15, 0x1c232a31, 0x383f464d, 0x545b6269,
        0x70777e85.toInt(), 0x8c939aa1.toInt(), 0xa8afb6bd.toInt(), 0xc4cbd2d9.toInt(),
        0xe0e7eef5.toInt(), 0xfc030a11.toInt(), 0x181f262d, 0x343b4249,
        0x50575e65, 0x6c737a81.toInt(), 0x888f969d.toInt(), 0xa4abb2b9.toInt(),
        0xc0c7ced5.toInt(), 0xdce3eaf1.toInt(), 0xf8ff060d.toInt(), 0x141b2229,
        0x30373e45, 0x4c535a61, 0x686f767d, 0x848b9299.toInt(),
        0xa0a7aeb5.toInt(), 0xbcc3cad1.toInt(), 0xd8dfe6ed.toInt(), 0xf4fb0209.toInt(),
        0x10171e25, 0x2c333a41, 0x484f565d, 0x646b7279
    )
    
    /**
     * Write a 32-bit integer to a byte array in big-endian format
     */
    private fun writeInt32BE(value: Int, buffer: ByteArray, offset: Int) {
        buffer[offset] = ((value ushr 24) and 0xFF).toByte()
        buffer[offset + 1] = ((value ushr 16) and 0xFF).toByte()
        buffer[offset + 2] = ((value ushr 8) and 0xFF).toByte()
        buffer[offset + 3] = (value and 0xFF).toByte()
    }
    
    /**
     * Read a 32-bit integer from a byte array in big-endian format
     */
    private fun readInt32BE(buffer: ByteArray, offset: Int): Int {
        return ((buffer[offset].toInt() and 0xFF) shl 24) or
                ((buffer[offset + 1].toInt() and 0xFF) shl 16) or
                ((buffer[offset + 2].toInt() and 0xFF) shl 8) or
                (buffer[offset + 3].toInt() and 0xFF)
    }
    
    /**
     * Rotate left operation
     */
    private fun rotl(value: Int, bits: Int): Int {
        return (value shl bits) or (value ushr (32 - bits))
    }
    
    /**
     * SM4 tau transformation using S-Box
     */
    private fun tau(value: Int): Int {
        val temp = ByteArray(4)
        val result = ByteArray(4)
        
        writeInt32BE(value, temp, 0)
        result[0] = SM4_SBOX[temp[0].toInt() and 0xFF].toByte()
        result[1] = SM4_SBOX[temp[1].toInt() and 0xFF].toByte()
        result[2] = SM4_SBOX[temp[2].toInt() and 0xFF].toByte()
        result[3] = SM4_SBOX[temp[3].toInt() and 0xFF].toByte()
        
        val ti = readInt32BE(result, 0)
        return ti xor rotl(ti, 2) xor rotl(ti, 10) xor rotl(ti, 18) xor rotl(ti, 24)
    }
    
    /**
     * SM4 block cipher encryption (one block = 16 bytes)
     */
    private fun sm4EncryptBlock(input: ByteArray): ByteArray {
        val output = ByteArray(16)
        val x = IntArray(36)
        
        // Read input as 4 32-bit integers
        x[0] = readInt32BE(input, 0)
        x[1] = readInt32BE(input, 4)
        x[2] = readInt32BE(input, 8)
        x[3] = readInt32BE(input, 12)
        
        // 32 rounds of SM4 encryption
        for (i in 0 until 32) {
            val ta = tau(x[i + 1] xor x[i + 2] xor x[i + 3] xor SM4_CK[i])
            x[i + 4] = x[i] xor ta
        }
        
        // Write output in reverse order
        writeInt32BE(x[35], output, 0)
        writeInt32BE(x[34], output, 4)
        writeInt32BE(x[33], output, 8)
        writeInt32BE(x[32], output, 12)
        
        return output
    }
    
    /**
     * XOR-based encryption with SM4
     * Matches the __g.x function in JavaScript
     */
    private fun xorEncrypt(data: ByteArray, initialKey: ByteArray): ByteArray {
        val result = mutableListOf<Byte>()
        var key = initialKey.copyOf(16)
        var offset = 0
        
        while (offset < data.size) {
            // Process 16-byte blocks
            val blockSize = minOf(16, data.size - offset)
            val xorBlock = ByteArray(16)
            
            // XOR data with key
            for (i in 0 until blockSize) {
                xorBlock[i] = (data[offset + i].toInt() xor key[i].toInt()).toByte()
            }
            
            // Encrypt the XOR'd block to get next key
            key = sm4EncryptBlock(xorBlock)
            
            // Add encrypted block to result
            for (i in 0 until blockSize) {
                result.add(key[i])
            }
            
            offset += 16
        }
        
        return result.toByteArray()
    }
    
    /**
     * Main encryption function
     * This matches the D function in the JavaScript code
     * which calls __g._encrypt(encodeURIComponent(input))
     * 
     * Note: __g._encrypt is defined by the VM at runtime,
     * so this implementation attempts to replicate its behavior
     * based on the visible __g.x and __g.r functions
     */
    fun encrypt(input: String): String {
        // The JavaScript uses encodeURIComponent which is similar to URLEncoder
        // but not exactly the same. JavaScript's encodeURIComponent doesn't encode:
        // A-Z a-z 0-9 - _ . ! ~ * ' ( )
        // while URLEncoder encodes * and ~
        val encoded = encodeURIComponent(input)
        val inputBytes = encoded.toByteArray(Charsets.UTF_8)
        
        // Use a 16-byte zero key as initial value
        // This matches the initialization in the JavaScript
        val key = ByteArray(16) { 0 }
        
        // Perform XOR encryption (matches __g.x)
        val encrypted = xorEncrypt(inputBytes, key)
        
        // Base64 encode the result
        return Base64.encodeToString(encrypted, Base64.NO_WRAP)
    }
    
    /**
     * JavaScript's encodeURIComponent equivalent
     * Encodes everything except: A-Z a-z 0-9 - _ . ! ~ * ' ( )
     */
    private fun encodeURIComponent(str: String): String {
        val unreserved = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_.!~*'()"
        val result = StringBuilder()
        
        for (char in str) {
            if (char in unreserved) {
                result.append(char)
            } else {
                // Encode as UTF-8 bytes then to %XX format
                val bytes = char.toString().toByteArray(Charsets.UTF_8)
                for (byte in bytes) {
                    result.append('%')
                    result.append(String.format("%02X", byte.toInt() and 0xFF))
                }
            }
        }
        
        return result.toString()
    }
}
