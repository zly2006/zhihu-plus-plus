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

package com.github.zly2006.zhihu.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals

class PaginatedListKeyTest {
    @Test
    fun uniqueKeysStayUnchanged() {
        val keys = uniquePaginatedListKeys(listOf("answer:1", "article:1", "answer:2")) { it }

        assertEquals(listOf("answer:1", "article:1", "answer:2"), keys)
    }

    @Test
    fun duplicateKeysKeepFirstKeyAndSuffixLaterOccurrences() {
        val keys = uniquePaginatedListKeys(listOf(2544209984L, 2544209984L, 42L, 2544209984L)) { it }

        assertEquals(
            listOf(
                2544209984L,
                "PaginatedListDuplicateKey:1:0:2544209984",
                42L,
                "PaginatedListDuplicateKey:2:0:2544209984",
            ),
            keys,
        )
    }

    @Test
    fun duplicateKeysAvoidExistingStringKeyCollisions() {
        val collidingKey = "PaginatedListDuplicateKey:1:0:same"
        val keys = uniquePaginatedListKeys(listOf("same", "same", collidingKey)) { it }

        assertEquals(
            listOf(
                "same",
                "PaginatedListDuplicateKey:1:1:same",
                collidingKey,
            ),
            keys,
        )
    }
}
