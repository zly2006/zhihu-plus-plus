package com.hrm.markdown.parser.block.starters

import com.hrm.markdown.parser.LineRange
import com.hrm.markdown.parser.ast.FrontMatter
import com.hrm.markdown.parser.block.OpenBlock
import com.hrm.markdown.parser.core.LineCursor

/**
 * 前置元数据块开启器：`---` (YAML) 或 `+++` (TOML)。
 * 仅在文档第一行生效。
 *
 * 无状态设计：不依赖 SourceText，关闭标记由 [BlockParser] 的 addLineToTip 检测。
 * 若文档结束时仍未遇到关闭标记，[BlockParser.finalizeBlock] 会将其降级为 ThematicBreak + Paragraph。
 */
class FrontMatterStarter : BlockStarter {
    override val priority: Int = 10
    override val canInterruptParagraph: Boolean = false

    override fun tryStart(cursor: LineCursor, lineIdx: Int, tip: OpenBlock): OpenBlock? {
        if (lineIdx != 0) return null
        val rest = cursor.rest().trim()
        val format = when {
            rest == "---" -> "yaml"
            rest == "+++" -> "toml"
            else -> return null
        }

        val block = FrontMatter(format = format)
        block.lineRange = LineRange(lineIdx, lineIdx + 1)
        val ob = OpenBlock(block, contentStartLine = lineIdx, lastLineIndex = lineIdx)
        ob.starterTag = this::class.simpleName
        return ob
    }
}
