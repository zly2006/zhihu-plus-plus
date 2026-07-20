package com.hrm.markdown.renderer.internal.layout.measure

import com.hrm.markdown.renderer.internal.layout.model.LayoutSize

data class WidgetMeasureResult(
    val size: LayoutSize,
    val baseline: Float? = null,
)
