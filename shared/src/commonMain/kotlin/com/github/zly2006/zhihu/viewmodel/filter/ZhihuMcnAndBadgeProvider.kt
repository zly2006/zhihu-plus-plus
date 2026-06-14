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

import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.data.ZhihuJson
import com.github.zly2006.zhihu.shared.data.officialBadge
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.url
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject

class ZhihuMcnAndBadgeProvider(
    private val httpClient: HttpClient,
    private val configureRequest: (HttpRequestBuilder) -> Unit = {},
) : McnAndBadgeProvider {
    override suspend fun getAuthorProfile(urlToken: String): McnAuthorProfile {
        val user = httpClient
            .get("https://www.zhihu.com/api/v4/members/$urlToken") {
                url {
                    parameters["include"] = "badge,mcn_company"
                }
                configureRequest(this)
            }.body<JsonElement>()
        val userObject = user.jsonObject
        val badge = userObject["badge_v2"]
            ?.let { ZhihuJson.decodeJson<DataHolder.BadgeV2>(it) }
            .officialBadge()
        return McnAuthorProfile(
            mcnCompany = (userObject["mcn_company"] as? JsonPrimitive)
                ?.contentOrNull
                .normalizeMcnCompany(),
            officialBadge = badge,
        )
    }
}
