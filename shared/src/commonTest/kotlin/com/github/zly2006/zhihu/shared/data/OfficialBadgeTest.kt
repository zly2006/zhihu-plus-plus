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
