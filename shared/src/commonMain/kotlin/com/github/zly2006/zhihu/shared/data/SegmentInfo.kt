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

package com.github.zly2006.zhihu.shared.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive

object BooleanCompatSerializer : KSerializer<Boolean> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("BooleanCompat", PrimitiveKind.BOOLEAN)

    override fun deserialize(decoder: Decoder): Boolean = when (decoder) {
        is JsonDecoder -> {
            val primitive = decoder.decodeJsonElement().jsonPrimitive
            primitive.booleanOrNull
                ?: primitive.intOrNull?.let { it != 0 }
                ?: primitive.content.equals("true", ignoreCase = true)
        }

        else -> decoder.decodeBoolean()
    }

    override fun serialize(
        encoder: Encoder,
        value: Boolean,
    ) {
        encoder.encodeBoolean(value)
    }
}

@Serializable
data class SegmentInfoParagraph(
    val pid: String,
    val text: String,
    val marks: List<SegmentInfoMark> = emptyList(),
)

@Serializable
data class SegmentInfoMark(
    val startIndex: Int,
    val endIndex: Int,
    val segInfo: SegmentInfoMeta? = null,
    val masterSegInfo: SegmentInfoMeta? = null,
)

val SegmentInfoMark.effectiveSegInfo: SegmentInfoMeta?
    get() = segInfo ?: masterSegInfo

@Serializable
data class SegmentInfoMeta(
    val segIds: List<String> = emptyList(),
    @Serializable(with = BooleanCompatSerializer::class)
    val isLike: Boolean = false,
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val myCommentCount: Int = 0,
    @Serializable(with = BooleanCompatSerializer::class)
    val isSpan: Boolean = false,
)
