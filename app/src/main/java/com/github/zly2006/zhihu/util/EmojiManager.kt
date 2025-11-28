package com.github.zly2006.zhihu.util

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.unit.em
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/**
 * 管理知乎emoji表情的加载和缓存
 */
object EmojiManager {
    private const val TAG = "EmojiManager"
    private const val EMOJI_API_URL = "https://www.zhihu.com/api/v4/sticker-groups/1114161698310770688"
    private const val EMOJI_MAPPING_FILE = "emoji_mapping.json"
    private const val EMOJI_CACHE_VERSION_FILE = "emoji_version.txt"
    private const val CURRENT_EMOJI_VERSION = "1" // 增加版本号可强制重新下载

    // emoji占位符到文件名的映射
    private val emojiMapping = mutableMapOf<String, String>()

    // emoji文件名到本地缓存路径的映射
    private val emojiCache = mutableMapOf<String, String>()

    private var isInitialized = false

    /**
     * 初始化emoji管理器，从Zhihu API动态下载emoji数据
     */
    suspend fun initialize(context: Context) = withContext(Dispatchers.IO) {
        if (isInitialized) return@withContext

        try {
            val emojiDir = File(context.filesDir, "emojis")
            if (!emojiDir.exists()) {
                emojiDir.mkdirs()
            }

            val mappingFile = File(context.filesDir, EMOJI_MAPPING_FILE)
            val versionFile = File(context.filesDir, EMOJI_CACHE_VERSION_FILE)

            // 检查是否需要重新下载
            val needsDownload = !mappingFile.exists() ||
                !versionFile.exists() ||
                versionFile.readText().trim() != CURRENT_EMOJI_VERSION

            if (needsDownload) {
                Log.i(TAG, "Downloading emoji data from Zhihu API...")
                downloadAndCacheEmojis(emojiDir, mappingFile, versionFile)
            } else {
                Log.i(TAG, "Loading emoji data from cache...")
                loadEmojiMapping(mappingFile, emojiDir)
            }

            Log.i(TAG, "Emoji manager initialized with ${emojiMapping.size} emojis")
            isInitialized = true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize emoji manager", e)
            // 尝试加载本地缓存
            try {
                val mappingFile = File(context.filesDir, EMOJI_MAPPING_FILE)
                val emojiDir = File(context.filesDir, "emojis")
                if (mappingFile.exists()) {
                    loadEmojiMapping(mappingFile, emojiDir)
                    isInitialized = true
                    Log.i(TAG, "Loaded emoji from cache after download failure")
                }
            } catch (e2: Exception) {
                Log.e(TAG, "Failed to load emoji from cache", e2)
            }
        }
    }

    /**
     * 从Zhihu API下载emoji数据并缓存到本地
     */
    private fun downloadAndCacheEmojis(
        emojiDir: File,
        mappingFile: File,
        versionFile: File,
    ) {
        val url = URL(EMOJI_API_URL)
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 10000
        connection.readTimeout = 10000

        try {
            val responseCode = connection.responseCode
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw Exception("HTTP error code: $responseCode")
            }

            val response = connection.inputStream.bufferedReader().use { it.readText() }
            val json = Json { ignoreUnknownKeys = true }
            val jsonObject = json.parseToJsonElement(response).jsonObject
            val stickers = jsonObject["data"]?.jsonObject?.get("stickers")?.jsonArray
                ?: throw Exception("Invalid response format")

            val newMapping = mutableMapOf<String, String>()
            var downloadCount = 0

            stickers.forEach { stickerElement ->
                val sticker = stickerElement.jsonObject
                val id = sticker["id"]?.jsonPrimitive?.content ?: return@forEach
                val placeholder = sticker["placeholder"]?.jsonPrimitive?.content ?: return@forEach
                val staticImageUrl = sticker["static_image_url"]?.jsonPrimitive?.content ?: return@forEach

                val fileName = "emoji_$id.png"
                newMapping[placeholder] = fileName

                // 解析base64图片数据
                if (staticImageUrl.startsWith("data:image/png;base64,")) {
                    val base64Data = staticImageUrl.substring("data:image/png;base64,".length)
                    val imageBytes = Base64.decode(base64Data, Base64.DEFAULT)

                    val targetFile = File(emojiDir, fileName)
                    targetFile.outputStream().use { it.write(imageBytes) }

                    emojiCache[fileName] = targetFile.absolutePath
                    downloadCount++
                }
            }

            // 保存映射文件
            val mappingJson = json.encodeToString(
                kotlinx.serialization.serializer<Map<String, String>>(),
                newMapping,
            )
            mappingFile.writeText(mappingJson)

            // 保存版本文件
            versionFile.writeText(CURRENT_EMOJI_VERSION)

            emojiMapping.clear()
            emojiMapping.putAll(newMapping)

            Log.i(TAG, "Downloaded and cached $downloadCount emojis")
        } finally {
            connection.disconnect()
        }
    }

    /**
     * 从本地缓存加载emoji映射
     */
    private fun loadEmojiMapping(mappingFile: File, emojiDir: File) {
        val json = Json { ignoreUnknownKeys = true }
        val mappingJson = mappingFile.readText()
        val mapping = json.decodeFromString<Map<String, String>>(mappingJson)

        emojiMapping.clear()
        emojiMapping.putAll(mapping)

        emojiCache.clear()
        mapping.values.forEach { fileName ->
            val file = File(emojiDir, fileName)
            if (file.exists()) {
                emojiCache[fileName] = file.absolutePath
            }
        }
    }

    /**
     * 根据emoji占位符获取本地文件路径
     * @param placeholder emoji占位符
     * @return 本地文件路径，如果emoji不存在返回null
     */
    fun getEmojiPath(placeholder: String): String? {
        val fileName = emojiMapping[placeholder] ?: return null
        return emojiCache[fileName]
    }

    /**
     * 根据emoji文件名获取本地文件路径
     * @param fileName emoji文件名，例如 "emoji_1114211823741685761.png"
     * @return 本地文件路径，如果emoji不存在返回null
     */
    fun getEmojiPathByFileName(fileName: String): String? = emojiCache[fileName]

    /**
     * 创建InlineTextContent映射，用于Text组件显示emoji
     */
    fun createInlineContentMap(emojis: Set<String>): Map<String, InlineTextContent> {
        return emojis
            .mapNotNull { emojiKey ->
                val path = getEmojiPathByFileName(emojiKey.removePrefix("emoji_")) ?: return@mapNotNull null

                emojiKey to InlineTextContent(
                    placeholder = Placeholder(
                        width = 1.3.em,
                        height = 1.3.em,
                        placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
                    ),
                ) {
                    val bitmap = BitmapFactory.decodeFile(path)
                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = emojiKey,
                            modifier = Modifier,
                        )
                    }
                }
            }.toMap()
    }
}
