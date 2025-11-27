package com.github.zly2006.zhihu.util

import com.github.zly2006.zhihu.ui.raiseForStatus
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.cookies.HttpCookies
import io.ktor.client.plugins.cookies.get
import io.ktor.client.plugins.pluginOrNull
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.Url
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.experimental.xor
import kotlin.random.Random

private object XZSE96V3 {
    // 头部操作密钥
    private val KEY_PAD = intArrayOf(48, 53, 57, 48, 53, 51, 102, 55, 100, 49, 53, 101, 48, 49, 100, 55)

    // 类似 Base64 的字符映射
    private const val BASE64_CHARS = "6fpLRqJO8M/c3jnYxFkUVC4ZIG12SiH=5v0mXDazWBTsuw7QetbKdoPyAl+hN9rgE"

    // 预计算的映射表 ZK
    private val MAPPING_ZK = intArrayOf(
        1170614578,
        1024848638,
        1413669199,
        -343334464,
        -766094290,
        -1373058082,
        -143119608,
        -297228157,
        1933479194,
        -971186181,
        -406453910,
        460404854,
        -547427574,
        -1891326262,
        -1679095901,
        2119585428,
        -2029270069,
        2035090028,
        -1521520070,
        -5587175,
        -77751101,
        -2094365853,
        -1243052806,
        1579901135,
        1321810770,
        456816404,
        -1391643889,
        -229302305,
        330002838,
        -788960546,
        363569021,
        -1947871109,
    )

    // 预计算的映射表 ZB (转为 IntArray 以提升性能)
    private val MAPPING_ZB = intArrayOf(
        20,
        223,
        245,
        7,
        248,
        2,
        194,
        209,
        87,
        6,
        227,
        253,
        240,
        128,
        222,
        91,
        237,
        9,
        125,
        157,
        230,
        93,
        252,
        205,
        90,
        79,
        144,
        199,
        159,
        197,
        186,
        167,
        39,
        37,
        156,
        198,
        38,
        42,
        43,
        168,
        217,
        153,
        15,
        103,
        80,
        189,
        71,
        191,
        97,
        84,
        247,
        95,
        36,
        69,
        14,
        35,
        12,
        171,
        28,
        114,
        178,
        148,
        86,
        182,
        32,
        83,
        158,
        109,
        22,
        255,
        94,
        238,
        151,
        85,
        77,
        124,
        254,
        18,
        4,
        26,
        123,
        176,
        232,
        193,
        131,
        172,
        143,
        142,
        150,
        30,
        10,
        146,
        162,
        62,
        224,
        218,
        196,
        229,
        1,
        192,
        213,
        27,
        110,
        56,
        231,
        180,
        138,
        107,
        242,
        187,
        54,
        120,
        19,
        44,
        117,
        228,
        215,
        203,
        53,
        239,
        251,
        127,
        81,
        11,
        133,
        96,
        204,
        132,
        41,
        115,
        73,
        55,
        249,
        147,
        102,
        48,
        122,
        145,
        106,
        118,
        74,
        190,
        29,
        16,
        174,
        5,
        177,
        129,
        63,
        113,
        99,
        31,
        161,
        76,
        246,
        34,
        211,
        13,
        60,
        68,
        207,
        160,
        65,
        111,
        82,
        165,
        67,
        169,
        225,
        57,
        112,
        244,
        155,
        51,
        236,
        200,
        233,
        58,
        61,
        47,
        100,
        137,
        185,
        64,
        17,
        70,
        234,
        163,
        219,
        108,
        170,
        166,
        59,
        149,
        52,
        105,
        24,
        212,
        78,
        173,
        45,
        0,
        116,
        226,
        119,
        136,
        206,
        135,
        175,
        195,
        25,
        92,
        121,
        208,
        126,
        139,
        3,
        75,
        141,
        21,
        130,
        98,
        241,
        40,
        154,
        66,
        184,
        49,
        181,
        46,
        243,
        88,
        101,
        183,
        8,
        23,
        72,
        188,
        104,
        179,
        210,
        134,
        250,
        201,
        164,
        89,
        216,
        202,
        220,
        50,
        221,
        152,
        140,
        33,
        235,
        214,
    )

