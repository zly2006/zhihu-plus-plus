package com.hrm.markdown.renderer.internal.core.compile

import com.hrm.markdown.parser.ast.Document
import com.hrm.markdown.renderer.internal.core.model.InternalRenderDocumentModel

interface RenderModelCompiler {
    fun compile(
        document: Document,
        environment: RenderCompileEnvironment,
    ): InternalRenderDocumentModel
}
