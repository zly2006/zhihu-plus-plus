package com.github.zly2006.zhihu.shared.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.put

object ZhihuJson {
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    @Suppress("FunctionName")
    fun snakeCaseToCamelCase(snakeCase: String): String = snakeCase
        .split("_")
        .joinToString("") { it.replaceFirstChar { char -> char.uppercase() } }
        .replaceFirstChar { it.lowercase() }

    fun snakeCaseToCamelCase(json: JsonElement): JsonElement = when (json) {
        is JsonObject -> buildJsonObject {
            for ((key, value) in json) {
                put(snakeCaseToCamelCase(key), snakeCaseToCamelCase(value))
            }
        }
        is JsonArray -> buildJsonArray {
            for (item in json) {
                add(snakeCaseToCamelCase(item))
            }
        }
        else -> json
    }

    inline fun <reified T> decodeJson(json: JsonElement): T =
        this.json.decodeFromJsonElement(snakeCaseToCamelCase(json))

    fun <T> decodeJson(serializer: KSerializer<T>, json: JsonElement): T =
        this.json.decodeFromJsonElement(serializer, snakeCaseToCamelCase(json))
}
