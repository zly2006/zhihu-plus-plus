package com.hrm.markdown.renderer.block

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.hrm.markdown.parser.ast.DiagramBlock
import com.hrm.markdown.renderer.LocalMarkdownTheme
import com.hrm.markdown.renderer.diagram.DiagramFallback
import com.hrm.markdown.renderer.diagram.GraphvizDiagram
import com.hrm.markdown.renderer.diagram.MermaidFlowchartDiagram
import com.hrm.markdown.renderer.diagram.MermaidSequenceDiagram
import com.hrm.markdown.renderer.diagram.PlantUMLSequenceDiagram

/**
 * 图表块渲染器（Mermaid / PlantUML 等）。
 *
 * 根据图表类型分发到对应的渲染引擎：
 * - Mermaid flowchart/graph → Canvas 绘制流程图
 * - PlantUML sequence → Canvas 绘制时序图
 * - 其他未支持类型 → 代码展示 fallback
 */
@Composable
internal fun DiagramBlockRenderer(
    node: DiagramBlock,
    modifier: Modifier = Modifier,
) {
    val theme = LocalMarkdownTheme.current
    val code = node.literal.trimEnd('\n')
    val diagramType = node.diagramType.lowercase()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(theme.codeBlockCornerRadius))
            .background(Color(0xFFF8FAFC))
            .padding(theme.codeBlockPadding),
    ) {
        when {
            diagramType == "mermaid" -> {
                // 判断是 flowchart / graph / sequence 等
                val firstLine = code.lines().firstOrNull()?.trim()?.lowercase() ?: ""
                when {
                    firstLine.startsWith("flowchart") || firstLine.startsWith("graph") -> {
                        MermaidFlowchartDiagram(code)
                    }
                    firstLine.startsWith("sequencediagram") || firstLine.startsWith("sequence") -> {
                        MermaidSequenceDiagram(code)
                    }
                    else -> {
                        // 尝试解析为 flowchart，失败则 fallback
                        MermaidFlowchartDiagram(code)
                    }
                }
            }
            diagramType == "plantuml" -> {
                PlantUMLSequenceDiagram(code)
            }
            diagramType in setOf("dot", "graphviz") -> {
                GraphvizDiagram(code)
            }
            else -> {
                val typeName = node.diagramType.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase() else it.toString()
                }
                DiagramFallback(code, typeName)
            }
        }
    }
}
