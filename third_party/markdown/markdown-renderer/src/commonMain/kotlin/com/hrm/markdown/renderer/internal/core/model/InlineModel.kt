package com.hrm.markdown.renderer.internal.core.model

import com.hrm.markdown.renderer.internal.core.identity.RenderIdentity

data class InlineModel(
    val identity: RenderIdentity,
    val atoms: List<InlineAtom>,
)

sealed interface InlineAtom {
    val identity: RenderIdentity
}

data class TextAtom(
    override val identity: RenderIdentity,
    val text: String,
    val marks: List<SpanMark> = emptyList(),
) : InlineAtom

data class WidgetAtom(
    override val identity: RenderIdentity,
    val widget: InlineWidgetModel,
) : InlineAtom

data class SpanMark(
    val kind: String,
    val payload: Map<String, String> = emptyMap(),
)
