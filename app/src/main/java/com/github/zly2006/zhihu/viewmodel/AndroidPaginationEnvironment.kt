/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
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

package com.github.zly2006.zhihu.viewmodel

import android.app.Activity
import android.app.AlertDialog
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.github.zly2006.zhihu.BuildConfig
import com.github.zly2006.zhihu.LoginActivity
import com.github.zly2006.zhihu.MainActivity
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.AccountData.json
import com.github.zly2006.zhihu.shared.util.HttpStatusException
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import com.github.zly2006.zhihu.util.clipboardManager
import com.github.zly2006.zhihu.util.signFetchRequest
import io.ktor.client.HttpClient
import io.ktor.client.plugins.UserAgent
import io.ktor.client.plugins.cache.HttpCache
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal class AndroidPaginationEnvironment(
    private val context: Context,
    private val allowGuestAccess: Boolean,
) : PaginationEnvironment {
    override fun httpClient(): HttpClient {
        val preferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        val loginForRecommendation = preferences.getBoolean("loginForRecommendation", true)
        if (allowGuestAccess && !loginForRecommendation) {
            return HttpClient {
                install(HttpCache)
                install(ContentNegotiation) {
                    json(json)
                }
                install(UserAgent) {
                    agent = AccountData.data.userAgent
                }
            }
        }
        if (context is MainActivity) {
            return context.httpClient
        }
        return AccountData.httpClient(context)
    }

    override suspend fun fetchJson(
        url: String,
        include: String,
    ): JsonObject? =
        AccountData.fetchGet(context, url) {
            addIncludeAndSign(include)
        }

    override fun logDecodeFailure(
        tag: String?,
        item: JsonElement,
        error: Exception,
    ) {
        Log.e(tag, "Failed to decode item: $item", error)
    }

    override suspend fun handleFetchFailure(
        tag: String?,
        error: Exception,
    ) {
        if (error is HttpStatusException && BuildConfig.DEBUG) {
            Log.e(tag, "Response: ${error.bodyText}", error)
            if (tryShowLoginExpiredDialog(error)) {
                return
            }
            showDebugErrorDialog(error)
        }
        Log.e(tag, "Failed to fetch feeds", error)
        context.mainExecutor.execute {
            Toast.makeText(context, "加载失败: ${error.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun tryShowLoginExpiredDialog(error: HttpStatusException): Boolean {
        try {
            val jojo = json.parseToJsonElement(error.bodyText)
            if (jojo.jsonObject["error"]!!
                    .jsonObject["code"]!!
                    .jsonPrimitive.int == 100 &&
                jojo.jsonObject["error"]!!
                    .jsonObject["message"]!!
                    .jsonPrimitive.content == "ERR_TICKET_NOT_EXIST"
            ) {
                context.mainExecutor.execute {
                    if (context.canSafelyShowDialog()) {
                        AlertDialog
                            .Builder(context)
                            .setTitle("登录已过期")
                            .setMessage("请重新登录以继续使用完整功能。")
                            .setPositiveButton("重新登录") { _, _ ->
                                AccountData.delete(context)
                                context.startActivity(Intent(context, LoginActivity::class.java))
                            }.setNegativeButton("取消", null)
                            .show()
                    }
                }
                return true
            }
        } catch (_: Exception) {
        }
        return false
    }

    private fun showDebugErrorDialog(error: HttpStatusException) {
        context.mainExecutor.execute {
            if (context.canSafelyShowDialog()) {
                AlertDialog
                    .Builder(context)
                    .setTitle("错误 ${error.status}")
                    .setMessage(error.bodyText)
                    .setNeutralButton("复制curl") { _, _ ->
                        val curl = error.dumpedCurlRequest
                        context.clipboardManager
                            .setPrimaryClip(
                                ClipData.newPlainText(
                                    "curl",
                                    curl,
                                ),
                            )
                        Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
                    }.show()
            }
        }
    }
}

private fun HttpRequestBuilder.addIncludeAndSign(include: String) {
    url {
        if (include.isNotEmpty()) {
            parameters["include"] = include
        }
    }
    signFetchRequest()
}

private fun Context.canSafelyShowDialog(): Boolean {
    val activity = this as? Activity ?: return false
    if (activity.isFinishing || activity.isDestroyed) return false
    val lifecycleOwner = activity as? LifecycleOwner ?: return true
    return lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
}
