package com.hrm.markdown.parser.block.starters

import com.hrm.markdown.parser.LineRange
import com.hrm.markdown.parser.ast.DirectiveBlock
import com.hrm.markdown.parser.block.OpenBlock
import com.hrm.markdown.parser.core.LineCursor

/**
 * block-level directive starter: `{% tag arg1 "arg2" key=value %}`.
 *
 * detects lines starting with `{%` and ending with `%}`.
 * block directives use `{% tag %}...{% endtag %}` syntax.
 * self-closing directives on a line by themselves are also block-level.
 */
internal class DirectiveBlockStarter : BlockStarter {
    override val priority: Int = 250
    override val canInterruptParagraph: Boolean = true

    override fun tryStart(cursor: LineCursor, lineIdx: Int, tip: OpenBlock): OpenBlock? {
        val indent = cursor.advanceSpaces(3)
        if (cursor.isAtEnd) return null

        if (cursor.peek() != '{') return null
        if (cursor.peek(1) != '%') return null

        val rest = cursor.rest().trim()
        if (!rest.startsWith("{%") || !rest.endsWith("%}")) return null

        // extract inner content between {% and %}
        val inner = rest.removePrefix("{%").removeSuffix("%}").trim()
        if (inner.isEmpty()) return null

        // check if this is an end tag
        if (inner.startsWith("end")) return null

        cursor.advance(cursor.remaining)

        val (tagName, args) = parseDirectiveArgs(inner)

        val block = DirectiveBlock(
            tagName = tagName,
            args = args,
        )
        block.lineRange = LineRange(lineIdx, lineIdx + 1)

        val ob = OpenBlock(block, contentStartLine = lineIdx, lastLineIndex = lineIdx)
        ob.isFenced = true
        ob.fenceChar = '%'
        ob.fenceLength = 2
        ob.fenceIndent = indent
        ob.starterTag = this::class.simpleName
        return ob
    }

    companion object {
        private val KV_REGEX = Regex("""([\w-]+)\s*=\s*(?:"([^"]*)"|'([^']*)'|([\w-]+))""")

        /**
         * parses directive arguments from the content between {% and %}.
         * supports positional args, quoted args, and key=value pairs.
         * positional args are stored with keys "_0", "_1", etc.
         */
        fun parseDirectiveArgs(inner: String): Pair<String, Map<String, String>> {
            val tokens = tokenize(inner)
            if (tokens.isEmpty()) return "" to emptyMap()

            val tagName = tokens[0]
            val args = mutableMapOf<String, String>()
            var positionalIndex = 0

            var i = 1
            while (i < tokens.size) {
                val token = tokens[i]
                // check if this is a key=value pair
                if (i + 2 < tokens.size && tokens[i + 1] == "=") {
                    args[token] = tokens[i + 2]
                    i += 3
                } else {
                    args["_$positionalIndex"] = token
                    positionalIndex++
                    i++
                }
            }

            return tagName to args
        }

        /**
         * tokenizes directive arguments, handling quoted strings.
         */
        private fun tokenize(input: String): List<String> {
            val tokens = mutableListOf<String>()
            var i = 0
            while (i < input.length) {
                when {
                    input[i].isWhitespace() -> i++
                    input[i] == '=' -> {
                        tokens.add("=")
                        i++
                    }
                    input[i] == '"' -> {
                        val end = input.indexOf('"', i + 1)
                        if (end >= 0) {
                            tokens.add(input.substring(i + 1, end))
                            i = end + 1
                        } else {
                            tokens.add(input.substring(i + 1))
                            i = input.length
                        }
                    }
                    input[i] == '\'' -> {
                        val end = input.indexOf('\'', i + 1)
                        if (end >= 0) {
                            tokens.add(input.substring(i + 1, end))
                            i = end + 1
                        } else {
                            tokens.add(input.substring(i + 1))
                            i = input.length
                        }
                    }
                    else -> {
                        val start = i
                        while (i < input.length && !input[i].isWhitespace() && input[i] != '=') {
                            i++
                        }
                        tokens.add(input.substring(start, i))
                    }
                }
            }
            return tokens
        }
    }
}
