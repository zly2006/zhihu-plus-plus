package com.hrm.markdown.renderer.internal.core.identity

data class RenderIdentity(
    val stableId: Long,
    val contentRevision: Long,
    val layoutRevision: Long,
    val paintRevision: Long,
)

internal const val RENDER_IDENTITY_OFFSET_BASIS: Long = 1469598103934665603L
internal const val RENDER_IDENTITY_FNV_PRIME: Long = 1099511628211L

internal fun renderIdentitySeed(): Long = RENDER_IDENTITY_OFFSET_BASIS

internal fun renderIdentityMix(acc: Long, value: Long): Long =
    (acc xor value) * RENDER_IDENTITY_FNV_PRIME

internal fun renderIdentityFromValues(vararg values: Long): Long {
    var acc = renderIdentitySeed()
    for (value in values) {
        acc = renderIdentityMix(acc, value)
    }
    return acc
}

internal fun renderIdentityFromText(text: String, seed: Long = renderIdentitySeed()): Long {
    var acc = seed
    for (char in text) {
        acc = renderIdentityMix(acc, char.code.toLong())
    }
    return acc
}