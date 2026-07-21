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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.max

// ─── Data Model ───

internal enum class ParticipantType { DEFAULT, ACTOR }

internal data class Participant(
    val id: String,
    val label: String,
    val type: ParticipantType = ParticipantType.DEFAULT,
)

internal enum class ArrowStyle {
    SOLID,       // ->
    DOTTED,      // -->
    SOLID_OPEN,  // ->>
    DOTTED_OPEN, // -->>
}

internal data class SeqMessage(
    val from: String,
    val to: String,
    val label: String,
    val arrowStyle: ArrowStyle = ArrowStyle.SOLID,
)

internal data class SequenceDiagramData(
    val participants: List<Participant>,
    val messages: List<SeqMessage>,
)

// ─── Parser ───

internal fun parsePlantUMLSequence(code: String): SequenceDiagramData? {
    val lines = code.lines().map { it.trim() }.filter { it.isNotBlank() }

    val participantsMap = mutableLinkedMapOf<String, Participant>()
    val messages = mutableListOf<SeqMessage>()

    for (line in lines) {
        if (line.startsWith("@start") || line.startsWith("@end") || line == "end") continue

        // participant / actor declarations
        val actorMatch = Regex("""^actor\s+(\S+)(?:\s+as\s+(.+))?$""").find(line)
        if (actorMatch != null) {
            val id = actorMatch.groupValues[1]
            val label = actorMatch.groupValues[2].ifBlank { id }
            participantsMap[id] = Participant(id, label, ParticipantType.ACTOR)
            continue
        }

        val participantMatch = Regex("""^participant\s+"?([^"]+)"?\s+as\s+(\S+)$""").find(line)
            ?: Regex("""^participant\s+(\S+)$""").find(line)
        if (participantMatch != null) {
            val groups = participantMatch.groupValues
            if (groups.size >= 3 && groups[2].isNotBlank()) {
                participantsMap[groups[2]] = Participant(groups[2], groups[1])
            } else {
                val id = groups[1]
                participantsMap[id] = Participant(id, id)
            }
            continue
        }

        // Messages: A -> B : label
        val msgMatch = Regex("""^(.+?)\s*(-->>|->(?:>)?|-->|<--(?:<)?|<-(?:<)?)\s*(.+?)\s*:\s*(.*)$""").find(line)
        if (msgMatch != null) {
            val rawFrom = msgMatch.groupValues[1].trim()
            val arrow = msgMatch.groupValues[2].trim()
            val rawTo = msgMatch.groupValues[3].trim()
            val label = msgMatch.groupValues[4].trim()

            val arrowStyle = when (arrow) {
                "-->", "-->>" -> ArrowStyle.DOTTED
                "->>" -> ArrowStyle.SOLID_OPEN
                "-->" -> ArrowStyle.DOTTED
                else -> ArrowStyle.SOLID
            }

            // Ensure participants exist
            if (rawFrom !in participantsMap) {
                participantsMap[rawFrom] = Participant(rawFrom, rawFrom)
            }
            if (rawTo !in participantsMap) {
                participantsMap[rawTo] = Participant(rawTo, rawTo)
            }

            messages.add(SeqMessage(rawFrom, rawTo, label, arrowStyle))
            continue
        }
    }

    if (participantsMap.isEmpty() && messages.isEmpty()) return null

    return SequenceDiagramData(
        participants = participantsMap.values.toList(),
        messages = messages,
    )
}

private fun <K, V> mutableLinkedMapOf(): LinkedHashMap<K, V> = LinkedHashMap()

// ─── Layout & Drawing ───

private const val PARTICIPANT_PAD_H = 20f
private const val PARTICIPANT_PAD_V = 10f
private const val MIN_PARTICIPANT_GAP = 80f
private const val MESSAGE_LABEL_MARGIN = 24f  // extra margin around message labels
private const val MESSAGE_ROW_H = 56f
private const val TOP_MARGIN = 16f
private const val LIFELINE_DASH = 6f

