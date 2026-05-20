package com.github.zly2006.zhihu.shared.data

data class FeedDisplayItem(
    val title: String,
    val summary: String?,
    val details: String,
    val feed: Feed?,
    val avatarSrc: String? = null,
    val authorName: String? = null,
    val authorBadgeV2: DataHolder.BadgeV2? = null,
    val isFiltered: Boolean = false,
    val content: String? = null,
    val segmentInfos: List<SegmentInfoParagraph> = emptyList(),
    val segmentSourceUrl: String? = null,
) {
    val stableKey: String
        get() = "${feed?.target?.url.orEmpty()}|$title|${summary.orEmpty()}|$details"
}

fun List<Feed>.flattenFeeds(): List<Feed> = flatMap {
    (it as? GroupFeed)?.list ?: listOf(it)
}

fun Feed.toDisplayItem(
    enableQualityFilter: Boolean = true,
    reverseBlock: Boolean = false,
): FeedDisplayItem = when (this) {
    is CommonFeed, is FeedItemIndexGroup, is MomentsFeed, is HotListFeed -> toTargetDisplayItem(
        enableQualityFilter = enableQualityFilter,
        reverseBlock = reverseBlock,
    )

    is AdvertisementFeed -> FeedDisplayItem(
        title = if (reverseBlock) {
            ad.creatives
                .firstOrNull()
                ?.title ?: ""
        } else {
            ""
        },
        summary = if (reverseBlock) {
            ad.creatives
                .firstOrNull()
                ?.description ?: actionText
        } else {
            actionText
        },
        details = actionText + "广告",
        feed = this,
        isFiltered = !reverseBlock,
        content = ad.creatives
            .firstOrNull()
            ?.landingUrl,
    )

    is GroupFeed -> error("GroupFeed should be flattened before creating display items")
    is QuestionFeedCard -> FeedDisplayItem(
        title = target.title,
        summary = target.excerpt,
        details = listOfNotNull(target.detailsText, actionText).joinToString(" · "),
        avatarSrc = target.author?.avatarUrl,
        authorName = target.author?.name,
        authorBadgeV2 = target.author?.badgeV2,
        feed = this,
    )
}

private fun Feed.toTargetDisplayItem(
    enableQualityFilter: Boolean,
    reverseBlock: Boolean,
): FeedDisplayItem {
    val target = target
    val filterReason = if (!enableQualityFilter || reverseBlock) null else target?.filterReason()

    if (filterReason != null) {
        return FeedDisplayItem(
            title = "已屏蔽",
            summary = filterReason,
            details = target!!.detailsText,
            feed = this,
            isFiltered = true,
        )
    }

    return when (target) {
        is Feed.AnswerTarget,
        is Feed.ArticleTarget,
        is Feed.QuestionTarget,
        -> FeedDisplayItem(
            title = target.title,
            summary = target.excerpt,
            details = listOfNotNull(target.detailsText, actionText).joinToString(" · "),
            avatarSrc = target.author?.avatarUrl,
            authorName = target.author?.name,
            authorBadgeV2 = target.author?.badgeV2,
            feed = this,
            segmentInfos = when (target) {
                is Feed.AnswerTarget -> target.segmentInfos
                is Feed.ArticleTarget -> target.segmentInfos
                else -> emptyList()
            },
            segmentSourceUrl = when (target) {
                is Feed.AnswerTarget -> "https://www.zhihu.com/question/${target.question.id}/answer/${target.id}"
                is Feed.ArticleTarget -> "https://zhuanlan.zhihu.com/p/${target.id}"
                else -> null
            },
        )

        is Feed.PinTarget -> FeedDisplayItem(
            title = target.author.name + "的想法",
            summary = target.excerpt,
            details = target.detailsText,
            avatarSrc = target.author.avatarUrl,
            authorName = target.author.name,
            authorBadgeV2 = target.author.badgeV2,
            feed = this,
        )

        else -> FeedDisplayItem(
            title = target?.description() ?: "广告",
            summary = "Not Implemented",
            details = target?.detailsText ?: "广告",
            feed = this,
        )
    }
}
