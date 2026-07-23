package com.hrm.markdown.parser.core

/**
 * 用于 Markdown 解析的 Unicode 字符分类工具。
 * 用于强调分隔符规则、自动链接检测等。
 */
object CharacterUtils {

    private val WHITESPACE_REGEX = Regex("\\s+")

    /** 可用反斜杠转义的 ASCII 标点字符。 */
    const val ESCAPABLE_CHARS = "\\`*_{}[]()#+-.!|~<>\"'/^=:;&@"

    /** 所有 ASCII 标点字符。 */
    private const val ASCII_PUNCTUATION = "!\"#\$%&'()*+,-./:;<=>?@[\\]^_`{|}~"

    fun isAsciiPunctuation(c: Char): Boolean = c in ASCII_PUNCTUATION

    fun isEscapable(c: Char): Boolean = isAsciiPunctuation(c)

    /**
     * Unicode 标点：ASCII 标点或 Unicode 类别 P（标点）或 S（符号）。
     */
    fun isUnicodePunctuation(c: Char): Boolean {
        if (isAsciiPunctuation(c)) return true
        val category = c.category
        return category == CharCategory.DASH_PUNCTUATION ||
                category == CharCategory.START_PUNCTUATION ||
                category == CharCategory.END_PUNCTUATION ||
                category == CharCategory.CONNECTOR_PUNCTUATION ||
                category == CharCategory.OTHER_PUNCTUATION ||
                category == CharCategory.INITIAL_QUOTE_PUNCTUATION ||
                category == CharCategory.FINAL_QUOTE_PUNCTUATION ||
                category == CharCategory.MATH_SYMBOL ||
                category == CharCategory.CURRENCY_SYMBOL ||
                category == CharCategory.MODIFIER_SYMBOL ||
                category == CharCategory.OTHER_SYMBOL
    }

    /**
     * Unicode 空白：空格、制表符、换行符、换页符、回车符或 Unicode Zs 类别。
     */
    fun isUnicodeWhitespace(c: Char): Boolean {
        return c == ' ' || c == '\t' || c == '\n' || c == '\u000C' || c == '\r' ||
                c.category == CharCategory.SPACE_SEPARATOR
    }

    fun isSpaceOrTab(c: Char): Boolean = c == ' ' || c == '\t'

    fun isBlank(line: String): Boolean = line.all { isSpaceOrTab(it) }

    /**
     * 计算前导空格数（制表符按 4 空格制表位计算）。
     */
    fun countLeadingSpaces(line: String): Int {
        var spaces = 0
        for (c in line) {
            when (c) {
                ' ' -> spaces++
                '\t' -> spaces = ((spaces / 4) + 1) * 4
                else -> break
            }
        }
        return spaces
    }

    /**
     * 从行首移除最多 [n] 个空格，展开制表符。
     */
    fun removeLeadingSpaces(line: String, n: Int): String {
        var spaces = 0
        var i = 0
        while (i < line.length && spaces < n) {
            when (line[i]) {
                ' ' -> {
                    spaces++
                    i++
                }
                '\t' -> {
                    val tabWidth = 4 - (spaces % 4)
                    if (spaces + tabWidth > n) {
                        // 部分制表符：用剩余空格替换
                        val remaining = n - spaces
                        return " ".repeat(tabWidth - remaining) + line.substring(i + 1)
                    }
                    spaces += tabWidth
                    i++
                }
                else -> break
            }
        }
        return line.substring(i)
    }

    /**
     * 规范化链接标签：去除首尾空格，折叠内部空白，应用 unicode case folding。
     * uses case folding rather than simple lowercase to handle cases like
     * ẞ (U+1E9E) -> "ss" and ß (U+00DF) -> "ss".
     */
    fun normalizeLinkLabel(label: String): String {
        return unicodeCaseFold(label.trim().replace(WHITESPACE_REGEX, " "))
    }

    /**
     * simple unicode case folding: lowercase + special cases where
     * lowercase() does not match full case folding (e.g. ẞ -> ss, ß -> ss).
     */
    private fun unicodeCaseFold(s: String): String {
        val sb = StringBuilder(s.length)
        for (ch in s) {
            when (ch) {
                '\u1E9E' -> sb.append("ss") // latin capital letter sharp s
                '\u00DF' -> sb.append("ss") // latin small letter sharp s
                else -> sb.append(ch.lowercaseChar())
            }
        }
        return sb.toString()
    }