private val PARTICIPANT_FILL = Color(0xFFE8F4FD)
private val PARTICIPANT_STROKE = Color(0xFF4A90D9)
private val ACTOR_COLOR = Color(0xFF4A90D9)
private val MSG_COLOR = Color(0xFF374151)
private val MSG_LABEL_COLOR = Color(0xFF1F2937)
private val LIFELINE_COLOR = Color(0xFFD1D5DB)

/**
 * Calculate dynamic gaps between adjacent participants based on message label widths.
 * Returns an array of gaps where gaps[i] is the center-to-center distance between participant i and i+1.
 */
private fun calculateParticipantGaps(
    data: SequenceDiagramData,
    partWidths: List<Float>,
    measureText: (String) -> Pair<Float, Float>,
): List<Float> {
    val n = data.participants.size
    if (n <= 1) return emptyList()

    val partIdToIndex = data.participants.withIndex().associate { (idx, p) -> p.id to idx }

    // For each pair of adjacent participants, find the max message label width spanning that gap
    // A message from index i to index j spans gaps [min(i,j), min(i,j)+1, ..., max(i,j)-1]
    // We need to ensure the total distance covers the label
    // For simplicity, distribute label width requirement to each spanned gap

    // First, calculate the minimum gap based on participant box widths
    val minGaps = (0 until n - 1).map { i ->
        max(partWidths[i] / 2 + partWidths[i + 1] / 2 + 20f, MIN_PARTICIPANT_GAP)
    }.toMutableList()

    // For messages between directly adjacent participants, ensure gap accommodates label
    for (msg in data.messages) {
        val fromIdx = partIdToIndex[msg.from] ?: continue
        val toIdx = partIdToIndex[msg.to] ?: continue
        if (fromIdx == toIdx) continue

        val lo = minOf(fromIdx, toIdx)
        val hi = maxOf(fromIdx, toIdx)
        val spanCount = hi - lo

        if (msg.label.isNotBlank()) {
            val (labelW, _) = measureText(msg.label)
            val requiredPerGap = (labelW + MESSAGE_LABEL_MARGIN) / spanCount
            for (g in lo until hi) {
                minGaps[g] = max(minGaps[g], requiredPerGap)
            }
        }
    }

    return minGaps
}

/**
 * Calculate participant center X positions based on dynamic gaps.
 */
private fun calculatePartCenterXs(
    partWidths: List<Float>,
    gaps: List<Float>,
): List<Float> {
    val xs = mutableListOf<Float>()
    var x = 30f + partWidths[0] / 2
    xs.add(x)
    for (i in gaps.indices) {
        x += gaps[i]
        xs.add(x)
    }
    return xs
}

