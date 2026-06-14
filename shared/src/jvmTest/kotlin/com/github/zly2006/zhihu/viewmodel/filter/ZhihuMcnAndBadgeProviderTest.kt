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

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ZhihuMcnAndBadgeProviderTest {
    @Test
    fun requestsMembersApiAndReadsMcnCompanyAndBadge() = runTest {
        val requests = mutableListOf<HttpRequestData>()
        val client = HttpClient(
            MockEngine { request ->
                requests += request
                respond(
                    content =
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
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            },
        ) {
            install(ContentNegotiation) {
                json()
            }
        }

        val provider = ZhihuMcnAndBadgeProvider(client)
        val profile = provider.getAuthorProfile("ddaa117")

        assertEquals("杭州含章文化传播有限公司", profile.mcnCompany)
        assertEquals("已认证的个人", profile.officialBadge?.title)
        assertEquals("https://pic.example/top.png", profile.officialBadge?.iconUrl)
        assertEquals("www.zhihu.com", requests.single().url.host)
        assertEquals("/api/v4/members/ddaa117", requests.single().url.encodedPath)
        assertEquals("badge,mcn_company", requests.single().url.parameters["include"])
    }

    @Test
    fun returnsEmptyProfileWhenFieldsAreMissing() = runTest {
        val client = HttpClient(
            MockEngine {
                respond(
                    content =
                        """
                        {
                          "id": "2cdae0466ed67e91366c971f3ad5ecaa"
                        }
                        """.trimIndent(),
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            },
        ) {
            install(ContentNegotiation) {
                json()
            }
        }

        val provider = ZhihuMcnAndBadgeProvider(client)
        val profile = provider.getAuthorProfile("ddaa117")

        assertNull(profile.mcnCompany)
        assertNull(profile.officialBadge)
    }
}