    private fun pkcs7Pad(data: ByteArray, blockSize: Int = 16): ByteArray {
        val paddingLen = blockSize - (data.size % blockSize)
        val padding = ByteArray(paddingLen) { paddingLen.toByte() }
        return data + padding
    }

    private fun pkcs7Unpad(data: ByteArray): ByteArray {
        if (data.isEmpty()) return data
        val paddingLen = data.last().toInt() and 0xFF
        if (paddingLen > data.size || paddingLen == 0) return data
        return data.copyOfRange(0, data.size - paddingLen)
    }

    private fun rotateXor(x: Int, rot: Int): Int {
        // Kotlin 的 ushr 对应 Python 的无符号右移
        // (x shl rot) or (x ushr (32 - rot)) 即循环左移
        return (x shl rot) or (x ushr (32 - rot))
    }

    private fun transformValue(e: Int): Int {
        // 1. 打包成 4 字节并替换 (Mapping ZB)
        // Kotlin Int 是 BigEndian 存储逻辑上
        val b0 = (e ushr 24) and 0xFF
        val b1 = (e ushr 16) and 0xFF
        val b2 = (e ushr 8) and 0xFF
        val b3 = e and 0xFF

        val t0 = MAPPING_ZB[b0]
        val t1 = MAPPING_ZB[b1]
        val t2 = MAPPING_ZB[b2]
        val t3 = MAPPING_ZB[b3]

        val r = (t0 shl 24) or (t1 shl 16) or (t2 shl 8) or t3

        // 2. 异或计算
        return r xor rotateXor(r, 2) xor rotateXor(r, 10) xor rotateXor(r, 18) xor rotateXor(r, 24)
    }

    /**
     * 将 4 个 Int 转换为 16 字节的 ByteArray (Big Endian)
     */
    private fun intsToBytes(w0: Int, w1: Int, w2: Int, w3: Int): ByteArray {
        val buffer = ByteArray(16)
        var offset = 0
        for (w in listOf(w0, w1, w2, w3)) {
            buffer[offset++] = (w ushr 24).toByte()
            buffer[offset++] = (w ushr 16).toByte()
            buffer[offset++] = (w ushr 8).toByte()
            buffer[offset++] = w.toByte()
        }
        return buffer
    }

    /**
     * 将 16 字节 ByteArray 解析为 4 个 Int (Big Endian)
     */
    private fun bytesToInts(data: ByteArray): IntArray {
        val result = IntArray(4)
        for (i in 0 until 4) {
            val offset = i * 4
            result[i] = ((data[offset].toInt() and 0xFF) shl 24) or
                ((data[offset + 1].toInt() and 0xFF) shl 16) or
                ((data[offset + 2].toInt() and 0xFF) shl 8) or
                (data[offset + 3].toInt() and 0xFF)
        }
        return result
    }

    private fun transformBlock(data: ByteArray): ByteArray {
        val w = bytesToInts(data) // w0, w1, w2, w3
        val words = IntArray(36)
        System.arraycopy(w, 0, words, 0, 4)

        for (r in 0 until 32) {
            val temp = words[r + 1] xor words[r + 2] xor words[r + 3] xor MAPPING_ZK[r]
            words[r + 4] = words[r] xor transformValue(temp)
        }

        // 返回最后 4 个，顺序逆序: 35, 34, 33, 32
        return intsToBytes(words[35], words[34], words[33], words[32])
    }

