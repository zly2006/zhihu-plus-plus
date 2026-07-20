package com.hrm.markdown.runtime

/**
 * HTML 导出端的 directive fallback。
 * 返回 null 表示继续走默认 generic directive HTML 输出。
 */
interface HtmlDirectiveFallback {
    fun render(snapshot: DirectiveBlockSnapshot): String?
}

/**
 * HTML 导出端的行内 directive fallback。
 * 返回 null 表示继续走默认 generic directive HTML 输出。
 */
interface HtmlInlineDirectiveFallback {
    fun render(snapshot: DirectiveInlineSnapshot): String?
}
