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

package com.github.zly2006.zhihu.data

import com.github.zly2006.zhihu.viewmodel.za.parseMobileHomeFeedDisplayItem
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Fixtures are selected from a 2026-08-16 zhurl production capture. Only structured person identity
 * fields are anonymized; HTML values are byte-identical to the private raw responses.
 */
class RealZhihuApiCorpusTest {
    @Test
    fun representativeProductionResponsesDecodeThroughTheirProductionParsers() {
        val answer = assertIs<Feed.AnswerTarget>(decodeTarget("answer-detail.json"))
        assertTrue(answer.content.startsWith("<p"))
        assertTrue(answer.author?.id?.startsWith("redacted-person-") == true)

        val comment = ZhihuJson.decodeJson<DataHolder.Comment>(fixtureJson("comment.json"))
        assertEquals("comment", comment.type)
        assertTrue(comment.content.startsWith("<p"))
        assertTrue(comment.author.id.startsWith("redacted-person-"))

        val daily = ZhihuJson.decodeJson<DailyStoriesResponse>(fixtureJson("daily-stories.json"))
        assertEquals("20260816", daily.date)
        assertEquals(9791925L, daily.stories.first().id)
        assertEquals(
            "古代贝壳就是货币，能换百亩良田，为何穷人不去海边多捡点？",
            daily.stories.first().title,
        )
        assertEquals(
            "https://picx.zhimg.com/v2-748b033ca166b5607bf4acd2c748c7fa.jpg?source=8673f162",
            daily.stories
                .first()
                .images
                .single(),
        )

        val mobile = assertNotNull(parseMobileHomeFeedDisplayItem(fixtureJson("mobile-home-card.json").jsonObject))
        assertNotNull(mobile.navDestination)

        val moments = fixtureJson("moments-feed-items.json").jsonArray.map { ZhihuJson.decodeJson<Feed>(it) }
        val voteupAnswer = assertIs<CommonFeed>(moments[0])
        val voteupArticle = assertIs<CommonFeed>(moments[1])
        assertTrue(voteupAnswer.sourceLabel?.endsWith("赞同了回答") == true)
        assertTrue(voteupArticle.sourceLabel?.endsWith("赞同了文章") == true)
        assertNull(GroupFeed(brief = "", groupText = "代表性分组", list = listOf(voteupAnswer)).sourceLabel)

        val notification = ZhihuJson.decodeJson<MobileNotificationTimelineItem>(
            fixtureJson("notification-timeline-item.json"),
        )
        assertEquals("aggregate_notification", notification.type)
        assertTrue(notification.stableId.isNotBlank())

        val pin = assertIs<Feed.PinTarget>(decodeTarget("pin-target.json"))
        assertTrue(pin.content.isNotEmpty())
        assertTrue(pin.author.id.startsWith("redacted-person-"))
    }

    private fun decodeTarget(name: String) = ZhihuJson.decodeJson<Feed.Target>(fixtureJson(name))

    private fun fixtureJson(name: String) =
        ZhihuJson.json.parseToJsonElement(checkNotNull(javaClass.getResource("/real-api/$name")).readText())
}
