package com.hrm.markdown.parser.inline

import com.hrm.markdown.parser.ast.*
import com.hrm.markdown.parser.core.CharacterUtils
import com.hrm.markdown.parser.core.HtmlEntities
import com.hrm.markdown.parser.block.BlockParser

/**
 * 高性能行内解析器，实现 CommonMark 分隔符算法。
 *
 * 使用链表方式处理分隔符（与参考实现一致），
 * 避免在处理强调/加重强调时出现索引失效问题。
 */
class InlineParser(
    private val document: Document,
    /** 自定义 Emoji 别名映射（shortcode → unicode），可由外部注入 */
    private val customEmojiMap: Map<String, String> = emptyMap(),
    /** 是否启用 ASCII 表情自动转换 */
    private val enableAsciiEmoticons: Boolean = false,
    private val enableGfmAutolinks: Boolean = true,
    private val enableExtendedInline: Boolean = true,
    private val enableEmphasisCoalescing: Boolean = false,
    private val enableStrikethrough: Boolean = true,
) : BlockParser.InlineParserInterface {

    override fun parseInlines(content: String, parent: ContainerNode) {
        if (content.isEmpty()) return
        val parser = InlineParserInstance(content, document, customEmojiMap, enableAsciiEmoticons, enableGfmAutolinks, enableExtendedInline, enableStrikethrough)
        val nodes = parser.parse()
        for (node in nodes) {
            parent.appendChild(node)
        }
        if (enableEmphasisCoalescing) {
            coalesceEmphasis(parent)
        }
    }

    /**
     * Recursively flattens redundant nested StrongEmphasis.
     *
     * GFM 0.29 collapses nested strong emphasis:
     * `<strong><strong>foo</strong></strong>` becomes `<strong>foo</strong>`.
     *
     * This only applies to StrongEmphasis inside StrongEmphasis, NOT to
     * Emphasis inside Emphasis (which the GFM spec keeps nested).
     */
    private fun coalesceEmphasis(node: Node) {
        if (node !is ContainerNode) return

        // First, recurse into children
        for (child in node.children.toList()) {
            coalesceEmphasis(child)
        }

        // Flatten: if any child StrongEmphasis is inside a parent StrongEmphasis,
        // unwrap the inner one by promoting its children.
        if (node is StrongEmphasis) {
            var i = 0
            while (i < node.children.size) {
                val child = node.children[i]
                if (child is StrongEmphasis) {
                    // Unwrap: replace `child` with its own children
                    val innerChildren = child.children.toList()
                    node.removeChildAt(i)
                    for ((j, ic) in innerChildren.withIndex()) {
                        node.insertChild(i + j, ic)
                    }
                    i += innerChildren.size
                } else {
                    i++
                }
            }
        }
    }

    companion object {
        internal val AUTOLINK_REGEX = Regex("<([a-zA-Z][a-zA-Z0-9+.-]{1,31}:[^\\s<>]*)>")
        internal val EMAIL_AUTOLINK_REGEX = Regex("<([a-zA-Z0-9.!#\$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*)>")
        internal val INLINE_HTML_REGEX = Regex(
            """<(?:""" +
            """[a-zA-Z][a-zA-Z0-9-]*(?:\s+[a-zA-Z_:][a-zA-Z0-9_.:-]*(?:\s*=\s*(?:[^\s"'=<>`]+|'[^']*'|"[^"]*"))?)*\s*/?>""" +
            """|/[a-zA-Z][a-zA-Z0-9-]*\s*>""" +
            """|!-->""" +
            """|!--->""" +
            """|!--(?!>)(?!->)[\s\S]*?-->""" +
            """|\?[\s\S]*?\?>""" +
            """|![A-Z]+\s+[\s\S]*?>""" +
            """|!\[CDATA\[[\s\S]*?\]\]>""" +
            """)"""
        )
        internal val GFM_URL_REGEX = Regex("""(?:https?://|www\.)[^\s<]*""")
        internal val GFM_EMAIL_REGEX = Regex("""[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)*""")
        internal val KBD_REGEX = Regex("""<kbd>(.*?)</kbd>""", RegexOption.IGNORE_CASE)

        /**
         * 标准 Emoji 指令到 Unicode 的映射表（常用子集）。
         */
        val STANDARD_EMOJI_MAP: Map<String, String> = mapOf(
            "smile" to "😄", "laughing" to "😆", "blush" to "😊", "smiley" to "😃",
            "relaxed" to "☺️", "smirk" to "😏", "heart_eyes" to "😍", "kissing_heart" to "😘",
            "kissing_closed_eyes" to "😚", "flushed" to "😳", "relieved" to "😌", "satisfied" to "😆",
            "grin" to "😁", "wink" to "😉", "stuck_out_tongue_winking_eye" to "😜",
            "stuck_out_tongue_closed_eyes" to "😝", "grinning" to "😀", "kissing" to "😗",
            "kissing_smiling_eyes" to "😙", "stuck_out_tongue" to "😛", "sleeping" to "😴",
            "worried" to "😟", "frowning" to "😦", "anguished" to "😧", "open_mouth" to "😮",
            "grimacing" to "😬", "confused" to "😕", "hushed" to "😯", "expressionless" to "😑",
            "unamused" to "😒", "sweat_smile" to "😅", "sweat" to "😓", "disappointed_relieved" to "😥",
            "weary" to "😩", "pensive" to "😔", "disappointed" to "😞", "confounded" to "😖",
            "fearful" to "😨", "cold_sweat" to "😰", "persevere" to "😣", "cry" to "😢",
            "sob" to "😭", "joy" to "😂", "astonished" to "😲", "scream" to "😱",
            "tired_face" to "😫", "angry" to "😠", "rage" to "😡", "triumph" to "😤",
            "sleepy" to "😪", "yum" to "😋", "mask" to "😷", "sunglasses" to "😎",
            "dizzy_face" to "😵", "imp" to "👿", "smiling_imp" to "😈", "neutral_face" to "😐",
            "no_mouth" to "😶", "innocent" to "😇", "alien" to "👽", "yellow_heart" to "💛",
            "blue_heart" to "💙", "purple_heart" to "💜", "heart" to "❤️", "green_heart" to "💚",
            "broken_heart" to "💔", "heartbeat" to "💓", "heartpulse" to "💗", "two_hearts" to "💕",
            "revolving_hearts" to "💞", "cupid" to "💘", "sparkling_heart" to "💖",
            "sparkles" to "✨", "star" to "⭐", "star2" to "🌟", "dizzy" to "💫",
            "boom" to "💥", "collision" to "💥", "anger" to "💢", "exclamation" to "❗",
            "question" to "❓", "grey_exclamation" to "❕", "grey_question" to "❔",
            "zzz" to "💤", "dash" to "💨", "sweat_drops" to "💦", "notes" to "🎶",
            "musical_note" to "🎵", "fire" to "🔥", "hankey" to "💩", "poop" to "💩",
            "thumbsup" to "👍", "+1" to "👍", "thumbsdown" to "👎", "-1" to "👎",
            "ok_hand" to "👌", "punch" to "👊", "fist" to "✊", "v" to "✌️",
            "wave" to "👋", "hand" to "✋", "raised_hand" to "✋", "open_hands" to "👐",
            "point_up" to "☝️", "point_down" to "👇", "point_left" to "👈", "point_right" to "👉",
            "raised_hands" to "🙌", "pray" to "🙏", "point_up_2" to "👆", "clap" to "👏",
            "muscle" to "💪", "metal" to "🤘", "fu" to "🖕", "walking" to "🚶",
            "runner" to "🏃", "running" to "🏃", "couple" to "👫", "family" to "👪",
            "two_men_holding_hands" to "👬", "two_women_holding_hands" to "👭",
            "dancer" to "💃", "bow" to "🙇", "couplekiss" to "💏",
            "couple_with_heart" to "💑", "massage" to "💆", "haircut" to "💇",
            "nail_care" to "💅", "boy" to "👦", "girl" to "👧", "woman" to "👩",
            "man" to "👨", "baby" to "👶", "older_woman" to "👵", "older_man" to "👴",
            "cop" to "👮", "angel" to "👼", "princess" to "👸", "guardsman" to "💂",
            "skull" to "💀", "feet" to "🐾", "lips" to "👄", "kiss" to "💋",
            "droplet" to "💧", "ear" to "👂", "eyes" to "👀", "nose" to "👃",
            "tongue" to "👅", "love_letter" to "💌", "ring" to "💍", "gem" to "💎",
            "sunny" to "☀️", "cloud" to "☁️", "umbrella" to "☂️", "snowflake" to "❄️",
            "snowman" to "⛄", "zap" to "⚡", "cyclone" to "🌀", "ocean" to "🌊",
            "cat" to "🐱", "dog" to "🐶", "mouse" to "🐭", "hamster" to "🐹",
            "rabbit" to "🐰", "wolf" to "🐺", "frog" to "🐸", "tiger" to "🐯",
            "koala" to "🐨", "bear" to "🐻", "pig" to "🐷", "cow" to "🐮",
            "boar" to "🐗", "monkey_face" to "🐵", "monkey" to "🐒", "horse" to "🐴",
            "racehorse" to "🐎", "camel" to "🐫", "sheep" to "🐑", "elephant" to "🐘",
            "snake" to "🐍", "bird" to "🐦", "penguin" to "🐧", "turtle" to "🐢",
            "bug" to "🐛", "honeybee" to "🐝", "ant" to "🐜", "beetle" to "🐞",
            "snail" to "🐌", "octopus" to "🐙", "fish" to "🐟", "whale" to "🐳",
            "dolphin" to "🐬", "dragon" to "🐉", "rocket" to "🚀", "airplane" to "✈️",
            "car" to "🚗", "taxi" to "🚕", "bus" to "🚌", "ambulance" to "🚑",
            "fire_engine" to "🚒", "bike" to "🚲", "ship" to "🚢", "train" to "🚆",
            "warning" to "⚠️", "checkered_flag" to "🏁", "crossed_flags" to "🎌",
            "triangular_flag_on_post" to "🚩", "white_check_mark" to "✅", "x" to "❌",
            "negative_squared_cross_mark" to "❎", "heavy_check_mark" to "✔️",
            "heavy_multiplication_x" to "✖️", "heavy_plus_sign" to "➕",
            "heavy_minus_sign" to "➖", "heavy_division_sign" to "➗",
            "100" to "💯", "copyright" to "©️", "registered" to "®️", "tm" to "™️",
            "lock" to "🔒", "unlock" to "🔓", "key" to "🔑", "bell" to "🔔",
            "bookmark" to "🔖", "link" to "🔗", "radio_button" to "🔘",
            "back" to "🔙", "end" to "🔚", "on" to "🔛", "soon" to "🔜",
            "top" to "🔝", "memo" to "📝", "pencil" to "✏️", "book" to "📖",
            "calendar" to "📅", "chart" to "📊", "clipboard" to "📋", "pushpin" to "📌",
            "paperclip" to "📎", "email" to "📧", "phone" to "📱", "computer" to "💻",
            "bulb" to "💡", "wrench" to "🔧", "hammer" to "🔨", "gear" to "⚙️",
            "trophy" to "🏆", "medal" to "🏅", "tada" to "🎉", "gift" to "🎁",
            "balloon" to "🎈", "party_popper" to "🎉", "confetti_ball" to "🎊",
            "christmas_tree" to "🎄", "jack_o_lantern" to "🎃", "ghost" to "👻",
            "santa" to "🎅", "coffee" to "☕", "beer" to "🍺", "wine_glass" to "🍷",
            "pizza" to "🍕", "hamburger" to "🍔", "apple" to "🍎", "lemon" to "🍋",
            "cherry" to "🍒", "grapes" to "🍇", "watermelon" to "🍉", "banana" to "🍌",
            "peach" to "🍑", "strawberry" to "🍓", "cookie" to "🍪", "cake" to "🎂",
            "ice_cream" to "🍨", "eyes_in_speech_bubble" to "👁️‍🗨️",
            "thinking" to "🤔", "rofl" to "🤣", "hugs" to "🤗", "cowboy" to "🤠",
            "clown_face" to "🤡", "nerd_face" to "🤓", "shushing_face" to "🤫",
            "face_with_monocle" to "🧐", "skull_and_crossbones" to "☠️",
        )

        /**
         * ASCII 表情到 Unicode 的映射表。
         */
        val ASCII_EMOTICON_MAP: Map<String, String> = mapOf(
            ":)" to "😊", ":-)" to "😊", ":D" to "😃", ":-D" to "😃",
            ";)" to "😉", ";-)" to "😉", ":P" to "😛", ":-P" to "😛",
            ":p" to "😛", ":-p" to "😛", ":(" to "😞", ":-(" to "😞",
            ":'(" to "😢", ":o" to "😮", ":-o" to "😮", ":O" to "😮",
            ":-O" to "😮", "B)" to "😎", "B-)" to "😎", ":/" to "😕",
            ":-/" to "😕", ":|" to "😐", ":-|" to "😐", ":*" to "😘",
            ":-*" to "😘", "<3" to "❤️", "</3" to "💔", "XD" to "😆",
            "xD" to "😆", ":>" to "😊", "8)" to "😎", "8-)" to "😎",
            "o_O" to "😳", "O_o" to "😳",
        )
    }
}

