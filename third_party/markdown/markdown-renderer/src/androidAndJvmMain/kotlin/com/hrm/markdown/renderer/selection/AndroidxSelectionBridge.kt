@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.hrm.markdown.renderer.selection

import androidx.compose.foundation.text.selection.SelectionLayout

internal fun selectionLayoutShouldRecompute(
    selectionLayout: SelectionLayout,
    previousSelectionLayout: SelectionLayout?,
): Boolean = selectionLayout.shouldRecomputeSelection(previousSelectionLayout)
