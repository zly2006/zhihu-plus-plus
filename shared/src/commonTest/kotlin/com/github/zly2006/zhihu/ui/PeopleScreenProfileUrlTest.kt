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

package com.github.zly2006.zhihu.ui

import com.github.zly2006.zhihu.navigation.Person
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.data.ZhihuJson
import com.github.zly2006.zhihu.viewmodel.ProfileLoadEnvironment
import io.ktor.client.HttpClient
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PeopleScreenProfileUrlTest {
    @Test
    fun usesApiPeopleEndpointWithUrlToken() {
        assertEquals(
            "https://api.zhihu.com/people/dong-xiao-fang-33",
            peopleProfileUrl(
                Person(
                    id = "c7d6ee7380aba6cc6c131d02b26b84b9",
                    name = "铁芒萁的研习社",
                    urlToken = "dong-xiao-fang-33",
                ),
            ),
        )
    }

    @Test
    fun fallsBackToIdWhenUrlTokenIsMissing() {
        assertEquals(
            "https://api.zhihu.com/people/c7d6ee7380aba6cc6c131d02b26b84b9",
            peopleProfileUrl(
                Person(
                    id = "c7d6ee7380aba6cc6c131d02b26b84b9",
                    name = "铁芒萁的研习社",
                    urlToken = "",
                ),
            ),
        )
    }

    @Test
    fun decodesLiveSocialMediaShapeAndMapsGithubStars() {
        val rawProfile = ZhihuJson.json
            .parseToJsonElement(
                """
                {
                  "id": "profile-id",
                  "url_token": "profile-user",
                  "name": "Profile User",
                  "avatar_url": "https://example.invalid/avatar.png",
                  "url": "https://www.zhihu.com/people/profile-user",
                  "headline": "",
                  "gender": 0,
                  "socialMedias": [
                    {
                      "icon": "https://example.invalid/github.png",
                      "title": "GitHub·zly2006",
                      "link": "https://github.com/zly2006",
                      "modules": [
                        {"title": "被关注人", "value": "169"},
                        {"title": "公开仓库数", "value": "119"},
                        {"title": "stars", "value": "4.0k"}
                      ]
                    }
                  ]
                }
                """.trimIndent(),
            ).jsonObject

        val githubSocial = ZhihuJson.decodeJson<DataHolder.People>(rawProfile).githubSocialUiState()

        assertEquals("GitHub·zly2006", githubSocial?.title)
        assertEquals("4.0k", githubSocial?.starCount)
        assertEquals("https://example.invalid/github.png", githubSocial?.iconUrl)
    }

    @Test
    fun ignoresNonGithubAndGithubEntriesWithoutStars() {
        val profile = people(
            socialMedias = listOf(
                DataHolder.SocialMedia(
                    title = "Other",
                    modules = listOf(DataHolder.SocialMediaModule("stars", "10")),
                ),
                DataHolder.SocialMedia(
                    title = "GitHub·zly2006",
                    modules = listOf(DataHolder.SocialMediaModule("公开仓库数", "119")),
                ),
            ),
        )

        assertNull(profile.githubSocialUiState())
    }

    @Test
    fun profileDetailOnlyAddsGithubDataAndKeepsBaseBlockingState() = runTest {
        val environment = RecordingProfileEnvironment(
            baseProfile = people(isBlocking = true).asJsonObject(),
            detailResult = Result.success(
                people(
                    isBlocking = false,
                    socialMedias = listOf(githubSocialMedia()),
                ).asJsonObject(),
            ),
        )
        val viewModel = PersonViewModel(person())

        viewModel.load(environment)

        assertTrue(viewModel.isBlocking)
        assertEquals("4.0k", viewModel.githubSocial?.starCount)
        assertEquals(
            listOf(
                "https://api.zhihu.com/people/profile-user" to PEOPLE_PROFILE_INCLUDE_PATH,
                "https://api.zhihu.com/people/profile-user/profile/detail" to "",
            ),
            environment.requests,
        )
    }

    @Test
    fun profileDetailFailureDoesNotFailBaseProfileLoad() = runTest {
        val environment = RecordingProfileEnvironment(
            baseProfile = people(isBlocking = true).asJsonObject(),
            detailResult = Result.failure(IllegalStateException("optional detail unavailable")),
        )
        val viewModel = PersonViewModel(person())

        viewModel.load(environment)

        assertEquals("Profile User", viewModel.name)
        assertTrue(viewModel.isBlocking)
        assertNull(viewModel.githubSocial)
    }

    private fun person() = Person(
        id = "profile-id",
        name = "Profile User",
        urlToken = "profile-user",
    )

    private fun people(
        isBlocking: Boolean = false,
        socialMedias: List<DataHolder.SocialMedia> = emptyList(),
    ) = DataHolder.People(
        id = "profile-id",
        urlToken = "profile-user",
        name = "Profile User",
        avatarUrl = "https://example.invalid/avatar.png",
        url = "https://www.zhihu.com/people/profile-user",
        headline = "",
        gender = 0,
        isBlocking = isBlocking,
        socialMedias = socialMedias,
    )

    private fun githubSocialMedia() = DataHolder.SocialMedia(
        icon = "https://example.invalid/github.png",
        title = "GitHub·zly2006",
        link = "https://github.com/zly2006",
        modules = listOf(
            DataHolder.SocialMediaModule("被关注人", "169"),
            DataHolder.SocialMediaModule("公开仓库数", "119"),
            DataHolder.SocialMediaModule("stars", "4.0k"),
        ),
    )

    private fun DataHolder.People.asJsonObject(): JsonObject = ZhihuJson.json.encodeToJsonElement(this).jsonObject

    private class RecordingProfileEnvironment(
        private val baseProfile: JsonObject,
        private val detailResult: Result<JsonObject?>,
    ) : ProfileLoadEnvironment {
        val requests = mutableListOf<Pair<String, String>>()

        override fun httpClient(): HttpClient = error("The test overrides fetchJson")

        override fun authenticatedCookies(): Map<String, String> = emptyMap()

        override suspend fun fetchJson(
            url: String,
            include: String,
        ): JsonObject? {
            requests += url to include
            return if (url.endsWith("/profile/detail")) {
                detailResult.getOrThrow()
            } else {
                baseProfile
            }
        }

        override suspend fun handleFetchFailure(
            tag: String?,
            error: Exception,
        ) = Unit
    }
}
