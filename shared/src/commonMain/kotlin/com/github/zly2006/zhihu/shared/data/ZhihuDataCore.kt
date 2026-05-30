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
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

data class FeedDisplayItem(
    val title: String,
    val summary: String?,
    val details: String,
    val feed: Feed?,
    val navDestinationJson: String? = null,
    val avatarSrc: String? = null,
    val authorName: String? = null,
    val authorBadgeV2: DataHolder.BadgeV2? = null,
    val isFiltered: Boolean = false,
    val content: String? = null,
    var raw: DataHolder.Content? = null,
    val localContentId: String? = null,
    val localFeedId: String? = null,
    val localReason: String? = null,
    val segmentInfos: List<SegmentInfoParagraph> = emptyList(),
    val segmentSourceUrl: String? = null,
) {
    val stableKey: String
        get() = localFeedId
            ?: localContentId
            ?: navDestinationJson
            ?: feed?.target?.stableTargetKey
            ?: "$title|${summary.orEmpty()}|$details"
}

private val Feed.Target.stableTargetKey: String
    get() = when (this) {
        is Feed.AnswerTarget -> "answer:$id"
        is Feed.ArticleTarget -> "article:$id"
        is Feed.QuestionTarget -> "question:$id"
        is Feed.PinTarget -> "pin:$id"
        is Feed.VideoTarget -> "video:$id"
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

@Serializable
data class OnlineHistoryItem(
    val cardType: String,
    val data: OnlineHistoryData,
)

@Serializable
data class OnlineHistoryData(
    val header: OnlineHistoryHeader,
    val content: OnlineHistoryContent? = null,
    val action: OnlineHistoryAction,
    val extra: OnlineHistoryExtra,
    val matrix: List<OnlineHistoryMatrixItem>? = null,
)

@Serializable
data class OnlineHistoryMatrixItem(
    val type: String,
    val data: OnlineHistoryMatrixData,
)

@Serializable
data class OnlineHistoryMatrixData(
    val text: String,
)

@Serializable
data class OnlineHistoryHeader(
    val icon: String,
    val title: String,
    val action: OnlineHistoryAction? = null,
)

@Serializable
data class OnlineHistoryContent(
    val authorName: String? = null,
    val summary: String? = null,
    val coverImage: String? = null,
)

@Serializable
data class OnlineHistoryAction(
    val type: String,
    val url: String,
)

@Serializable
data class OnlineHistoryExtra(
    val contentToken: String,
    val contentType: String,
    val readTime: Long,
    val questionToken: String,
)

@Serializable
data class OfficialBadge(
    val title: String,
    val description: String,
    val iconUrl: String = "",
    val nightIconUrl: String = "",
    val url: String = "",
    val type: String = "",
    val detailType: String = "",
) {
    val isGenericCertification: Boolean
        get() = title == "认证"

    val isUsefulInList: Boolean
        get() = !isGenericCertification
}

fun DataHolder.BadgeV2?.officialBadge(): OfficialBadge? {
    this ?: return null
    val details = officialBadgeDetails()
    val primary = details.firstOrNull { it.type != "identity" && it.iconUrl.isNotBlank() }
        ?: details.firstOrNull { it.iconUrl.isNotBlank() }
        ?: return null

    return primary
        .copy(
            iconUrl = icon.ifBlank { primary.iconUrl },
            nightIconUrl = nightIcon.ifBlank { primary.nightIconUrl },
        )
}

fun DataHolder.BadgeV2?.officialBadgeDetails(): List<OfficialBadge> {
    this ?: return emptyList()
    val details = detailBadges
        ?.mapNotNull(DataHolder.BadgeV2.Badge::asOfficialBadge)
        .orEmpty()
    if (details.isNotEmpty()) return details
    return mergedBadges
        ?.mapNotNull(DataHolder.BadgeV2.Badge::asOfficialBadge)
        .orEmpty()
}

private fun DataHolder.BadgeV2.Badge.asOfficialBadge(): OfficialBadge? {
    if (badgeStatus != null && badgeStatus != "passed") return null
    val title = title.takeIf { it.isNotBlank() } ?: return null
    val description = description.takeIf { it.isNotBlank() } ?: title
    return OfficialBadge(
        title = title,
        description = description,
        iconUrl = icon,
        nightIconUrl = nightIcon,
        url = url,
        type = type,
        detailType = detailType,
    )
}

object BooleanCompatSerializer : KSerializer<Boolean> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("BooleanCompat", PrimitiveKind.BOOLEAN)

    override fun deserialize(decoder: Decoder): Boolean = when (decoder) {
        is JsonDecoder -> {
            val primitive = decoder.decodeJsonElement().jsonPrimitive
            primitive.booleanOrNull
                ?: primitive.intOrNull?.let { it != 0 }
                ?: primitive.content.equals("true", ignoreCase = true)
        }

        else -> decoder.decodeBoolean()
    }

    override fun serialize(
        encoder: Encoder,
        value: Boolean,
    ) {
        encoder.encodeBoolean(value)
    }
}

@Serializable
data class SegmentInfoParagraph(
    val pid: String,
    val text: String,
    val marks: List<SegmentInfoMark> = emptyList(),
)

@Serializable
data class SegmentInfoMark(
    val startIndex: Int,
    val endIndex: Int,
    val segInfo: SegmentInfoMeta? = null,
    val masterSegInfo: SegmentInfoMeta? = null,
)

val SegmentInfoMark.effectiveSegInfo: SegmentInfoMeta?
    get() = segInfo ?: masterSegInfo

@Serializable
data class SegmentInfoMeta(
    val segIds: List<String> = emptyList(),
    @Serializable(with = BooleanCompatSerializer::class)
    val isLike: Boolean = false,
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val myCommentCount: Int = 0,
    @Serializable(with = BooleanCompatSerializer::class)
    val isSpan: Boolean = false,
)
