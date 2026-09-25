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

package com.github.zly2006.zhihu.util

import kotlin.test.Test
import kotlin.test.assertEquals

class Sha256Test {
    @Test
    fun matchesNistVectors() {
        assertEquals("e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855", sha256Hex(""))
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", sha256Hex("abc"))
        assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824", sha256Hex("hello"))
    }

    /** 补位在块边界附近的取值最容易写错，这里覆盖 55/56/64/65 字节四种临界长度。 */
    @Test
    fun padsAroundBlockBoundary() {
        assertEquals("9f4390f8d30c2dd92ec9f095b65e2b9ae9b0a925a5258e241c9f1e910f734318", sha256Hex("a".repeat(55)))
        assertEquals("b35439a4ac6f0948b6d6f9e3c6af0f5f590ce20f1bde7090ef7970686ec6738a", sha256Hex("a".repeat(56)))
        assertEquals("ffe054fe7ae0cb6dc65c3af9b61d5209f439851db43d0ba5997337df154668eb", sha256Hex("a".repeat(64)))
        assertEquals("635361c48bb9eab14198e76ea8ab7f1a41685d6ad62aa9146d301d4f17eb0ae0", sha256Hex("a".repeat(65)))
    }

    @Test
    fun chunkedUpdatesMatchSingleShotDigest() {
        val message = (1..500).joinToString("") { "chunk-$it;" }

        val chunked = Sha256()
        var offset = 0
        while (offset < message.length) {
            val end = minOf(offset + 7, message.length)
            chunked.update(message.substring(offset, end).encodeToByteArray())
            offset = end
        }

        assertEquals(sha256Hex(message), chunked.digestHex())
    }
}

private fun sha256Hex(text: String): String = Sha256()
    .apply { update(text.encodeToByteArray()) }
    .digestHex()