internal fun DrawScope.drawSequenceDiagram(
    data: SequenceDiagramData,
    textMeasurer: TextMeasurer,
) {
    if (data.participants.isEmpty()) return

    val measureText: (String) -> Pair<Float, Float> = { text ->
        val result = textMeasurer.measure(text, style = TextStyle(fontSize = 13.sp))
        Pair(result.size.width.toFloat(), result.size.height.toFloat())
    }

    // Measure participant labels
    val partMeasures = data.participants.map { p ->
        val result = textMeasurer.measure(
            p.label,
            style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium),
        )
        Triple(p, result.size.width.toFloat(), result.size.height.toFloat())
    }

    val partWidths = partMeasures.map { max(it.second + PARTICIPANT_PAD_H * 2, 80f) }
    val partHeight = partMeasures.maxOfOrNull { it.third + PARTICIPANT_PAD_V * 2 } ?: 36f

    // Calculate dynamic gaps and positions
    val gaps = calculateParticipantGaps(data, partWidths, measureText)
    val partCenterXs = calculatePartCenterXs(partWidths, gaps)

    val boxY = TOP_MARGIN
    val lifelineStartY = boxY + partHeight
    val messagesStartY = lifelineStartY + 20f
    val totalMessagesH = data.messages.size * MESSAGE_ROW_H
    val lifelineEndY = messagesStartY + totalMessagesH + 20f

    // Draw lifelines
    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(LIFELINE_DASH, LIFELINE_DASH))
    for (i in data.participants.indices) {
        drawLine(
            LIFELINE_COLOR,
            start = Offset(partCenterXs[i], lifelineStartY),
            end = Offset(partCenterXs[i], lifelineEndY),
            strokeWidth = 1.5f,
            pathEffect = dashEffect,
        )
    }

    // Draw participant boxes (top)
    val partIdToIndex = data.participants.withIndex().associate { (idx, p) -> p.id to idx }
    for (i in data.participants.indices) {
        val p = data.participants[i]
        val cx = partCenterXs[i]
        val w = partWidths[i]

        if (p.type == ParticipantType.ACTOR) {
            drawActor(cx, boxY, partHeight, textMeasurer, p.label)
        } else {
            val bx = cx - w / 2
            drawRoundRect(
                PARTICIPANT_FILL,
                topLeft = Offset(bx, boxY),
                size = Size(w, partHeight),
                cornerRadius = CornerRadius(6f),
            )
            drawRoundRect(
                PARTICIPANT_STROKE,
                topLeft = Offset(bx, boxY),
                size = Size(w, partHeight),
                cornerRadius = CornerRadius(6f),
                style = Stroke(width = 2f),
            )

            val labelResult = textMeasurer.measure(
                p.label,
                style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1F2937)),
            )
            drawText(
                labelResult,
                topLeft = Offset(cx - labelResult.size.width / 2f, boxY + (partHeight - labelResult.size.height) / 2f),
            )
        }
    }

    // Draw bottom participant boxes
    for (i in data.participants.indices) {
        val p = data.participants[i]
        val cx = partCenterXs[i]
        val w = partWidths[i]

        if (p.type == ParticipantType.ACTOR) {
            drawActor(cx, lifelineEndY, partHeight, textMeasurer, p.label)
        } else {
            val bx = cx - w / 2
            drawRoundRect(
                PARTICIPANT_FILL,
                topLeft = Offset(bx, lifelineEndY),
                size = Size(w, partHeight),
                cornerRadius = CornerRadius(6f),
            )
            drawRoundRect(
                PARTICIPANT_STROKE,
                topLeft = Offset(bx, lifelineEndY),
                size = Size(w, partHeight),
                cornerRadius = CornerRadius(6f),
                style = Stroke(width = 2f),
            )

            val labelResult = textMeasurer.measure(
                p.label,
                style = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1F2937)),
            )
            drawText(
                labelResult,
                topLeft = Offset(cx - labelResult.size.width / 2f, lifelineEndY + (partHeight - labelResult.size.height) / 2f),
            )
        }
    }

    // Draw messages
    for ((idx, msg) in data.messages.withIndex()) {
        val fromIdx = partIdToIndex[msg.from] ?: continue
        val toIdx = partIdToIndex[msg.to] ?: continue
        val y = messagesStartY + idx * MESSAGE_ROW_H + MESSAGE_ROW_H / 2

        val fromX = partCenterXs[fromIdx]
        val toX = partCenterXs[toIdx]
        val isLeftToRight = toX >= fromX

        // Draw arrow line
        val isDashed = msg.arrowStyle == ArrowStyle.DOTTED || msg.arrowStyle == ArrowStyle.DOTTED_OPEN
        if (isDashed) {
            drawLine(
                MSG_COLOR,
                start = Offset(fromX, y),
                end = Offset(toX, y),
                strokeWidth = 1.5f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 4f)),
            )
        } else {
            drawLine(MSG_COLOR, Offset(fromX, y), Offset(toX, y), strokeWidth = 1.5f)
        }

        // Arrow head
        val arrowSize = 8f
        val tipX = toX
        val direction = if (isLeftToRight) -1f else 1f
        val arrowPath = Path().apply {
            moveTo(tipX + direction * arrowSize, y - arrowSize * 0.5f)
            lineTo(tipX, y)
            lineTo(tipX + direction * arrowSize, y + arrowSize * 0.5f)
            close()
        }
        drawPath(arrowPath, MSG_COLOR, style = Fill)

        // Message label
        if (msg.label.isNotBlank()) {
            val labelResult = textMeasurer.measure(
                msg.label,
                style = TextStyle(fontSize = 12.sp, color = MSG_LABEL_COLOR),
            )
            val midX = (fromX + toX) / 2
            drawText(
                labelResult,
                topLeft = Offset(midX - labelResult.size.width / 2f, y - labelResult.size.height - 4f),
            )
        }
    }
}

