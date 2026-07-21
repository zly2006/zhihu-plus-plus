package com.hrm.markdown.renderer.diagram

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max

// ─── Data Model ───

internal enum class FlowDirection { TB, TD, BT, LR, RL }

internal enum class NodeShape { RECT, ROUND_RECT, STADIUM, DIAMOND, HEXAGON, PARALLELOGRAM, CIRCLE }

internal data class FlowNode(
    val id: String,
    val label: String,
    val shape: NodeShape = NodeShape.RECT,
)

internal data class FlowEdge(
    val from: String,
    val to: String,
    val label: String = "",
    val style: EdgeStyle = EdgeStyle.SOLID_ARROW,
)

internal enum class EdgeStyle {
    SOLID_ARROW,     // -->
    DOTTED_ARROW,    // -.->
    THICK_ARROW,     // ==>
    SOLID_LINE,      // ---
    DOTTED_LINE,     // -.-
    THICK_LINE,      // ===
}

internal data class FlowchartData(
    val direction: FlowDirection,
    val nodes: List<FlowNode>,
    val edges: List<FlowEdge>,
)

// ─── Layout Data ───

internal data class NodeLayout(
    val node: FlowNode,
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
) {
    val centerX get() = x + width / 2
    val centerY get() = y + height / 2
}

// ─── Parser ───

internal fun parseMermaidFlowchart(code: String): FlowchartData? {
    val lines = code.lines().map { it.trim() }.filter { it.isNotBlank() }
    if (lines.isEmpty()) return null

    val firstLine = lines.first().lowercase()
    val direction = when {
        firstLine.startsWith("flowchart") || firstLine.startsWith("graph") -> {
            val parts = firstLine.split("\\s+".toRegex())
            when (parts.getOrNull(1)?.uppercase()) {
                "LR" -> FlowDirection.LR
                "RL" -> FlowDirection.RL
                "BT" -> FlowDirection.BT
                else -> FlowDirection.TD
            }
        }
        else -> return null
    }

    val nodesMap = mutableMapOf<String, FlowNode>()
    val edges = mutableListOf<FlowEdge>()

    for (i in 1 until lines.size) {
        val line = lines[i]
        if (line.startsWith("%%") || line == "end") continue
        parseLine(line, nodesMap, edges)
    }

    return FlowchartData(
        direction = direction,
        nodes = nodesMap.values.toList(),
        edges = edges,
    )
}

// Edge patterns: -->, -.-> , ==>, ---, -.- , ===, -- text -->, -. text .->
private val EDGE_PATTERN = Regex(
    """^(\S+?)\s*(-->|-.->|==>|---|(?:-\.-)|===|--\s+.+?\s+-->|--\s+.+?\s+---|-\.\s+.+?\s+\.->)\s*(\S+.*)$"""
)

private val NODE_DEF_RECT = Regex("""^([A-Za-z_][\w]*)\[(.+?)\]$""")
private val NODE_DEF_ROUND = Regex("""^([A-Za-z_][\w]*)\((.+?)\)$""")
private val NODE_DEF_STADIUM = Regex("""^([A-Za-z_][\w]*)\(\[(.+?)\]\)$""")
private val NODE_DEF_DIAMOND = Regex("""^([A-Za-z_][\w]*)\{(.+?)\}$""")
private val NODE_DEF_HEXAGON = Regex("""^([A-Za-z_][\w]*)\{\{(.+?)\}\}$""")
private val NODE_DEF_CIRCLE = Regex("""^([A-Za-z_][\w]*)\(\((.+?)\)\)$""")

private fun parseLine(
    line: String,
    nodesMap: MutableMap<String, FlowNode>,
    edges: MutableList<FlowEdge>,
) {
    // Try to parse as edge: A --> B, A -->|text| B, A -- text --> B
    val edgeParts = splitEdge(line)
    if (edgeParts != null) {
        val (fromRaw, edgeStr, toRaw) = edgeParts
        val fromNode = parseNodeDef(fromRaw.trim())
        val toNode = parseNodeDef(toRaw.trim())

        if (fromNode.id !in nodesMap) nodesMap[fromNode.id] = fromNode
        if (toNode.id !in nodesMap) nodesMap[toNode.id] = toNode
        // If the existing one has default label (id), update with the richer definition
        if (nodesMap[fromNode.id]?.label == fromNode.id && fromNode.label != fromNode.id) {
            nodesMap[fromNode.id] = fromNode
        }
        if (nodesMap[toNode.id]?.label == toNode.id && toNode.label != toNode.id) {
            nodesMap[toNode.id] = toNode
        }

        val edgeLabel = extractEdgeLabel(edgeStr)
        val edgeStyle = parseEdgeStyle(edgeStr)
        edges.add(FlowEdge(fromNode.id, toNode.id, edgeLabel, edgeStyle))
        return
    }

    // Try as standalone node definition
    val node = parseNodeDef(line)
    if (node.label != node.id || node.shape != NodeShape.RECT) {
        if (node.id !in nodesMap) nodesMap[node.id] = node
    }
}

