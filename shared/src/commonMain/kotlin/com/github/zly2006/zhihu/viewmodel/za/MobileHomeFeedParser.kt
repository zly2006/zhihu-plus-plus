/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 */

package com.github.zly2006.zhihu.viewmodel.za

import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.resolveContent
import com.github.zly2006.zhihu.shared.data.FeedDisplayItem
import com.github.zly2006.zhihu.shared.data.toFeedDisplayItemNavDestinationJson
import io.ktor.http.decodeURLPart
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

fun parseMobileHomeFeedDisplayItem(card: JsonObject): FeedDisplayItem? {
    if (card["type"]?.jsonPrimitive?.content != "ComponentCard") {
        return null
    }
    val route =
        card["action"]!!
            .jsonObject["parameter"]!!
            .jsonPrimitive.content
            .substringAfter("route_url=")
    val routeDest = resolveContent(route.decodeURLPart()) ?: return null
    val children = card["children"]?.jsonArray?.map { it.jsonObject } ?: return null
    val title = children.joStrMatch("id", "Text")["text"]!!.jsonPrimitive.content
    val summary = children.joStrMatch("id", "text_pin_summary")["text"]!!.jsonPrimitive.content
    val footer = children.filter { it["type"]!!.jsonPrimitive.content == "Line" }.getOrNull(1) ?: return null
    val footerText = if (footer["style"]!!.jsonPrimitive.content == "LineFooterReaction_feed_v3") {
        val footerLine = footer["elements"]!!.jsonArray.map { it.jsonObject }
        val voteUp = footerLine.joStrMatch("reaction", "Vote")["count"]!!.jsonPrimitive.int
        val comment = footerLine.joStrMatch("reaction", "Comment")["count"]!!.jsonPrimitive.int
        val collect = footerLine.joStrMatch("reaction", "Collect")["count"]!!.jsonPrimitive.int
        "$voteUp 赞同 · $comment 评论 · $collect 收藏"
    } else {
        val footerLine = footer["elements"]!!.jsonArray.map { it.jsonObject }
        footerLine.joStrMatch("type", "Text")["text"]!!.jsonPrimitive.content
    }
    val lineAuthor =
        children
            .first {
                it["style"]!!.jsonPrimitive.content.startsWith("RecommendAuthorLine") ||
                    it["style"]!!.jsonPrimitive.content.startsWith("LineAuthor_default")
            }["elements"]!!
            .jsonArray
            .map { it.jsonObject }
    val avatar = lineAuthor
        .joStrMatch("style", "Avatar_default")["image"]!!
        .jsonObject["url"]!!
        .jsonPrimitive.content
    val authorName = lineAuthor.joStrMatch("type", "Text")["text"]!!.jsonPrimitive.content
    if (routeDest is Article) {
        routeDest.authorName = authorName
        routeDest.title = title
        routeDest.avatarSrc = avatar
    }

    return FeedDisplayItem(
        navDestinationJson = routeDest.toFeedDisplayItemNavDestinationJson(),
        avatarSrc = avatar,
        authorName = authorName,
        summary = summary,
        title = title,
        details = "$footerText · 手机版推荐",
        feed = null,
    )
}

/**
 * Find the first JsonObject in the list where the value associated with [key] matches [value].
 */
@Suppress("NOTHING_TO_INLINE")
private inline fun List<JsonObject>.joStrMatch(key: String, value: String): JsonObject =
    this.firstOrNull { it[key]?.jsonPrimitive?.content == value }
        ?: throw IllegalStateException("No matching JsonObject found for $key = $value: $this")
