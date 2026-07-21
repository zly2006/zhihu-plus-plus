package com.hrm.markdown.renderer.diagram

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier

// parses basic DOT graph syntax into FlowchartData for reuse with flowchart renderer
internal fun parseDotGraph(code: String): FlowchartData? {
    val lines = code.lines().map { it.trim() }.filter { it.isNotBlank() }

    val nodesMap = mutableMapOf<String, FlowNode>()
    val edges = mutableListOf<FlowEdge>()

    // detect direction from rankdir attribute
    var direction = FlowDirection.TB

    for (line in lines) {
        // skip graph/digraph header and closing brace
        if (line.startsWith("graph") || line.startsWith("digraph") || line.startsWith("strict") || line == "}" || line == "{") continue

        // rankdir=LR
        val rankdirMatch = RANKDIR_RE.find(line)
        if (rankdirMatch != null) {
            direction = when (rankdirMatch.groupValues[1].uppercase()) {
                "LR" -> FlowDirection.LR
                "RL" -> FlowDirection.RL
                "BT" -> FlowDirection.BT
                else -> FlowDirection.TB
            }
            continue
        }

        // edge: A -> B [label="..."]
        val edgeMatch = EDGE_RE.find(line)
        if (edgeMatch != null) {
            val from = edgeMatch.groupValues[1].trim().trimQuotes()
            val to = edgeMatch.groupValues[2].trim().trimQuotes()
            val attrs = edgeMatch.groupValues[3]
            val label = extractDotAttr(attrs, "label")

            ensureNode(nodesMap, from)
            ensureNode(nodesMap, to)

            val style = if (edgeMatch.groupValues[0].contains("--")) EdgeStyle.SOLID_LINE else EdgeStyle.SOLID_ARROW
            edges.add(FlowEdge(from, to, label, style))
            continue
        }

        // node declaration: A [label="...", shape=...]
        val nodeMatch = NODE_RE.find(line)
        if (nodeMatch != null) {
            val id = nodeMatch.groupValues[1].trim().trimQuotes()
            val attrs = nodeMatch.groupValues[2]
            val label = extractDotAttr(attrs, "label").ifEmpty { id }
            val shape = when (extractDotAttr(attrs, "shape").lowercase()) {
                "box", "rect", "rectangle" -> NodeShape.RECT
                "ellipse", "oval", "circle" -> NodeShape.CIRCLE
                "diamond" -> NodeShape.DIAMOND
                "hexagon" -> NodeShape.HEXAGON
                else -> NodeShape.ROUND_RECT
            }
            nodesMap[id] = FlowNode(id, label, shape)
            continue
        }

        // bare node id (no attrs, not a keyword)
        val bareMatch = BARE_NODE_RE.find(line)
        if (bareMatch != null) {
            val id = bareMatch.groupValues[1].trimQuotes()
            if (id !in DOT_KEYWORDS) {
                ensureNode(nodesMap, id)
            }
        }
    }

    if (nodesMap.isEmpty()) return null
    return FlowchartData(direction, nodesMap.values.toList(), edges)
}

private fun ensureNode(map: MutableMap<String, FlowNode>, id: String) {
    if (id !in map) {
        map[id] = FlowNode(id, id, NodeShape.ROUND_RECT)
    }
}

private fun extractDotAttr(attrs: String, key: String): String {
    val match = Regex("""$key\s*=\s*"([^"]*)"""").find(attrs)
        ?: Regex("""$key\s*=\s*(\S+)""").find(attrs)
    return match?.groupValues?.get(1) ?: ""
}

private fun String.trimQuotes(): String = this.removeSurrounding("\"")

private val EDGE_RE = Regex("""^"?(\w+)"?\s*(-[->]+)\s*"?(\w+)"?\s*(\[.*])?""")
private val NODE_RE = Regex("""^"?(\w+)"?\s*\[(.+)]""")
private val BARE_NODE_RE = Regex("""^"?(\w+)"?\s*;?\s*$""")
private val RANKDIR_RE = Regex("""rankdir\s*=\s*"?(\w+)"?""", RegexOption.IGNORE_CASE)
private val DOT_KEYWORDS = setOf("node", "edge", "graph", "digraph", "subgraph", "strict")

// reuses mermaid flowchart rendering engine for DOT graphs
@Composable
internal fun GraphvizDiagram(
    code: String,
    modifier: Modifier = Modifier,
) {
    val data = remember(code) { parseDotGraph(code) }
    if (data == null || data.nodes.isEmpty()) {
        DiagramFallback(code, "Graphviz", modifier)
        return
    }
    // reuse flowchart renderer via FlowchartDiagramRenderer
    FlowchartDiagramRenderer(data, modifier)
}