// Split a line into (from, edgeOperator, to)
private fun splitEdge(line: String): Triple<String, String, String>? {
    // Patterns ordered by specificity
    val edgeOps = listOf(
        "-.->", "==>", "-->", "-.-", "===", "---",
    )
    // Also handle label forms: --|text|--> , -->|text|
    val labelEdge = Regex("""^(.+?)\s*(-->|-.->|==>)\s*\|(.+?)\|\s*(.+)$""")
    labelEdge.find(line)?.let { m ->
        val from = m.groupValues[1]
        val op = m.groupValues[2]
        val label = m.groupValues[3]
        val to = m.groupValues[4]
        return Triple(from, "$op|$label|", to)
    }

    // inline label: -- text -->
    val inlineLabel = Regex("""^(.+?)\s*--\s+(.+?)\s+-->\s*(.+)$""")
    inlineLabel.find(line)?.let { m ->
        return Triple(m.groupValues[1], "-- ${m.groupValues[2]} -->", m.groupValues[3])
    }

    for (op in edgeOps) {
        val idx = line.indexOf(op)
        if (idx > 0) {
            val from = line.substring(0, idx).trim()
            val to = line.substring(idx + op.length).trim()
            if (from.isNotEmpty() && to.isNotEmpty()) {
                return Triple(from, op, to)
            }
        }
    }
    return null
}

private fun extractEdgeLabel(edgeStr: String): String {
    // -->|label|
    val pipeLabel = Regex("""\|(.+?)\|""")
    pipeLabel.find(edgeStr)?.let { return it.groupValues[1] }
    // -- label -->
    val inlineLabel = Regex("""--\s+(.+?)\s+-->""")
    inlineLabel.find(edgeStr)?.let { return it.groupValues[1] }
    return ""
}

private fun parseEdgeStyle(edgeStr: String): EdgeStyle = when {
    edgeStr.contains("-.->") -> EdgeStyle.DOTTED_ARROW
    edgeStr.contains("==>") -> EdgeStyle.THICK_ARROW
    edgeStr.contains("-->") -> EdgeStyle.SOLID_ARROW
    edgeStr.contains("-.-") -> EdgeStyle.DOTTED_LINE
    edgeStr.contains("===") -> EdgeStyle.THICK_LINE
    edgeStr.contains("---") -> EdgeStyle.SOLID_LINE
    else -> EdgeStyle.SOLID_ARROW
}

private fun parseNodeDef(raw: String): FlowNode {
    NODE_DEF_HEXAGON.find(raw)?.let { return FlowNode(it.groupValues[1], it.groupValues[2], NodeShape.HEXAGON) }
    NODE_DEF_CIRCLE.find(raw)?.let { return FlowNode(it.groupValues[1], it.groupValues[2], NodeShape.CIRCLE) }
    NODE_DEF_STADIUM.find(raw)?.let { return FlowNode(it.groupValues[1], it.groupValues[2], NodeShape.STADIUM) }
    NODE_DEF_ROUND.find(raw)?.let { return FlowNode(it.groupValues[1], it.groupValues[2], NodeShape.ROUND_RECT) }
    NODE_DEF_DIAMOND.find(raw)?.let { return FlowNode(it.groupValues[1], it.groupValues[2], NodeShape.DIAMOND) }
    NODE_DEF_RECT.find(raw)?.let { return FlowNode(it.groupValues[1], it.groupValues[2], NodeShape.RECT) }
    // Plain id
    val id = raw.split("\\s+".toRegex()).first()
    return FlowNode(id, id, NodeShape.RECT)
}

