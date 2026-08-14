package com.hrm.markdown.parser.block.postprocessors

import com.hrm.markdown.parser.ast.Document

/**
 * 后处理器接口。
 *
 * 在块结构解析和行内解析完成后对 AST 进行变换。
 * 通过 [PostProcessorRegistry] 注册制管理，支持插件化扩展。
 */
interface PostProcessor {
    /**
     * 优先级（数字越小越先执行）。
     */
    val priority: Int

    /**
     * 对文档 AST 进行后处理变换。
     */
    fun process(document: Document)
}
