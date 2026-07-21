package com.hrm.markdown.parser.block

import com.hrm.markdown.parser.ast.Node

/**
 * 表示解析过程中一个已打开（尚未关闭）的块。
 *
 * 从 BlockParser 中提取为包级类，供 BlockStarters、BlockStarter 等共享。
 */
class OpenBlock(
    val node: Node,
    var lastLineIndex: Int = 0,
    var contentLines: MutableList<String> = mutableListOf(),
    var contentStartLine: Int = 0,
) {
    var paragraphContent: StringBuilder? = null
    var isFenced: Boolean = false
    var fenceChar: Char = ' '
    var fenceLength: Int = 0
    var fenceIndent: Int = 0
    var htmlType: Int = 0
    var blankLineCount: Int = 0
    var blankLineAfterContent: Boolean = false

    /** 列表项元数据（仅 ListItem 类型使用） */
    var listItemMeta: ListItemMeta? = null

    /** 创建此块的开启器标签（用于注册制匹配） */
    var starterTag: String? = null

    /** 当前行内自包含块闭合标记之后剩余的文本。 */
    var trailingContent: String? = null
}
