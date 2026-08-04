package com.hrm.markdown.parser.block.starters

import com.hrm.markdown.parser.LineRange
import com.hrm.markdown.parser.ast.DefinitionDescription
import com.hrm.markdown.parser.ast.DefinitionList
import com.hrm.markdown.parser.ast.FootnoteDefinition
import com.hrm.markdown.parser.ast.IndentedCodeBlock
import com.hrm.markdown.parser.block.OpenBlock
import com.hrm.markdown.parser.core.LineCursor

/**
 * 缩进代码块开启器：4 个空格或 1 个制表符缩进。
 * 优先级最低，不能中断段落。
 *
 * 在以下容器内部不触发，避免与扩展语法冲突：
 * - [DefinitionList] / [DefinitionDescription]：缩进内容属于定义描述的续行
 * - [FootnoteDefinition]：缩进内容属于脚注的续行
 */
internal class IndentedCodeBlockStarter : BlockStarter {
    override val priority: Int = 600
    override val canInterruptParagraph: Boolean = false

    override fun tryStart(cursor: LineCursor, lineIdx: Int, tip: OpenBlock): OpenBlock? {
        if (tip.paragraphContent != null) return null
        // 在定义列表或脚注内部，缩进是续行内容而非代码块
        if (tip.node is DefinitionList || tip.node is DefinitionDescription || tip.node is FootnoteDefinition) {
            return null
        }
        val indent = cursor.advanceSpaces(4)
        if (indent < 4) return null

        val block = IndentedCodeBlock()
        block.lineRange = LineRange(lineIdx, lineIdx + 1)

        val ob = OpenBlock(block, contentStartLine = lineIdx, lastLineIndex = lineIdx)
        ob.starterTag = this::class.simpleName
        return ob
    }
}
