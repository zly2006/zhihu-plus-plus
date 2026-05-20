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

package com.github.zly2006.zhihu.shared.data

import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.navigation.Pin
import com.github.zly2006.zhihu.navigation.Question
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val feedNavigationJson = Json {
    ignoreUnknownKeys = true
}

val FeedDisplayItem.navDestination: NavDestination?
    get() = navDestinationJson
        ?.let { runCatching { feedNavigationJson.decodeFromString<NavDestination>(it) }.getOrNull() }
        ?: feed?.target?.navDestination

fun NavDestination.toFeedDisplayItemNavDestinationJson(): String = feedNavigationJson.encodeToString<NavDestination>(this)

val Feed.Target.navDestination: NavDestination?
    get() = when (this) {
        is Feed.AnswerTarget -> Article(
            title = question.title,
            type = ArticleType.Answer,
            id = id,
            authorName = author?.name ?: "loading...",
            authorBio = author?.headline ?: "",
            avatarSrc = author?.avatarUrl,
            excerpt = excerpt,
        )

        is Feed.ArticleTarget -> Article(
            title = title,
            type = ArticleType.Article,
            id = id,
            authorName = author.name,
            authorBio = author.headline,
            avatarSrc = author.avatarUrl,
            excerpt = excerpt,
        )

        is Feed.PinTarget -> Pin(id)

        is Feed.QuestionTarget -> Question(
            questionId = id,
            title = title,
        )

        is Feed.VideoTarget -> null
    }
