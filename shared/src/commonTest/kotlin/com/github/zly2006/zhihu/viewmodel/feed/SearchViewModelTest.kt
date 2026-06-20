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

package com.github.zly2006.zhihu.viewmodel.feed

import io.ktor.http.Url
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SearchViewModelTest {
    @Test
    fun globalSearchUrlDoesNotCarryMemberRestriction() {
        val url = Url(SearchViewModel("kmp 搜索").initialRequestUrl)

        assertEquals("kmp 搜索", url.parameters["q"])
        assertNull(url.parameters["restricted_scene"])
        assertNull(url.parameters["restricted_field"])
        assertNull(url.parameters["restricted_value"])
    }

    @Test
    fun memberScopedSearchUrlCarriesZhihuRestrictionFields() {
        val url = Url(SearchViewModel("用户创作", restrictedMemberHashId = "member-hash-id").initialRequestUrl)

        assertEquals("用户创作", url.parameters["q"])
        assertEquals("member", url.parameters["restricted_scene"])
        assertEquals("member_hash_id", url.parameters["restricted_field"])
        assertEquals("member-hash-id", url.parameters["restricted_value"])
        assertEquals("Normal", url.parameters["search_source"])
        assertEquals("0", url.parameters["lc_idx"])
    }
}
