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

package com.github.zly2006.zhihu.navigation

import com.github.zly2006.zhihu.viewmodel.ArticleViewModel.CachedAnswerContent

enum class NeighborPhase {
    None,
    Preview,
    Loading,
    Ready,
}

/**
 * 可观测的邻居回答槽位：队列元数据 + 预取阶段。
 * UI 在 [Preview]/[Loading] 时显示 skeleton，[Ready] 时展示正文。
 */
data class NeighborSlot(
    val article: Article,
    val title: String,
    val authorName: String,
    val authorAvatarUrl: String,
    val phase: NeighborPhase,
    val cached: CachedAnswerContent? = null,
) {
    fun toDisplayContent(sourceLabel: String): CachedAnswerContent = cached ?: CachedAnswerContent(
        article = article,
        title = title,
        authorName = authorName,
        authorBio = article.authorBio,
        authorAvatarUrl = authorAvatarUrl,
        content = "",
        voteUpCount = 0,
        commentCount = 0,
        sourceLabel = sourceLabel,
    )

    companion object {
        fun previewFrom(article: Article, sourceLabel: String): NeighborSlot = NeighborSlot(
            article = article,
            title = article.title,
            authorName = article.authorName,
            authorAvatarUrl = article.avatarSrc ?: "",
            phase = NeighborPhase.Preview,
        )

        fun ready(cached: CachedAnswerContent): NeighborSlot = NeighborSlot(
            article = cached.article,
            title = cached.title,
            authorName = cached.authorName,
            authorAvatarUrl = cached.authorAvatarUrl,
            phase = NeighborPhase.Ready,
            cached = cached,
        )

        fun loadingFrom(article: Article, sourceLabel: String): NeighborSlot = NeighborSlot(
            article = article,
            title = article.title,
            authorName = article.authorName,
            authorAvatarUrl = article.avatarSrc ?: "",
            phase = NeighborPhase.Loading,
        )
    }
}