    /**
     * 对 URL 中的特殊字符进行百分号编码。
     * 保留已编码的 %XX 序列和 URL 合法字符，仅编码非法字符。
     */
    fun percentEncodeUrl(url: String): String {
        val sb = StringBuilder()
        var i = 0
        while (i < url.length) {
            val c = url[i]
            when {
                // 保留已有的百分号编码
                c == '%' && i + 2 < url.length &&
                        url[i + 1].isHexDigit() && url[i + 2].isHexDigit() -> {
                    sb.append(url, i, i + 3)
                    i += 3
                }
                // URL 合法字符不编码（仅 ASCII 范围）
                c in 'a'..'z' || c in 'A'..'Z' || c in '0'..'9' ||
                        c in "-._~:/?#@!$&'()*+,;=" -> {
                    sb.append(c)
                    i++
                }
                else -> {
                    // 对非 ASCII 字符进行 UTF-8 百分号编码
                    val str = c.toString()
                    val bytes = str.encodeToByteArray()
                    for (b in bytes) {
                        sb.append('%')
                        sb.append(HEX_DIGITS[(b.toInt() and 0xFF) shr 4])
                        sb.append(HEX_DIGITS[b.toInt() and 0x0F])
                    }
                    i++
                }
            }
        }
        return sb.toString()
    }

    private fun Char.isHexDigit(): Boolean =
        this in '0'..'9' || this in 'a'..'f' || this in 'A'..'F'

    private val HEX_DIGITS = "0123456789ABCDEF".toCharArray()

    // ────── CJK / 中文本地化优化 ──────

    /**
     * 判断字符是否为 CJK（中日韩）表意文字。
     *
     * 覆盖 Unicode 块：
     * - CJK Unified Ideographs (4E00-9FFF)
     * - CJK Unified Ideographs Extension A (3400-4DBF)
     * - CJK Unified Ideographs Extension B (20000-2A6DF) — 超出 Char 范围，暂不处理
     * - CJK Compatibility Ideographs (F900-FAFF)
     * - CJK Radicals Supplement (2E80-2EFF)
     * - Kangxi Radicals (2F00-2FDF)
     * - Ideographic Description Characters (2FF0-2FFF)
     * - CJK Symbols and Punctuation (3000-303F)
     * - Hiragana (3040-309F)
     * - Katakana (30A0-30FF)
     * - Bopomofo (3100-312F)
     * - Hangul Syllables (AC00-D7AF)
     * - Hangul Jamo (1100-11FF)
     * - Hangul Compatibility Jamo (3130-318F)
     */
    fun isCJK(c: Char): Boolean {
        val code = c.code
        return code in 0x4E00..0x9FFF ||       // CJK Unified Ideographs
                code in 0x3400..0x4DBF ||       // CJK Extension A
                code in 0xF900..0xFAFF ||       // CJK Compatibility Ideographs
                code in 0x2E80..0x2EFF ||       // CJK Radicals Supplement
                code in 0x2F00..0x2FDF ||       // Kangxi Radicals
                code in 0x3040..0x309F ||       // Hiragana
                code in 0x30A0..0x30FF ||       // Katakana
                code in 0x3100..0x312F ||       // Bopomofo
                code in 0xAC00..0xD7AF ||       // Hangul Syllables
                code in 0x1100..0x11FF ||       // Hangul Jamo
                code in 0x3130..0x318F          // Hangul Compatibility Jamo
    }

    /**
     * 判断字符是否为全角标点符号。
     *
     * 全角标点在中文排版中起到类似 ASCII 标点的作用，
     * 但在 CommonMark 强调规则中不被归类为"Unicode 标点"（不会阻止 flanking），
     * 这导致 `*中文*。` 中 `*` 在句号前无法正确形成右侧 flanking。
     *
     * 包含常见的全角标点：
     * - 中文标点：。，、；：？！""''【】《》（）—…·
     * - 日文标点：。、「」『』
     * - 全角 ASCII 标点：！＂＃等 (FF01-FF0F, FF1A-FF20, FF3B-FF40, FF5B-FF5E)
     */
    fun isFullWidthPunctuation(c: Char): Boolean {
        val code = c.code
        // 全角 ASCII 标点 (FF01-FF5E 范围内的标点)
        if (code in 0xFF01..0xFF0F || code in 0xFF1A..0xFF20 ||
            code in 0xFF3B..0xFF40 || code in 0xFF5B..0xFF5E) return true
        // 中日韩常见标点
        return when (c) {
            '。', '，', '、', '；', '：', '？', '！',
            '\u201C', '\u201D', // ""
            '\u2018', '\u2019', // ''
            '【', '】', '《', '》', '（', '）',
            '—', '…', '·',
            '「', '」', '『', '』',
            '\u3000', // 全角空格
            '\u3001', '\u3002', // 顿号、句号
            '\u3008', '\u3009', '\u300A', '\u300B', // 角括号
            '\u300C', '\u300D', '\u300E', '\u300F', // 括号
            '\u3010', '\u3011', // 黑括号
            '\u3014', '\u3015', // 龟甲括号
            '\uFE4F', // 下划线
            -> true
            else -> false
        }
    }

    /**
     * 判断字符是否为 CJK 字符或全角标点。
     *
     * 用于强调分隔符的边界判断：在 CJK 上下文中，
     * CJK 字符和全角标点都应视为"普通字符"（非空白、非 ASCII 标点），
     * 使得 `*中文*` 和 `*中文*。` 都能正确解析为强调。
     */
    fun isCJKOrFullWidthPunctuation(c: Char): Boolean = isCJK(c) || isFullWidthPunctuation(c)

}