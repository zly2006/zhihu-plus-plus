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
