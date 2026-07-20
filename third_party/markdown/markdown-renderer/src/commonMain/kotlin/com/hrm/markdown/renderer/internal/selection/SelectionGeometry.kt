package com.hrm.markdown.renderer.internal.selection

import com.hrm.markdown.renderer.internal.layout.inline.runPlacements
import com.hrm.markdown.renderer.internal.layout.model.LayoutInlineBlockModel
import com.hrm.markdown.renderer.internal.layout.model.LayoutTextRun
import kotlin.math.abs

/**
 * 命中到的文本 run 及 run-local 坐标（与 painter 放置坐标一致）。
 */
internal data class RunHit(
    val lineIndex: Int,
    val runIndex: Int,
    val run: LayoutTextRun,
    val runLocalX: Float,
    val runLocalY: Float,
)

/**
 * 一个被选中的 run 及其在 run 文本内的字符切片 `[startInRun, endInRun)`。
 */
internal data class RunCharSlice(
    val span: SelectionRunSpan,
    val startInRun: Int,
    val endInRun: Int,
)

/**
 * 在 block-local 坐标系（原点为 block.frame 左上）内命中一个文本 run。
 * 坐标变换与 painter 放置完全一致：`runLocalLeft = run.frame.left - block.frame.left`。
 *
 * 若 (localX, localY) 直接落在某 run 矩形内，返回该 run 的 run-local 坐标；
 * 否则吸附到纵向最近行内、横向最近的 run（保证活动端点总能产出锚点）。
 */
internal fun hitTestRunInBlock(
    block: LayoutInlineBlockModel,
    localX: Float,
    localY: Float,
): RunHit? {
    var fallback: RunHit? = null
    var fallbackDist = Float.MAX_VALUE

    for (placement in block.runPlacements()) {
        val run = placement.run
        if (run !is LayoutTextRun) continue
        val left = placement.x.toFloat()
        val top = placement.y.toFloat()
        val right = left + placement.width
        val bottom = top + placement.height

        val insideY = localY in top..bottom
        val insideX = localX in left..right
        if (insideY && insideX) {
            return RunHit(placement.lineIndex, placement.runIndex, run, localX - left, localY - top)
        }

        // Distance for snapping: prioritize vertical proximity, then horizontal.
        val dyPenalty = when {
            localY < top -> top - localY
            localY > bottom -> localY - bottom
            else -> 0f
        }
        val clampedX = localX.coerceIn(left, right)
        val dx = abs(localX - clampedX)
        val dist = dyPenalty * 1000f + dx
        if (dist < fallbackDist) {
            fallbackDist = dist
            fallback = RunHit(
                lineIndex = placement.lineIndex,
                runIndex = placement.runIndex,
                run = run,
                runLocalX = clampedX - left,
                runLocalY = (localY - top).coerceIn(0f, placement.height.toFloat()),
            )
        }
    }
    return fallback
}

/**
 * 给定 block 内字符区间 `[blockCharStart, blockCharEnd)`，计算每个被覆盖的 run
 * 及其在 run 文本内的字符切片，用于高亮。
 */
internal fun runRangeForBlock(
    entry: SelectionBlockEntry,
    blockCharStart: Int,
    blockCharEnd: Int,
): List<RunCharSlice> {
    val from = blockCharStart.coerceIn(0, entry.totalChars)
    val to = blockCharEnd.coerceIn(0, entry.totalChars)
    if (to <= from) return emptyList()

    val slices = ArrayList<RunCharSlice>()
    for (span in entry.runs) {
        val overlapStart = maxOf(from, span.charStart)
        val overlapEnd = minOf(to, span.charEnd)
        if (overlapEnd > overlapStart) {
            slices += RunCharSlice(
                span = span,
                startInRun = overlapStart - span.charStart,
                endInRun = overlapEnd - span.charStart,
            )
        }
    }
    return slices
}
