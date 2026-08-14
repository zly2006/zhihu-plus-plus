package com.hrm.markdown.parser.block.postprocessors

import com.hrm.markdown.parser.ast.*

/**
 * 后处理：将 info string 为 mermaid/plantuml 等的 FencedCodeBlock 转换为 DiagramBlock。
 */
class DiagramProcessor : PostProcessor {
    override val priority: Int = 400

    override fun process(document: Document) {
        processRecursive(document)
    }

    private fun processRecursive(node: Node) {
        if (node is ContainerNode) {
            val children = node.children.toList()
            for (child in children) {
                if (child is FencedCodeBlock && child.language.lowercase() in DIAGRAM_LANGUAGES) {
                    val diagram = DiagramBlock(
                        diagramType = child.language.lowercase(),
                        literal = child.literal,
                    )
                    diagram.lineRange = child.lineRange
                    diagram.sourceRange = child.sourceRange
                    diagram.contentHash = child.contentHash
                    node.replaceChild(child, diagram)
                } else {
                    processRecursive(child)
                }
            }
        }
    }

    companion object {
        private val DIAGRAM_LANGUAGES = setOf(
            "mermaid", "plantuml", "dot", "graphviz", "ditaa",
            "flowchart", "sequence", "gantt", "pie", "mindmap",
        )
    }
}
