package com.github.zly2006.zhihu.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object HTMLDecoder : KSerializer<String> {
    override val descriptor = PrimitiveSerialDescriptor("HTMLDecoder", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: String) {
        // never used
        encoder.encodeString(value)
    }

    override fun deserialize(decoder: Decoder): String {
        val string = decoder.decodeString()
        return string
            .replace("&quot;", "\"")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&nbsp;", " ")
            .replace("&apos;", "'")
            .replace("&#39;", "'")
            .replace("&#x2F;", "/")
            .replace("&#x27;", "'")
            .replace("&#x60;", "`")
            .replace("&#x3D;", "=")
            .replace("&amp;", "&")
    }
}
