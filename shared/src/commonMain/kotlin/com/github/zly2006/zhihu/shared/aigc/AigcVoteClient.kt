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

package com.github.zly2006.zhihu.shared.aigc

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val MIN_CREDIT_DURATION_MS = 15_000L
private const val MIN_CREDIT_SCROLL_RATIO = 0.25
const val AIGC_MARKING_ENABLED_PREFERENCE_KEY = "enableAigcMarking"

class AigcVoteClient(
    private val httpClient: HttpClient,
    baseUrl: String,
    private val clientId: String,
) {
    private val normalizedBaseUrl = baseUrl.trimEnd('/')

    suspend fun syncReadEvent(event: AigcVoteReadEvent): AigcVoteReadEventsResponse =
        httpClient
            .post("$normalizedBaseUrl/v1/read-events:batch") {
                contentType(ContentType.Application.Json)
                setBody(
                    AigcVoteReadEventsRequest(
                        clientId = clientId,
                        events = listOf(event.toRequestEvent()),
                    ),
                )
            }.body()

    suspend fun submitFlag(submission: AigcVoteFlagSubmission): AigcVoteFlagResponse =
        httpClient
            .post("$normalizedBaseUrl/v1/contents/${submission.contentType}/${submission.contentId}/aigc-flag") {
                contentType(ContentType.Application.Json)
                setBody(
                    AigcVoteFlagRequest(
                        clientId = clientId,
                        voter = submission.voter,
                        title = submission.title,
                        authorHash = submission.authorHash,
                        contentHtml = submission.contentHtml,
                        contentUpdatedAt = submission.contentUpdatedAt,
                        evidence = submission.evidence.toFlagEvidence(),
                    ),
                )
            }.body()

    suspend fun getFlagStatus(
        contentType: String,
        contentId: String,
        voter: AigcVoteVoter? = null,
    ): AigcVoteFlagStatusResponse =
        httpClient
            .get("$normalizedBaseUrl/v1/contents/$contentType/$contentId/aigc-flag") {
                parameter("client_id", clientId)
                if (voter != null) {
                    parameter("voter_id", voter.id)
                    parameter("voter_name", voter.name)
                    if (!voter.urlToken.isNullOrBlank()) {
                        parameter("voter_url_token", voter.urlToken)
                    }
                }
            }.body()
}

data class AigcVoteReadEvidence(
    val foregroundDurationMs: Long,
    val maxScrollRatio: Double,
    val openedAtEpochSeconds: Long,
) {
    fun isEligibleForCredit(): Boolean =
        foregroundDurationMs >= MIN_CREDIT_DURATION_MS &&
            maxScrollRatio >= MIN_CREDIT_SCROLL_RATIO &&
            openedAtEpochSeconds > 0

    fun toFlagEvidence(): AigcVoteFlagEvidenceRequest = AigcVoteFlagEvidenceRequest(
        clientViewDurationMs = foregroundDurationMs,
        scrollDepth = maxScrollRatio,
        openedAt = openedAtEpochSeconds,
    )
}

data class AigcVoteReadEvent(
    val contentType: String,
    val contentId: String,
    val title: String,
    val authorHash: String,
    val contentHtml: String,
    val contentUpdatedAt: Long,
    val evidence: AigcVoteReadEvidence,
) {
    fun toRequestEvent(): AigcVoteReadEventRequest = AigcVoteReadEventRequest(
        contentType = contentType,
        contentId = contentId,
        title = title,
        authorHash = authorHash,
        contentHtml = contentHtml,
        contentUpdatedAt = contentUpdatedAt,
        openedAt = evidence.openedAtEpochSeconds,
        foregroundDurationMs = evidence.foregroundDurationMs,
        maxScrollRatio = evidence.maxScrollRatio,
    )
}

data class AigcVoteFlagSubmission(
    val contentType: String,
    val contentId: String,
    val voter: AigcVoteVoter,
    val title: String,
    val authorHash: String,
    val contentHtml: String,
    val contentUpdatedAt: Long,
    val evidence: AigcVoteReadEvidence,
)

@Serializable
data class AigcVoteVoter(
    val id: String,
    val name: String,
    @SerialName("url_token")
    val urlToken: String? = null,
    @SerialName("avatar_url")
    val avatarUrl: String? = null,
)

@Serializable
private data class AigcVoteReadEventsRequest(
    @SerialName("client_id")
    val clientId: String,
    val events: List<AigcVoteReadEventRequest>,
)

