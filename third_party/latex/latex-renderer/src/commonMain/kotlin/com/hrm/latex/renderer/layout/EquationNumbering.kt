/*
 * Copyright (c) 2026 huarangmeng
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.hrm.latex.renderer.layout

import com.hrm.latex.parser.model.LatexNode

/**
 * 公式自动编号状态
 *
 * 在测量阶段共享，提供：
 * 1. 自增编号计数器（每遇到一个需要编号的环境自动递增）
 * 2. label → 编号字符串 的映射表（供 \ref/\eqref 引用）
 *
 * ## 使用流程
 * 1. 测量前，调用 [EquationNumbering.buildLabelMap] 预计算所有 label → 编号映射
 * 2. 将 [EquationNumberingState] 注入到 [RenderContext] 中
 * 3. 测量每个环境时，调用 [nextNumber] 获取下一个编号
 * 4. 测量 \ref/\eqref 时，从 [labelToNumber] 查找实际编号
 */
internal class EquationNumberingState(
    val labelToNumber: Map<String, String>
) {
    private var counter = 0

    /**
     * 获取下一个公式编号
     */
    fun nextNumber(): String {
        counter++
        return counter.toString()
    }

    /**
     * 查询 label 对应的编号
     * @return 编号字符串，找不到时返回 null
     */
    fun resolveLabel(key: String): String? = labelToNumber[key]
}

/**
 * 公式编号自动计算器
 *
 * 遍历 AST 节点列表，预计算 \label{key} → 编号字符串 的映射表，
 * 供渲染阶段的 \ref/\eqref 引用。
 *
 * 需要编号的环境（非星号变体、无手动 \tag）：
 * - `equation`（非 `equation*`）
 * - `align`（非 `align*`、非 `aligned`）
 * - `gather`（非 `gather*`、非 `gathered`）
 * - `multline`（非 `multline*`）
 * - `eqnarray`（非 `eqnarray*`）
 * - `flalign`（非 `flalign*`）
 * - `alignat`（非 `alignat*`）
 */
internal object EquationNumbering {

    /**
     * 需要自动编号的环境名列表（非星号变体）。
     * 注意：aligned/gathered 是内嵌环境，不独立编号。
     */
    private val NUMBERED_ENVIRONMENTS = setOf(
        "equation", "align", "gather", "multline",
        "eqnarray", "flalign", "alignat"
    )

    /**
     * 预计算整个文档的 label → 编号映射
     *
     * 遍历所有节点，按顺序为需要编号的环境分配递增编号，
     * 同时收集环境中的 \label 建立映射。
     *
     * @param children 文档根节点的子节点列表
     * @return label → 编号字符串 的映射表
     */
    fun buildLabelMap(children: List<LatexNode>): Map<String, String> {
        val labelToNumber = mutableMapOf<String, String>()
        var counter = 0

        fun nextNumber(): String {
            counter++
            return counter.toString()
        }

        fun process(nodes: List<LatexNode>) {
            for (node in nodes) {
                when (node) {
                    is LatexNode.Environment -> {
                        if (shouldNumberEnvironment(node.name, node.content)) {
                            val number = nextNumber()
                            // 在该环境内容中查找 \label，绑定编号
                            findLabelsInNodes(node.content).forEach { labelKey ->
                                labelToNumber[labelKey] = number
                            }
                        }
                        // 递归进入环境内容（处理嵌套环境）
                        process(node.content)
                    }

                    is LatexNode.Aligned -> {
                        if (shouldNumberAligned(node)) {
                            val number = nextNumber()
                            findLabelsInAllRows(node.rows).forEach { labelKey ->
                                labelToNumber[labelKey] = number
                            }
                        }
                        // 递归进入行内容
                        for (row in node.rows) {
                            process(row)
                        }
                    }

                    is LatexNode.Multline -> {
                        if (shouldNumberMultline(node)) {
                            val number = nextNumber()
                            findLabelsInNodes(node.lines).forEach { labelKey ->
                                labelToNumber[labelKey] = number
                            }
                        }
                    }

                    is LatexNode.Eqnarray -> {
                        if (shouldNumberEqnarray(node)) {
                            val number = nextNumber()
                            findLabelsInAllRows(node.rows).forEach { labelKey ->
                                labelToNumber[labelKey] = number
                            }
                        }
                        for (row in node.rows) {
                            process(row)
                        }
                    }

                    is LatexNode.Subequations -> {
                        // subequations 内部的环境使用子编号（如 1a, 1b, 1c）
                        val parentNumber = nextNumber()
                        var subCounter = 'a'

                        // 顶层 label 绑定到父编号
                        findLabelsDirectly(node.content).forEach { labelKey ->
                            labelToNumber[labelKey] = parentNumber
                        }

                        for (child in node.content) {
                            when (child) {
                                is LatexNode.Environment -> {
                                    if (shouldNumberEnvironment(child.name, child.content)) {
                                        val subNumber = "$parentNumber${subCounter}"
                                        subCounter++
                                        findLabelsInNodes(child.content).forEach { labelKey ->
                                            labelToNumber[labelKey] = subNumber
                                        }
                                    }
                                }

                                is LatexNode.Aligned -> {
                                    if (shouldNumberAligned(child)) {
                                        val subNumber = "$parentNumber${subCounter}"
                                        subCounter++
                                        findLabelsInAllRows(child.rows).forEach { labelKey ->
                                            labelToNumber[labelKey] = subNumber
                                        }
                                    }
                                }

                                else -> { /* 忽略非环境节点 */ }
                            }
                        }
                    }

                    // 递归进入子节点
                    is LatexNode.Group -> process(node.children)
                    is LatexNode.Document -> process(node.children)
                    is LatexNode.Delimited -> process(node.content)
                    is LatexNode.DisplayMath -> process(node.children)
                    is LatexNode.InlineMath -> process(node.children)

                    else -> {
                        val children = node.children()
                        if (children.isNotEmpty()) {
                            process(children)
                        }
                    }
                }
            }
        }

        process(children)
        return labelToNumber
    }

