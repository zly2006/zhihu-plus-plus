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

import com.github.zly2006.zhihu.shared.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.url
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject

class ZhihuMcnCompanyProvider(
    private val httpClient: HttpClient,
    private val configureRequest: (HttpRequestBuilder) -> Unit = {},
) : McnCompanyProvider {
    override suspend fun getMcnCompany(urlToken: String): String? {
        var successfulResponses = 0
        var lastError: Throwable? = null
        listOf<suspend () -> String?>(
            {
                fetchMcnCompanyFromPeopleApi(urlToken)
            },
            {
                fetchMcnCompanyFromMembersApi(urlToken)
            },
        ).forEach { fetch ->
            runCatching { fetch() }
                .onSuccess { company ->
                    successfulResponses += 1
                    company.normalizeMcnCompany()?.let { return it }
                }.onFailure { error ->
                    lastError = error
                    Log.e("ZhihuMcnCompanyProvider", "Failed to fetch MCN company for $urlToken", error)
                }
        }
        if (successfulResponses == MCN_LOOKUP_ENDPOINT_COUNT) return null
        throw lastError ?: IllegalStateException("Failed to fetch MCN company for $urlToken")
    }

    private suspend fun fetchMcnCompanyFromPeopleApi(urlToken: String): String? {
        val user = httpClient
            .get("https://api.zhihu.com/people/$urlToken") {
                url {
                    parameters["include"] = "badge_v2,mcn_company,mcnCompany"
                }
                configureRequest(this)
            }.body<JsonElement>()
        return extractMcnCompanyFromPeopleApi(user)
    }

    private suspend fun fetchMcnCompanyFromMembersApi(urlToken: String): String? {
        val user = httpClient
            .get("https://www.zhihu.com/api/v4/members/$urlToken") {
                url {
                    parameters["include"] = "badge_v2,mcn_company,mcnCompany,mcn_user_info"
                }
                configureRequest(this)
            }.body<JsonElement>()
        return extractMcnCompanyFromPeopleApi(user)
    }
}

private const val MCN_LOOKUP_ENDPOINT_COUNT = 2

internal fun extractMcnCompanyFromPeopleApi(user: JsonElement): String? {
    val root = user.jsonObject
    findMcnCompanyString(root)?.let { return it }

    val badgeV2 = root["badge_v2"]?.jsonObject ?: root["badgeV2"]?.jsonObject
    if (badgeV2 == null) {
        return null
    }
    val badges = listOfNotNull(
        badgeV2["detail_badges"]?.jsonArray,
        badgeV2["detailBadges"]?.jsonArray,
        badgeV2["merged_badges"]?.jsonArray,
        badgeV2["mergedBadges"]?.jsonArray,
    ).flatten()

    return badges.firstNotNullOfOrNull { badge ->
        val badgeObject = badge as? JsonObject ?: return@firstNotNullOfOrNull null
        val isMcnBadge = listOf("type", "detail_type", "detailType", "title")
            .mapNotNull { key -> findString(badgeObject, key) }
            .any { it.contains("mcn", ignoreCase = true) || it.contains("MCN", ignoreCase = true) }
        if (!isMcnBadge) return@firstNotNullOfOrNull null

        badgeObject["sources"]
            ?.takeIf { it is JsonArray }
            ?.jsonArray
            ?.firstNotNullOfOrNull { source ->
                (source as? JsonObject)?.let { findString(it, "name") }
            }
            ?: findString(badgeObject, "description")?.takeUnless { it.equals("MCN", ignoreCase = true) }
    }
}

private fun findMcnCompanyString(jsonObject: JsonObject): String? {
    findString(jsonObject, "mcnCompany", "mcn_company")?.let { return it }
    jsonObject.forEach { (key, value) ->
        if (key.contains("mcn", ignoreCase = true)) {
            when (value) {
                is JsonObject -> {
                    findString(value, "company", "companyName", "organization", "organizationName", "name")?.let { return it }
                    findMcnCompanyString(value)?.let { return it }
                }
                is JsonArray ->
                    value
                        .firstNotNullOfOrNull { item ->
                            (item as? JsonObject)?.let { itemObject ->
                                findString(itemObject, "company", "companyName", "organization", "organizationName", "name")
                                    ?: findMcnCompanyString(itemObject)
                            }
                        }?.let { return it }
                else -> Unit
            }
        }
    }
    return null
}

private fun findString(
    jsonObject: JsonObject,
    vararg keys: String,
): String? = keys.firstNotNullOfOrNull { key ->
    (jsonObject[key] as? JsonPrimitive)
        ?.contentOrNull
        ?.takeIf { it.isNotBlank() }
}

internal fun String?.normalizeMcnCompany(): String? {
    val value = this?.trim()?.takeIf { it.isNotBlank() } ?: return null
    return value.takeUnless { it.equals("false", ignoreCase = true) || it.equals("true", ignoreCase = true) }
}