    private fun reverseTransformBlock(data: ByteArray): ByteArray {
        val wRaw = bytesToInts(data)
        // 逆向时，传入的数据对应原 words[35], 34, 33, 32
        // 但 bytesToInts 按顺序解包，所以 wRaw[0]=words[35], wRaw[1]=words[34]...

        // 这里的 words 数组即使填满 36 个位置，主要操作是从后往前恢复
        val words = IntArray(36)
        // 恢复末尾
        words[35] = wRaw[0]
        words[34] = wRaw[1]
        words[33] = wRaw[2]
        words[32] = wRaw[3]

        // 逆向循环
        for (r in 31 downTo 0) {
            val temp = words[r + 1] xor words[r + 2] xor words[r + 3] xor MAPPING_ZK[r]
            words[r] = transformValue(temp) xor words[r + 4]
        }

        return intsToBytes(words[0], words[1], words[2], words[3])
    }

    private fun processBlocks(data: ByteArray, iv: ByteArray): ByteArray {
        val output = ArrayList<Byte>()
        var currentChain = iv

        for (i in data.indices step 16) {
            val chunk = data.copyOfRange(i, (i + 16).coerceAtMost(data.size))
            // 补齐 16 位在外部通常已经做了，但这里 chunk_list 逻辑
            val chunkBytes = if (chunk.size < 16) chunk + ByteArray(16 - chunk.size) else chunk

            val xored = ByteArray(16)
            for (j in 0 until 16) {
                xored[j] = chunkBytes[j] xor currentChain[j]
            }

            currentChain = transformBlock(xored)
            for (b in currentChain) output.add(b)
        }
        return output.toByteArray()
    }

    private fun reverseProcessBlocks(data: ByteArray, iv: ByteArray): ByteArray {
        val output = ArrayList<Byte>()
        var prevChain = iv

        for (i in data.indices step 16) {
            val chunk = data.copyOfRange(i, (i + 16).coerceAtMost(data.size))
            if (chunk.size < 16) break // 应该总是16倍数

            val decryptedBlock = reverseTransformBlock(chunk)
            val plainBlock = ByteArray(16)
            for (j in 0 until 16) {
                plainBlock[j] = decryptedBlock[j] xor prevChain[j]
            }

            for (b in plainBlock) output.add(b)
            prevChain = chunk
        }
        return output.toByteArray()
    }

    fun b64encode(md5Bytes: ByteArray, device: Int = 0, seed: Int = 63): String {
        // 1. 构造 Header
        val header = byteArrayOf(seed.toByte(), device.toByte()) + md5Bytes
        val padded = pkcs7Pad(header) // 应该是 16 的倍数

        val headerBlock = padded.copyOfRange(0, 16)

        // 混淆 Header
        val transformedHeaderInput = ByteArray(16)
        for (i in 0 until 16) {
            val b = headerBlock[i].toInt() and 0xFF
            val k = KEY_PAD[i]
            transformedHeaderInput[i] = (b xor k xor 42).toByte()
        }

        val iv = transformBlock(transformedHeaderInput)
        val body = padded.copyOfRange(16, padded.size)
        val transformedBody = processBlocks(body, iv)

        // Combined
        val combined = ArrayList<Byte>()
        for (b in iv) combined.add(b)
        for (b in transformedBody) combined.add(b)

        // 补齐 3 的倍数
        val paddingCount = (3 - combined.size % 3) % 3
        repeat(paddingCount) { combined.add(0) }

        val result = StringBuilder()
        var shiftCounter = 0

        // 倒序处理，步长 -3
        // range(len - 1, 1, -3) in Python means end at index 2 (exclusive of 1)
        for (i in combined.size - 1 downTo 2 step 3) {
            val b0Val = combined[i].toInt() and 0xFF
            val b1Val = combined[i - 1].toInt() and 0xFF
            val b2Val = combined[i - 2].toInt() and 0xFF

            // Python: combined[i] ^ (58 >> ...)
            // logical shift in python for signed int usually implies mimicking unsigned if dealing with bits,
            // but here 58 is positive so >> is fine.
            val x0 = b0Val xor (58 ushr (8 * (shiftCounter % 4)))
            shiftCounter++
            val x1 = b1Val xor (58 ushr (8 * (shiftCounter % 4)))
            shiftCounter++
            val x2 = b2Val xor (58 ushr (8 * (shiftCounter % 4)))
            shiftCounter++

            val num = x0 or (x1 shl 8) or (x2 shl 16)

            result.append(BASE64_CHARS[num and 63])
            result.append(BASE64_CHARS[(num ushr 6) and 63])
            result.append(BASE64_CHARS[(num ushr 12) and 63])
            result.append(BASE64_CHARS[(num ushr 18) and 63])
        }

        return result.toString()
    }
}

