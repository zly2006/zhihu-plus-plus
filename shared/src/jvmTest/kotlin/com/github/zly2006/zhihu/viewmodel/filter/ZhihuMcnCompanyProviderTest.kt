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
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ZhihuMcnCompanyProviderTest {
    @Test
    fun parsesMcnCompanyFromPeopleApiBadgeSources() = runTest {
        val json =
            """
            {
              "id": "user-id",
              "url_token": "lihuawei",
              "name": "李明殊",
              "badge_v2": {
                "detail_badges": [
                  {
                    "type": "mcn",
                    "title": "MCN",
                    "description": "知加传媒（深圳）有限公司",
                    "sources": [
                      { "name": "知加传媒（深圳）有限公司", "type": "mcn" }
                    ]
                  }
                ]
              }
            }
            """.trimIndent()

        assertEquals(
            "知加传媒（深圳）有限公司",
            extractMcnCompanyFromPeopleApi(Json.parseToJsonElement(json)),
        )
    }

    @Test
    fun parsesMcnCompanyFromNestedPeopleApiField() = runTest {
        val json =
            """
            {
              "id": "user-id",
              "url_token": "lihuawei",
              "name": "李明殊",
              "mcn_user_info": {
                "company": "知加传媒（深圳）有限公司"
              }
            }
            """.trimIndent()

        assertEquals(
            "知加传媒（深圳）有限公司",
            extractMcnCompanyFromPeopleApi(Json.parseToJsonElement(json)),
        )
    }

    @Test
    fun parsesMcnCompanyFromObjectField() = runTest {
        val json =
            """
            {
              "id": "user-id",
              "url_token": "lihuawei",
              "name": "李明殊",
              "mcn_company": {
                "name": "知加传媒（深圳）有限公司"
              }
            }
            """.trimIndent()

        assertEquals(
            "知加传媒（深圳）有限公司",
            extractMcnCompanyFromPeopleApi(Json.parseToJsonElement(json)),
        )
    }

    @Test
    fun returnsNullWhenPeopleApiHasNoMcnCompany() = runTest {
        val json =
            """
            {
              "id": "user-id",
              "url_token": "plain-author",
              "name": "普通作者",
              "is_org": false,
              "badge_v2": {
                "detail_badges": [],
                "merged_badges": []
              }
            }
            """.trimIndent()

        assertNull(extractMcnCompanyFromPeopleApi(Json.parseToJsonElement(json)))
    }

    @Test
    fun returnsNullOnlyWhenBothEndpointsConfirmNoMcn() = runTest {
        val provider = ZhihuMcnCompanyProvider(
            HttpClient(
                MockEngine {
                    respond(
                        content = NO_MCN_USER_JSON,
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                    )
                },
            ) {
                install(ContentNegotiation) { json(Json) }
            },
        )

        assertNull(provider.getMcnCompany("plain-author"))
    }

    @Test
    fun treatsPartialEndpointFailureAsUnknownInsteadOfNoMcn() = runTest {
        val provider = ZhihuMcnCompanyProvider(
            HttpClient(
                MockEngine { request ->
                    if (request.url.host == "api.zhihu.com") {
                        respond(
                            content = NO_MCN_USER_JSON,
                            status = HttpStatusCode.OK,
                            headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()),
                        )
                    } else {
                        error("members endpoint failed")
                    }
                },
            ) {
                install(ContentNegotiation) { json(Json) }
            },
        )

        assertFailsWith<IllegalStateException> {
            provider.getMcnCompany("plain-author")
        }
    }
}

private const val NO_MCN_USER_JSON =
    """
    {
      "id": "user-id",
      "url_token": "plain-author",
      "name": "普通作者",
      "is_org": false,
      "badge_v2": {
        "detail_badges": [],
        "merged_badges": []
      }
    }
    """
