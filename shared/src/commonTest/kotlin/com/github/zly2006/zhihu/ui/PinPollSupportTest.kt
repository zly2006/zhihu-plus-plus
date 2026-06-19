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

package com.github.zly2006.zhihu.ui

import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.data.ZhihuJson
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class PinPollSupportTest {
    @Test
    fun pinDetailDecodesBottomPollAndInlinePollContent() {
        val pin = decodePin(pinJson(id = "101", pollId = "poll-101"))

        val inlinePoll = assertIs<DataHolder.Pin.ContentPoll>(pin.content[1])
        assertEquals(2051253919255360130L, inlinePoll.pollId)
        assertEquals("poll-101", pin.bottomPoll?.voting?.id)
        assertEquals("知乎++好用吗", pin.bottomPoll?.voting?.title)
        assertEquals(
            listOf("option-a", "option-b"),
            pin.bottomPoll
                ?.voting
                ?.options
                ?.map { it.id },
        )
    }

    @Test
    fun selectingPollOptionUpdatesOnlyLocalPollCounters() {
        val pin = decodePin(pinJson(id = "101", pollId = "poll-101", memberCount = 2))

        val updated = pin.withSelectedPinPollOption(pollId = "poll-101", optionId = "option-b")
        val poll = updated.bottomPoll?.voting ?: error("投票不存在")

        assertFalse(pin.bottomPoll?.voting?.isVoted ?: true)
        assertTrue(poll.isVoted)
        assertEquals(3, poll.memberCount)
        assertEquals(3, poll.votingCount)
        assertEquals(false, poll.options[0].isSelected)
        assertEquals(1, poll.options[0].votingCount)
        assertEquals(true, poll.options[1].isSelected)
        assertEquals(2, poll.options[1].votingCount)
    }

    @Test
    fun homeAnnouncementExtractionKeepsAllActiveAuthorPolls() {
        val response = ZhihuJson.json
            .parseToJsonElement(
                """
                {
                  "data": [
                    ${pinJson(id = "101", pollId = "poll-101", title = "第一个反馈投票")},
                    ${pinJson(id = "102", pollId = "poll-102", title = "第二个反馈投票")},
                    ${pinJson(id = "103", pollId = "poll-103", title = "已结束反馈投票", endAt = 1000)}
                  ]
                }
                """.trimIndent(),
            ).jsonObject

        val announcements = decodeHomePollAnnouncements(response, nowEpochSeconds = 2_000)

        assertEquals(listOf(101L, 102L), announcements.map { it.pinId })
        assertEquals(listOf("poll-101", "poll-102"), announcements.map { it.pollId })
        assertEquals("第一个反馈投票", announcements.first().title)
        assertEquals(2, announcements.first().optionCount)
    }

    private fun decodePin(json: String): DataHolder.Pin =
        ZhihuJson.decodeJson(ZhihuJson.json.parseToJsonElement(json).jsonObject)

    private fun pinJson(
        id: String,
        pollId: String,
        title: String = "知乎++好用吗",
        memberCount: Int = 0,
        endAt: Long = -1,
    ): String =
        """
        {
          "id": "$id",
          "type": "pin",
          "url": "https://www.zhihu.com/pin/$id",
          "author": {
            "avatar_url": "",
            "gender": 0,
            "headline": "作者简介",
            "id": "author-id",
            "is_advertiser": false,
            "is_org": false,
            "name": "实名开导",
            "type": "people",
            "url": "https://www.zhihu.com/people/scanmenge",
            "url_token": "scanmenge",
            "user_type": "people"
          },
          "content": [
            {
              "type": "text",
              "content": "给知乎++打个分吧",
              "title": ""
            },
            {
              "type": "poll",
              "duration": 0,
              "poll_id": 2051253919255360130
            }
          ],
          "excerpt_title": "给知乎++打个分吧",
          "content_html": "<p>给知乎++打个分吧</p>",
          "bottom_poll": {
            "voting": {
              "id": "$pollId",
              "title": "$title",
              "max_selections": 1,
              "type": "single",
              "begin_at": 1781837044,
              "end_at": $endAt,
              "voting_count": $memberCount,
              "member_count": $memberCount,
              "is_voted": false,
              "is_reviewing": false,
              "options": [
                {
                  "id": "option-a",
                  "title": "五颗星",
                  "voting_count": 1,
                  "is_selected": false
                },
                {
                  "id": "option-b",
                  "title": "四颗星",
                  "voting_count": 1,
                  "is_selected": false
                }
              ]
            }
          }
        }
        """.trimIndent()
}
