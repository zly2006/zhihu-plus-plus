package com.hrm.markdown.renderer.internal.layout.table

internal fun computeAutoTableColumnWidths(
    minContentWidths: List<Float>,
    maxContentWidths: List<Float>,
    availableWidth: Float?,
): List<Float> {
    val columnCount = maxOf(minContentWidths.size, maxContentWidths.size)
    if (columnCount == 0) return emptyList()

    val minWidths = List(columnCount) { index ->
        minContentWidths.getOrElse(index) { 0f }.coerceAtLeast(0f)
    }
    val maxWidths = List(columnCount) { index ->
        maxContentWidths.getOrElse(index) { minWidths[index] }
            .coerceAtLeast(minWidths[index])
    }
    val minTotal = minWidths.sum()
    val maxTotal = maxWidths.sum()
    val finiteAvailable = availableWidth?.takeIf { it.isFinite() && it > 0f }

    val targetWidth = when {
        finiteAvailable == null -> maxTotal
        finiteAvailable >= maxTotal -> finiteAvailable
        finiteAvailable >= minTotal -> finiteAvailable
        else -> minTotal
    }

    return when {
        targetWidth >= maxTotal -> {
            val extra = targetWidth - maxTotal
            if (extra <= 0f) {
                maxWidths
            } else {
                val extraPerColumn = extra / columnCount.toFloat()
                maxWidths.map { it + extraPerColumn }
            }
        }

        targetWidth <= minTotal -> minWidths

        else -> {
            val shrinkBudget = maxTotal - targetWidth
            val flexWidths = maxWidths.mapIndexed { index, width ->
                (width - minWidths[index]).coerceAtLeast(0f)
            }
            val totalFlex = flexWidths.sum()
            if (totalFlex <= 0f) {
                maxWidths
            } else {
                maxWidths.mapIndexed { index, width ->
                    width - shrinkBudget * (flexWidths[index] / totalFlex)
                }
            }
        }
    }
}
