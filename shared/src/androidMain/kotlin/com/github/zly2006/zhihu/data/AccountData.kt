/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.github.zly2006.zhihu.data

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.github.zly2006.zhihu.account.DEFAULT_ZHIHU_USER_AGENT
import com.github.zly2006.zhihu.account.ZhihuAccountProfileSnapshot
import com.github.zly2006.zhihu.account.ZhihuAccountSession
import com.github.zly2006.zhihu.account.androidZhihuAccountStore
import com.github.zly2006.zhihu.account.currentAndroidZhihuAccountStore
import com.github.zly2006.zhihu.data.Person
import com.github.zly2006.zhihu.data.ZhihuJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement

@SuppressLint("StaticFieldLeak")
object AccountData {
    val json = ZhihuJson.json

    internal val ANDROID_HEADERS = mapOf(
        "x-api-version" to "3.1.8",
        "x-app-version" to "10.61.0",
        "x-app-za" to
            "OS=Android&Release=12&Model=sdk_gphone64_arm64&VersionName=10.61.0&VersionCode=26107&Product=com.zhihu.android&Width=1440&Height=2952&Installer=%E7%81%B0%E5%BA%A6&DeviceType=AndroidPhone&Brand=google",
    )

    const val ANDROID_USER_AGENT = "com.zhihu.android/Futureve/10.61.0 Mozilla/5.0 (Linux; Android 12; sdk_gphone64_arm64 " +
        "Build/SE1A.220630.001.A1; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/57.0.1000.10 Mobile Safari/537.36"

    @Serializable
    data class Data(
        val login: Boolean = false,
        val username: String = "",
        val cookies: MutableMap<String, String> = mutableMapOf(),
        val userAgent: String = DEFAULT_ZHIHU_USER_AGENT,
        val self: Person? = null,
        val mobileAccessToken: String? = null,
        val mobileRefreshToken: String? = null,
        val mobileTokenType: String? = null,
        val mobileTokenExpiresAt: Long? = null,
    )

    fun loadData(context: Context): Data {
        androidZhihuAccountStore(context)
        return data
    }

    val data: Data
        get() = currentAndroidZhihuAccountStore().session.toAndroidData()

    fun saveData(context: Context, data: Data) {
        androidZhihuAccountStore(context).save(data.toSession())
    }

    private fun Data.toSession(): ZhihuAccountSession = ZhihuAccountSession(
        login = login,
        username = username,
        cookies = cookies.toMutableMap(),
        userAgent = userAgent,
        profile = self?.let {
            ZhihuAccountProfileSnapshot(
                id = it.id,
                name = it.name,
                urlToken = it.urlToken,
                userType = it.userType,
                avatarUrl = it.avatarUrl,
            )
        },
        self = self?.let { json.encodeToJsonElement(it) },
        mobileAccessToken = mobileAccessToken,
        mobileRefreshToken = mobileRefreshToken,
        mobileTokenType = mobileTokenType,
        mobileTokenExpiresAt = mobileTokenExpiresAt,
    )

    private fun ZhihuAccountSession.toAndroidData(): Data = Data(
        login = login,
        username = username,
        cookies = cookies.toMutableMap(),
        userAgent = userAgent,
        self = self?.let {
            runCatching {
                ZhihuJson.decodeJson<Person>(it)
            }.getOrNull()
        },
        mobileAccessToken = mobileAccessToken,
        mobileRefreshToken = mobileRefreshToken,
        mobileTokenType = mobileTokenType,
        mobileTokenExpiresAt = mobileTokenExpiresAt,
    )

    /**
     * 将snake_case的JSON转换为camelCase并解析为对象
     */
    internal inline fun <reified T> decodeJson(json: JsonElement): T {
        val convertedJson = ZhihuJson.snakeCaseToCamelCase(json)
        try {
            return this.json.decodeFromJsonElement<T>(convertedJson)
        } catch (e: SerializationException) {
            Log.e("AccountData", "Failed to parse JSON: $convertedJson", e)
            throw SerializationException("Failed to parse JSON: ${e.message}\n\n$convertedJson", e)
        }
    }

    class ZhPlusJsonSerializationException(
        val originalJson: JsonElement,
        message: String,
        cause: Throwable?,
    ) : SerializationException(message, cause)

    internal fun <T> decodeJson(serializer: KSerializer<T>, json: JsonElement): T {
        val convertedJson = ZhihuJson.snakeCaseToCamelCase(json)
        try {
            return this.json.decodeFromJsonElement(serializer, convertedJson)
        } catch (e: SerializationException) {
            throw ZhPlusJsonSerializationException(convertedJson, "Failed to parse JSON: ${e.message}", e)
        }
    }
}
