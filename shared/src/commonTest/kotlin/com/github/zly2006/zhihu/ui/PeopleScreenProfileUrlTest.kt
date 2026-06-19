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

package com.github.zly2006.zhihu.ui

import com.github.zly2006.zhihu.navigation.Person
import kotlin.test.Test
import kotlin.test.assertEquals

class PeopleScreenProfileUrlTest {
    @Test
    fun usesMemberEndpointWithUrlToken() {
        assertEquals(
            "https://www.zhihu.com/api/v4/members/dong-xiao-fang-33",
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
            "https://www.zhihu.com/api/v4/members/c7d6ee7380aba6cc6c131d02b26b84b9",
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
    fun requestsMemberBadgeIncludeForBadgeV2() {
        assertEquals(
            "allow_message,is_followed,is_following,is_org,is_blocking,badge,mcn_company,answer_count,follower_count,following_count,articles_count,question_count,pins_count",
            PEOPLE_PROFILE_INCLUDE_PATH,
        )
    }
}
