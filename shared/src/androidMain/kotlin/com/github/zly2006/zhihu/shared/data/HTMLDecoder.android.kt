package com.github.zly2006.zhihu.shared.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.jsoup.Jsoup

actual object HTMLDecoder : KSerializer<String> {
    override val descriptor = PrimitiveSerialDescriptor("HTMLDecoder", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: String,
    ) {
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
