package com.hrm.markdown.renderer.internal.core.model

import com.hrm.markdown.renderer.internal.core.identity.RenderIdentity

sealed interface RenderWidgetModel {
    val identity: RenderIdentity
}

sealed interface InlineWidgetModel : RenderWidgetModel
sealed interface BlockWidgetModel : RenderWidgetModel

data class InlineCodeWidgetModel(
    override val identity: RenderIdentity,
    val code: String,
) : InlineWidgetModel

data class InlineMathWidgetModel(
    override val identity: RenderIdentity,
    val latex: String,
) : InlineWidgetModel

data class ImageWidgetModel(
    override val identity: RenderIdentity,
    val url: String,
    val altText: String,
    val title: String?,
    val width: Int?,
    val height: Int?,
    val attributes: Map<String, String> = emptyMap(),
) : InlineWidgetModel

data class SpoilerWidgetModel(
    override val identity: RenderIdentity,
    val content: InlineModel,
    val alternateText: String,
) : InlineWidgetModel

data class DirectiveInlineWidgetModel(
    override val identity: RenderIdentity,
    val tagName: String,
    val args: Map<String, String>,
    val alternateText: String,
) : InlineWidgetModel

data class RubyTextWidgetModel(
    override val identity: RenderIdentity,
    val base: String,
    val annotation: String,
) : InlineWidgetModel

data class CodeBlockWidgetModel(
    override val identity: RenderIdentity,
    val code: String,
    val language: String,
    val title: String?,
) : BlockWidgetModel

data class MathBlockWidgetModel(
    override val identity: RenderIdentity,
    val latex: String,
) : BlockWidgetModel

data class DiagramBlockWidgetModel(
    override val identity: RenderIdentity,
    val hostKey: Long,
    val diagramType: String,
    val code: String,
) : BlockWidgetModel