    /**
     * 判断 Environment 是否需要自动编号
     */
    private fun shouldNumberEnvironment(envName: String, content: List<LatexNode>): Boolean {
        if (envName.endsWith("*")) return false
        if (envName !in NUMBERED_ENVIRONMENTS) return false
        if (containsTag(content)) return false
        return true
    }

    /**
     * 判断 Aligned 节点是否需要自动编号
     */
    private fun shouldNumberAligned(node: LatexNode.Aligned): Boolean {
        val name = node.envName ?: return false
        if (name.endsWith("*")) return false
        // aligned/gathered 是内嵌环境，不独立编号
        if (name == "aligned" || name == "gathered") return false
        if (name.removeSuffix("*") !in NUMBERED_ENVIRONMENTS) return false
        if (containsTagInRows(node.rows)) return false
        return true
    }

    /**
     * 判断 Multline 节点是否需要自动编号
     */
    private fun shouldNumberMultline(node: LatexNode.Multline): Boolean {
        val name = node.envName ?: return false
        if (name.endsWith("*")) return false
        if (containsTag(node.lines)) return false
        return true
    }

    /**
     * 判断 Eqnarray 节点是否需要自动编号
     */
    private fun shouldNumberEqnarray(node: LatexNode.Eqnarray): Boolean {
        val name = node.envName ?: return false
        if (name.endsWith("*")) return false
        if (containsTagInRows(node.rows)) return false
        return true
    }

    /**
     * 判断环境名是否需要编号
     */
    internal fun isNumberedEnvName(envName: String): Boolean {
        if (envName.endsWith("*")) return false
        if (envName == "aligned" || envName == "gathered") return false
        return envName in NUMBERED_ENVIRONMENTS
    }

    /**
     * 检查节点列表中是否包含 \tag 命令
     */
    private fun containsTag(nodes: List<LatexNode>): Boolean {
        for (node in nodes) {
            if (node is LatexNode.Tag) return true
            val children = node.children()
            if (children.isNotEmpty() && containsTag(children)) return true
        }
        return false
    }

    /**
     * 检查行列结构中是否包含 \tag 命令
     */
    private fun containsTagInRows(rows: List<List<LatexNode>>): Boolean {
        return rows.any { containsTag(it) }
    }

    /**
     * 在节点列表中查找所有 \label 的 key（含递归，但不进入嵌套环境）
     */
    private fun findLabelsInNodes(nodes: List<LatexNode>): List<String> {
        val labels = mutableListOf<String>()
        for (node in nodes) {
            when (node) {
                is LatexNode.Label -> labels.add(node.key)
                is LatexNode.Group -> labels.addAll(findLabelsInNodes(node.children))
                // 不递归到嵌套环境中（嵌套环境有自己的编号）
                is LatexNode.Environment, is LatexNode.Subequations -> { /* skip */ }
                else -> {
                    val children = node.children()
                    if (children.isNotEmpty()) {
                        labels.addAll(findLabelsInNodes(children))
                    }
                }
            }
        }
        return labels
    }

    /**
     * 仅查找直接子节点中的 \label（不递归，用于 subequations 顶层）
     */
    private fun findLabelsDirectly(nodes: List<LatexNode>): List<String> {
        return nodes.filterIsInstance<LatexNode.Label>().map { it.key }
    }

    /**
     * 在行列结构中查找所有 \label 的 key
     */
    private fun findLabelsInAllRows(rows: List<List<LatexNode>>): List<String> {
        return rows.flatMap { findLabelsInNodes(it) }
    }
}