@Serializable
data class AigcVoteReadEventRequest(
    @SerialName("content_type")
    val contentType: String,
    @SerialName("content_id")
    val contentId: String,
    val title: String,
    @SerialName("author_hash")
    val authorHash: String,
    @SerialName("content_html")
    val contentHtml: String,
    @SerialName("content_updated_at")
    val contentUpdatedAt: Long,
    @SerialName("opened_at")
    val openedAt: Long,
    @SerialName("foreground_duration_ms")
    val foregroundDurationMs: Long,
    @SerialName("max_scroll_ratio")
    val maxScrollRatio: Double,
)

@Serializable
private data class AigcVoteFlagRequest(
    @SerialName("client_id")
    val clientId: String,
    val voter: AigcVoteVoter,
    val title: String,
    @SerialName("author_hash")
    val authorHash: String,
    @SerialName("content_html")
    val contentHtml: String,
    @SerialName("content_updated_at")
    val contentUpdatedAt: Long,
    val evidence: AigcVoteFlagEvidenceRequest,
)

@Serializable
data class AigcVoteFlagEvidenceRequest(
    @SerialName("client_view_duration_ms")
    val clientViewDurationMs: Long,
    @SerialName("scroll_depth")
    val scrollDepth: Double,
    @SerialName("opened_at")
    val openedAt: Long,
)

@Serializable
data class AigcVoteReadEventsResponse(
    val credit: Int,
    val progress: Int,
    val cap: Int,
    @SerialName("accepted_events")
    val acceptedEvents: Int,
    @SerialName("rejected_events")
    val rejectedEvents: Int,
)

@Serializable
data class AigcVoteFlagResponse(
    @SerialName("my_flagged")
    val myFlagged: Boolean,
    val credit: Int,
    @SerialName("credit_bypass_available")
    val creditBypassAvailable: Boolean = false,
    /** 自家后端支持人数：每个有效的 Zhihu++ AIGC 标记用户计 1 人。 */
    @SerialName("effective_flag_count")
    val effectiveFlagCount: Int,
    /** 自家后端原始支持人数；当前与 effectiveFlagCount 相同。 */
    @SerialName("raw_flag_count")
    val rawFlagCount: Int,
    /** 自家后端中当前正文 HTML 版本的支持人数。 */
    @SerialName("current_version_flag_count")
    val currentVersionFlagCount: Int,
    @SerialName("content_hash")
    val contentHash: String,
    @SerialName("content_updated_at")
    val contentUpdatedAt: Long,
    val confidence: String,
    val voters: List<AigcVoteNamedVoter> = emptyList(),
    /** 来自 zhihuai.sx349.xyz 的外部数据源统计。 */
    @SerialName("external_source")
    val externalSource: AigcVoteExternalSource? = null,
)

@Serializable
data class AigcVoteFlagStatusResponse(
    @SerialName("my_flagged")
    val myFlagged: Boolean,
    val credit: Int,
    val progress: Int,
    val cap: Int,
    @SerialName("credit_bypass_available")
    val creditBypassAvailable: Boolean = false,
    /** 自家后端支持人数：每个有效的 Zhihu++ AIGC 标记用户计 1 人。 */
    @SerialName("effective_flag_count")
    val effectiveFlagCount: Int,
    /** 自家后端原始支持人数；当前与 effectiveFlagCount 相同。 */
    @SerialName("raw_flag_count")
    val rawFlagCount: Int,
    val confidence: String,
    val voters: List<AigcVoteNamedVoter> = emptyList(),
    /** 来自 zhihuai.sx349.xyz 的外部数据源统计。 */
    @SerialName("external_source")
    val externalSource: AigcVoteExternalSource? = null,
)

@Serializable
data class AigcVoteExternalSource(
    val source: String,
    @SerialName("content_type")
    val contentType: String,
    @SerialName("content_id")
    val contentId: String,
    /** zhihuai 支持票数，含义是“疑似AI生成低质量内容”。 */
    @SerialName("total_votes")
    val totalVotes: Int,
    /** zhihuai 支持票投票人数；这是外部来源的 AIGC 支持人数。 */
    @SerialName("voter_count")
    val voterCount: Int,
    /** zhihuai 反对票数，含义是“并非AI生成低质量内容”。 */
    @SerialName("total_downvotes")
    val totalDownvotes: Int,
    /** zhihuai 反对票投票人数；这不计入 AIGC 支持人数。 */
    @SerialName("downvoter_count")
    val downvoterCount: Int,
    @SerialName("refreshed_at")
    val refreshedAt: Long,
)

@Serializable
data class AigcVoteNamedVoter(
    @SerialName("voter_id")
    val voterId: String,
    @SerialName("voter_name")
    val voterName: String,
    @SerialName("voter_url_token")
    val voterUrlToken: String? = null,
    @SerialName("voter_avatar_url")
    val voterAvatarUrl: String? = null,
    @SerialName("created_at")
    val createdAt: Long,
    @SerialName("credit_bypassed")
    val creditBypassed: Boolean = false,
)