// ─── Layout Engine ───

private const val NODE_H_PAD = 28f
private const val NODE_V_PAD = 14f
private const val MIN_H_GAP = 60f
private const val MIN_V_GAP = 50f
private const val EDGE_LABEL_MARGIN = 24f  // extra margin around edge labels
private const val MIN_NODE_W = 90f
private const val MIN_NODE_H = 44f

internal fun layoutFlowchart(
    data: FlowchartData,
    measureText: (String) -> Pair<Float, Float>,
): Pair<List<NodeLayout>, Size> {
    if (data.nodes.isEmpty()) return Pair(emptyList(), Size.Zero)

    val isVertical = data.direction == FlowDirection.TD ||
            data.direction == FlowDirection.TB ||
            data.direction == FlowDirection.BT

    // Measure each node
    val nodeSizes = data.nodes.associate { node ->
        val (tw, th) = measureText(node.label)
        val w = max(tw + NODE_H_PAD * 2, MIN_NODE_W)
        val h = max(th + NODE_V_PAD * 2, MIN_NODE_H)
        val finalW = if (node.shape == NodeShape.DIAMOND) max(w, h) * 1.4f else w
        val finalH = if (node.shape == NodeShape.DIAMOND) max(w, h) * 1.0f else h
        node.id to Pair(finalW, finalH)
    }

    // Topological layering via BFS from roots
    val adjacency = mutableMapOf<String, MutableList<String>>()
    val inDegree = mutableMapOf<String, Int>()
    data.nodes.forEach { adjacency[it.id] = mutableListOf(); inDegree[it.id] = 0 }
    data.edges.forEach { e ->
        adjacency[e.from]?.add(e.to)
        inDegree[e.to] = (inDegree[e.to] ?: 0) + 1
    }

    val layers = mutableListOf<MutableList<String>>()
    val assigned = mutableSetOf<String>()
    var queue = data.nodes.filter { (inDegree[it.id] ?: 0) == 0 }.map { it.id }.toMutableList()
    if (queue.isEmpty()) queue = mutableListOf(data.nodes.first().id)

    while (queue.isNotEmpty()) {
        layers.add(queue.toMutableList())
        assigned.addAll(queue)
        val next = mutableListOf<String>()
        for (nodeId in queue) {
            for (neighbor in adjacency[nodeId].orEmpty()) {
                if (neighbor !in assigned && neighbor !in next) {
                    // Check if all predecessors are assigned
                    val allPredAssigned = data.edges
                        .filter { it.to == neighbor }
                        .all { it.from in assigned || it.from in queue }
                    if (allPredAssigned) next.add(neighbor)
                }
            }
        }
        if (next.isEmpty()) {
            // Add remaining unassigned nodes
            val remaining = data.nodes.filter { it.id !in assigned }
            if (remaining.isNotEmpty()) {
                queue = remaining.map { it.id }.toMutableList()
            } else break
        } else {
            queue = next
        }
    }

    if (data.direction == FlowDirection.BT) layers.reverse()

    // Position nodes
    val padding = 30f
    val layouts = mutableListOf<NodeLayout>()
    val nodeLayoutMap = mutableMapOf<String, NodeLayout>()

    // Build a map from nodeId to its layer index
    val nodeLayerMap = mutableMapOf<String, Int>()
    for ((layerIdx, layer) in layers.withIndex()) {
        for (nodeId in layer) {
            nodeLayerMap[nodeId] = layerIdx
        }
    }

    // Calculate dynamic inter-layer gaps based on edge label sizes
    // For vertical: gap between layer i and i+1 is based on labels of edges from layer i to i+1
    // For horizontal: same but for horizontal spacing
    val interLayerGaps = (0 until (layers.size - 1).coerceAtLeast(0)).map { i ->
        var maxLabelSize = 0f
        for (edge in data.edges) {
            val fromLayer = nodeLayerMap[edge.from] ?: continue
            val toLayer = nodeLayerMap[edge.to] ?: continue
            val lo = minOf(fromLayer, toLayer)
            val hi = maxOf(fromLayer, toLayer)
            if (lo <= i && hi > i && edge.label.isNotBlank()) {
                val (lw, lh) = measureText(edge.label)
                val spanCount = hi - lo
                if (isVertical) {
                    maxLabelSize = max(maxLabelSize, (lh + EDGE_LABEL_MARGIN) / spanCount)
                } else {
                    maxLabelSize = max(maxLabelSize, (lw + EDGE_LABEL_MARGIN) / spanCount)
                }
            }
        }
        if (isVertical) max(MIN_V_GAP, maxLabelSize) else max(MIN_H_GAP, maxLabelSize)
    }

    // Also compute dynamic in-layer gaps (for nodes within same layer in vertical mode)
    // This ensures sibling nodes don't overlap when edge labels between them are wide
    val H_GAP = MIN_H_GAP  // in-layer gap for vertical, keep as min
    val V_GAP = MIN_V_GAP  // in-layer gap for horizontal, keep as min

    if (isVertical) {
        var y = padding
        for ((layerIdx, layer) in layers.withIndex()) {
            val layerHeight = layer.maxOfOrNull { nodeSizes[it]?.second ?: MIN_NODE_H } ?: MIN_NODE_H
            val totalWidth = layer.sumOf { (nodeSizes[it]?.first ?: MIN_NODE_W).toDouble() }.toFloat() +
                    (layer.size - 1) * H_GAP
            var x = padding + (if (layer.size == 1) {
                val maxLayerWidth = layers.maxOfOrNull { l ->
                    l.sumOf { (nodeSizes[it]?.first ?: MIN_NODE_W).toDouble() }.toFloat() +
                            (l.size - 1) * H_GAP
                } ?: totalWidth
                (maxLayerWidth - totalWidth) / 2
            } else 0f)

            for (nodeId in layer) {
                val (w, h) = nodeSizes[nodeId] ?: Pair(MIN_NODE_W, MIN_NODE_H)
                val node = data.nodes.first { it.id == nodeId }
                val layout = NodeLayout(node, x, y + (layerHeight - h) / 2, w, h)
                layouts.add(layout)
                nodeLayoutMap[nodeId] = layout
                x += w + H_GAP
            }
            val gap = interLayerGaps.getOrElse(layerIdx) { MIN_V_GAP }
            y += layerHeight + gap
        }
    } else {
        // LR / RL
        var x = padding
        if (data.direction == FlowDirection.RL) layers.reverse()
        for ((layerIdx, layer) in layers.withIndex()) {
            val layerWidth = layer.maxOfOrNull { nodeSizes[it]?.first ?: MIN_NODE_W } ?: MIN_NODE_W
            val totalHeight = layer.sumOf { (nodeSizes[it]?.second ?: MIN_NODE_H).toDouble() }.toFloat() +
                    (layer.size - 1) * V_GAP
            var y = padding + (if (layer.size == 1) {
                val maxLayerHeight = layers.maxOfOrNull { l ->
                    l.sumOf { (nodeSizes[it]?.second ?: MIN_NODE_H).toDouble() }.toFloat() +
                            (l.size - 1) * V_GAP
                } ?: totalHeight
                (maxLayerHeight - totalHeight) / 2
            } else 0f)

            for (nodeId in layer) {
                val (w, h) = nodeSizes[nodeId] ?: Pair(MIN_NODE_W, MIN_NODE_H)
                val node = data.nodes.first { it.id == nodeId }
                val layout = NodeLayout(node, x + (layerWidth - w) / 2, y, w, h)
                layouts.add(layout)
                nodeLayoutMap[nodeId] = layout
                y += h + V_GAP
            }
            val gap = interLayerGaps.getOrElse(layerIdx) { MIN_H_GAP }
            x += layerWidth + gap
        }
    }

    val totalW = layouts.maxOfOrNull { it.x + it.width }?.plus(padding) ?: 100f
    val totalH = layouts.maxOfOrNull { it.y + it.height }?.plus(padding) ?: 100f

    return Pair(layouts, Size(totalW, totalH))
}

