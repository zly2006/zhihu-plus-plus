package com.hrm.markdown.renderer.internal.layout.model

data class LayoutSize(
    val width: Float,
    val height: Float,
)

data class LayoutInsets(
    val left: Float = 0f,
    val top: Float = 0f,
    val right: Float = 0f,
    val bottom: Float = 0f,
)

data class LayoutRect(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
)
