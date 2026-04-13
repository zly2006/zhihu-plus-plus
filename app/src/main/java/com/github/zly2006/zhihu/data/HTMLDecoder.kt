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

package com.github.zly2006.zhihu.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.jsoup.Jsoup

object HTMLDecoder : KSerializer<String> {
    override val descriptor = PrimitiveSerialDescriptor("HTMLDecoder", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: String) {
        // never used
        encoder.encodeString(value)
    }

    override fun deserialize(decoder: Decoder): String {
        val string = decoder.decodeString()
        return Jsoup
            .parse(string)
            .apply {
                selectStream(".invisible").forEach { it.remove() }
            }.text()
    }
}
