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

package com.github.zly2006.zhihu.ui

import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.data.ZhihuJson
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlin.time.Clock

const val ZHIHU_PLUS_AUTHOR_URL_TOKEN = "scanmenge"
const val ZHIHU_PLUS_AUTHOR_PINS_URL = "https://www.zhihu.com/api/v4/v2/pins/$ZHIHU_PLUS_AUTHOR_URL_TOKEN/moments"

data class HomePollAnnouncement(
    val pinId: Long,
    val pollId: String,
    val title: String,
    val optionCount: Int,
    val memberCount: Int,
    val isVoted: Boolean,
)

internal fun decodeHomePollAnnouncements(
    response: JsonObject,
    nowEpochSeconds: Long = Clock.System.now().epochSeconds,
): List<HomePollAnnouncement> =
    response["data"]
        ?.jsonArray
        ?.mapNotNull { item ->
            val pin = runCatching {
                ZhihuJson.decodeJson<DataHolder.Pin>(item.jsonObject)
            }.getOrNull()
            pin?.toHomePollAnnouncement(nowEpochSeconds)
        }
        ?: emptyList()

internal fun DataHolder.Pin.withSelectedPinPollOption(
    pollId: String,
    optionId: String,
): DataHolder.Pin {
    val bottomPoll = bottomPoll ?: return this
    val voting = bottomPoll.voting ?: return this
    if (voting.id != pollId || voting.isVoted || voting.options.none { it.id == optionId }) {
        return this
    }

    val updatedVoting = voting.copy(
        isVoted = true,
        votingCount = voting.votingCount + 1,
        memberCount = voting.memberCount + 1,
        options = voting.options.map { option ->
            if (option.id == optionId) {
                option.copy(
                    votingCount = option.votingCount + 1,
                    isSelected = true,
                )
            } else {
                option
            }
        },
    )
    return copy(bottomPoll = bottomPoll.copy(voting = updatedVoting))
}

internal fun DataHolder.Pin.Poll.acceptsVote(nowEpochSeconds: Long = Clock.System.now().epochSeconds): Boolean =
    !isReviewing && (endAt < 0 || endAt > nowEpochSeconds)

internal fun DataHolder.Pin.Poll.statusText(nowEpochSeconds: Long = Clock.System.now().epochSeconds): String {
    val voteState = if (isVoted) {
        "已投票"
    } else if (maxSelections > 1) {
        "最多选择 $maxSelections 项"
    } else {
        "最多选择一项"
    }
    val validity = when {
        endAt < 0 -> "长期有效"
        endAt <= nowEpochSeconds -> "投票已结束"
        else -> null
    }
    return buildString {
        append(voteState)
        if (isVoted || memberCount > 0) {
            append("，")
            append(memberCount)
            append(" 人参与")
        }
        if (validity != null) {
            append("，")
            append(validity)
        }
    }
}

internal fun DataHolder.Pin.toHomePollAnnouncement(nowEpochSeconds: Long): HomePollAnnouncement? {
    val poll = bottomPoll?.voting ?: return null
    if (!poll.acceptsVote(nowEpochSeconds)) {
        return null
    }
    return HomePollAnnouncement(
        pinId = id.toLongOrNull() ?: return null,
        pollId = poll.id,
        title = poll.title.ifBlank { "想法投票" },
        optionCount = poll.options.size,
        memberCount = poll.memberCount,
        isVoted = poll.isVoted,
    )
}
