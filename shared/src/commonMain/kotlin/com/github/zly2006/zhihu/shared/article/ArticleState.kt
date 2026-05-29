package com.github.zly2006.zhihu.shared.article

import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.shared.data.OfficialBadge

/**
 * 缓存的回答完整内容，用于水平滑动预览。
 */
data class CachedAnswerContent(
    val article: Article,
    val title: String,
    val authorName: String,
    val authorBio: String,
    val authorAvatarUrl: String,
    val authorBadge: OfficialBadge? = null,
    val content: String,
    val voteUpCount: Int,
    val commentCount: Int,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val ipInfo: String? = null,
    /** 来源标签，用于 UI 显示，例如 "此问题"、"「收藏夹名称」" */
    val sourceLabel: String = "此问题",
)
