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

package com.github.zly2006.zhihu

import com.github.zly2006.zhihu.viewmodel.feed.SearchContentType
import com.github.zly2006.zhihu.viewmodel.feed.SearchSortOption
import com.github.zly2006.zhihu.viewmodel.feed.SearchTimeRange
import com.github.zly2006.zhihu.viewmodel.feed.zhihuSearchUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URL
import java.net.URLDecoder

class SearchViewModelUrlTest {
    @Test
    fun buildsNormalSearchUrlWithoutFilterParameters() {
        val url = URL(zhihuSearchUrl("deepseek"))
        val params = url.queryParameters()

        assertEquals("www.zhihu.com", url.host)
        assertEquals("/api/v4/search_v3", url.path)
        assertEquals("gz-gaokao", params["gk_version"])
        assertEquals("general", params["t"])
        assertEquals("deepseek", params["q"])
        assertEquals("1", params["correction"])
        assertEquals("0", params["offset"])
        assertEquals("20", params["limit"])
        assertEquals("Normal", params["search_source"])
        assertEquals("0", params["show_all_topics"])
        assertNull(params["sort"])
        assertNull(params["vertical"])
        assertNull(params["vertical_info"])
        assertNull(params["time_interval"])
    }

    @Test
    fun encodesQueryAndUsesZhihuFilterParameters() {
        val url = zhihuSearchUrl(
            query = "知乎 搜索/排序",
            sortOption = SearchSortOption.Latest,
            contentType = SearchContentType.Answer,
            timeRange = SearchTimeRange.Week,
        )
        val params = URL(url).queryParameters()

        assertEquals("知乎 搜索/排序", params["q"])
        assertEquals("Filter", params["search_source"])
        assertEquals("created_time", params["sort"])
        assertEquals("answer", params["vertical"])
        assertEquals("0,0,0,0,0,0,0,0,0,0,0,0", params["vertical_info"])
        assertEquals("a_week", params["time_interval"])
        assertTrue(url.contains("q=%E7%9F%A5%E4%B9%8E+%E6%90%9C%E7%B4%A2%2F%E6%8E%92%E5%BA%8F"))
        assertTrue(url.contains("vertical_info=0%2C0%2C0%2C0%2C0%2C0%2C0%2C0%2C0%2C0%2C0%2C0"))
    }

    @Test
    fun treatsAnySingleNonDefaultFilterAsFilterSearch() {
        assertEquals(
            "Filter",
            URL(zhihuSearchUrl("query", sortOption = SearchSortOption.MostVoted))
                .queryParameters()["search_source"],
        )
        assertEquals(
            "Filter",
            URL(zhihuSearchUrl("query", contentType = SearchContentType.Article))
                .queryParameters()["search_source"],
        )
        assertEquals(
            "Filter",
            URL(zhihuSearchUrl("query", timeRange = SearchTimeRange.Year))
                .queryParameters()["search_source"],
        )
    }

    @Test
    fun buildsMemberRestrictedSearchUrlWithEncodedRestrictionParameters() {
        val url = zhihuSearchUrl(
            query = "用户 创作",
            restrictedMemberHashId = "member hash/id",
        )
        val params = URL(url).queryParameters()

        assertEquals("用户 创作", params["q"])
        assertEquals("Normal", params["search_source"])
        assertEquals("", params["filter_fields"])
        assertEquals("0", params["lc_idx"])
        assertEquals("member", params["restricted_scene"])
        assertEquals("member_hash_id", params["restricted_field"])
        assertEquals("member hash/id", params["restricted_value"])
        assertTrue(url.contains("restricted_value=member+hash%2Fid"))
    }

    private fun URL.queryParameters(): Map<String, String> =
        query
            .split("&")
            .associate { pair ->
                val key = pair.substringBefore("=")
                val value = pair.substringAfter("=", "")
                key to URLDecoder.decode(value, "UTF-8")
            }
}
