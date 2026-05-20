package com.github.zly2006.zhihu.shared

import kotlin.test.Test
import kotlin.test.assertTrue

class SharedCommonTest {
    @Test
    fun platformNameIsProvided() {
        assertTrue(platformName.isNotBlank())
    }
}
