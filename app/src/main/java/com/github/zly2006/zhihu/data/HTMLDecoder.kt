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

    // Compile regex once for better performance
    private val htmlEntityRegex = Regex("&(?:quot|lt|gt|nbsp|apos|amp|#39|#x2F|#x27|#x60|#x3D);")
    
    // Map for entity replacements
    private val entityMap = mapOf(
        "&quot;" to "\"",
        "&lt;" to "<",
        "&gt;" to ">",
        "&nbsp;" to " ",
        "&apos;" to "'",
        "&#39;" to "'",
        "&#x2F;" to "/",
        "&#x27;" to "'",
        "&#x60;" to "`",
        "&#x3D;" to "=",
        "&amp;" to "&"
    )

    override fun deserialize(decoder: Decoder): String {
        val string = decoder.decodeString()
        // Early return if no HTML entities found
        if (!string.contains('&')) return string
        
        // Use regex replace for single-pass decoding
        return htmlEntityRegex.replace(string) { matchResult ->
            entityMap[matchResult.value] ?: matchResult.value
        }
    }
}
