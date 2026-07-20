package com.hrm.markdown.parser.block.postprocessors

import com.hrm.markdown.parser.ast.*

/**
 * Figure 后处理器：将独立段落中的单个图片转换为 [Figure] 节点。
 *
 * Pandoc implicit_figures 语义：当一个段落仅包含一个 [Image] 节点
 * （忽略前后空白文本和软换行）时，将该段落替换为 Figure 块。
 * 图片的 alt 文本作为 figcaption 标题。
 *
 * 优先级 450：在图表处理（300）和 HTML 过滤（400）之后运行，
 * 确保不会干扰其他后处理器。
 */
class FigureProcessor : PostProcessor {
    override val priority: Int = 450

    override fun process(document: Document) {
        processContainer(document)
    }

    private fun processContainer(container: ContainerNode) {
        val children = container.children.toList()
        for (child in children) {
            if (child is Paragraph) {
                val figure = tryConvertToFigure(child)
                if (figure != null) {
                    // 复制源位置信息
                    figure.sourceRange = child.sourceRange
                    figure.lineRange = child.lineRange
                    container.replaceChild(child, figure)
                }
            } else if (child is ContainerNode) {
                processContainer(child)
            }
        }
    }

    /**
     * 检查段落是否仅包含单个图片节点，如果是则创建 Figure。
     * 忽略前后的纯空白 Text 和 SoftLineBreak 节点。
     */
    private fun tryConvertToFigure(paragraph: Paragraph): Figure? {
        val significantChildren = paragraph.children.filter { node ->
            when (node) {
                is SoftLineBreak -> false
                is Text -> node.literal.isNotBlank()
                else -> true
            }
        }

        // 必须恰好有一个有意义的子节点，且为 Image
        if (significantChildren.size != 1) return null
        val image = significantChildren.first() as? Image ?: return null

        // 提取 alt 文本作为 caption
        val altText = image.children.filterIsInstance<Text>()
            .joinToString("") { it.literal }

        return Figure(
            imageUrl = image.destination,
            caption = image.title ?: altText,
            imageWidth = image.imageWidth,
            imageHeight = image.imageHeight,
            attributes = image.attributes,
        )
    }
}
