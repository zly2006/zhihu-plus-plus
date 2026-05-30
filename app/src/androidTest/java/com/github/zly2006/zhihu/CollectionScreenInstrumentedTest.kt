/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
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

package com.github.zly2006.zhihu

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.zly2006.zhihu.navigation.CollectionContent
import com.github.zly2006.zhihu.test.MainActivityComposeRule
import com.github.zly2006.zhihu.test.resetAppPreferences
import com.github.zly2006.zhihu.test.setScreenContent
import com.github.zly2006.zhihu.ui.Collection
import com.github.zly2006.zhihu.ui.CollectionScreen
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CollectionScreenInstrumentedTest {
    @get:Rule
    val composeRule: MainActivityComposeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        composeRule.resetAppPreferences()
    }

    @Test
    fun toolbarAndSeededCollectionsRenderOfflineAndBackStaysDeterministic() {
        // Seed a fixed offline list directly into the screen so the test never depends on account
        // state, network reachability, or remote collection ordering. Expected behavior:
        // 1. The screen title and back button are visible immediately.
        // 2. The first seeded collection rows render exactly as provided.
        // 3. Pressing back only records one back event and must not emit any forward navigation.
        val navigator = setCollectionScreen(seedCollections(count = 12))

        composeRule.onNodeWithTag(COLLECTION_SCREEN_TITLE_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(COLLECTION_SCREEN_BACK_BUTTON_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(COLLECTION_SCREEN_LIST_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("固定收藏夹 1").assertIsDisplayed()
        composeRule.onNodeWithText("固定收藏夹 2").assertIsDisplayed()

        composeRule.onNodeWithTag(COLLECTION_SCREEN_BACK_BUTTON_TAG).performClick()
        composeRule.runOnIdle {
            assertEquals(emptyList<CollectionContent>(), navigator.destinations)
            assertEquals(1, navigator.backCount)
        }
    }

    @Test
    fun scrollingToDeepCollectionAndClickingCardNavigatesToCollectionContent() {
        // Provide enough deterministic rows to require a real lazy-list scroll. Expected behavior:
        // 1. The list can scroll to an off-screen collection card using stable item tags.
        // 2. The target card becomes visible after scrolling.
        // 3. Tapping that card navigates to CollectionContent with the exact collection id.
        val seededCollections = seedCollections(count = 30)
        val targetCollection = seededCollections.last()
        val navigator = setCollectionScreen(seededCollections)

        composeRule
            .onNodeWithTag(COLLECTION_SCREEN_LIST_TAG)
            .performScrollToNode(hasTestTag(collectionItemTag(targetCollection.id)))
        composeRule.onNodeWithTag(collectionItemTag(targetCollection.id)).assertIsDisplayed()
        composeRule.onNodeWithText(targetCollection.title).assertIsDisplayed()

        composeRule.onNodeWithTag(collectionItemTag(targetCollection.id)).performClick()
        composeRule.runOnIdle {
            assertEquals(listOf(CollectionContent(targetCollection.id)), navigator.destinations)
            assertEquals(0, navigator.backCount)
        }
    }

    @Test
    fun emptySeededListRemainsStableAndDoesNotAttemptUnexpectedNavigation() {
        // Start from an explicitly empty offline list so the screen cannot trigger network loading.
        // Expected behavior:
        // 1. The toolbar still renders normally.
        // 2. The lazy list remains mounted and reaches its terminal footer state instead of crashing.
        // 3. No forward navigation or back events happen unless the test explicitly requests them.
        val navigator = setCollectionScreen(emptyList())

        composeRule.onNodeWithTag(COLLECTION_SCREEN_TITLE_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(COLLECTION_SCREEN_BACK_BUTTON_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(COLLECTION_SCREEN_LIST_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("已经到底啦").assertIsDisplayed()

        composeRule.runOnIdle {
            assertEquals(emptyList<CollectionContent>(), navigator.destinations)
            assertEquals(0, navigator.backCount)
        }
    }

    private fun setCollectionScreen(testCollections: List<Collection>) = composeRule.setScreenContent {
        CollectionScreen(
            urlToken = "offline-test-user",
            testCollections = testCollections,
        )
    }

    private fun seedCollections(count: Int): List<Collection> = List(count) { index ->
        val collectionIndex = index + 1
        Collection(
            id = "collection-$collectionIndex",
            title = "固定收藏夹 $collectionIndex",
            description = "用于 CollectionScreen 仪器测试的固定收藏夹 $collectionIndex",
            itemCount = collectionIndex * 3,
            likeCount = collectionIndex * 5,
            commentCount = collectionIndex,
        )
    }

    private companion object {
        const val COLLECTION_SCREEN_TITLE_TAG = "collection_screen_title"
        const val COLLECTION_SCREEN_BACK_BUTTON_TAG = "collection_screen_back_button"
        const val COLLECTION_SCREEN_LIST_TAG = "collection_screen_list"

        fun collectionItemTag(collectionId: String) = "collection_screen_item_$collectionId"
    }
}
