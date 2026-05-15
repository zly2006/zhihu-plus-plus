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

package com.github.zly2006.zhihu.viewmodel.filter

import android.content.Context
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.CollectionContent
import com.github.zly2006.zhihu.navigation.History
import com.github.zly2006.zhihu.navigation.Home
import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.navigation.Notification
import com.github.zly2006.zhihu.navigation.OnlineHistory
import com.github.zly2006.zhihu.navigation.Pin
import com.github.zly2006.zhihu.navigation.Question
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class TrackedContentIdentity(
    val type: String,
    val id: String,
)

object ContentOpenFrom {
    const val ANSWER_SWITCH = "answer_switch"
    const val COLLECTION = "collection"
    const val HISTORY = "history"
    const val HOME_FEED = "home_feed"
    const val NOTIFICATION = "notification"
    const val QUESTION_FEED = "question_feed"
    const val UNKNOWN = "unknown"
}

object ContentOpenEventSupport {
    fun toTrackedContentIdentity(destination: NavDestination): TrackedContentIdentity? = when (destination) {
        is Article -> {
            val type = when (destination.type) {
                ArticleType.Answer -> ContentType.ANSWER
                ArticleType.Article -> ContentType.ARTICLE
            }
            TrackedContentIdentity(type = type, id = destination.id.toString())
        }
        is Question -> TrackedContentIdentity(type = ContentType.QUESTION, id = destination.questionId.toString())
        is Pin -> TrackedContentIdentity(type = ContentType.PIN, id = destination.id.toString())
        else -> null
    }

    fun inferOpenFrom(
        source: NavDestination?,
        target: NavDestination,
    ): String = when {
        source is Article &&
            source.type == ArticleType.Answer &&
            target is Article &&
            target.type == ArticleType.Answer -> ContentOpenFrom.ANSWER_SWITCH
        source is Home -> ContentOpenFrom.HOME_FEED
        source is Question -> ContentOpenFrom.QUESTION_FEED
        source is CollectionContent -> ContentOpenFrom.COLLECTION
        source is History || source is OnlineHistory -> ContentOpenFrom.HISTORY
        source is Notification -> ContentOpenFrom.NOTIFICATION
        else -> ContentOpenFrom.UNKNOWN
    }

    suspend fun recordOpenEvent(
        context: Context,
        destination: NavDestination,
        questionId: Long? = null,
        openFrom: String = ContentOpenFrom.UNKNOWN,
    ) {
        val identity = toTrackedContentIdentity(destination) ?: return
        withContext(Dispatchers.IO) {
            ContentFilterDatabase
                .getDatabase(context)
                .contentOpenEventDao()
                .insert(
                    ContentOpenEvent(
                        contentType = identity.type,
                        contentId = identity.id,
                        questionId = questionId,
                        openFrom = openFrom,
                    ),
                )
        }
    }
}