// ─── Drawing ───

private val NODE_FILL = Color(0xFFE8F4FD)
private val NODE_STROKE = Color(0xFF4A90D9)
private val EDGE_COLOR = Color(0xFF6B7280)
private val LABEL_COLOR = Color(0xFF1F2937)
private val EDGE_LABEL_COLOR = Color(0xFF6B7280)

internal fun DrawScope.drawFlowchart(
    data: FlowchartData,
    layouts: List<NodeLayout>,
    textMeasurer: TextMeasurer,
) {
    val layoutMap = layouts.associateBy { it.node.id }

    // Draw edges first (behind nodes)
    for (edge in data.edges) {
        val from = layoutMap[edge.from] ?: continue
        val to = layoutMap[edge.to] ?: continue
        drawFlowEdge(from, to, edge, data.direction, textMeasurer)
    }

    // Draw nodes
    for (layout in layouts) {
        drawFlowNode(layout, textMeasurer)
    }
}

private fun DrawScope.drawFlowNode(layout: NodeLayout, textMeasurer: TextMeasurer) {
    val x = layout.x
    val y = layout.y
    val w = layout.width
    val h = layout.height

    when (layout.node.shape) {
        NodeShape.RECT -> {
            drawRoundRect(
                color = NODE_FILL,
                topLeft = Offset(x, y),
                size = Size(w, h),
                cornerRadius = CornerRadius(4f),
            )
            drawRoundRect(
                color = NODE_STROKE,
                topLeft = Offset(x, y),
                size = Size(w, h),
                cornerRadius = CornerRadius(4f),
                style = Stroke(width = 2f),
            )
        }
        NodeShape.ROUND_RECT, NodeShape.STADIUM -> {
            val cr = if (layout.node.shape == NodeShape.STADIUM) h / 2 else 12f
            drawRoundRect(
                color = NODE_FILL,
                topLeft = Offset(x, y),
                size = Size(w, h),
                cornerRadius = CornerRadius(cr),
            )
            drawRoundRect(
                color = NODE_STROKE,
                topLeft = Offset(x, y),
                size = Size(w, h),
                cornerRadius = CornerRadius(cr),
                style = Stroke(width = 2f),
            )
        }
        NodeShape.DIAMOND -> {
            val cx = layout.centerX
            val cy = layout.centerY
            val path = Path().apply {
                moveTo(cx, y)
                lineTo(x + w, cy)
                lineTo(cx, y + h)
                lineTo(x, cy)
                close()
            }
            drawPath(path, NODE_FILL)
            drawPath(path, NODE_STROKE, style = Stroke(width = 2f))
        }
        NodeShape.HEXAGON -> {
            val cx = layout.centerX
            val indent = w * 0.15f
            val path = Path().apply {
                moveTo(x + indent, y)
                lineTo(x + w - indent, y)
                lineTo(x + w, y + h / 2)
                lineTo(x + w - indent, y + h)
                lineTo(x + indent, y + h)
                lineTo(x, y + h / 2)
                close()
            }
            drawPath(path, NODE_FILL)
            drawPath(path, NODE_STROKE, style = Stroke(width = 2f))
        }
        NodeShape.CIRCLE -> {
            val r = max(w, h) / 2
            drawCircle(NODE_FILL, radius = r, center = Offset(layout.centerX, layout.centerY))
            drawCircle(NODE_STROKE, radius = r, center = Offset(layout.centerX, layout.centerY), style = Stroke(width = 2f))
        }
        NodeShape.PARALLELOGRAM -> {
            val skew = w * 0.15f
            val path = Path().apply {
                moveTo(x + skew, y)
                lineTo(x + w, y)
                lineTo(x + w - skew, y + h)
                lineTo(x, y + h)
                close()
            }
            drawPath(path, NODE_FILL)
            drawPath(path, NODE_STROKE, style = Stroke(width = 2f))
        }
    }

    // Draw label
    val textResult = textMeasurer.measure(
        layout.node.label,
        style = TextStyle(fontSize = 13.sp, color = LABEL_COLOR),
    )
    drawText(
        textResult,
        topLeft = Offset(
            layout.centerX - textResult.size.width / 2f,
            layout.centerY - textResult.size.height / 2f,
        ),
    )
}

