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

package com.github.zly2006.zhihu.shared.aigc

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AigcVoteClientTest {
    @Test
    fun readEvidenceRequiresRealForegroundReadingSignals() {
        assertFalse(
            AigcVoteReadEvidence(
                foregroundDurationMs = 2_000,
                maxScrollRatio = 0.9,
                openedAtEpochSeconds = 1_781_020_123,
            ).isEligibleForCredit(),
        )
        assertFalse(
            AigcVoteReadEvidence(
                foregroundDurationMs = 20_000,
                maxScrollRatio = 0.05,
                openedAtEpochSeconds = 1_781_020_123,
            ).isEligibleForCredit(),
        )
        assertTrue(
            AigcVoteReadEvidence(
                foregroundDurationMs = 20_000,
                maxScrollRatio = 0.35,
                openedAtEpochSeconds = 1_781_020_123,
            ).isEligibleForCredit(),
        )
    }

    @Test
    fun syncReadEventPostsContentVersionAndEvidence() = runTest {
        val client = AigcVoteClient(
            httpClient = mockClient { requestBody ->
                assertEquals("/v1/read-events:batch", requestBody.path)
                assertEquals(
                    "answer",
                    requestBody.json["events"]!!
                        .jsonArraySingle()["content_type"]!!
                        .jsonPrimitive.content,
                )
                assertEquals(
                    "42",
                    requestBody.json["events"]!!
                        .jsonArraySingle()["content_id"]!!
                        .jsonPrimitive.content,
                )
                assertEquals(
                    "测试回答",
                    requestBody.json["events"]!!
                        .jsonArraySingle()["title"]!!
                        .jsonPrimitive.content,
                )
                assertEquals(
                    "author-md5",
                    requestBody.json["events"]!!
                        .jsonArraySingle()["author_hash"]!!
                        .jsonPrimitive.content,
                )
                assertEquals(
                    "<p>正文</p>",
                    requestBody.json["events"]!!
                        .jsonArraySingle()["content_html"]!!
                        .jsonPrimitive.content,
                )
                assertEquals(
                    "1781020000",
                    requestBody.json["events"]!!
                        .jsonArraySingle()["content_updated_at"]!!
                        .jsonPrimitive.content,
                )
                assertEquals(
                    "45000",
                    requestBody.json["events"]!!
                        .jsonArraySingle()["foreground_duration_ms"]!!
                        .jsonPrimitive.content,
                )
            },
            baseUrl = "http://127.0.0.1:8787",
            clientId = "client-a",
        )

        val response = client.syncReadEvent(
            AigcVoteReadEvent(
                contentType = "answer",
                contentId = "42",
                title = "测试回答",
                authorHash = "author-md5",
                contentHtml = "<p>正文</p>",
                contentUpdatedAt = 1_781_020_000,
                evidence = AigcVoteReadEvidence(
                    foregroundDurationMs = 45_000,
                    maxScrollRatio = 0.82,
                    openedAtEpochSeconds = 1_781_020_123,
                ),
            ),
        )

        assertEquals(3, response.credit)
        assertEquals(27, response.progress)
        assertEquals(5, response.cap)
    }

    @Test
    fun submitFlagPostsHtmlAndContentUpdatedAt() = runTest {
        val client = AigcVoteClient(
            httpClient = mockClient { requestBody ->
                assertEquals("/v1/contents/article/99/aigc-flag", requestBody.path)
                assertEquals(
                    "voter-a",
                    requestBody.json["voter"]!!
                        .jsonObject["id"]!!
                        .jsonPrimitive.content,
                )
                assertEquals(
                    "投票人 A",
                    requestBody.json["voter"]!!
                        .jsonObject["name"]!!
                        .jsonPrimitive.content,
                )
                assertEquals("疑似 AIGC 文章", requestBody.json["title"]!!.jsonPrimitive.content)
                assertEquals("<p>完整 HTML</p>", requestBody.json["content_html"]!!.jsonPrimitive.content)
                assertEquals("1781020000", requestBody.json["content_updated_at"]!!.jsonPrimitive.content)
                assertEquals(
                    "45000",
                    requestBody.json["evidence"]!!
                        .jsonObject["client_view_duration_ms"]!!
                        .jsonPrimitive.content,
                )
            },
            baseUrl = "http://127.0.0.1:8787/",
            clientId = "client-a",
        )

        val response = client.submitFlag(
            AigcVoteFlagSubmission(
                contentType = "article",
                contentId = "99",
                voter = AigcVoteVoter(
                    id = "voter-a",
                    name = "投票人 A",
                    urlToken = "voter-a-token",
                    avatarUrl = "https://pic.example/avatar.jpg",
                ),
                title = "疑似 AIGC 文章",
                authorHash = "author-hash",
                contentHtml = "<p>完整 HTML</p>",
                contentUpdatedAt = 1_781_020_000,
                evidence = AigcVoteReadEvidence(
                    foregroundDurationMs = 45_000,
                    maxScrollRatio = 0.82,
                    openedAtEpochSeconds = 1_781_020_123,
                ),
            ),
        )

        assertTrue(response.myFlagged)
        assertEquals(2, response.credit)
        assertFalse(response.creditBypassAvailable)
        assertEquals(8, response.effectiveFlagCount)
        assertEquals(5, response.currentVersionFlagCount)
        assertEquals("medium", response.confidence)
        assertEquals("投票人 A", response.voters.single().voterName)
    }

    @Test
    fun getFlagStatusSendsNamedVoterIdentity() = runTest {
        val client = AigcVoteClient(
            httpClient = HttpClient(
                MockEngine { request ->
                    assertEquals(HttpMethod.Get, request.method)
                    assertEquals("/v1/contents/answer/42/aigc-flag", request.url.encodedPath)
                    assertEquals("client-a", request.url.parameters["client_id"])
                    assertEquals("voter-a", request.url.parameters["voter_id"])
                    assertEquals("投票人 A", request.url.parameters["voter_name"])
                    assertEquals("voter-a-token", request.url.parameters["voter_url_token"])
                    respond(
                        content =
                            """
                            {
                              "my_flagged": false,
                              "credit": 0,
                              "progress": 0,
                              "cap": 5,
                              "credit_bypass_available": true,
                              "effective_flag_count": 0,
                              "raw_flag_count": 0,
                              "confidence": "low",
                              "voters": []
                            }
                            """.trimIndent(),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                },
            ) {
                install(ContentNegotiation) {
                    json(Json { ignoreUnknownKeys = true })
                }
            },
            baseUrl = "http://127.0.0.1:8787",
            clientId = "client-a",
        )

        val response = client.getFlagStatus(
            contentType = "answer",
            contentId = "42",
            voter = AigcVoteVoter(
                id = "voter-a",
                name = "投票人 A",
                urlToken = "voter-a-token",
            ),
        )

        assertTrue(response.creditBypassAvailable)
        assertFalse(response.myFlagged)
    }

    private fun mockClient(assertBody: (CapturedRequestBody) -> Unit): HttpClient = HttpClient(
        MockEngine { request ->
            assertEquals(HttpMethod.Post, request.method)
            val bodyText = (request.body as TextContent).text
            val json = Json.parseToJsonElement(bodyText).jsonObject
            assertEquals("client-a", json["client_id"]!!.jsonPrimitive.content)
            val path = request.url.encodedPath
            assertBody(CapturedRequestBody(path, json))
            respond(
                content = if (path == "/v1/read-events:batch") {
                    """
                    {
                      "credit": 3,
                      "progress": 27,
                      "cap": 5,
                      "accepted_events": 1,
                      "rejected_events": 0
                    }
                    """.trimIndent()
                } else {
                    """
                    {
                      "credit": 2,
                      "my_flagged": true,
                      "effective_flag_count": 8,
                      "raw_flag_count": 9,
                      "current_version_flag_count": 5,
                      "content_hash": "abc",
                      "content_updated_at": 1781020000,
                      "confidence": "medium",
                      "credit_bypass_available": false,
                      "voters": [
                        {
                          "voter_id": "voter-a",
                          "voter_name": "投票人 A",
                          "voter_url_token": "voter-a-token",
                          "voter_avatar_url": "https://pic.example/avatar.jpg",
                          "created_at": 1781020100,
                          "credit_bypassed": false
                        }
                      ]
                    }
                    """.trimIndent()
                },
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        },
    ) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }
}

private data class CapturedRequestBody(
    val path: String,
    val json: kotlinx.serialization.json.JsonObject,
)

private fun kotlinx.serialization.json.JsonElement.jsonArraySingle(): kotlinx.serialization.json.JsonObject =
    jsonArray.single().jsonObject
