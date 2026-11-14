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

package com.github.zly2006.zhihu.account

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PhoneCountriesTest {
    @Test
    fun publicCountriesResponseDecodesThroughLoginClient() = runTest {
        // 2026-10-27 采集自 https://api.zhihu.com/sms/supported_countries，保持原始响应。
        val response = checkNotNull(javaClass.getResource("/phone-login/supported-countries-2026-10-27.json")).readText()
        accountHttpClientEngineForTesting = MockEngine { request ->
            assertEquals("https://api.zhihu.com/sms/supported_countries", request.url.toString())
            respond(response, headers = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString()))
        }
        val client = ZhihuPhoneLoginClient()
        try {
            val countries = client.supportedCountries()
            assertEquals(182, countries.size)
            assertTrue(ZhihuPhoneCountry("中国香港", "+852") in countries)
            assertTrue(ZhihuPhoneCountry("中国台湾", "+886") in countries)
            assertEquals(2, countries.count { it.code == "+1" })
        } finally {
            client.close()
            accountHttpClientEngineForTesting = null
        }
    }
}
