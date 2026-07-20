package com.hrm.markdown.renderer.internal.selection

/**
 * 防止字符偏移落在 UTF-16 代理对（surrogate pair）中间，切坏 emoji 等补充平面字符。
 * 若 [offset] 落在低代理位上（前一个是高代理），回退到这对的起点。
 */
internal fun clampToCharBoundary(text: CharSequence, offset: Int): Int {
    if (offset <= 0) return 0
    if (offset >= text.length) return text.length
    val prev = text[offset - 1]
    val cur = text[offset]
    return if (prev.isHighSurrogate() && cur.isLowSurrogate()) offset - 1 else offset
}
