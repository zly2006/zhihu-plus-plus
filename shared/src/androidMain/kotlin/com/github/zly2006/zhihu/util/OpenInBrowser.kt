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
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.shared.data.decodeZhihuCollection
import com.github.zly2006.zhihu.shared.data.decodeZhihuCollectionList
import com.github.zly2006.zhihu.viewmodel.zhihuCollectionContentUrl
import com.github.zly2006.zhihu.viewmodel.zhihuCollectionCreateBody
import com.github.zly2006.zhihu.viewmodel.zhihuCollectionsUrl
import com.github.zly2006.zhihu.viewmodel.zhihuPeopleCollectionsUrl
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import io.ktor.http.isSuccess

object OpenInBrowser {
    suspend fun openUrlInBrowser(context: Context, destination: NavDestination): Boolean {
        val urlToken = AccountData.data.self?.urlToken ?: return false
        val jojo = AccountData.signedFetchGet(context, zhihuPeopleCollectionsUrl(urlToken, limit = 50))!!
        val collection = decodeZhihuCollectionList(jojo["data"]!!)
            .firstOrNull { it.description == "com.github.zly2006.zhplus.openinbrowser" }
            ?: decodeZhihuCollection(
                AccountData.signedFetchPost(context, zhihuCollectionsUrl()) {
                    contentType(ContentType.Application.Json)
                    setBody(
                        zhihuCollectionCreateBody(
                            title = "Zhihu++: 要在浏览器中打开的内容",
                            description = "com.github.zly2006.zhplus.openinbrowser",
                            isPublic = false,
                        ),
                    )
                }!!["collection"]!!,
            )
        if (destination is Article) {
            val contentType = when (destination.type) {
                ArticleType.Answer -> "answer"
                ArticleType.Article -> "article"
            }
            val url = zhihuCollectionContentUrl(contentType, destination.id)
            val body = "add_collections=${collection.id}"
            return AccountData.withAuthenticatedResponse(
                context = context,
                url = url,
                block = {
                    method = HttpMethod.Put
                    contentType(ContentType.Application.FormUrlEncoded)
                    setBody(body)
                },
            ) { response ->
                response.status.isSuccess()
            }
        }
        return false
    }
}
