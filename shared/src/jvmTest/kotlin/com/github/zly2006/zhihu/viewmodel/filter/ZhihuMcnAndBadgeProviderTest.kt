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

package com.github.zly2006.zhihu.viewmodel.filter

import com.github.zly2006.zhihu.shared.data.ZhihuJson
import com.github.zly2006.zhihu.viewmodel.ZhihuApiEnvironment
import io.ktor.client.HttpClient
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ZhihuMcnAndBadgeProviderTest {
    @Test
    fun requestsMembersApiAndReadsMcnCompanyAndBadge() = runTest {
        val environment = FakeZhihuApiEnvironment {
            ZhihuJson.json
                .parseToJsonElement(
                    """
                    {
                      "id": "2cdae0466ed67e91366c971f3ad5ecaa",
                      "url_token": "ddaa117",
                      "name": "DDAA117",
                      "mcn_company": "杭州含章文化传播有限公司",
                      "badge_v2": {
                        "title": "",
                        "merged_badges": [],
                        "detail_badges": [{
                          "type": "identity",
                          "detail_type": "identity_people",
                          "title": "已认证的个人",
                          "description": "电脑吧评测室",
                          "url": "",
                          "sources": [],
                          "icon": "https://pic.example/badge.png",
                          "night_icon": "https://pic.example/badge-night.png",
                          "badge_status": "passed"
                        }],
                        "icon": "https://pic.example/top.png",
                        "night_icon": "https://pic.example/top-night.png"
                      }
                    }
                    """.trimIndent(),
                ).jsonObject
        }
        val provider = ZhihuMcnAndBadgeProvider(environment)
        val profile = provider.getAuthorProfile("ddaa117")

        assertEquals("杭州含章文化传播有限公司", profile.mcnCompany)
        assertEquals("已认证的个人", profile.officialBadge?.title)
        assertEquals("https://pic.example/top.png", profile.officialBadge?.iconUrl)
        assertEquals("https://www.zhihu.com/api/v4/members/ddaa117", environment.requests.single().first)
        assertEquals("badge,mcn_company", environment.requests.single().second)
    }

    @Test
    fun returnsEmptyProfileWhenFieldsAreMissing() = runTest {
        val environment = FakeZhihuApiEnvironment {
            ZhihuJson.json
                .parseToJsonElement(
                    """
                    {
                      "id": "2cdae0466ed67e91366c971f3ad5ecaa"
                    }
                    """.trimIndent(),
                ).jsonObject
        }
        val provider = ZhihuMcnAndBadgeProvider(environment)
        val profile = provider.getAuthorProfile("ddaa117")

        assertNull(profile.mcnCompany)
        assertNull(profile.officialBadge)
    }

    @Test
    fun returnsEmptyProfileWhenFetchFails() = runTest {
        val provider = ZhihuMcnAndBadgeProvider(FakeZhihuApiEnvironment { null })

        val profile = provider.getAuthorProfile("ddaa117")

        assertNull(profile.mcnCompany)
        assertNull(profile.officialBadge)
    }
}

private class FakeZhihuApiEnvironment(
    private val fetchJsonResult: suspend () -> JsonObject?,
) : ZhihuApiEnvironment {
    val requests = mutableListOf<Pair<String, String>>()

    override fun httpClient(): HttpClient = error("not used")

    override fun authenticatedCookies(): Map<String, String> = emptyMap()

    override suspend fun fetchJson(
        url: String,
        include: String,
    ): JsonObject? {
        requests += url to include
        return fetchJsonResult()
    }

    override suspend fun handleFetchFailure(
        tag: String?,
        error: Exception,
    ) = Unit
}
