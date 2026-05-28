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
import com.github.zly2006.zhihu.navigation.Pin
import com.github.zly2006.zhihu.navigation.Question
import com.github.zly2006.zhihu.shared.data.DataHolder
import kotlinx.coroutines.CancellationException

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
    val apiUrl = zhihuArticleContentDetailUrl(dest)

    return runCatching {
        val jo = AccountData.signedFetchGet(context, apiUrl)!!
        // 解析为对应的Content类型
        decodeArticleContentDetail(dest, jo)
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
    val apiUrl = zhihuQuestionContentDetailUrl(question)

    return runCatching {
        val jo = AccountData.signedFetchGet(context, apiUrl)!!
        // 解析为对应的Content类型
        decodeQuestionContentDetail(jo)
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
    val apiUrl = zhihuPinContentDetailUrl(pin)

    return runCatching {
        val jo = AccountData.signedFetchGet(context, apiUrl)!!
        decodePinContentDetail(jo)
    }.getOrElse { e ->
        if (e !is CancellationException) {
            Log.e("getContentDetail", "Failed to fetch content detail for pin id=${pin.id}", e)
        }
        null
    }
}
