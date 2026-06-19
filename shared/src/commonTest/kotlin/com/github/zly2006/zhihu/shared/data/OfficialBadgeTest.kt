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

package com.github.zly2006.zhihu.shared.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OfficialBadgeTest {
    @Test
    fun decodesOfficialBadgeWithDefaults() {
        val badge = ZhihuJson.json.decodeFromString<OfficialBadge>(
            """
            {
              "title": "优秀答主",
              "description": "科学话题优秀答主"
            }
            """.trimIndent(),
        )

        assertEquals("优秀答主", badge.title)
        assertEquals("科学话题优秀答主", badge.description)
        assertEquals("", badge.iconUrl)
        assertTrue(badge.isUsefulInList)
    }

    @Test
    fun genericCertificationIsNotUsefulInList() {
        val badge = OfficialBadge(
            title = "认证",
            description = "认证信息",
        )

        assertTrue(badge.isGenericCertification)
        assertFalse(badge.isUsefulInList)
    }
}
