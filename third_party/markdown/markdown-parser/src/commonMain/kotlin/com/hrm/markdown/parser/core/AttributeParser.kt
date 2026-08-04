package com.hrm.markdown.parser.core

/**
 * parsed result of `{.class1 .class2 #id key=value key="quoted value"}` syntax.
 *
 * usage:
 * ```
 * val (attrs, remaining) = AttributeParser.parse("kotlin {.highlight #my-block linenos=true}")
 * // attrs.classes = ["highlight"]
 * // attrs.id = "my-block"
 * // attrs.pairs = {"linenos" to "true"}
 * // remaining = "kotlin"
 * ```
 */
data class Attributes(
    val id: String? = null,
    val classes: List<String> = emptyList(),
    val pairs: Map<String, String> = emptyMap(),
) {
    val isEmpty: Boolean get() = id == null && classes.isEmpty() && pairs.isEmpty()

    // merge into flat map for interop with existing Image.attributes style
    fun toMap(): Map<String, String> = buildMap {
        id?.let { put("id", it) }
        if (classes.isNotEmpty()) put("class", classes.joinToString(" "))
        putAll(pairs)
    }
}

data class AttributeParseResult(
    val attributes: Attributes,
    val remaining: String,
)

object AttributeParser {

    private val ATTR_BLOCK = Regex("""\{([^}]*)\}""")
    private val CLASS_TOKEN = Regex("""\.([\w-]+)""")
    private val ID_TOKEN = Regex("""#([\w-]+)""")
    // key=value or key="value" or key='value'
    private val KV_TOKEN = Regex("""([\w-]+)\s*=\s*(?:"([^"]*)"|'([^']*)'|([\w-]+))""")

    // parses attribute block from input, returns parsed attrs + text with attr block removed
    fun parse(input: String): AttributeParseResult {
        val match = ATTR_BLOCK.find(input)
            ?: return AttributeParseResult(Attributes(), input)

        val content = match.groupValues[1]
        val classes = CLASS_TOKEN.findAll(content).map { it.groupValues[1] }.toList()
        val id = ID_TOKEN.find(content)?.groupValues?.get(1)
        val pairs = mutableMapOf<String, String>()

        for (kv in KV_TOKEN.findAll(content)) {
            val key = kv.groupValues[1]
            // value is in group 2 (double-quoted), 3 (single-quoted), or 4 (unquoted)
            val value = kv.groupValues[2].ifEmpty {
                kv.groupValues[3].ifEmpty { kv.groupValues[4] }
            }
            pairs[key] = value
        }

        val remaining = input.removeRange(match.range).trim()
        return AttributeParseResult(
            attributes = Attributes(id = id, classes = classes, pairs = pairs),
            remaining = remaining,
        )
    }
}
