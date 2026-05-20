package com.github.zly2006.zhihu.shared.util

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class ZseSignerTest {
    @Test
    fun encryptZseV4IsDeterministic() {
        val first = ZseSigner.encryptZseV4("hello")
        val second = ZseSigner.encryptZseV4("hello")

        assertEquals(first, second)
        assertTrue(first.isNotBlank())
    }

    @Test
    fun encryptZseV4ChangesWithInput() {
        assertNotEquals(
            ZseSigner.encryptZseV4("hello"),
            ZseSigner.encryptZseV4("world"),
        )
    }
}