private fun DrawScope.drawFlowEdge(
    from: NodeLayout,
    to: NodeLayout,
    edge: FlowEdge,
    direction: FlowDirection,
    textMeasurer: TextMeasurer,
) {
    val isVertical = direction == FlowDirection.TD ||
            direction == FlowDirection.TB ||
            direction == FlowDirection.BT

    val (startX, startY) = if (isVertical) {
        Pair(from.centerX, from.y + from.height)
    } else {
        Pair(from.x + from.width, from.centerY)
    }

    val (endX, endY) = if (isVertical) {
        Pair(to.centerX, to.y)
    } else {
        Pair(to.x, to.centerY)
    }

    val strokeWidth = when (edge.style) {
        EdgeStyle.THICK_ARROW, EdgeStyle.THICK_LINE -> 3f
        else -> 1.5f
    }

    val isDashed = edge.style == EdgeStyle.DOTTED_ARROW || edge.style == EdgeStyle.DOTTED_LINE
    val hasArrow = edge.style == EdgeStyle.SOLID_ARROW ||
            edge.style == EdgeStyle.DOTTED_ARROW ||
            edge.style == EdgeStyle.THICK_ARROW

    if (isDashed) {
        // Draw dashed line
        val dx = endX - startX
        val dy = endY - startY
        val length = kotlin.math.sqrt(dx * dx + dy * dy)
        val dashLen = 6f
        val gapLen = 4f
        val ux = dx / length
        val uy = dy / length
        var t = 0f
        while (t < length) {
            val segEnd = (t + dashLen).coerceAtMost(length)
            drawLine(
                EDGE_COLOR,
                start = Offset(startX + ux * t, startY + uy * t),
                end = Offset(startX + ux * segEnd, startY + uy * segEnd),
                strokeWidth = strokeWidth,
            )
            t = segEnd + gapLen
        }
    } else {
        drawLine(EDGE_COLOR, Offset(startX, startY), Offset(endX, endY), strokeWidth = strokeWidth)
    }

    // Arrow head
    if (hasArrow) {
        val arrowSize = 8f
        val dx = endX - startX
        val dy = endY - startY
        val length = kotlin.math.sqrt(dx * dx + dy * dy)
        if (length > 0) {
            val ux = dx / length
            val uy = dy / length
            val path = Path().apply {
                moveTo(endX, endY)
                moveTo(endX - ux * arrowSize - uy * arrowSize * 0.5f,
                    endY - uy * arrowSize + ux * arrowSize * 0.5f)
                lineTo(endX, endY)
                lineTo(endX - ux * arrowSize + uy * arrowSize * 0.5f,
                    endY - uy * arrowSize - ux * arrowSize * 0.5f)
                close()
            }
            drawPath(path, EDGE_COLOR, style = Fill)
        }
    }

    // Edge label
    if (edge.label.isNotBlank()) {
        val midX = (startX + endX) / 2
        val midY = (startY + endY) / 2
        val textResult = textMeasurer.measure(
            edge.label,
            style = TextStyle(fontSize = 11.sp, color = EDGE_LABEL_COLOR),
        )
        val bgPad = 3f
        drawRoundRect(
            Color.White,
            topLeft = Offset(midX - textResult.size.width / 2f - bgPad, midY - textResult.size.height / 2f - bgPad),
            size = Size(textResult.size.width + bgPad * 2f, textResult.size.height + bgPad * 2f),
            cornerRadius = CornerRadius(3f),
        )
        drawText(
            textResult,
            topLeft = Offset(midX - textResult.size.width / 2f, midY - textResult.size.height / 2f),
        )
    }
}

