package com.github.zly2006.zhihu.shared.data

import kotlinx.serialization.Serializable

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
