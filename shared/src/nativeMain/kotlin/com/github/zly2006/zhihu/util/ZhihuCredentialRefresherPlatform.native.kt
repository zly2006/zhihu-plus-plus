package com.github.zly2006.zhihu.util

actual fun hmacSha1Hex(key: String, message: String): String {
    error("hmacSha1Hex is not implemented for iOS yet")
} // TODO: iOS HMAC-SHA1 实现 (需要 CommonCrypto interop)
