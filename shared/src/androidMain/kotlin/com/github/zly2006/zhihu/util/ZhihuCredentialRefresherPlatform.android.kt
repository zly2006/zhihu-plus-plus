package com.github.zly2006.zhihu.util

import java.nio.charset.StandardCharsets
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

actual fun hmacSha1Hex(key: String, message: String): String {
    val signingKey = SecretKeySpec(key.toByteArray(StandardCharsets.UTF_8), "HmacSHA1")
    val mac = Mac.getInstance("HmacSHA1")
    mac.init(signingKey)
    val rawHmac = mac.doFinal(message.toByteArray(StandardCharsets.UTF_8))
    return rawHmac.joinToString("") { "%02x".format(it) }
}
