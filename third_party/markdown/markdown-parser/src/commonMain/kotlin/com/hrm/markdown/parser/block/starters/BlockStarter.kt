package com.hrm.markdown.parser.block.starters

import com.hrm.markdown.parser.block.OpenBlock
import com.hrm.markdown.parser.core.LineCursor

/**
 * 块级开启器接口。
 *
 * 每个 BlockStarter 实现负责检测并创建一种块级节点。
 * 通过 [BlockStarterRegistry] 注册制管理，支持插件化扩展。
 *
 * 设计原则：
 * - 单一职责：每个实现只处理一种块类型
 * - 无状态：tryStart 为纯函数，所有上下文通过参数传入
 * - 可组合：通过 priority 控制检测顺序
 */
interface BlockStarter {
    /**
     * 优先级（数字越小越先检测）。
     *
     * 推荐范围：
     * - 0-99: 最高优先级（FrontMatter 等文档级结构）
     * - 100-199: Setext 标题、ATX 标题
     * - 200-299: 表格、主题分隔线
     * - 300-399: 自定义容器、围栏代码块、数学块
     * - 400-499: HTML 块、块引用
     * - 500-599: 列表项、脚注、定义列表
     * - 600-699: 缩进代码块（最低）
     */
    val priority: Int

    /**
     * 此块是否能中断段落。
     *
     * 当在段落上下文中检测到新块时，如果该块不能中断段落，
     * 则当前行将被作为段落的懒延续行处理。
     */
    val canInterruptParagraph: Boolean

    /**
     * 尝试从当前游标位置开启新块。
     *
     * @param cursor 当前行的游标（调用方已推进到合适位置）
     * @param lineIdx 当前行号
     * @param tip 当前最深的已打开块
     * @return 若成功开启块则返回 [OpenBlock]，否则返回 null
     *
     * 注意：如果返回 null，调用方会恢复游标；
     * 如果返回非 null，游标应已推进到块内容的起始位置。
     */
    fun tryStart(cursor: LineCursor, lineIdx: Int, tip: OpenBlock): OpenBlock?
}
