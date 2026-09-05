package com.github.zly2006.zhihu.harmonyprobe

/**
 * 日报 P2 切片的 HTML 子集适配，不执行 HTML。正式阅读页应迁移 shared/MdAst.kt 的 Ksoup 转换器。
 * 保留正文、粗体、引用、列表、代码与 HTTPS 图片；未知标签只保留文字。
 */
internal fun dailyHtmlToMarkdown(html: String): String {
    val cleaned = html.replace(
        Regex("""<(script|style)\b[^>]*>.*?</\1\s*>|<!--.*?-->""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)),
        "",
    )
    var pre = false
    var quoteDepth = 0
    val attributes = Regex("""([a-zA-Z-]+)\s*=\s*["']([^"']*)["']""")
    return buildString {
        Regex("""<[^>]*>|[^<]+""").findAll(cleaned).forEach { match ->
            val token = match.value
            if (!token.startsWith("<")) {
                val text = token.decodeDailyEntities()
                if (pre) append(text) else append(text.replace(Regex("""\s+"""), " "))
            } else {
                val closing = token.startsWith("</")
                val tag = token.removePrefix("<").removePrefix("/").takeWhile { it.isLetterOrDigit() }.lowercase()
                when (tag) {
                    "pre" -> {
                        append(if (closing) "\n~~~\n\n" else "\n\n~~~\n")
                        pre = !closing
                    }
                    "code" -> if (!pre) append(96.toChar())
                    "b", "strong" -> if (!pre) append("**")
                    "em", "i" -> if (!pre) append('*')
                    "h1", "h2", "h3", "h4", "h5", "h6" ->
                        append(if (closing) "\n\n" else "\n\n" + "#".repeat(tag.last().digitToInt()) + " ")
                    "p", "div", "section" -> {
                        append("\n\n")
                        if (quoteDepth > 0) append("> ".repeat(quoteDepth))
                    }
                    "br" -> append("  \n")
                    "blockquote" -> {
                        quoteDepth = (quoteDepth + if (closing) -1 else 1).coerceAtLeast(0)
                        append("\n\n")
                        if (quoteDepth > 0) append("> ".repeat(quoteDepth))
                    }
                    "li" -> append(if (closing) "\n" else "\n- ")
                    "ul", "ol" -> append("\n")
                    "img" -> {
                        val attrs = attributes.findAll(token).associate { it.groupValues[1].lowercase() to it.groupValues[2].decodeDailyEntities() }
                        val url = attrs["src"].orEmpty()
                        if (Regex("""https://[a-zA-Z0-9-]+\.zhimg\.com/[^\s<>]*""").matches(url)) {
                            val alt = attrs["alt"].orEmpty().replace("[", "").replace("]", "")
                            append("\n\n![" + alt + "](<" + url + ">)\n\n")
                        }
                    }
                }
            }
        }
    }.trim()
}

private fun String.decodeDailyEntities(): String =
    Regex("""&(#x[0-9a-fA-F]+|#[0-9]+|nbsp|amp|lt|gt|quot|apos);""").replace(this) { match ->
        when (val entity = match.groupValues[1]) {
            "nbsp" -> " "
            "amp" -> "&"
            "lt" -> "<"
            "gt" -> ">"
            "quot" -> "\""
            "apos" -> "'"
            else -> {
                val number = if (entity.startsWith("#x")) entity.drop(2).toIntOrNull(16) else entity.drop(1).toIntOrNull()
                when {
                    number == null || number !in 1..0x10ffff || number in 0xd800..0xdfff -> match.value
                    number <= 0xffff -> number.toChar().toString()
                    else -> {
                        val scalar = number - 0x10000
                        (0xd800 + (scalar shr 10)).toChar().toString() + (0xdc00 + (scalar and 1023)).toChar()
                    }
                }
            }
        }
    }

/** 固定压力输入，避免依赖当天日报长度；仍使用正式同源解析器和渲染器。 */
internal fun p2StressMarkdown(): String = buildString {
    append("# Markdown / LaTeX / Kotlin\n\n**粗体检查**与*斜体检查*。\n\n> 引用应保留独立缩进。\n\n")
    append("~~~kotlin\n// Kotlin 高亮与缩进\nfun square(value: Int): Int {\n    return value * value\n}\n~~~\n\n")
    repeat(80) { index ->
        append("## 公式 " + (index + 1) + "/80\n\n")
        append("\$\$\n\\frac{1}{2} + \\sqrt{x^{2}+1} = \\sum_{n=1}^{10} n\n\$\$\n\n")
        append("第 " + (index + 1) + " 段：这是跨平台长文滚动测试。中文、English 与 0123456789 应自然换行，返回首页后仍可再次进入。\n\n")
    }
    append("## 列表压力\n\n")
    repeat(300) { append("- 列表项 " + (it + 1) + "/300：连续滚动检查，不应丢行或截断。\n") }
    append("\n## 表格压力\n\n| 序号 | 内容 | 状态 |\n| --- | --- | --- |\n")
    repeat(200) { append("| " + (it + 1) + " | OHOS 行 " + (it + 1) + "/200 | 已渲染 |\n") }
    append("\n## 校验终点\n\n已到达第 80 个公式、300 个列表项、200 行表格之后。\n")
}

