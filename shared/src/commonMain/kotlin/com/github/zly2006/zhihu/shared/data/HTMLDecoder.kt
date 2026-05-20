package com.github.zly2006.zhihu.shared.data

import com.fleeksoft.ksoup.Ksoup
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object HTMLDecoder : KSerializer<String> {
    override val descriptor = PrimitiveSerialDescriptor("HTMLDecoder", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: String,
    ) {
        encoder.encodeString(value)
    }

    override fun deserialize(decoder: Decoder): String {
        val string = decoder.decodeString()
        return Ksoup
            .parse(string)
            .apply {
                select(".invisible").forEach { it.remove() }
            }.text()
    }
}
