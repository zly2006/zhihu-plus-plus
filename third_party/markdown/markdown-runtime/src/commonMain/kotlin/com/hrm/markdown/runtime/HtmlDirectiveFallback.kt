package com.hrm.markdown.runtime

import com.hrm.markdown.parser.ast.DirectiveBlock
import com.hrm.markdown.parser.ast.DirectiveInline

/**
 * HTML 导出端的 directive fallback。
 * 返回 null 表示继续走默认 generic directive HTML 输出。
 */
interface HtmlDirectiveFallback {
    fun render(node: DirectiveBlock): String?
}

/**
 * HTML 导出端的行内 directive fallback。
 * 返回 null 表示继续走默认 generic directive HTML 输出。
 */
interface HtmlInlineDirectiveFallback {
    fun render(node: DirectiveInline): String?
}
