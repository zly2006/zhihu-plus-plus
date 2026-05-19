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

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OfficialBadgeTest {
    @Test
    fun extractsPassedMergedBadge() {
        val badge = DataHolder
            .BadgeV2(
                title = "英语等 5 个话题下的优秀答主",
                mergedBadges = listOf(
                    Json
                        .parseToJsonElement(
                            """
                            {
                              "type": "best",
                              "detail_type": "best",
                              "title": "优秀答主",
                              "description": "英语等 5 个话题下的优秀答主",
                              "badge_status": "passed"
                            }
                            """.trimIndent(),
                        ).jsonObject,
                ),
            ).officialBadge()

        assertEquals(
            OfficialBadge(
                title = "优秀答主",
                description = "英语等 5 个话题下的优秀答主",
                type = "best",
                detailType = "best",
            ),
            badge,
        )
    }

    @Test
    fun ignoresNonPassedBadge() {
        val badge = DataHolder
            .BadgeV2(
                title = "",
                mergedBadges = listOf(
                    Json
                        .parseToJsonElement(
                            """
                            {
                              "title": "优秀答主",
                              "description": "英语话题下的优秀答主",
                              "badge_status": "failed"
                            }
                            """.trimIndent(),
                        ).jsonObject,
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
                Json
                    .parseToJsonElement(
                        """
                        {
                          "type": "identity",
                          "detail_type": "identity_people",
                          "title": "认证",
                          "description": "华东师范大学 理学硕士",
                          "badge_status": "passed"
                        }
                        """.trimIndent(),
                    ).jsonObject,
            ),
            detailBadges = listOf(
                Json
                    .parseToJsonElement(
                        """
                        {
                          "type": "reward",
                          "detail_type": "super_activity",
                          "title": "社区成就",
                          "description": "知势榜教育校园领域影响力榜答主",
                          "icon": "https://picx.zhimg.com/detail_reward.png",
                          "night_icon": "https://picx.zhimg.com/detail_reward_night.png",
                          "badge_status": "passed"
                        }
                        """.trimIndent(),
                    ).jsonObject,
                Json
                    .parseToJsonElement(
                        """
                        {
                          "type": "identity",
                          "detail_type": "identity_people",
                          "title": "已认证的个人",
                          "description": "华东师范大学 理学硕士",
                          "icon": "https://picx.zhimg.com/detail_identity.png",
                          "badge_status": "passed"
                        }
                        """.trimIndent(),
                    ).jsonObject,
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
}
