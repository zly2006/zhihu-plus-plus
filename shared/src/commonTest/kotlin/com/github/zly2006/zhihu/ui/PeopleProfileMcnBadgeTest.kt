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
import com.github.zly2006.zhihu.shared.data.mcnOfficialBadge
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class PeopleProfileMcnBadgeTest {
    @Test
    fun mapsMcnCompanyToProfileBadge() {
        val people = ZhihuJson.decodeJson<DataHolder.People>(
            Json.parseToJsonElement(
                """
                {
                  "id": "2cdae0466ed67e91366c971f3ad5ecaa",
                  "url_token": "ddaa117",
                  "name": "DDAA117",
                  "avatar_url": "https://picx.zhimg.com/avatar.jpg",
                  "headline": "欢迎关注电脑吧评测室",
                  "gender": 1,
                  "url": "https://www.zhihu.com/api/v4/members/ddaa117",
                  "mcn_company": "杭州含章文化传播有限公司",
                  "follower_count": 100,
                  "following_count": 2,
                  "answer_count": 3,
                  "articles_count": 4,
                  "question_count": 5,
                  "pins_count": 6
                }
                """.trimIndent(),
            ),
        )

        val profile = toPeopleProfileLoadResult(people, isBlockedInRecommendations = false).profile

        assertEquals(mcnOfficialBadge("杭州含章文化传播有限公司"), profile.officialBadge)
        assertEquals(listOf(mcnOfficialBadge("杭州含章文化传播有限公司")), profile.officialBadgeDetails)
        assertEquals(5, people.questionCount)
        assertEquals(6, people.pinsCount)
    }
}
