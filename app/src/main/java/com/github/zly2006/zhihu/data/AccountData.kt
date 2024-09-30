package com.github.zly2006.zhihu.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

object AccountData {
    val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    @Serializable
    data class Data(
        val login: Boolean = false,
        val username: String = "",
        val cookies: Map<String, String> = mutableMapOf(),
    )

    private var data = Data()
    fun getData(context: Context): Data {
        val file = File(context.filesDir, "account.json")
        if (file.exists()) {
            data = json.decodeFromString<Data>(file.readText())
        }
        return data
    }

    fun saveData(context: Context) {
        val file = File(context.filesDir, "account.json")
        file.writeText(json.encodeToString(data))
    }
}
