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
import androidx.room.RoomDatabase
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.navigation.Pin
import com.github.zly2006.zhihu.navigation.Question
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.util.signFetchRequest
import kotlinx.coroutines.CancellationException

suspend fun DataHolder.getContentDetail(
    context: Context,
    dest: Article,
): DataHolder.Content? {
    val apiUrl = when (dest.type) {
        ArticleType.Article -> "https://www.zhihu.com/api/v4/articles/${dest.id}?include=content,topics,paid_info,can_comment,excerpt,thanks_count,voteup_count,comment_count,visited_count,relationship,ip_info,relationship.vote,author.badge_v2"
        ArticleType.Answer -> "https://www.zhihu.com/api/v4/answers/${dest.id}?include=content,paid_info,can_comment,excerpt,thanks_count,voteup_count,comment_count,visited_count,attachment,reaction,ip_info,pagination_info,question.topics,reaction.relation.voting,author.badge_v2"
    }

    return runCatching {
        val jo = AccountData.fetchGet(context, apiUrl) { signFetchRequest() }!!
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
    val apiUrl = "https://www.zhihu.com/api/v4/questions/${question.questionId}?include=read_count,visit_count,answer_count,voteup_count,comment_count,follower_count,detail,excerpt,author,relationship.is_following,topics"

    return runCatching {
        val jo = AccountData.fetchGet(context, apiUrl) { signFetchRequest() }!!
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
    val apiUrl = "https://www.zhihu.com/api/v4/pins/${pin.id}?include=topics"

    return runCatching {
        val jo = AccountData.fetchGet(context, apiUrl) { signFetchRequest() }!!
        decodePinContentDetail(jo)
    }.getOrElse { e ->
        if (e !is CancellationException) {
            Log.e("getContentDetail", "Failed to fetch content detail for pin id=${pin.id}", e)
        }
        null
    }
}

suspend fun ContentDetailCache.getOrFetch(
    context: Context,
    navDestination: NavDestination,
): DataHolder.Content? = getOrFetch(navDestination) { destination ->
    when (destination) {
        is Article -> DataHolder.getContentDetail(context, destination)
        is Question -> DataHolder.getContentDetail(context, destination)
        is Pin -> DataHolder.getContentDetail(context, destination)
        else -> null
    }
}

actual fun <T : RoomDatabase> RoomDatabase.Builder<T>.applyPlatformDriver(): RoomDatabase.Builder<T> = this
