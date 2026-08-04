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
import com.github.zly2006.zhihu.viewmodel.collectionRandomPageOffsets
import com.github.zly2006.zhihu.viewmodel.shouldReuseCollectionRandomSession
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
                refreshOnNextActivation = true,
            ),
        )
    }

    @Test
    fun doesNotRefreshWhenReturningFromContent() {
        assertFalse(
            shouldRefreshCollectionDataOnActivation(
                isActive = true,
                useTestCollections = false,
                refreshOnNextActivation = false,
            ),
        )
    }

    @Test
    fun doesNotRefreshWhileInactiveOrForInjectedTestData() {
        assertFalse(
            shouldRefreshCollectionDataOnActivation(
                isActive = false,
                useTestCollections = false,
                refreshOnNextActivation = true,
            ),
        )
        assertFalse(
            shouldRefreshCollectionDataOnActivation(
                isActive = true,
                useTestCollections = true,
                refreshOnNextActivation = true,
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

    @Test
    fun appendingNextPageKeepsPreviouslyLoadedRandomOrder() {
        val firstPage = List(12) { index -> displayItem("first-$index") }
        val nextPage = List(8) { index -> displayItem("next-$index") }
        val allKeys = (firstPage + nextPage).mapTo(mutableSetOf()) { it.stableKey }

        listOf(Int.MIN_VALUE, -1, 0, 1, 42, Int.MAX_VALUE).forEach { seed ->
            val initialOrder = orderCollectionItems(firstPage, true, seed)
            val orderAfterAppend = orderCollectionItems(
                items = firstPage + nextPage,
                randomMode = true,
                randomSeed = seed,
                previousRandomOrderKeys = initialOrder.map { it.stableKey },
            )

            assertEquals(
                initialOrder.map { it.stableKey },
                orderAfterAppend.take(initialOrder.size).map { it.stableKey },
            )
            assertEquals(
                nextPage.mapTo(mutableSetOf()) { it.stableKey },
                orderAfterAppend.drop(initialOrder.size).mapTo(mutableSetOf()) { it.stableKey },
            )
            assertEquals(allKeys, orderAfterAppend.mapTo(mutableSetOf()) { it.stableKey })
        }
    }

    @Test
    fun restoringRandomSessionKeepsTheExactLoadedOrder() {
        val items = List(20) { index -> displayItem("item-$index") }
        val initialOrder = orderCollectionItems(items, true, 42)

        val restoredOrder = orderCollectionItems(
            items = items,
            randomMode = true,
            randomSeed = 42,
            previousRandomOrderKeys = initialOrder.map { it.stableKey },
        )

        assertEquals(initialOrder.map { it.stableKey }, restoredOrder.map { it.stableKey })
    }

    @Test
    fun returningToAnExistingRandomSessionDoesNotRefreshIt() {
        assertTrue(
            shouldReuseCollectionRandomSession(
                activeRandomSeed = 42,
                activeRandomItemCount = 2_159,
                requestedRandomSeed = 42,
                requestedItemCount = 2_159,
                hasLoadedItems = true,
                isLoading = false,
                isEnd = false,
            ),
        )
        assertTrue(
            shouldReuseCollectionRandomSession(
                activeRandomSeed = 42,
                activeRandomItemCount = 0,
                requestedRandomSeed = 42,
                requestedItemCount = 0,
                hasLoadedItems = false,
                isLoading = false,
                isEnd = true,
            ),
        )
    }

    @Test
    fun changedRandomRoundOrCollectionSizeStartsANewSession() {
        assertFalse(
            shouldReuseCollectionRandomSession(
                activeRandomSeed = 42,
                activeRandomItemCount = 2_159,
                requestedRandomSeed = 43,
                requestedItemCount = 2_159,
                hasLoadedItems = true,
                isLoading = false,
                isEnd = false,
            ),
        )
        assertFalse(
            shouldReuseCollectionRandomSession(
                activeRandomSeed = 42,
                activeRandomItemCount = 0,
                requestedRandomSeed = 42,
                requestedItemCount = 2_159,
                hasLoadedItems = true,
                isLoading = false,
                isEnd = false,
            ),
        )
        assertFalse(
            shouldReuseCollectionRandomSession(
                activeRandomSeed = 42,
                activeRandomItemCount = 2_159,
                requestedRandomSeed = 42,
                requestedItemCount = 2_159,
                hasLoadedItems = false,
                isLoading = false,
                isEnd = false,
            ),
        )
    }

    @Test
    fun randomModeShufflesPagesAcrossTheWholeCollection() {
        val offsets = collectionRandomPageOffsets(
            itemCount = 2_159,
            randomSeed = 42,
            previousFirstOffset = 0,
        )
        val sequentialOffsets = (0..2_140 step 20).toList()

        assertEquals(sequentialOffsets.size, offsets.size)
        assertEquals(sequentialOffsets.toSet(), offsets.toSet())
        assertNotEquals(sequentialOffsets, offsets)
        assertNotEquals(0, offsets.first())
        assertEquals(
            offsets,
            collectionRandomPageOffsets(
                itemCount = 2_159,
                randomSeed = 42,
                previousFirstOffset = 0,
            ),
        )
    }

    @Test
    fun nextRandomRoundAvoidsThePreviousStartingPage() {
        val firstRound = collectionRandomPageOffsets(itemCount = 2_159, randomSeed = 42)
        val nextRound = collectionRandomPageOffsets(
            itemCount = 2_159,
            randomSeed = 43,
            previousFirstOffset = firstRound.first(),
        )

        assertNotEquals(firstRound.first(), nextRound.first())
    }

    @Test
    fun randomPageOffsetsAdaptToSmallCollectionSizes() {
        assertEquals(listOf(0), collectionRandomPageOffsets(itemCount = 0, randomSeed = 42))
        assertEquals(listOf(0), collectionRandomPageOffsets(itemCount = 1, randomSeed = 42))
        assertEquals(listOf(0), collectionRandomPageOffsets(itemCount = 20, randomSeed = 42))
        assertEquals(
            setOf(0, 20),
            collectionRandomPageOffsets(itemCount = 21, randomSeed = 42).toSet(),
        )
        assertEquals(
            setOf(0, 20),
            collectionRandomPageOffsets(itemCount = 35, randomSeed = 42).toSet(),
        )
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
