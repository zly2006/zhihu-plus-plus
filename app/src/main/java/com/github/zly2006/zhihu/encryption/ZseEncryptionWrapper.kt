package com.github.zly2006.zhihu.encryption

import android.webkit.WebView
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Production-ready wrapper for ZSE encryption that uses the original
 * JavaScript implementation through WebView.
 * 
 * This ensures 100% compatibility with the original zse-v4.js implementation
 * while providing a Kotlin-friendly async API.
 * 
 * Usage:
 * ```kotlin
 * val wrapper = ZseEncryptionWrapper(webView)
 * val encrypted = wrapper.encrypt("input_string")
 * ```
 */
class ZseEncryptionWrapper(private val webView: WebView) {
    
    /**
     * Encrypts the input string using the JavaScript ZSE implementation
     * 
     * @param input The string to encrypt (typically an MD5 hash)
     * @return Base64-encoded encrypted result
     */
    suspend fun encrypt(input: String): String = withContext(Dispatchers.Main) {
        val future = CompletableDeferred<String>()
        
        webView.evaluateJavascript("exports.encrypt('$input')") { result ->
            // Remove surrounding quotes from the JavaScript string result
            val cleanResult = result.trim('"')
            future.complete(cleanResult)
        }
        
        future.await()
    }
    
    /**
     * Encrypts the input string and returns it with the "2.0_" prefix
     * as used in MainActivity.signRequest96
     * 
     * @param input The string to encrypt
     * @return Encrypted result with "2.0_" prefix
     */
    suspend fun encryptWithPrefix(input: String): String {
        val encrypted = encrypt(input)
        return "2.0_$encrypted"
    }
}