// ─── Composable ───

@Composable
internal fun MermaidFlowchartDiagram(
    code: String,
    modifier: Modifier = Modifier,
) {
    val data = remember(code) { parseMermaidFlowchart(code) }
    if (data == null || data.nodes.isEmpty()) {
        DiagramFallback(code, "Mermaid", modifier)
        return
    }
    FlowchartDiagramRenderer(data, modifier)
}

// shared renderer used by both mermaid flowchart and graphviz
@Composable
internal fun FlowchartDiagramRenderer(
    data: FlowchartData,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    val (layouts, canvasSize) = remember(data) {
        layoutFlowchart(data) { text ->
            val result = textMeasurer.measure(
                text,
                style = TextStyle(fontSize = 13.sp),
            )
            Pair(result.size.width.toFloat(), result.size.height.toFloat())
        }
    }

    val canvasWidthDp = with(density) { canvasSize.width.toDp() }
    val canvasHeightDp = with(density) { canvasSize.height.toDp() }

    Box(
        modifier = modifier
            .horizontalScroll(rememberScrollState()),
    ) {
        Canvas(
            modifier = Modifier
                .width(canvasWidthDp + 8.dp)
                .height(canvasHeightDp + 8.dp)
                .padding(4.dp),
        ) {
            drawFlowchart(data, layouts, textMeasurer)
        }
    }
}