private fun DrawScope.drawActor(
    cx: Float,
    topY: Float,
    totalHeight: Float,
    textMeasurer: TextMeasurer,
    label: String,
) {
    // Stick figure actor
    val headR = 10f
    val bodyTop = topY + headR * 2 + 2f
    val bodyBottom = bodyTop + 14f
    val labelOffset = bodyBottom + 12f

    // Head
    drawCircle(ACTOR_COLOR, radius = headR, center = Offset(cx, topY + headR), style = Stroke(width = 2f))
    // Body
    drawLine(ACTOR_COLOR, Offset(cx, topY + headR * 2), Offset(cx, bodyBottom), strokeWidth = 2f)
    // Arms
    drawLine(ACTOR_COLOR, Offset(cx - 14f, bodyTop + 2f), Offset(cx + 14f, bodyTop + 2f), strokeWidth = 2f)
    // Legs
    drawLine(ACTOR_COLOR, Offset(cx, bodyBottom), Offset(cx - 10f, bodyBottom + 12f), strokeWidth = 2f)
    drawLine(ACTOR_COLOR, Offset(cx, bodyBottom), Offset(cx + 10f, bodyBottom + 12f), strokeWidth = 2f)

    // Label
    val labelResult = textMeasurer.measure(
        label,
        style = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1F2937)),
    )
    drawText(
        labelResult,
        topLeft = Offset(cx - labelResult.size.width / 2f, labelOffset),
    )
}

internal fun calculateSequenceDiagramSize(
    data: SequenceDiagramData,
    measureText: (String) -> Pair<Float, Float>,
): Size {
    if (data.participants.isEmpty()) return Size(200f, 100f)

    val partWidths = data.participants.map { p ->
        val (tw, _) = measureText(p.label)
        max(tw + PARTICIPANT_PAD_H * 2, 80f)
    }

    val partHeight = data.participants.maxOfOrNull { p ->
        val (_, th) = measureText(p.label)
        th + PARTICIPANT_PAD_V * 2
    } ?: 36f

    // Use dynamic gaps based on message label widths
    val gaps = calculateParticipantGaps(data, partWidths, measureText)
    val totalGaps = gaps.sumOf { it.toDouble() }.toFloat()

    val totalW = 30f + partWidths[0] / 2 + totalGaps + partWidths.last() / 2 + 30f
    val messagesH = data.messages.size * MESSAGE_ROW_H
    val totalH = TOP_MARGIN + partHeight + 20f + messagesH + 20f + partHeight + TOP_MARGIN

    return Size(totalW, totalH)
}

// ─── Composable ───

@Composable
internal fun PlantUMLSequenceDiagram(
    code: String,
    modifier: Modifier = Modifier,
) {
    val data = remember(code) { parsePlantUMLSequence(code) }
    if (data == null || data.participants.isEmpty()) {
        DiagramFallback(code, "PlantUML", modifier)
        return
    }
    SequenceDiagramRenderer(data, modifier)
}

// shared renderer used by both plantuml and mermaid sequence diagrams
@Composable
internal fun SequenceDiagramRenderer(
    data: SequenceDiagramData,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    val canvasSize = remember(data) {
        calculateSequenceDiagramSize(data) { text ->
            val result = textMeasurer.measure(text, style = TextStyle(fontSize = 13.sp))
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
            drawSequenceDiagram(data, textMeasurer)
        }
    }
}
