package com.hrm.markdown.renderer.internal.layout.inline

internal data class InlineLayoutEpoch(
    val themeHash: Int,
    val codeThemeHash: Int,
    val directiveRegistryHash: Int,
    val configHash: Int,
    val densityBits: Int,
    val fontScaleBits: Int,
    val textMeasurerHash: Int,
    val latexMeasurerHash: Int,
    val onLinkClickHash: Int,
    val onFootnoteClickHash: Int,
)
