package com.hrm.markdown.renderer.internal.core.model

import com.hrm.markdown.renderer.internal.core.identity.RenderIdentity

data class InternalRenderDocumentModel(
    val identity: RenderIdentity,
    val blocks: List<InternalRenderBlockModel>,
    val metadata: InternalRenderDocumentMetadata = InternalRenderDocumentMetadata(),
)

data class InternalRenderDocumentMetadata(
    val footnotes: Map<String, RenderIdentity> = emptyMap(),
    val footnoteDefinitions: List<FootnoteDefinitionMetadata> = emptyList(),
    val frontMatter: FrontMatterMetadata? = null,
    val linkReferences: List<LinkReferenceMetadata> = emptyList(),
    val abbreviations: List<AbbreviationMetadata> = emptyList(),
)

data class FootnoteDefinitionMetadata(
    val label: String,
    val index: Int,
    val identity: RenderIdentity,
)

data class FrontMatterMetadata(
    val format: String,
    val literal: String,
)

data class LinkReferenceMetadata(
    val label: String,
    val destination: String,
    val title: String?,
)

data class AbbreviationMetadata(
    val abbreviation: String,
    val fullText: String,
)
