package com.github.zly2006.zhihu.shared.data

import kotlinx.serialization.KSerializer
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
