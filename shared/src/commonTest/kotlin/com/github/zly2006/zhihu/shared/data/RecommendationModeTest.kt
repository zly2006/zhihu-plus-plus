package com.github.zly2006.zhihu.shared.data

import kotlin.test.Test
import kotlin.test.assertEquals

class RecommendationModeTest {
    @Test
    fun keepsPersistedKeysStable() {
        assertEquals("server", RecommendationMode.WEB.key)
        assertEquals("android", RecommendationMode.ANDROID.key)
        assertEquals("local", RecommendationMode.LOCAL.key)
        assertEquals("mixed", RecommendationMode.MIXED.key)
    }
}
