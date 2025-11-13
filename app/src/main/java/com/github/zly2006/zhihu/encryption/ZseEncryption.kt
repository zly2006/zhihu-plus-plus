package com.github.zly2006.zhihu.encryption

/**
 * Kotlin implementation of ZSE v4 encryption algorithm
 * Ported from zse-v4.js
 *
 * This provides a clean API over the ZseVM implementation.
 *
 * For production use, consider using ZseEncryptionWrapper which calls
 * the JavaScript implementation through WebView for guaranteed 100% compatibility.
 */
object ZseEncryption {
    const val VERSION = "3.0"

    /**
     * Main encryption function
     *
     * @param input String to encrypt
     * @param timestamp Optional timestamp for deterministic encryption
     * @return Encrypted string
     */
    fun encrypt(input: String, timestamp: Long? = null): String {
        val vm = ZseVM(timestamp ?: System.currentTimeMillis())
        return vm.encrypt(input)
    }

    /**
     * Encrypt with "2.0+" prefix (matching zse96 format)
     */
    fun encryptWithPrefix(input: String, timestamp: Long? = null): String {
        return "2.0_" + encrypt(input, timestamp)
    }
}
