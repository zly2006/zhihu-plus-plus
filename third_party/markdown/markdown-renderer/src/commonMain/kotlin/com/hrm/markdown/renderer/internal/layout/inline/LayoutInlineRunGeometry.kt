package com.hrm.markdown.renderer.internal.layout.inline

import kotlin.math.ceil
import kotlin.math.floor
import com.hrm.markdown.renderer.internal.layout.model.LayoutInlineBlockModel
import com.hrm.markdown.renderer.internal.layout.model.LayoutInlineRun

/**
 * A flattened inline run with block-local geometry.
 *
 * The renderer, selection index, and selection hit-testing all depend on the
 * same coordinate convention: x/y are relative to [LayoutInlineBlockModel.frame].
 */
internal data class LayoutInlineRunPlacement(
    val lineIndex: Int,
    val runIndex: Int,
    val run: LayoutInlineRun,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
)

internal fun LayoutInlineBlockModel.runPlacements(): List<LayoutInlineRunPlacement> {
    val placements = ArrayList<LayoutInlineRunPlacement>()
    lines.forEachIndexed { lineIndex, line ->
        line.runs.forEachIndexed { runIndex, run ->
            val localLeft = run.frame.left - frame.left
            val localTop = run.frame.top - frame.top
            val localRight = localLeft + run.frame.width
            val localBottom = localTop + run.frame.height
            val x = floor(localLeft).toInt()
            val y = floor(localTop).toInt()
            placements += LayoutInlineRunPlacement(
                lineIndex = lineIndex,
                runIndex = runIndex,
                run = run,
                x = x,
                y = y,
                width = (ceil(localRight).toInt() - x).coerceAtLeast(0),
                height = (ceil(localBottom).toInt() - y).coerceAtLeast(0),
            )
        }
    }
    return placements
}
