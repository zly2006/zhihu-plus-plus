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

package com.github.zly2006.zhihu

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.zly2006.zhihu.data.Collection
import com.github.zly2006.zhihu.test.MainActivityComposeRule
import com.github.zly2006.zhihu.test.resetAppPreferences
import com.github.zly2006.zhihu.test.setScreenContent
import com.github.zly2006.zhihu.ui.CollectionBrowseScreen
import com.github.zly2006.zhihu.ui.CollectionScreen
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

    /**
     * Contract: https://github.com/zly2006/zhihu-plus-plus/issues/525
     * Introduced by: https://github.com/zly2006/zhihu-plus-plus/pull/607
     */
    @Test
    fun createActionOpensExistingCollectionDialog() {
        setCollectionScreen(seedCollections(count = 1))

        composeRule.onNodeWithTag(COLLECTION_SCREEN_CREATE_BUTTON_TAG).performClick()

        composeRule.onNodeWithTag(CREATE_COLLECTION_DIALOG_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(CREATE_COLLECTION_TITLE_INPUT_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("新建收藏夹").assertIsDisplayed()
    }

    /**
     * Contract: https://github.com/zly2006/zhihu-plus-plus/issues/525
     * Introduced by: https://github.com/zly2006/zhihu-plus-plus/pull/607
     */
    @Test
    fun onlySelectedNonDefaultCollectionOffersDeleteConfirmation() {
        val defaultCollection = Collection(
            id = "default-collection",
            title = "默认收藏夹",
            isDefault = true,
        )
        val selectedCollection = Collection(
            id = "selected-collection",
            title = "待删除收藏夹",
        )
        setCollectionScreen(listOf(defaultCollection, selectedCollection))

        composeRule
            .onNodeWithTag(collectionDeleteButtonTag(defaultCollection.id))
            .assertDoesNotExist()
        composeRule
            .onNodeWithTag(collectionDeleteButtonTag(selectedCollection.id))
            .performClick()

        composeRule
            .onNodeWithTag(collectionDeleteDialogTag(selectedCollection.id))
            .assertIsDisplayed()
        composeRule
            .onNodeWithTag(collectionDeleteConfirmTag(selectedCollection.id))
            .assertIsDisplayed()
        composeRule
            .onNodeWithText("删除后无法恢复，确认删除收藏夹“${selectedCollection.title}”吗？")
            .assertIsDisplayed()
    }

    /**
     * Contract: https://github.com/zly2006/zhihu-plus-plus/issues/609
     * Introduced by: https://github.com/zly2006/zhihu-plus-plus/pull/611
     */
    @Test
    fun directBrowseSupportsPullRefreshAndDeleteConfirmation() {
        val defaultCollection = Collection(
            id = "direct-default",
            title = "直达默认收藏夹",
            isDefault = true,
        )
        val deletableCollection = Collection(
            id = "direct-deletable",
            title = "直达待删除收藏夹",
        )
        composeRule.setScreenContent {
            CollectionBrowseScreen(
                urlToken = "offline-test-user",
                testCollections = listOf(defaultCollection, deletableCollection),
            )
        }

        composeRule.onNodeWithTag(COLLECTION_BROWSE_PULL_TO_REFRESH_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(COLLECTION_BROWSE_MODE_BUTTON_TAG).assertIsDisplayed()
        composeRule.onNodeWithContentDescription("当前为顺序模式，点击切换为随机模式").assertIsDisplayed()
        composeRule.onNodeWithTag(COLLECTION_BROWSE_RANDOM_REFRESH_BUTTON_TAG).assertDoesNotExist()
        composeRule.onNodeWithTag(COLLECTION_BROWSE_MODE_BUTTON_TAG).performClick()
        composeRule.onNodeWithContentDescription("当前为随机模式，点击切换为顺序模式").assertIsDisplayed()
        composeRule.onNodeWithTag(COLLECTION_BROWSE_RANDOM_REFRESH_BUTTON_TAG).assertIsDisplayed().performClick()
        composeRule.onNodeWithTag(COLLECTION_BROWSE_MODE_BUTTON_TAG).performClick()
        composeRule.onNodeWithTag(COLLECTION_BROWSE_RANDOM_REFRESH_BUTTON_TAG).assertDoesNotExist()
        composeRule.onNodeWithTag(COLLECTION_BROWSE_FOLDER_SWITCH_BUTTON_TAG).performClick()
        composeRule.onNodeWithTag(collectionBrowseDeleteButtonTag(defaultCollection.id)).assertDoesNotExist()
        composeRule.onNodeWithTag(collectionBrowseDeleteButtonTag(deletableCollection.id)).performClick()

        composeRule.onNodeWithTag(collectionBrowseDeleteDialogTag(deletableCollection.id)).assertIsDisplayed()
        composeRule.onNodeWithTag(collectionBrowseDeleteConfirmTag(deletableCollection.id)).assertIsDisplayed()
        composeRule
            .onNodeWithText("删除后无法恢复，确认删除收藏夹“${deletableCollection.title}”吗？")
            .assertIsDisplayed()
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
        const val COLLECTION_SCREEN_CREATE_BUTTON_TAG = "collection_screen_create_button"
        const val CREATE_COLLECTION_DIALOG_TAG = "create_collection_dialog"
        const val CREATE_COLLECTION_TITLE_INPUT_TAG = "create_collection_title_input"
        const val COLLECTION_BROWSE_PULL_TO_REFRESH_TAG = "collection_browse_pull_to_refresh"
        const val COLLECTION_BROWSE_FOLDER_SWITCH_BUTTON_TAG = "collection_browse_folder_switch_button"
        const val COLLECTION_BROWSE_MODE_BUTTON_TAG = "collection_browse_mode_button"
        const val COLLECTION_BROWSE_RANDOM_REFRESH_BUTTON_TAG = "collection_browse_random_refresh_button"

        fun collectionItemTag(collectionId: String) = "collection_screen_item_$collectionId"

        fun collectionDeleteButtonTag(collectionId: String) = "collection_screen_delete_button_$collectionId"

        fun collectionDeleteDialogTag(collectionId: String) = "collection_screen_delete_dialog_$collectionId"

        fun collectionDeleteConfirmTag(collectionId: String) = "collection_screen_delete_confirm_$collectionId"

        fun collectionBrowseDeleteButtonTag(collectionId: String) = "collection_browse_delete_button_$collectionId"

        fun collectionBrowseDeleteDialogTag(collectionId: String) = "collection_browse_delete_dialog_$collectionId"

        fun collectionBrowseDeleteConfirmTag(collectionId: String) = "collection_browse_delete_confirm_$collectionId"
    }
}