/**
 * 处理单个行内内容字符串的内部实例。
 * 使用双向链表来存放"行内节点"，分隔符条目穿插其中。
 */
private class InlineParserInstance(
    private val input: String,
    private val document: Document,
    private val customEmojiMap: Map<String, String> = emptyMap(),
    private val enableAsciiEmoticons: Boolean = false,
    private val enableGfmAutolinks: Boolean = true,
    private val enableExtendedInline: Boolean = true,
    private val enableStrikethrough: Boolean = true,
) {
    // 链表包装 AST 节点
    private class LLNode(var astNode: Node) {
        var prev: LLNode? = null
        var next: LLNode? = null
        var delimEntry: DelimEntry? = null
    }

    private class DelimEntry(
        val llNode: LLNode,
        val char: Char,
        var count: Int,
        val origCount: Int,
        val canOpen: Boolean,
        val canClose: Boolean
    ) {
        var prev: DelimEntry? = null
        var next: DelimEntry? = null
    }

    private class BracketEntry(
        val llNode: LLNode,
        val isImage: Boolean,
        var active: Boolean,
        val prevDelim: DelimEntry?, // delimiter stack bottom
        var prev: BracketEntry?,
        val contentStartPos: Int = 0 // position in input right after the opening [
    )

    // 链表头/尾
    private var llHead: LLNode? = null
    private var llTail: LLNode? = null

    // 复用的文本缓冲，避免每次 appendText() 分配 StringBuilder
    private val textBuffer = StringBuilder(64)

    // 分隔符栈（双向链表）
    private var delimHead: DelimEntry? = null
    private var delimTail: DelimEntry? = null

    // 方括号栈
    private var bracketTop: BracketEntry? = null

    private val scanner = InlineScanner(input)

    fun parse(): List<Node> {
        // 第一阶段：扫描并构建链表
        while (!scanner.isAtEnd) {
            val c = scanner.peek()
            when {
                c == '\\' -> appendEscape()
                c == '`' -> appendBackticks()
                c == '<' -> appendAngleBracket()
                c == '&' -> appendEntity()
                c == '[' -> {
                    // 检测 Wiki 链接 [[...]]
                    if (enableExtendedInline && scanner.peek(1) == '[') {
                        val wikiResult = tryAppendWikiLink()
                        if (!wikiResult) appendOpenBracket(isImage = false)
                    } else {
                        appendOpenBracket(isImage = false)
                    }
                }
                c == '!' && scanner.peek(1) == '[' -> {
                    scanner.advance() // !
                    appendOpenBracket(isImage = true)
                }
                c == ']' -> appendCloseBracket()
                c == '*' || c == '_' -> appendDelimiterRun(c)
                c == '~' && (enableExtendedInline || enableStrikethrough) -> appendTildeRun()
                c == '=' && scanner.peek(1) == '=' && enableExtendedInline -> appendPairedDelim('=', 2)
                c == '+' && scanner.peek(1) == '+' && enableExtendedInline -> appendPairedDelim('+', 2)
                c == '^' && enableExtendedInline -> appendPairedDelim('^', 1)
                c == '$' && enableExtendedInline -> appendDollar()
                c == ':' && enableExtendedInline -> appendPossibleEmoji()
                c == '{' && scanner.peek(1) == '%' && enableExtendedInline -> appendDirective()
                c == '>' && scanner.peek(1) == '!' && enableExtendedInline -> appendSpoiler()
                c == '{' && enableExtendedInline && scanner.peek(1) != '%' -> appendPossibleRuby()
                c == '\n' -> appendLineBreak()
                else -> appendText()
            }
        }

        // 第二阶段：处理强调/分隔符
        processEmphasis(null)

        // 第三阶段：收集结果并合并相邻的 Text 节点
        val result = mutableListOf<Node>()
        var cur = llHead
        while (cur != null) {
            val astNode = cur.astNode
            if (astNode is Text && result.isNotEmpty() && result.last() is Text) {
                // 合并相邻的文本节点
                (result.last() as Text).literal += astNode.literal
            } else {
                result.add(astNode)
            }
            cur = cur.next
        }
        return result
    }

    // ────── 链表操作 ──────

    private fun appendLL(node: Node): LLNode {
        val ll = LLNode(node)
        ll.prev = llTail
        if (llTail != null) {
            llTail!!.next = ll
        } else {
            llHead = ll
        }
        llTail = ll
        return ll
    }

    private fun insertAfterLL(after: LLNode, node: Node): LLNode {
        val ll = LLNode(node)
        ll.prev = after
        ll.next = after.next
        if (after.next != null) {
            after.next!!.prev = ll
        } else {
            llTail = ll
        }
        after.next = ll
        return ll
    }

    private fun removeLL(ll: LLNode) {
        if (ll.prev != null) ll.prev!!.next = ll.next else llHead = ll.next
        if (ll.next != null) ll.next!!.prev = ll.prev else llTail = ll.prev
    }

    // ────── 分隔符栈操作 ──────

    private fun pushDelim(entry: DelimEntry) {
        entry.prev = delimTail
        if (delimTail != null) delimTail!!.next = entry else delimHead = entry
        delimTail = entry
    }

    private fun removeDelim(entry: DelimEntry) {
        if (entry.prev != null) entry.prev!!.next = entry.next else delimHead = entry.next
        if (entry.next != null) entry.next!!.prev = entry.prev else delimTail = entry.prev
    }

    // ────── 扫描器 ──────

    private fun appendEscape() {
        val pos = scanner.pos
        scanner.advance() // skip '\'
        if (scanner.isAtEnd) {
            appendLL(Text("\\"))
            return
        }
        val next = scanner.peek()
        if (next == '\n') {
            scanner.advance()
            appendLL(HardLineBreak())
            // skip leading spaces on the next line
            while (!scanner.isAtEnd && scanner.peek() == ' ') {
                scanner.advance()
            }
        } else if (CharacterUtils.isAsciiPunctuation(next)) {
            scanner.advance()
            appendLL(EscapedChar(next.toString()))
        } else {
            appendLL(Text("\\"))
        }
    }

    private fun appendBackticks() {
        val pos = scanner.pos
        var count = 0
        while (!scanner.isAtEnd && scanner.peek() == '`') {
            scanner.advance()
            count++
        }

        val startContent = scanner.pos
        val saved = scanner.pos
        while (!scanner.isAtEnd) {
            if (scanner.peek() == '`') {
                val closeStart = scanner.pos
                var closeCount = 0
                while (!scanner.isAtEnd && scanner.peek() == '`') {
                    scanner.advance()
                    closeCount++
                }
                if (closeCount == count) {
                    var content = input.substring(startContent, closeStart)
                    content = content.replace('\n', ' ')
                    if (content.length >= 2 && content[0] == ' ' && content.last() == ' ' && !content.all { it == ' ' }) {
                        content = content.substring(1, content.length - 1)
                    }
                    appendLL(InlineCode(content))
                    return
                }
            } else {
                scanner.advance()
            }
        }
        scanner.pos = saved
        appendLL(Text("`".repeat(count)))
    }

    private fun appendAngleBracket() {
        val pos = scanner.pos
        scanner.advance() // skip '<'

        // 轻量前缀检查：仅当下一个字符可能开始有效结构时才启用正则
        if (!scanner.isAtEnd) {
            val nextChar = scanner.peek()

            // 自动链接：<scheme:...>  — 下一个字符必须是字母
            if (nextChar.isLetter()) {
                val autolinkMatch = InlineParser.AUTOLINK_REGEX.find(input, pos)
                if (autolinkMatch != null && autolinkMatch.range.first == pos) {
                    scanner.pos = autolinkMatch.range.last + 1
                    val raw = autolinkMatch.groupValues[1]
                    appendLL(Autolink(destination = CharacterUtils.percentEncodeUrl(raw), isEmail = false, rawText = raw))
                    return
                }

                // 邮件自动链接也以字母开头
                val emailMatch = InlineParser.EMAIL_AUTOLINK_REGEX.find(input, pos)
                if (emailMatch != null && emailMatch.range.first == pos) {
                    scanner.pos = emailMatch.range.last + 1
                    appendLL(Autolink(destination = emailMatch.groupValues[1], isEmail = true))
                    return
                }
            }

            // <kbd>...</kbd> 键盘按键标签
            if (nextChar == 'k' || nextChar == 'K') {
                val kbdMatch = InlineParser.KBD_REGEX.find(input, pos)
                if (kbdMatch != null && kbdMatch.range.first == pos) {
                    scanner.pos = kbdMatch.range.last + 1
                    appendLL(KeyboardInput(kbdMatch.groupValues[1]))
                    return
                }
            }

            // 行内 HTML：<tag>、</tag>、<!--、<?、<!、<![CDATA[
            if (nextChar.isLetter() || nextChar == '/' || nextChar == '!' || nextChar == '?') {
                val htmlMatch = InlineParser.INLINE_HTML_REGEX.find(input, pos)
                if (htmlMatch != null && htmlMatch.range.first == pos) {
                    scanner.pos = htmlMatch.range.last + 1
                    appendLL(InlineHtml(htmlMatch.value))
                    return
                }
            }
        }

        appendLL(Text("<"))
    }

    private fun appendEntity() {
        val pos = scanner.pos
        val match = HtmlEntities.matchAt(input, pos)
        if (match != null) {
            val entity = match.value
            val resolved = HtmlEntities.resolve(entity)
            scanner.pos = pos + entity.length
            if (resolved != null) {
                appendLL(HtmlEntity(entity, resolved))
            } else {
                appendLL(Text(entity))
            }
        } else {
            scanner.advance()
            appendLL(Text("&"))
        }
    }

    private fun appendOpenBracket(isImage: Boolean) {
        scanner.advance() // skip '['
        val contentStart = scanner.pos // position right after [
        val text = if (isImage) "![" else "["
        val ll = appendLL(Text(text))
        bracketTop = BracketEntry(
            llNode = ll,
            isImage = isImage,
            active = true,
            prevDelim = delimTail,
            prev = bracketTop,
            contentStartPos = contentStart
        )
    }

    private fun appendCloseBracket() {
        scanner.advance() // skip ']'
        val bracket = bracketTop
        if (bracket == null || !bracket.active) {
            appendLL(Text("]"))
            if (bracket != null) bracketTop = bracket.prev
            return
        }

        // 尝试脚注引用：[^label]
        if (!bracket.isImage) {
            val footnoteRef = tryParseFootnoteReference(bracket)
            if (footnoteRef != null) {
                // 移除方括号内的所有节点
                var cur = bracket.llNode.next
                while (cur != null) {
                    val next = cur.next
                    removeLL(cur)
                    cur.delimEntry?.let { removeDelim(it) }
                    cur = next
                }
                // 用脚注引用节点替换方括号开始符
                bracket.llNode.astNode = footnoteRef
                bracketTop = bracket.prev
                return
            }
        }

        // 尝试参考文献引用：[@key]
        if (!bracket.isImage && enableExtendedInline) {
            val citationRef = tryParseCitationReference(bracket)
            if (citationRef != null) {
                var cur = bracket.llNode.next
                while (cur != null) {
                    val next = cur.next
                    removeLL(cur)
                    cur.delimEntry?.let { removeDelim(it) }
                    cur = next
                }
                bracket.llNode.astNode = citationRef
                bracketTop = bracket.prev
                return
            }
        }

        // 尝试行内链接：[text](url "title")
        val linkResult = tryParseLinkTail(bracket.isImage)
        if (linkResult != null) {
            // 处理方括号内容中的强调
            processEmphasis(bracket.prevDelim)

            val node: ContainerNode = if (bracket.isImage) {
                val img = Image(
                    destination = linkResult.destination,
                    title = linkResult.title,
                    imageWidth = linkResult.width,
                    imageHeight = linkResult.height,
                )
                // 解析图片后的属性块 {.class #id key=value}
                val attrs = tryParseAttributes()
                if (attrs != null) {
                    img.attributes = attrs
                }
                img
            } else {
                val link = Link(destination = linkResult.destination, title = linkResult.title)
                // 解析链接后的属性块 {rel="nofollow" target="_blank" download="file.pdf"}
                val attrs = tryParseAttributes()
                if (attrs != null) {
                    link.attributes = attrs
                }
                link
            }

            // 收集方括号开始符和当前位置之间的节点
            var cur = bracket.llNode.next
            while (cur != null) {
                val next = cur.next
                removeLL(cur)
                node.appendChild(cur.astNode)
                cur = next
            }

            // 用链接/图片节点替换方括号开始符
            bracket.llNode.astNode = node

            bracketTop = bracket.prev

            // 对于链接，停用之前的方括号
            if (!bracket.isImage) {
                var b = bracketTop
                while (b != null) {
                    if (!b.isImage) b.active = false
                    b = b.prev
                }
            }
            return
        }

        // 尝试引用链接
        val refResult = tryParseRefLink(bracket)
        if (refResult != null) {
            processEmphasis(bracket.prevDelim)

            val node: ContainerNode = if (bracket.isImage) {
                val img = Image(destination = refResult.first, title = refResult.second)
                // 引用链接后也支持属性块
                val attrs = tryParseAttributes()
                if (attrs != null) {
                    img.attributes = attrs
                }
                img
            } else {
                val link = Link(destination = refResult.first, title = refResult.second)
                // 引用链接后也支持属性块
                val attrs = tryParseAttributes()
                if (attrs != null) {
                    link.attributes = attrs
                }
                link
            }

            var cur = bracket.llNode.next
            while (cur != null) {
                val next = cur.next
                removeLL(cur)
                node.appendChild(cur.astNode)
                cur = next
            }

            bracket.llNode.astNode = node
            bracketTop = bracket.prev

            if (!bracket.isImage) {
                var b = bracketTop
                while (b != null) {
                    if (!b.isImage) b.active = false
                    b = b.prev
                }
            }
            return
        }

        // 尝试自定义行内样式：[text]{.class style="..."} （非链接、非图片时）
        if (!bracket.isImage) {
            val styledResult = tryParseStyledText()
            if (styledResult != null) {
                processEmphasis(bracket.prevDelim)

                val styledNode = StyledText(attributes = styledResult)

                var cur = bracket.llNode.next
                while (cur != null) {
                    val next = cur.next
                    removeLL(cur)
                    styledNode.appendChild(cur.astNode)
                    cur = next
                }

                bracket.llNode.astNode = styledNode
                bracketTop = bracket.prev
                return
            }
        }

        // 不是链接
        bracketTop = bracket.prev
        appendLL(Text("]"))
    }

    private fun appendDelimiterRun(delimChar: Char) {
        val pos = scanner.pos
        var count = 0
        while (!scanner.isAtEnd && scanner.peek() == delimChar) {
            scanner.advance()
            count++
        }

        val charBefore = if (pos > 0) input[pos - 1] else '\n'
        val charAfter = if (scanner.pos < input.length) input[scanner.pos] else '\n'

        // ── CJK 本地化优化 ──
        // 将全角标点视为等同于 Unicode 标点进行 flanking 判断，并将 CJK 字符视为边界，
        // 以修复「标点 + CJK」紧邻时强调/粗体无法闭合的问题（CommonMark flanking 规则对 CJK 不友好）。
        //
        // 典型问题：
        // - `**'确实'**厉害啊`：closer 前是 `'`(punct)，closer 后是 `厉`(CJK)，按原规则不算 right-flanking
        // - `厉害啊**'确实'**`：opener 前是 `啊`(CJK)，opener 后是 `'`(punct)，按原规则不算 left-flanking
        //
        // 预期行为（更符合中文排版直觉）是把 CJK 当作“词边界”来参与 flanking 判断。
        //
        // 同时将全角空格（\u3000）视为空白，避免在中文排版中被误判为非空白。
        // - `*中文*。` 中 `*` 在全角句号前正确识别为右侧 flanking
        // - `「*中文*」` 中 `*` 在全角引号边界正确开启/关闭强调
        // - `*中文*，继续` 等场景正常工作
        val beforeIsPunct = CharacterUtils.isUnicodePunctuation(charBefore) ||
                CharacterUtils.isFullWidthPunctuation(charBefore)
        val afterIsPunct = CharacterUtils.isUnicodePunctuation(charAfter) ||
                CharacterUtils.isFullWidthPunctuation(charAfter)
        val beforeIsCJK = CharacterUtils.isCJK(charBefore)
        val afterIsCJK = CharacterUtils.isCJK(charAfter)

        val leftFlanking = !CharacterUtils.isUnicodeWhitespace(charAfter) &&
                (!afterIsPunct ||
                        CharacterUtils.isUnicodeWhitespace(charBefore) ||
                        beforeIsPunct ||
                        beforeIsCJK)

        val rightFlanking = !CharacterUtils.isUnicodeWhitespace(charBefore) &&
                (!beforeIsPunct ||
                        CharacterUtils.isUnicodeWhitespace(charAfter) ||
                        afterIsPunct ||
                        afterIsCJK)

        val canOpen = if (delimChar == '_') {
            leftFlanking && (!rightFlanking || beforeIsPunct)
        } else {
            leftFlanking
        }

        val canClose = if (delimChar == '_') {
            rightFlanking && (!leftFlanking || afterIsPunct)
        } else {
            rightFlanking
        }

        val textNode = Text(delimChar.toString().repeat(count))
        val ll = appendLL(textNode)

        if (canOpen || canClose) {
            val entry = DelimEntry(ll, delimChar, count, count, canOpen, canClose)
            ll.delimEntry = entry
            pushDelim(entry)
        }
    }

    private fun appendTildeRun() {
        val pos = scanner.pos
        var count = 0
        while (!scanner.isAtEnd && scanner.peek() == '~') {
            scanner.advance()
            count++
        }

        if (count == 2) {
            // ~~ strikethrough: enabled by either enableExtendedInline or enableStrikethrough
            val charBefore = if (pos > 0) input[pos - 1] else '\n'
            val charAfter = if (scanner.pos < input.length) input[scanner.pos] else '\n'
            val canOpen = !CharacterUtils.isUnicodeWhitespace(charAfter)
            val canClose = !CharacterUtils.isUnicodeWhitespace(charBefore)
            val textNode = Text("~~")
            val ll = appendLL(textNode)
            if (canOpen || canClose) {
                val entry = DelimEntry(ll, '~', 2, 2, canOpen, canClose)
                ll.delimEntry = entry
                pushDelim(entry)
            }
        } else if (count == 1 && enableExtendedInline) {
            // Single ~ subscript: only with enableExtendedInline (not in GFM-only mode)
            val charBefore = if (pos > 0) input[pos - 1] else '\n'
            val charAfter = if (scanner.pos < input.length) input[scanner.pos] else '\n'
            val canOpen = !CharacterUtils.isUnicodeWhitespace(charAfter)
            val canClose = !CharacterUtils.isUnicodeWhitespace(charBefore)
            val textNode = Text("~")
            val ll = appendLL(textNode)
            if (canOpen || canClose) {
                val entry = DelimEntry(ll, '~', 1, 1, canOpen, canClose)
                ll.delimEntry = entry
                pushDelim(entry)
            }
        } else {
            appendLL(Text("~".repeat(count)))
        }
    }

    private fun appendPairedDelim(char: Char, count: Int) {
        val pos = scanner.pos
        repeat(count) { scanner.advance() }
        val charBefore = if (pos > 0) input[pos - 1] else '\n'
        val charAfter = if (scanner.pos < input.length) input[scanner.pos] else '\n'
        val canOpen = !CharacterUtils.isUnicodeWhitespace(charAfter)
        val canClose = !CharacterUtils.isUnicodeWhitespace(charBefore)
        val textNode = Text(char.toString().repeat(count))
        val ll = appendLL(textNode)
        if (canOpen || canClose) {
            val entry = DelimEntry(ll, char, count, count, canOpen, canClose)
            ll.delimEntry = entry
            pushDelim(entry)
        }
    }

    private fun appendDollar() {
        val pos = scanner.pos
        if (scanner.peek(1) == '$') {
            scanner.advance()
            scanner.advance()
            val startContent = scanner.pos
            val endIdx = input.indexOf("$$", startContent)
            if (endIdx >= 0) {
                val content = input.substring(startContent, endIdx)
                scanner.pos = endIdx + 2
                appendLL(InlineMath(content))
                return
            }
            appendLL(Text("$$"))
            return
        }

        scanner.advance()
        if (pos > 0 && input[pos - 1].isDigit()) {
            appendLL(Text("$"))
            return
        }
        if (scanner.isAtEnd || scanner.peek().isWhitespace()) {
            appendLL(Text("$"))
            return
        }

        val startContent = scanner.pos
        val savedPos = scanner.pos
        while (!scanner.isAtEnd) {
            if (scanner.peek() == '$') {
                val content = input.substring(startContent, scanner.pos)
                scanner.advance()
                if (content.isNotEmpty() && !content.last().isWhitespace()) {
                    appendLL(InlineMath(content))
                    return
                }
                appendLL(Text("\$$content\$"))
                return
            }
            if (scanner.peek() == '\\' && scanner.pos + 1 < input.length) {
                scanner.advance()
            }
            scanner.advance()
        }
        scanner.pos = pos + 1
        appendLL(Text("$"))
    }

    private fun appendDirective() {
        val pos = scanner.pos
        // find the closing %}
        val closeIdx = input.indexOf("%}", pos + 2)
        if (closeIdx < 0) {
            // not a valid directive, emit as text
            scanner.advance() // {
            appendLL(Text("{"))
            return
        }
        val inner = input.substring(pos + 2, closeIdx).trim()
        if (inner.isEmpty() || inner.startsWith("end")) {
            scanner.advance()
            appendLL(Text("{"))
            return
        }
        // advance past the closing %}
        scanner.pos = closeIdx + 2
        val (tagName, args) = com.hrm.markdown.parser.block.starters.DirectiveBlockStarter.parseDirectiveArgs(inner)
        appendLL(DirectiveInline(tagName = tagName, args = args))
    }

    private fun appendPossibleEmoji() {
        val pos = scanner.pos
        scanner.advance() // skip ':'

        // 尝试 ASCII 表情匹配（如 :) :D 等）
        if (enableAsciiEmoticons) {
            tryMatchAsciiEmoticon(pos)
            return
        }

        if (scanner.isAtEnd || !(scanner.peek().isLetterOrDigit() || scanner.peek() == '_' || scanner.peek() == '+' || scanner.peek() == '-')) {
            appendLL(Text(":"))
            return
        }

        val startName = scanner.pos
        while (!scanner.isAtEnd && (scanner.peek().isLetterOrDigit() || scanner.peek() == '_' || scanner.peek() == '-' || scanner.peek() == '+')) {
            scanner.advance()
        }

        if (!scanner.isAtEnd && scanner.peek() == ':') {
            val name = input.substring(startName, scanner.pos)
            scanner.advance()
            // 查找 Unicode 映射：先查自定义映射，再查标准映射
            val unicode = customEmojiMap[name]
                ?: InlineParser.STANDARD_EMOJI_MAP[name]
            val emoji = Emoji(
                shortcode = name,
                literal = unicode ?: ":$name:",
            )
            emoji.unicode = unicode
            appendLL(emoji)
            return
        }

        scanner.pos = pos + 1
        appendLL(Text(":"))
    }

    /**
     * 尝试匹配 ASCII 表情符号（从当前 ':' 位置开始向前回溯检查）。
     * 在 `:` 之后检查是否能构成 `:)` `:D` 等模式。
     */
    private fun tryMatchAsciiEmoticon(colonPos: Int): Boolean {
        // 从 colonPos 开始，尝试匹配 ASCII 表情（最长匹配）
        val maxLen = 4 // ASCII 表情最大长度
        val remaining = input.length - colonPos
        for (len in minOf(maxLen, remaining) downTo 2) {
            val candidate = input.substring(colonPos, colonPos + len)
            val unicode = InlineParser.ASCII_EMOTICON_MAP[candidate]
            if (unicode != null) {
                scanner.pos = colonPos + len
                val emoji = Emoji(
                    shortcode = candidate,
                    literal = unicode,
                )
                emoji.unicode = unicode
                appendLL(emoji)
                return true
            }
        }
        return false
    }

    /**
     * 尝试解析 Wiki 链接 `[[target]]` 或 `[[target|label]]`。
     * 成功则追加 WikiLink 节点并返回 true，否则不移动 scanner 位置并返回 false。
     */
    private fun tryAppendWikiLink(): Boolean {
        val pos = scanner.pos
        // 期望 [[
        if (scanner.peek() != '[' || scanner.peek(1) != '[') return false
        scanner.advance() // skip first [
        scanner.advance() // skip second [

        val startContent = scanner.pos
        var pipePos = -1

        // 寻找 ]] 闭合，同时记录 | 位置
        while (!scanner.isAtEnd) {
            val c = scanner.peek()
            if (c == ']' && scanner.pos + 1 < input.length && input[scanner.pos + 1] == ']') {
                // 找到 ]]
                val content = input.substring(startContent, scanner.pos)
                scanner.advance() // skip first ]
                scanner.advance() // skip second ]

                val target: String
                val label: String?
                if (pipePos >= 0) {
                    target = input.substring(startContent, pipePos).trim()
                    label = input.substring(pipePos + 1, scanner.pos - 2).trim()
                } else {
                    target = content.trim()
                    label = null
                }

                if (target.isEmpty()) {
                    // 空目标无效，回退
                    scanner.pos = pos
                    return false
                }

                appendLL(WikiLink(target = target, label = label))
                return true
            }
            if (c == '|' && pipePos < 0) {
                pipePos = scanner.pos
            }
            if (c == '\n') {
                // Wiki 链接不跨行
                break
            }
            scanner.advance()
        }

        // 未找到 ]]，回退
        scanner.pos = pos
        return false
    }

    /**
     * 尝试解析 Ruby 注音 `{base|annotation}`。
     * 格式：左花括号 + 基础文本 + | + 注音文本 + 右花括号。
     * 不跨行，基础文本和注音文本均不能为空。
     */
    private fun appendPossibleRuby() {
        val pos = scanner.pos
        scanner.advance() // skip '{'

        val startBase = scanner.pos
        var pipePos = -1

        while (!scanner.isAtEnd) {
            val c = scanner.peek()
            if (c == '}') {
                if (pipePos < 0) {
                    // 没有 |，不是 Ruby 语法
                    break
                }
                val base = input.substring(startBase, pipePos).trim()
                val annotation = input.substring(pipePos + 1, scanner.pos).trim()
                if (base.isEmpty() || annotation.isEmpty()) {
                    break
                }
                scanner.advance() // skip '}'
                appendLL(RubyText(base = base, annotation = annotation))
                return
            }
            if (c == '|' && pipePos < 0) {
                pipePos = scanner.pos
            }
            if (c == '\n' || c == '{') {
                // Ruby 不跨行，不嵌套
                break
            }
            scanner.advance()
        }

        // 不是 Ruby 语法，回退并输出 { 为文本
        scanner.pos = pos + 1
        appendLL(Text("{"))
    }

    private fun appendLineBreak() {
        val pos = scanner.pos
        scanner.advance()
        // strip trailing spaces from the preceding text node
        val prevText = llTail?.astNode as? Text
        if (prevText != null) {
            prevText.literal = prevText.literal.trimEnd(' ')
        }
        if (pos >= 2 && input[pos - 1] == ' ' && input[pos - 2] == ' ') {
            appendLL(HardLineBreak())
        } else {
            appendLL(SoftLineBreak())
        }
        // skip leading spaces on the next line
        while (!scanner.isAtEnd && scanner.peek() == ' ') {
            scanner.advance()
        }
    }

    private fun appendText() {
        val sb = textBuffer
        sb.clear()
        while (!scanner.isAtEnd) {
            val c = scanner.peek()
            if (c == '\\' || c == '`' || c == '<' || c == '&' || c == '[' || c == ']' ||
                c == '*' || c == '_' || c == '\n') {
                break
            }
            if (c == '~' && (enableExtendedInline || enableStrikethrough)) break
            if (enableExtendedInline && (c == '$' || c == ':' || c == '^')) {
                break
            }
            if (c == '!' && scanner.peek(1) == '[') break
            if (enableExtendedInline && c == '=' && scanner.peek(1) == '=') break
            if (enableExtendedInline && c == '+' && scanner.peek(1) == '+') break
            if (enableExtendedInline && c == '{' && scanner.peek(1) == '%') break
            if (enableExtendedInline && c == '{' && scanner.peek(1) != '%') break
            if (enableExtendedInline && c == '>' && scanner.peek(1) == '!') break

            // ASCII 表情检测（非 : 开头的，如 ;) B) XD 等）
            if (enableAsciiEmoticons && (c == ';' || c == 'B' || c == 'X' || c == 'x' || c == '8' || c == 'O' || c == 'o')) {
                val maxLen = 4
                val remaining = input.length - scanner.pos
                var matched = false
                for (len in minOf(maxLen, remaining) downTo 2) {
                    val candidate = input.substring(scanner.pos, scanner.pos + len)
                    val unicode = InlineParser.ASCII_EMOTICON_MAP[candidate]
                    if (unicode != null) {
                        if (sb.isNotEmpty()) {
                            appendLL(Text(sb.toString()))
                            sb.clear()
                        }
                        scanner.pos += len
                        val emoji = Emoji(shortcode = candidate, literal = unicode)
                        emoji.unicode = unicode
                        appendLL(emoji)
                        matched = true
                        break
                    }
                }
                if (matched) return
            }

            // GFM bare URL autolink detection
            if (enableGfmAutolinks && (c == 'h' || c == 'H' || c == 'w' || c == 'W')) {
                val urlMatch = InlineParser.GFM_URL_REGEX.find(input, scanner.pos)
                if (urlMatch != null && urlMatch.range.first == scanner.pos) {
                    // 先输出已收集的文本
                    if (sb.isNotEmpty()) {
                        appendLL(Text(sb.toString()))
                        sb.clear()
                    }
                    var url = urlMatch.value
                    url = trimAutolinkTrailing(url)
                    scanner.pos += url.length
                    val fullUrl = if (url.lowercase().startsWith("www.")) "http://$url" else url
                    val encodedUrl = CharacterUtils.percentEncodeUrl(fullUrl)
                    val link = Link(destination = encodedUrl)
                    link.appendChild(Text(url))
                    appendLL(link)
                    return
                }
            }

            sb.append(c)
            scanner.advance()
        }
        if (sb.isNotEmpty()) {
            appendLL(Text(sb.toString()))
        }
    }

    private fun trimAutolinkTrailing(url: String): String {
        var result = url
        while (result.isNotEmpty()) {
            val last = result.last()
            when {
                last == '.' || last == ',' || last == ':' || last == ';' ||
                        last == '!' || last == '?' || last == '\'' || last == '"' -> {
                    result = result.dropLast(1)
                }
                last == ')' -> {
                    if (result.count { it == ')' } > result.count { it == '(' }) {
                        result = result.dropLast(1)
                    } else break
                }
                else -> break
            }
        }
        return result
    }

    // ────── 强调处理（CommonMark 算法） ──────

    private fun processEmphasis(stackBottom: DelimEntry?) {
        // 查找 stackBottom 上方的第一个分隔符
        var closer = if (stackBottom != null) stackBottom.next else delimHead

        while (closer != null) {
            if (!closer.canClose) {
                closer = closer.next
                continue
            }

            // 查找匹配的开始分隔符
            var opener = closer.prev
            var openerFound = false
            while (opener != null && opener !== stackBottom) {
                if (opener.canOpen && opener.char == closer.char && delimsMatch(opener, closer)) {
                    openerFound = true
                    break
                }
                opener = opener.prev
            }

            if (!openerFound || opener == null) {
                closer = closer.next
                continue
            }

            // 创建适当的包装节点
            val wrapperNode: ContainerNode
            val useCount: Int

            when (closer.char) {
                '*', '_' -> {
                    useCount = if (opener.count >= 2 && closer.count >= 2) 2 else 1
                    wrapperNode = if (useCount == 2) {
                        StrongEmphasis().also { it.delimiter = closer.char }
                    } else {
                        Emphasis().also { it.delimiter = closer.char }
                    }
                }
                '~' -> {
                    if (opener.count >= 2 && closer.count >= 2) {
                        useCount = 2
                        wrapperNode = Strikethrough()
                    } else {
                        useCount = 1
                        wrapperNode = Subscript()
                    }
                }
                '=' -> {
                    useCount = 2
                    wrapperNode = Highlight()
                }
                '+' -> {
                    useCount = 2
                    wrapperNode = InsertedText()
                }
                '^' -> {
                    useCount = 1
                    wrapperNode = Superscript()
                }
                else -> {
                    closer = closer.next
                    continue
                }
            }

            opener.count -= useCount
            closer.count -= useCount

            // 更新开始和关闭分隔符的文本
            if (opener.count > 0) {
                (opener.llNode.astNode as Text).literal = opener.char.toString().repeat(opener.count)
            }
            if (closer.count > 0) {
                (closer.llNode.astNode as Text).literal = closer.char.toString().repeat(closer.count)
            }

            // 将开始和关闭分隔符之间的节点移入包装节点
            var node = opener.llNode.next
            while (node != null && node !== closer.llNode) {
                val next = node.next
                removeLL(node)
                // 如果有分隔符条目，也从分隔符栈中移除
                node.delimEntry?.let { removeDelim(it) }
                wrapperNode.appendChild(node.astNode)
                node = next
            }

            // 在开始分隔符之后插入包装节点
            val wrapperLL = insertAfterLL(opener.llNode, wrapperNode)

            // 如果计数为 0 则移除开始分隔符
            if (opener.count == 0) {
                removeLL(opener.llNode)
                removeDelim(opener)
            }

            // 如果计数为 0 则移除关闭分隔符，并前进
            if (closer.count == 0) {
                val nextCloser = closer.next
                removeLL(closer.llNode)
                removeDelim(closer)
                closer = nextCloser
            }
            // 如果 closer 还有剩余计数，保持不变继续下一轮匹配
            // （例如 ***text*** 消耗2个后剩余1个，需要继续匹配斜体）
        }

        // 移除 stackBottom 上方的剩余分隔符
        var d = if (stackBottom != null) stackBottom.next else delimHead
        while (d != null) {
            val next = d.next
            removeDelim(d)
            d = next
        }
    }

    private fun delimsMatch(opener: DelimEntry, closer: DelimEntry): Boolean {
        if (opener.char != closer.char) return false
        if (opener.char == '*' || opener.char == '_') {
            if ((opener.canOpen && opener.canClose) || (closer.canOpen && closer.canClose)) {
                if ((opener.origCount + closer.origCount) % 3 == 0 &&
                    opener.origCount % 3 != 0 && closer.origCount % 3 != 0
                ) {
                    return false
                }
            }
        }
        if (opener.char == '~') {
            // 双 ~ 匹配删除线，单 ~ 匹配下标，不混合
            if (opener.count >= 2 && closer.count >= 2) return true
            if (opener.count == 1 && closer.count == 1) return true
            return false
        }
        if (opener.char == '=' || opener.char == '+') {
            return opener.count >= 2 && closer.count >= 2
        }
        return true
    }

    // ────── 链接解析 ──────

    /**
     * 链接尾部解析结果。
     */
    private data class LinkTailResult(
        val destination: String,
        val title: String?,
        val width: Int? = null,
        val height: Int? = null,
    )

    private fun resolveBackslashEscapes(s: String): String {
        val sb = StringBuilder(s.length)
        var i = 0
        while (i < s.length) {
            if (s[i] == '\\' && i + 1 < s.length && CharacterUtils.isAsciiPunctuation(s[i + 1])) {
                sb.append(s[i + 1])
                i += 2
            } else {
                sb.append(s[i])
                i++
            }
        }
        return sb.toString()
    }

    private fun tryParseLinkTail(isImage: Boolean = false): LinkTailResult? {
        val pos = scanner.pos
        if (pos >= input.length || input[pos] != '(') return null

        var i = pos + 1
        while (i < input.length && (input[i] == ' ' || input[i] == '\t' || input[i] == '\n')) i++

        if (i >= input.length) return null

        // 空链接 ()
        if (input[i] == ')') {
            scanner.pos = i + 1
            return LinkTailResult("", null)
        }

        val (dest, nextPos, isAngleBracket) = parseLinkDestination(i) ?: return null
        i = nextPos

        while (i < input.length && (input[i] == ' ' || input[i] == '\t' || input[i] == '\n')) i++

        // 对图片尝试解析 =WxH 尺寸后缀
        var imgWidth: Int? = null
        var imgHeight: Int? = null
        if (isImage && i < input.length && input[i] == '=') {
            val sizeResult = parseImageSize(i)
            if (sizeResult != null) {
                imgWidth = sizeResult.width
                imgHeight = sizeResult.height
                i = sizeResult.nextPos
                while (i < input.length && (input[i] == ' ' || input[i] == '\t' || input[i] == '\n')) i++
            }
        }

        var title: String? = null
        if (i < input.length && (input[i] == '"' || input[i] == '\'' || input[i] == '(')) {
            val (t, tNext) = parseLinkTitle(i) ?: return null
            title = t
            i = tNext
            while (i < input.length && (input[i] == ' ' || input[i] == '\t')) i++
        }

        if (i >= input.length || input[i] != ')') return null
        scanner.pos = i + 1
        val resolvedDest = HtmlEntities.replaceAll(dest)
        val finalDest = CharacterUtils.percentEncodeUrl(resolvedDest)
        val resolvedTitle = title?.let { HtmlEntities.replaceAll(it) }
        return LinkTailResult(finalDest, resolvedTitle, imgWidth, imgHeight)
    }

    /**
     * 解析图片尺寸后缀 `=WxH`、`=Wx`、`=xH`。
     * 例如 `=200x300`、`=200x`、`=x300`。
     */
    private data class ImageSizeResult(val width: Int?, val height: Int?, val nextPos: Int)

    private fun parseImageSize(start: Int): ImageSizeResult? {
        var i = start
        if (i >= input.length || input[i] != '=') return null
        i++ // 跳过 '='

        // 解析宽度（可选）
        val widthStart = i
        while (i < input.length && input[i].isDigit()) i++
        val widthStr = input.substring(widthStart, i)

        // 期望 'x' 或 'X' 分隔符
        if (i >= input.length || (input[i] != 'x' && input[i] != 'X')) {
            // 没有 x 分隔符，仅有数字也可以作为纯宽度
            if (widthStr.isNotEmpty()) {
                return ImageSizeResult(widthStr.toIntOrNull(), null, i)
            }
            return null
        }
        i++ // 跳过 'x'

        // 解析高度（可选）
        val heightStart = i
        while (i < input.length && input[i].isDigit()) i++
        val heightStr = input.substring(heightStart, i)

        val width = widthStr.takeIf { it.isNotEmpty() }?.toIntOrNull()
        val height = heightStr.takeIf { it.isNotEmpty() }?.toIntOrNull()

        // 至少需要指定宽度或高度之一
        if (width == null && height == null) return null

        return ImageSizeResult(width, height, i)
    }

    /**
     * 尝试解析属性块 `{.class #id key=value ...}`。
     * 返回属性映射，如果没有属性块则返回 null。
     */
    private fun tryParseAttributes(): Map<String, String>? {
        val pos = scanner.pos
        if (pos >= input.length || input[pos] != '{') return null

        var i = pos + 1
        val attrs = mutableMapOf<String, String>()
        val classes = mutableListOf<String>()

        while (i < input.length && input[i] != '}') {
            // 跳过空白
            while (i < input.length && (input[i] == ' ' || input[i] == '\t')) i++
            if (i >= input.length || input[i] == '}') break

            when (input[i]) {
                '.' -> {
                    // CSS class: .classname
                    i++ // 跳过 '.'
                    val nameStart = i
                    while (i < input.length && input[i] != ' ' && input[i] != '\t' &&
                        input[i] != '}' && input[i] != '.' && input[i] != '#'
                    ) i++
                    val className = input.substring(nameStart, i)
                    if (className.isNotEmpty()) classes.add(className)
                }
                '#' -> {
                    // CSS ID: #idname
                    i++ // 跳过 '#'
                    val nameStart = i
                    while (i < input.length && input[i] != ' ' && input[i] != '\t' &&
                        input[i] != '}' && input[i] != '.' && input[i] != '#'
                    ) i++
                    val idName = input.substring(nameStart, i)
                    if (idName.isNotEmpty()) attrs["id"] = idName
                }
                else -> {
                    // key=value 或 key="value" 或 key='value'
                    val keyStart = i
                    while (i < input.length && input[i] != '=' && input[i] != ' ' &&
                        input[i] != '\t' && input[i] != '}'
                    ) i++
                    val key = input.substring(keyStart, i)
                    if (i < input.length && input[i] == '=') {
                        i++ // 跳过 '='
                        val value: String
                        if (i < input.length && (input[i] == '"' || input[i] == '\'')) {
                            val quote = input[i]
                            i++ // 跳过引号
                            val valStart = i
                            while (i < input.length && input[i] != quote) i++
                            value = input.substring(valStart, i)
                            if (i < input.length) i++ // 跳过闭合引号
                        } else {
                            val valStart = i
                            while (i < input.length && input[i] != ' ' && input[i] != '\t' &&
                                input[i] != '}'
                            ) i++
                            value = input.substring(valStart, i)
                        }
                        if (key.isNotEmpty()) attrs[key] = value
                    } else {
                        // 没有 = 的布尔属性
                        if (key.isNotEmpty()) attrs[key] = ""
                    }
                }
            }
        }

        // 必须以 } 关闭
        if (i >= input.length || input[i] != '}') return null
        i++ // 跳过 '}'

        if (classes.isNotEmpty()) {
            attrs["class"] = classes.joinToString(" ")
        }

        scanner.pos = i
        return attrs
    }

    /**
     * 尝试解析 `[text]{attrs}` 的属性部分。
     * 即紧接 `]` 之后的 `{...}` 块，且前面没有 `(` 链接尾部。
     * 返回属性映射，如果不匹配则返回 null。
     */
    private fun tryParseStyledText(): Map<String, String>? {
        val pos = scanner.pos
        if (pos >= input.length || input[pos] != '{') return null
        return tryParseAttributes()
    }

    private data class LinkDestResult(val dest: String, val nextPos: Int, val isAngleBracket: Boolean)

    private fun parseLinkDestination(start: Int): LinkDestResult? {
        var i = start
        if (i >= input.length) return null

        if (input[i] == '<') {
            i++
            val sb = StringBuilder()
            while (i < input.length && input[i] != '>') {
                if (input[i] == '<' || input[i] == '\n') return null
                if (input[i] == '\\' && i + 1 < input.length && CharacterUtils.isAsciiPunctuation(input[i + 1])) {
                    i++
                    sb.append(input[i])
                } else {
                    sb.append(input[i])
                }
                i++
            }
            if (i >= input.length) return null
            i++
            return LinkDestResult(sb.toString(), i, true)
        }

        val sb = StringBuilder()
        var parenDepth = 0
        while (i < input.length) {
            val c = input[i]
            when {
                c == ' ' || c == '\t' || c == '\n' -> break
                c == ')' && parenDepth == 0 -> break
                c == '(' -> { parenDepth++; sb.append(c) }
                c == ')' -> { parenDepth--; sb.append(c) }
                c == '\\' && i + 1 < input.length && CharacterUtils.isAsciiPunctuation(input[i + 1]) -> {
                    i++; sb.append(input[i])
                }
                c == '\\' -> sb.append(c)
                c.code < 0x20 -> break
                else -> sb.append(c)
            }
            i++
        }
        return LinkDestResult(sb.toString(), i, false)
    }

    private fun parseLinkTitle(start: Int): Pair<String, Int>? {
        var i = start
        if (i >= input.length) return null
        val openChar = input[i]
        val closeChar = when (openChar) {
            '"' -> '"'; '\'' -> '\''; '(' -> ')'; else -> return null
        }
        i++
        val sb = StringBuilder()
        while (i < input.length && input[i] != closeChar) {
            if (input[i] == '\\' && i + 1 < input.length && CharacterUtils.isAsciiPunctuation(input[i + 1])) {
                i++
                sb.append(input[i])
            } else {
                sb.append(input[i])
            }
            i++
        }
        if (i >= input.length) return null
        i++
        return Pair(sb.toString(), i)
    }

    private fun tryParseRefLink(bracket: BracketEntry): Pair<String, String?>? {
        val pos = scanner.pos

        // [text][label] - full reference link
        if (pos < input.length && input[pos] == '[') {
            val closeIdx = findCloseBracket(pos + 1)
            if (closeIdx >= 0) {
                val label = input.substring(pos + 1, closeIdx)
                // reject labels that contain unescaped [
                if (isValidLinkLabel(label)) {
                    val normalized = CharacterUtils.normalizeLinkLabel(label)
                    val def = document.linkDefinitions[normalized]
                    if (def != null) {
                        scanner.pos = closeIdx + 1
                        return Pair(def.destination, def.title)
                    }
                }
            }
        }

        // use raw text from input for label matching (preserves backslash escapes)
        val closeBracketPos = scanner.pos - 1 // position of the ] we just consumed
        val rawLabel = input.substring(bracket.contentStartPos, closeBracketPos)

        // collapsed [label][]
        if (pos + 1 < input.length && input[pos] == '[' && input[pos + 1] == ']') {
            if (isValidLinkLabel(rawLabel)) {
                val normalized = CharacterUtils.normalizeLinkLabel(rawLabel)
                val def = document.linkDefinitions[normalized]
                if (def != null) {
                    scanner.pos = pos + 2
                    return Pair(def.destination, def.title)
                }
            }
        }

        // shortcut [label] - only if next char is NOT [
        // (per spec, full ref takes precedence, so [foo][bar] should not
        // let [foo] match as shortcut when [bar][...] might match later)
        if (pos >= input.length || input[pos] != '[') {
            if (isValidLinkLabel(rawLabel)) {
                val normalized = CharacterUtils.normalizeLinkLabel(rawLabel)
                val def = document.linkDefinitions[normalized]
                if (def != null) {
                    return Pair(def.destination, def.title)
                }
            }
        }

        return null
    }

    /**
     * find the next unescaped ] starting from position start.
     * returns the index of ], or -1 if not found.
     */
    private fun findCloseBracket(start: Int): Int {
        var i = start
        while (i < input.length) {
            when (input[i]) {
                '\\' -> i += 2 // skip escaped char
                ']' -> return i
                else -> i++
            }
        }
        return -1
    }

    /**
     * check that a link label does not contain unescaped [ characters.
     * per commonmark spec, link labels cannot contain unescaped brackets.
     */
    private fun isValidLinkLabel(label: String): Boolean {
        var i = 0
        while (i < label.length) {
            when (label[i]) {
                '\\' -> i += 2 // skip escaped char
                '[' -> return false
                else -> i++
            }
        }
        return true
    }

    private fun tryParseFootnoteReference(bracket: BracketEntry): FootnoteReference? {
        // 提取方括号内的文本内容
        val text = extractBracketText(bracket)
        // 脚注引用格式：[^label]，内容必须以 ^ 开头
        if (!text.startsWith("^")) return null
        val label = text.substring(1)
        if (label.isEmpty() || label.contains(' ') || label.contains('\n')) return null

        // 不后跟 ( 或 [（否则可能是链接）
        val pos = scanner.pos
        if (pos < input.length && (input[pos] == '(' || input[pos] == '[')) return null

        // 查找对应的脚注定义
        val def = document.footnoteDefinitions[label]
        val index = if (def != null) {
            if (def.index == 0) {
                def.index = document.footnoteDefinitions.values.count { it.index > 0 } + 1
            }
            def.index
        } else {
            0
        }

        return FootnoteReference(label = label, index = index)
    }

    private fun extractBracketText(bracket: BracketEntry): String {
        val sb = StringBuilder()
        var cur = bracket.llNode.next
        while (cur != null) {
            sb.append(nodeToText(cur.astNode))
            cur = cur.next
        }
        return sb.toString()
    }

    private fun nodeToText(node: Node): String = when (node) {
        is Text -> node.literal
        is InlineCode -> node.literal
        is EscapedChar -> node.literal
        is SoftLineBreak -> " "
        is HardLineBreak -> " "
        is ContainerNode -> node.children.joinToString("") { nodeToText(it) }
        is LeafNode -> node.literal
    }

    /**
     * 尝试解析参考文献引用：[@key]
     */
    private fun tryParseCitationReference(bracket: BracketEntry): CitationReference? {
        val text = extractBracketText(bracket)
        // 格式：[@key]
        if (!text.startsWith("@")) return null
        val key = text.substring(1).trim()
        if (key.isEmpty() || key.contains(' ') || key.contains('\n')) return null

        // 不后跟 ( 或 [（否则可能是链接）
        val pos = scanner.pos
        if (pos < input.length && (input[pos] == '(' || input[pos] == '[')) return null

        return CitationReference(key = key)
    }

    /**
     * 解析剧透/折叠文本：>!spoiler text!<
     */
    private fun appendSpoiler() {
        val pos = scanner.pos
        scanner.advance() // skip '>'
        if (scanner.isAtEnd || scanner.peek() != '!') {
            scanner.pos = pos
            appendText()
            return
        }
        scanner.advance() // skip '!'

        // 寻找匹配的 !<
        val startContent = scanner.pos
        while (!scanner.isAtEnd) {
            if (scanner.peek() == '!' && scanner.pos + 1 < input.length && input[scanner.pos + 1] == '<') {
                val content = input.substring(startContent, scanner.pos)
                scanner.advance() // skip '!'
                scanner.advance() // skip '<'

                // 创建 Spoiler 容器节点，将内容作为子 Text
                val spoiler = Spoiler()
                spoiler.appendChild(Text(content))
                appendLL(spoiler)
                return
            }
            if (scanner.peek() == '\n') {
                // 剧透不跨行
                break
            }
            scanner.advance()
        }

        // 未找到匹配的 !<，回退
        scanner.pos = pos
        appendText()
    }
}
