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

package com.github.zly2006.zhihu.data

import android.content.Context
import android.util.Log
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.Pin
import com.github.zly2006.zhihu.navigation.Question
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.util.signFetchRequest
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import kotlinx.serialization.json.put

/**
 * 此API没有缓存，谨慎使用！
 *
 * 优先考虑 [ContentDetailCache.getOrFetch]
 *
 * 使用此方法的场景：主要显示内容的获取，只有时效性要求特别高才允许使用。
 */
suspend fun DataHolder.getContentDetail(
    context: Context,
    dest: Article,
): DataHolder.Content? {
    val apiUrl = when (dest.type) {
        ArticleType.Article -> "https://www.zhihu.com/api/v4/articles/${dest.id}?include=content,topics,paid_info,can_comment,excerpt,thanks_count,voteup_count,comment_count,visited_count,relationship,ip_info,relationship.vote,author.badge_v2"
        ArticleType.Answer -> "https://www.zhihu.com/api/v4/answers/${dest.id}?include=content,paid_info,can_comment,excerpt,thanks_count,voteup_count,comment_count,visited_count,attachment,reaction,ip_info,pagination_info,question.topics,reaction.relation.voting,author.badge_v2"
        // ^ question.topics 后面的字段可能有点bug。
    }

    return runCatching {
        val jo = AccountData.fetchGet(context, apiUrl) {
            signFetchRequest()
        }!!
        val jojo = buildJsonObject {
            jo.entries.forEach { (key, value) ->
                if (key == "id") {
                    put(key, value.jsonPrimitive.long)
                } else {
                    put(key, value)
                }
            }
        }
        // 解析为对应的Content类型
        when (dest.type) {
            ArticleType.Answer -> AccountData.decodeJson<DataHolder.Answer>(jojo)
            ArticleType.Article -> AccountData.decodeJson<DataHolder.Article>(jojo)
        }
    }.getOrElse { e ->
        if (e !is CancellationException) {
            Log.e("getContentDetail", "Failed to fetch content detail for ${dest.type} id=${dest.id}", e)
        }
        null
    }
}

suspend fun DataHolder.getContentDetail(
    context: Context,
    question: Question,
): DataHolder.Question? {
    val apiUrl = "https://www.zhihu.com/api/v4/questions/${question.questionId}?include=read_count,visit_count,answer_count,voteup_count,comment_count,follower_count,detail,excerpt,author,relationship.is_following,topics"

    return runCatching {
        val jo = AccountData.fetchGet(context, apiUrl) {
            signFetchRequest()
        }!!
        val jojo = buildJsonObject {
            jo.entries.forEach { (key, value) ->
                if (key == "id") {
                    put(key, value.jsonPrimitive.long)
                } else {
                    put(key, value)
                }
            }
        }
        // 解析为对应的Content类型
        AccountData.decodeJson<DataHolder.Question>(jojo)
    }.getOrElse { e ->
        if (e !is CancellationException) {
            Log.e("getContentDetail", "Failed to fetch content detail for question id=${question.questionId}", e)
        }
        null
    }
}

suspend fun DataHolder.getContentDetail(
    context: Context,
    pin: Pin,
): DataHolder.Pin? {
    val apiUrl = "https://www.zhihu.com/api/v4/pins/${pin.id}"

    return runCatching {
        val jo = AccountData.fetchGet(context, apiUrl) {
            signFetchRequest()
        }!!
        AccountData.decodeJson<DataHolder.Pin>(jo)
    }.getOrElse { e ->
        if (e !is CancellationException) {
            Log.e("getContentDetail", "Failed to fetch content detail for pin id=${pin.id}", e)
        }
        null
    }
}
