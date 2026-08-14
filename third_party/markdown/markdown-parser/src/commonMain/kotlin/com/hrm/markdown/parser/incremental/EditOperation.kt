package com.hrm.markdown.parser.incremental

/**
 * 编辑操作定义。
 *
 * 描述对源文本的一次编辑变更，用于增量解析引擎计算脏区域。
 */
sealed class EditOperation {
    /**
     * 在指定偏移量处插入文本。
     */
    data class Insert(val offset: Int, val text: String) : EditOperation()

    /**
     * 从指定偏移量开始删除指定长度的文本。
     */
    data class Delete(val offset: Int, val length: Int) : EditOperation()

    /**
     * 替换：从指定偏移量开始，删除指定长度的文本并插入新文本。
     */
    data class Replace(val offset: Int, val length: Int, val newText: String) : EditOperation()

    /**
     * 追加：在文本末尾追加（流式场景的特化形式）。
     */
    data class Append(val text: String) : EditOperation()
}
