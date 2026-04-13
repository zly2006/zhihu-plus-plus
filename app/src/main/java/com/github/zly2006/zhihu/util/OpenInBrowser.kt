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

package com.github.zly2006.zhihu.util

import android.content.Context
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.ui.Collection
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

object OpenInBrowser {
    suspend fun openUrlInBrowser(context: Context, destination: NavDestination): Boolean {
        val urlToken = AccountData.data.self?.urlToken ?: return false
        val jojo = AccountData.fetchGet(context, "https://www.zhihu.com/api/v4/people/$urlToken/collections?limit=50")!!
        val collection = AccountData
            .decodeJson<List<Collection>>(jojo["data"]!!)
            .firstOrNull { it.description == "com.github.zly2006.zhplus.openinbrowser" }
            ?: AccountData.decodeJson<Collection>(
                AccountData.fetchPost(context, "https://www.zhihu.com/api/v4/collections") {
                    contentType(ContentType.Application.Json)
                    setBody(
                        buildJsonObject {
                            put("title", "Zhihu++: 要在浏览器中打开的内容")
                            put("description", "com.github.zly2006.zhplus.openinbrowser")
                            put("is_public", false)
                        },
                    )
                    signFetchRequest()
                }!!["collection"]!!,
            )
        if (destination is Article) {
            val contentType = when (destination.type) {
                ArticleType.Answer -> "answer"
                ArticleType.Article -> "article"
            }
            val url = "https://api.zhihu.com/collections/contents/$contentType/${destination.id}"
            val body = "add_collections=${collection.id}"
            val response = AccountData.httpClient(context).put(url) {
                contentType(ContentType.Application.FormUrlEncoded)
                setBody(body)
            }
            return response.status.isSuccess()
        }
        return false
    }
}
