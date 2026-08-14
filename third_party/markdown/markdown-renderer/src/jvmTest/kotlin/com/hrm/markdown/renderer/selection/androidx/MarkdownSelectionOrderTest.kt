package com.hrm.markdown.renderer.selection.androidx

import kotlin.test.Test
import kotlin.test.assertEquals

class MarkdownSelectionOrderTest {
    @Test
    fun sortsByDocumentOrderInsteadOfRegistrationOrder() {
        val reverseVisitedOrders = (199 downTo 0).map { listOf(it, 0) }

        assertEquals(
            (0..199).map { listOf(it, 0) },
            reverseVisitedOrders.sortedWith(::compareMarkdownDocumentOrder),
        )
    }

    @Test
    fun nestedGroupStaysBetweenParentSelectables() {
        val parentBeforeGroup = listOf(4, 0)
        val childInGroup = listOf(4, 1, 12, 0)
        val parentAfterGroup = listOf(4, 2)

        assertEquals(
            listOf(parentBeforeGroup, childInGroup, parentAfterGroup),
            listOf(parentAfterGroup, childInGroup, parentBeforeGroup)
                .sortedWith(::compareMarkdownDocumentOrder),
        )
    }
}
