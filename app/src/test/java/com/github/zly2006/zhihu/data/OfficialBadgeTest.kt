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

package com.github.zly2006.zhihu.data

import com.github.zly2006.zhihu.shared.data.DataHolder
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OfficialBadgeTest {
    @Test
    fun ignoresTextOnlyMergedBadgeAsPrimaryBadge() {
        val badgeV2 = DataHolder.BadgeV2(
            title = "英语等 5 个话题下的优秀答主",
            mergedBadges = listOf(
                DataHolder.BadgeV2.Badge(
                    type = "best",
                    detailType = "best",
                    title = "优秀答主",
                    description = "英语等 5 个话题下的优秀答主",
                    badgeStatus = "passed",
                ),
            ),
        )

        assertNull(badgeV2.officialBadge())
        assertEquals(
            listOf(
                OfficialBadge(
                    title = "优秀答主",
                    description = "英语等 5 个话题下的优秀答主",
                    type = "best",
                    detailType = "best",
                ),
            ),
            badgeV2.officialBadgeDetails(),
        )
    }

    @Test
    fun ignoresNonPassedBadge() {
        val badge = DataHolder
            .BadgeV2(
                title = "",
                mergedBadges = listOf(
                    DataHolder.BadgeV2.Badge(
                        title = "优秀答主",
                        description = "英语话题下的优秀答主",
                        badgeStatus = "failed",
                    ),
                ),
            ).officialBadge()

        assertNull(badge)
    }

    @Test
    fun usesOfficialContainerIconForPrimaryBadgeAndKeepsDetailBadges() {
        val badgeV2 = DataHolder.BadgeV2(
            title = "华东师范大学 理学硕士",
            icon = "https://picx.zhimg.com/profile_primary_icon.png",
            nightIcon = "https://picx.zhimg.com/profile_primary_icon_night.png",
            mergedBadges = listOf(
                DataHolder.BadgeV2.Badge(
                    type = "identity",
                    detailType = "identity_people",
                    title = "认证",
                    description = "华东师范大学 理学硕士",
                    badgeStatus = "passed",
                ),
            ),
            detailBadges = listOf(
                DataHolder.BadgeV2.Badge(
                    type = "reward",
                    detailType = "super_activity",
                    title = "社区成就",
                    description = "知势榜教育校园领域影响力榜答主",
                    icon = "https://picx.zhimg.com/detail_reward.png",
                    nightIcon = "https://picx.zhimg.com/detail_reward_night.png",
                    badgeStatus = "passed",
                ),
                DataHolder.BadgeV2.Badge(
                    type = "identity",
                    detailType = "identity_people",
                    title = "已认证的个人",
                    description = "华东师范大学 理学硕士",
                    icon = "https://picx.zhimg.com/detail_identity.png",
                    badgeStatus = "passed",
                ),
            ),
        )

        assertEquals(
            OfficialBadge(
                title = "社区成就",
                description = "知势榜教育校园领域影响力榜答主",
                iconUrl = "https://picx.zhimg.com/profile_primary_icon.png",
                nightIconUrl = "https://picx.zhimg.com/profile_primary_icon_night.png",
                type = "reward",
                detailType = "super_activity",
            ),
            badgeV2.officialBadge(),
        )
        assertEquals(
            listOf(
                OfficialBadge(
                    title = "社区成就",
                    description = "知势榜教育校园领域影响力榜答主",
                    iconUrl = "https://picx.zhimg.com/detail_reward.png",
                    nightIconUrl = "https://picx.zhimg.com/detail_reward_night.png",
                    type = "reward",
                    detailType = "super_activity",
                ),
                OfficialBadge(
                    title = "已认证的个人",
                    description = "华东师范大学 理学硕士",
                    iconUrl = "https://picx.zhimg.com/detail_identity.png",
                    type = "identity",
                    detailType = "identity_people",
                ),
            ),
            badgeV2.officialBadgeDetails(),
        )
    }

    @Test
    fun decodesSnakeCaseBadgeV2WithTypedSerializer() {
        val badgeV2 = AccountData.decodeJson<DataHolder.BadgeV2>(
            Json.parseToJsonElement(
                """
                {
                  "title": "笔记本电脑话题下的优秀答主",
                  "merged_badges": [
                    {
                      "type": "best",
                      "detail_type": "best",
                      "title": "优秀答主",
                      "description": "笔记本电脑话题下的优秀答主",
                      "url": "https://www.zhihu.com/question/48509984",
                      "sources": null,
                      "icon": "",
                      "night_icon": "",
                      "badge_status": "passed"
                    }
                  ],
                  "detail_badges": [
                    {
                      "type": "best",
                      "detail_type": "best_answerer",
                      "title": "优秀答主",
                      "description": "笔记本电脑话题下的优秀答主",
                      "url": "https://www.zhihu.com/question/48509984",
                      "sources": [
                        {
                          "id": "19559604",
                          "token": "19559604",
                          "type": "topic",
                          "url": "https://www.zhihu.com/topic/19559604",
                          "name": "笔记本电脑",
                          "avatar_path": "v2-topic",
                          "avatar_url": "https://pic1.zhimg.com/topic.jpg",
                          "description": "",
                          "priority": 0
                        }
                      ],
                      "icon": "https://picx.zhimg.com/detail_best.png",
                      "night_icon": "https://picx.zhimg.com/detail_best_night.png",
                      "badge_status": "passed"
                    }
                  ],
                  "icon": "https://picx.zhimg.com/profile_primary_icon.png",
                  "night_icon": "https://pica.zhimg.com/profile_primary_icon_night.png"
                }
                """.trimIndent(),
            ),
        )

        assertEquals("best_answerer", badgeV2.detailBadges?.single()?.detailType)
        assertEquals(
            "19559604",
            badgeV2.detailBadges
                ?.single()
                ?.sources
                ?.single()
                ?.id,
        )
        assertEquals(
            OfficialBadge(
                title = "优秀答主",
                description = "笔记本电脑话题下的优秀答主",
                iconUrl = "https://picx.zhimg.com/profile_primary_icon.png",
                nightIconUrl = "https://pica.zhimg.com/profile_primary_icon_night.png",
                url = "https://www.zhihu.com/question/48509984",
                type = "best",
                detailType = "best_answerer",
            ),
            badgeV2.officialBadge(),
        )
    }

    @Test
    fun decodesZhihuAdminOfficialAccountCertification() {
        val badgeV2 = AccountData.decodeJson<DataHolder.BadgeV2>(
            Json.parseToJsonElement(
                """
                {
                  "title": "知乎 官方账号",
                  "merged_badges": [
                    {
                      "type": "identity",
                      "detail_type": "identity_people",
                      "title": "认证",
                      "description": "知乎 官方账号",
                      "url": "https://zhuanlan.zhihu.com/p/96956163",
                      "sources": [],
                      "icon": "",
                      "night_icon": "",
                      "badge_status": "passed"
                    }
                  ],
                  "detail_badges": [
                    {
                      "type": "identity",
                      "detail_type": "identity_people",
                      "title": "已认证的个人",
                      "description": "知乎 官方账号",
                      "url": "https://zhuanlan.zhihu.com/p/96956163",
                      "sources": [],
                      "icon": "https://picx.zhimg.com/v2-2ddc5cc683982648f6f123616fb4ec09_l.png?source=32738c0c",
                      "night_icon": "https://picx.zhimg.com/v2-2ddc5cc683982648f6f123616fb4ec09_l.png?source=32738c0c",
                      "badge_status": "passed"
                    }
                  ],
                  "icon": "https://picx.zhimg.com/v2-2ddc5cc683982648f6f123616fb4ec09_l.png?source=32738c0c",
                  "night_icon": "https://picx.zhimg.com/v2-2ddc5cc683982648f6f123616fb4ec09_l.png?source=32738c0c"
                }
                """.trimIndent(),
            ),
        )

        assertEquals(
            OfficialBadge(
                title = "已认证的个人",
                description = "知乎 官方账号",
                iconUrl = "https://picx.zhimg.com/v2-2ddc5cc683982648f6f123616fb4ec09_l.png?source=32738c0c",
                nightIconUrl = "https://picx.zhimg.com/v2-2ddc5cc683982648f6f123616fb4ec09_l.png?source=32738c0c",
                url = "https://zhuanlan.zhihu.com/p/96956163",
                type = "identity",
                detailType = "identity_people",
            ),
            badgeV2.officialBadge(),
        )
        assertEquals(
            listOf(
                OfficialBadge(
                    title = "已认证的个人",
                    description = "知乎 官方账号",
                    iconUrl = "https://picx.zhimg.com/v2-2ddc5cc683982648f6f123616fb4ec09_l.png?source=32738c0c",
                    nightIconUrl = "https://picx.zhimg.com/v2-2ddc5cc683982648f6f123616fb4ec09_l.png?source=32738c0c",
                    url = "https://zhuanlan.zhihu.com/p/96956163",
                    type = "identity",
                    detailType = "identity_people",
                ),
            ),
            badgeV2.officialBadgeDetails(),
        )
    }

    @Test
    fun injectsZhPlusAuthorBadgeForMaintainerPeopleDto() {
        val people = AccountData.decodeJson<DataHolder.People>(
            Json.parseToJsonElement(
                """
                {
                  "id": "ea09b6c82124e0162caa10d658058c10",
                  "url_token": "scanmenge",
                  "name": "实名开导",
                  "avatar_url": "https://pic1.zhimg.com/avatar.jpg",
                  "url": "https://api.zhihu.com/people/scanmenge",
                  "headline": "",
                  "gender": 0,
                  "badge_v2": null
                }
                """.trimIndent(),
            ),
        )

        assertEquals(
            OfficialBadge(
                title = "知乎++",
                description = "作者",
                iconUrl = DataHolder.ZH_PLUS_AUTHOR_BADGE_ICON,
                nightIconUrl = DataHolder.ZH_PLUS_AUTHOR_BADGE_ICON,
                type = "zhihu_plus_author",
                detailType = "zhihu_plus_author",
            ),
            people.badgeV2.officialBadge(),
        )
    }

    @Test
    fun injectsZhPlusAuthorBadgeForMaintainerFeedAuthorDto() {
        val person = AccountData.decodeJson<Person>(
            Json.parseToJsonElement(
                """
                {
                  "id": "ea09b6c82124e0162caa10d658058c10",
                  "url": "https://api.zhihu.com/people/scanmenge",
                  "user_type": "people",
                  "url_token": "scanmenge",
                  "name": "实名开导",
                  "headline": "",
                  "avatar_url": "https://pic1.zhimg.com/avatar.jpg",
                  "badge_v2": null
                }
                """.trimIndent(),
            ),
        )

        assertEquals("知乎++", person.badgeV2.officialBadge()?.title)
        assertEquals("作者", person.badgeV2.officialBadge()?.description)
        assertEquals(DataHolder.ZH_PLUS_AUTHOR_BADGE_ICON, person.badgeV2.officialBadge()?.iconUrl)
    }
}
