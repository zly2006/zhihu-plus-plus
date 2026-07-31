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

package com.chloemlla.zhplus.ui

import com.chloemlla.zhplus.shared.data.DataHolder
import com.chloemlla.zhplus.shared.data.ZhihuJson
import com.fleeksoft.ksoup.Ksoup
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlin.time.Clock

const val ZHIHU_PLUS_AUTHOR_URL_TOKEN = "scanmenge"
const val ZHIHU_PLUS_AUTHOR_PINS_URL = "https://www.zhihu.com/api/v4/v2/pins/$ZHIHU_PLUS_AUTHOR_URL_TOKEN/moments"
const val ZHIHU_PLUS_TOPIC_ID = "2064846813258109867"

enum class HomePinAnnouncementKind {
    Poll,
    Topic,
}

data class HomePinAnnouncement(
    val pinId: Long,
    val kind: HomePinAnnouncementKind,
    val title: String,
    val optionCount: Int,
    val memberCount: Int,
)

internal fun decodeHomePinAnnouncements(
    response: JsonObject,
): List<HomePinAnnouncement> =
    response["data"]
        ?.jsonArray
        ?.mapNotNull { item ->
            val pin = runCatching {
                ZhihuJson.decodeJson<DataHolder.Pin>(item.jsonObject)
            }.getOrNull()
            pin?.toHomePinAnnouncement()
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
    !isReviewing && (endAt !in 0..nowEpochSeconds)

internal fun DataHolder.Pin.Poll.statusText(): String {
    val voteState = if (isVoted) {
        "已投票"
    } else if (maxSelections > 1) {
        "最多选择 $maxSelections 项"
    } else {
        "最多选择一项"
    }
    val validity = when {
        endAt < 0 -> "长期有效"
        endAt <= Clock.System.now().epochSeconds -> "投票已结束"
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

internal fun DataHolder.Pin.toHomePinAnnouncement(): HomePinAnnouncement? {
    val pinId = id.toLongOrNull() ?: return null
    val poll = bottomPoll?.voting
    if (poll != null && poll.acceptsVote() && !poll.isVoted) {
        return HomePinAnnouncement(
            pinId = pinId,
            kind = HomePinAnnouncementKind.Poll,
            title = poll.title.ifBlank { "想法投票" },
            optionCount = poll.options.size,
            memberCount = poll.memberCount,
        )
    }

    if (topics.orEmpty().none { it.id == ZHIHU_PLUS_TOPIC_ID }) {
        return null
    }
    return HomePinAnnouncement(
        pinId = pinId,
        kind = HomePinAnnouncementKind.Topic,
        title = Ksoup.parse(excerptTitle.substringBefore("<br")).text().ifBlank { "知乎++新动态" },
        optionCount = 0,
        memberCount = 0,
    )
}