/**
 * 知乎 Token 刷新业务逻辑
 */
object ZhihuCredentialRefresher {
    private const val CLIENT_ID = "c3cef7c66a1843f8b3a9e6a1e3160e20"
    private const val CLIENT_SECRET = "d1b964811afb40118a12068ff74a12f4"
    private const val GRANT_TYPE = "refresh_token"
    private const val SOURCE = "com.zhihu.web"

    private fun hmacSha1(key: String, message: String): String {
        val signingKey = SecretKeySpec(key.toByteArray(StandardCharsets.UTF_8), "HmacSHA1")
        val mac = Mac.getInstance("HmacSHA1")
        mac.init(signingKey)
        val rawHmac = mac.doFinal(message.toByteArray(StandardCharsets.UTF_8))
        // Hex encode
        return rawHmac.joinToString("") { "%02x".format(it) }
    }

    private fun generateRefreshPayload(refreshToken: String, timestamp: Long): Map<String, String> {
        val message = "$GRANT_TYPE$CLIENT_ID$SOURCE$timestamp"
        val signature = hmacSha1(CLIENT_SECRET, message)

        return mapOf(
            "client_id" to CLIENT_ID,
            "grant_type" to GRANT_TYPE,
            "timestamp" to timestamp.toString(),
            "source" to SOURCE,
            "signature" to signature,
            "refresh_token" to refreshToken,
        )
    }

    suspend fun fetchRefreshToken(httpClient: HttpClient): String {
        val jojo = httpClient
            .post("https://www.zhihu.com/api/account/prod/token/refresh") {
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36")
                header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")
                header("Origin", "https://www.zhihu.com")
                header("Referer", "https://www.zhihu.com/signin")
                header("x-requested-with", "fetch")
            }.raiseForStatus()
            .body<JsonObject>()
        return jojo["refresh_token"]!!.jsonPrimitive.content
    }

    /**
     * 执行刷新 Token 操作
     */
    suspend fun refreshZhihuToken(refreshToken: String, httpClient: HttpClient): String {
        httpClient.pluginOrNull(HttpCookies)?.get(Url("https://www.zhihu.com/"))?.get("z_c0")
            ?: throw IllegalArgumentException("刷新失败：缺失关键 cookie z_c0，请重新登录")

        val timestamp = System.currentTimeMillis()
        val payloadMap = generateRefreshPayload(refreshToken, timestamp)
        println("请求原始数据: $payloadMap")

        val formData = payloadMap.entries.joinToString("&") {
            "${URLEncoder.encode(it.key, "UTF-8")}=${URLEncoder.encode(it.value, "UTF-8")}"
        }
        val dataBytes = formData.toByteArray(StandardCharsets.UTF_8)

        val seed = Random.nextInt(0, 256)
        val encryptedData = XZSE96V3.b64encode(dataBytes, device = 0, seed = seed)

        val jojo = httpClient
            .post("https://www.zhihu.com/api/v3/oauth/sign_in") {
                header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Safari/537.36")
                header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")
                header("Origin", "https://www.zhihu.com")
                header("Referer", "https://www.zhihu.com/signin")
                header("x-zse-83", "3_3.0")
                header("x-requested-with", "fetch")
                setBody(encryptedData.toByteArray(StandardCharsets.UTF_8))
            }.raiseForStatus()
            .body<JsonObject>()

        return jojo["access_token"]!!.jsonPrimitive.content
    }
}
