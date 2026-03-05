package com.github.zly2006.zhihu.util

import android.util.Log

object ZseRustSigner {
    private const val TAG = "ZseRustSigner"

    private var loaded = false

    init {
        loaded = try {
            System.loadLibrary("zsesigner")
            true
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to load libzsesigner", t)
            false
        }
    }

    @JvmStatic
    external fun encrypt(input: String, nowMs: Long): String

    fun encryptOrNull(input: String, nowMs: Long): String? {
        if (!loaded) return null
        return try {
            encrypt(input, nowMs)
        } catch (t: Throwable) {
            Log.e(TAG, "Rust signer failed", t)
            null
        }
    }
}
