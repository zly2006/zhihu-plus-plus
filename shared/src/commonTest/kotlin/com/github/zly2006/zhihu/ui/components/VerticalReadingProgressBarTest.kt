package com.github.zly2006.zhihu.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals

class VerticalReadingProgressBarTest {
    @Test
    fun lazy_progress_includes_offset_inside_first_visible_item() {
        assertEquals(
            expected = 0.25f,
            actual = calculateLazyReadingProgress(
                firstVisibleItemIndex = 2,
                firstVisibleItemScrollOffset = 50,
                firstVisibleItemSize = 100,
                totalItemsCount = 11,
            ),
        )
    }

    @Test
    fun lazy_progress_is_clamped_to_document_bounds() {
        assertEquals(0f, calculateLazyReadingProgress(0, 0, 0, 1))
        assertEquals(1f, calculateLazyReadingProgress(20, 0, 100, 10))
    }
}
