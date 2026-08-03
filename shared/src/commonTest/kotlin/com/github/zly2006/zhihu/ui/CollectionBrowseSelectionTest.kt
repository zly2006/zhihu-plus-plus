/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.github.zly2006.zhihu.ui

import com.github.zly2006.zhihu.data.Collection
import com.github.zly2006.zhihu.data.FeedDisplayItem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CollectionBrowseSelectionTest {
    private fun collection(id: String, isDefault: Boolean = false, title: String = "c-$id") =
        Collection(id = id, isDefault = isDefault, title = title)

    @Test
    fun picksDefaultCollectionWhenPresent() {
        val collections = listOf(
            collection("c1"),
            collection("c2", isDefault = true),
            collection("c3"),
        )
        assertEquals("c2", pickDefaultCollectionId(collections))
    }

    @Test
    fun fallsBackToFirstWhenNoDefault() {
        val collections = listOf(
            collection("c1"),
            collection("c2"),
        )
        assertEquals("c1", pickDefaultCollectionId(collections))
    }

    @Test
    fun returnsNullForEmptyList() {
        assertNull(pickDefaultCollectionId(emptyList()))
    }

    @Test
    fun picksFirstDefaultWhenMultipleDefaults() {
        val collections = listOf(
            collection("c1", isDefault = true),
            collection("c2", isDefault = true),
        )
        assertEquals("c1", pickDefaultCollectionId(collections))
    }

    @Test
    fun refreshesWhenCollectionPageBecomesActiveAgain() {
        assertTrue(
            shouldRefreshCollectionDataOnActivation(
                isActive = true,
                useTestCollections = false,
            ),
        )
    }

    @Test
    fun doesNotRefreshWhileInactiveOrForInjectedTestData() {
        assertFalse(
            shouldRefreshCollectionDataOnActivation(
                isActive = false,
                useTestCollections = false,
            ),
        )
        assertFalse(
            shouldRefreshCollectionDataOnActivation(
                isActive = true,
                useTestCollections = true,
            ),
        )
    }

    @Test
    fun randomModeIsStableAndKeepsEveryLoadedItem() {
        val items = List(12) { index -> displayItem("item-$index") }

        val first = orderCollectionItems(items, true, 42)
        val second = orderCollectionItems(items, true, 42)
        val nextRound = orderCollectionItems(items, true, 43)

        assertEquals(first, second)
        assertEquals(items.map { it.stableKey }.toSet(), first.map { it.stableKey }.toSet())
        assertNotEquals(first, nextRound)
    }

    private fun displayItem(
        title: String,
    ) = FeedDisplayItem(
        title = title,
        summary = null,
        details = "details-$title",
        feed = null,
    )
}
