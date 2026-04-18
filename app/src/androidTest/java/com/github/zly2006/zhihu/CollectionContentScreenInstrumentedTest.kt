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

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso.pressBack
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.navigation.Question
import com.github.zly2006.zhihu.test.MainActivityComposeRule
import com.github.zly2006.zhihu.test.RecordingNavigator
import com.github.zly2006.zhihu.test.performHorizontalSwipeCycle
import com.github.zly2006.zhihu.test.performVerticalSwipeCycle
import com.github.zly2006.zhihu.test.resetAppPreferences
import com.github.zly2006.zhihu.test.setScreenContent
import com.github.zly2006.zhihu.ui.Collection
import com.github.zly2006.zhihu.ui.CollectionContentScreen
import com.github.zly2006.zhihu.ui.CollectionContentScreenTestOverrides
import com.github.zly2006.zhihu.ui.YMDHMS
import com.github.zly2006.zhihu.viewmodel.CollectionContentViewModel
import com.github.zly2006.zhihu.viewmodel.feed.BaseFeedViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

@RunWith(AndroidJUnit4::class)
class CollectionContentScreenInstrumentedTest {
    @get:Rule
    val composeRule: MainActivityComposeRule = createAndroidComposeRule()

    @Before
    fun setUp() {
        composeRule.resetAppPreferences()
    }

    @Test
    fun seededStateRendersTitleStatsItemsAndBackButtonDeterministically() {
        /*
         * Expected behavior:
         * 1. The screen should render entirely from a locally prefilled CollectionContentViewModel,
         *    so the toolbar, stats block, and feed rows never depend on network reachability.
         * 2. The seeded collection title and statistics text must be shown exactly as the screen
         *    formats them in production, which protects the visible contract of this page.
         * 3. Pressing the back button should delegate to the injected navigator back callback once,
         *    without recording any forward navigation destination.
         */
        val setup = setCollectionContentScreen()

        composeRule.onNodeWithTag(TITLE_TAG).assertIsDisplayed()
        composeRule.onNodeWithText(SEEDED_COLLECTION_TITLE).assertIsDisplayed()
        composeRule.onNodeWithTag(STATS_TAG).assertIsDisplayed()
        composeRule.onNodeWithText(expectedStatsText()).assertIsDisplayed()
        composeRule.onNodeWithTag(setup.itemTag(1)).assertIsDisplayed()
        composeRule.onNodeWithText(seedTitle(1)).assertIsDisplayed()

        composeRule.onNodeWithTag(BACK_BUTTON_TAG).assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        assertEquals(1, setup.navigator.backCount)
        assertTrue(setup.navigator.destinations.isEmpty())
    }

    @Test
    fun overflowMenuOpensAndSystemBackClosesItWithoutNavigationSideEffects() {
        /*
         * Expected behavior:
         * 1. Tapping the overflow button should open the stable export action entry every time.
         * 2. Pressing the system back button while the popup is open should dismiss only the popup,
         *    because this back gesture targets transient UI chrome rather than page navigation.
         * 3. Dismissing the popup in this way must not trigger either forward navigation or the
         *    toolbar back callback.
         */
        val setup = setCollectionContentScreen()

        composeRule.onNodeWithTag(MORE_BUTTON_TAG).assertIsDisplayed().performClick()
        composeRule.onNodeWithTag(EXPORT_ACTION_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("全部导出HTML").assertIsDisplayed()

        pressBack()
        composeRule.waitForIdle()

        composeRule.onAllNodesWithTag(EXPORT_ACTION_TAG).assertCountEquals(0)
        assertTrue(setup.navigator.destinations.isEmpty())
        assertEquals(0, setup.navigator.backCount)
    }

    @Test
    fun exportDialogSupportsImageToggleAndBothCancelAndConfirmPathsLocally() {
        /*
         * Expected behavior:
         * 1. Opening the export action should show the local options dialog instead of starting any
         *    export work immediately.
         * 2. The include-images checkbox should start checked, be toggleable, and preserve the
         *    chosen boolean when the confirm button is pressed.
         * 3. Cancelling the dialog must not invoke the export callback, while confirming after
         *    turning the checkbox off must invoke it exactly once with `false`.
         */
        val exportSelections = mutableListOf<Boolean>()
        setCollectionContentScreen(onExportAllToHtmlZip = { includeImages -> exportSelections += includeImages })

        openExportOptionsDialog()
        composeRule.onNodeWithText("导出收藏夹 HTML").assertIsDisplayed()
        composeRule.onNodeWithTag(EXPORT_INCLUDE_IMAGES_TAG).assertIsOn()
        composeRule.onNodeWithTag(EXPORT_CANCEL_TAG).performClick()
        composeRule.waitForIdle()
        composeRule.onAllNodesWithText("导出收藏夹 HTML").assertCountEquals(0)
        assertTrue(exportSelections.isEmpty())

        openExportOptionsDialog()
        composeRule.onNodeWithTag(EXPORT_INCLUDE_IMAGES_TAG).assertIsOn().performClick()
        composeRule.onNodeWithTag(EXPORT_INCLUDE_IMAGES_TAG).assertIsOff()
        composeRule.onNodeWithTag(EXPORT_CONFIRM_TAG).performClick()
        composeRule.waitForIdle()
        composeRule.onAllNodesWithText("导出收藏夹 HTML").assertCountEquals(0)
        assertEquals(listOf(false), exportSelections)
    }

    @Test
    fun listClickNavigatesAndSwipeCyclesKeepTheSeededListStable() {
        /*
         * Expected behavior:
         * 1. A larger locally seeded list should remain scrollable and safe under vertical and
         *    horizontal swipe cycles, without triggering network pagination.
         * 2. After those gestures, the toolbar and seeded list rows should still be interactive,
         *    proving that the page layout and semantics tree remain intact.
         * 3. Clicking a seeded row should navigate to that row's destination exactly once.
         */
        val setup = setCollectionContentScreen(itemCount = 24)

        composeRule.onNodeWithTag(LIST_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(LIST_TAG).performVerticalSwipeCycle()
        composeRule.onNodeWithTag(LIST_TAG).performHorizontalSwipeCycle()
        composeRule.waitForIdle()

        composeRule.onNodeWithText(SEEDED_COLLECTION_TITLE).assertIsDisplayed()
        composeRule.onNodeWithTag(setup.itemTag(1)).assertIsDisplayed()
        composeRule.onNodeWithTag(setup.itemTag(3)).performClick()
        composeRule.waitForIdle()

        assertEquals(0, setup.navigator.backCount)
        assertEquals(listOf(seedQuestionDestination(3)), setup.navigator.destinations)
    }

    private fun openExportOptionsDialog() {
        composeRule.onNodeWithTag(MORE_BUTTON_TAG).performClick()
        composeRule.onNodeWithTag(EXPORT_ACTION_TAG).performClick()
    }

    private fun setCollectionContentScreen(
        itemCount: Int = SEEDED_ITEM_COUNT,
        onExportAllToHtmlZip: ((Boolean) -> Unit)? = null,
    ): SeededCollectionScreenSetup {
        val seededViewModel = seedCollectionContentViewModel(itemCount)
        val navigator = composeRule.setScreenContent {
            CollectionContentScreen(
                collectionId = SEEDED_COLLECTION_ID,
                innerPadding = PaddingValues(),
                testOverrides = CollectionContentScreenTestOverrides(
                    viewModel = seededViewModel,
                    isEnd = true,
                    onLoadMore = {},
                    onExportAllToHtmlZip = onExportAllToHtmlZip,
                ),
            )
        }
        return SeededCollectionScreenSetup(
            navigator = navigator,
            viewModel = seededViewModel,
        )
    }

    private fun seedCollectionContentViewModel(itemCount: Int): CollectionContentViewModel {
        val seededViewModel = CollectionContentViewModel(SEEDED_COLLECTION_ID)
        val seededDisplayItems = List(itemCount) { index ->
            BaseFeedViewModel.FeedDisplayItem(
                title = seedTitle(index + 1),
                summary = "用于 CollectionContentScreen 仪器测试的固定摘要 ${index + 1}",
                details = "固定详情 ${index + 1}",
                navDestination = seedQuestionDestination(index + 1),
                feed = null,
                authorName = "作者 ${index + 1}",
                localFeedId = "collection-content-test-item-${index + 1}",
            )
        }
        val seededAllData = List(itemCount) { index ->
            CollectionContentViewModel.CollectionItem(
                created = "2026-04-18T12:00:00+08:00",
                content = Feed.QuestionTarget(
                    id = SEEDED_BASE_QUESTION_ID + index + 1L,
                    _title = seedTitle(index + 1),
                    url = "https://www.zhihu.com/question/${SEEDED_BASE_QUESTION_ID + index + 1L}",
                    type = "question",
                    answerCount = 10 + index,
                    commentCount = 3 + index,
                    followerCount = 100 + index,
                    detail = "固定详情 ${index + 1}",
                    excerpt = "用于 CollectionContentScreen 仪器测试的固定摘要 ${index + 1}",
                ),
            )
        }

        composeRule.activity.runOnUiThread {
            seededViewModel.collection = Collection(
                id = SEEDED_COLLECTION_ID,
                title = SEEDED_COLLECTION_TITLE,
                itemCount = itemCount,
                likeCount = SEEDED_LIKE_COUNT,
                commentCount = SEEDED_COMMENT_COUNT,
                updatedTime = SEEDED_UPDATED_TIME_SECONDS,
            )
            seededViewModel.displayItems.clear()
            seededViewModel.displayItems.addAll(seededDisplayItems)
            seededViewModel.allData.clear()
            seededViewModel.allData.addAll(seededAllData)
        }
        composeRule.waitForIdle()
        return seededViewModel
    }

    private fun expectedStatsText(): String = listOf(
        "${SEEDED_ITEM_COUNT} 条收藏",
        "${SEEDED_LIKE_COUNT} 个赞同",
        "${SEEDED_COMMENT_COUNT} 条评论",
        "${YMDHMS.format(Date(SEEDED_UPDATED_TIME_SECONDS * 1000))} 更新",
    ).joinToString(" · ")

    private fun seedQuestionDestination(index: Int) = Question(
        questionId = SEEDED_BASE_QUESTION_ID + index,
        title = seedTitle(index),
    )

    private fun seedTitle(index: Int) = "固定收藏条目 $index"

    private data class SeededCollectionScreenSetup(
        val navigator: RecordingNavigator,
        val viewModel: CollectionContentViewModel,
    ) {
        fun itemTag(index: Int): String = "collection_content_item_${viewModel.displayItems[index - 1].stableKey}"
    }

    private companion object {
        const val SEEDED_COLLECTION_ID = "instrumented-test-collection"
        const val SEEDED_COLLECTION_TITLE = "离线收藏夹测试"
        const val SEEDED_ITEM_COUNT = 24
        const val SEEDED_LIKE_COUNT = 88
        const val SEEDED_COMMENT_COUNT = 13
        const val SEEDED_UPDATED_TIME_SECONDS = 1_713_420_000L
        const val SEEDED_BASE_QUESTION_ID = 9_000L

        const val TITLE_TAG = "collection_content_title"
        const val STATS_TAG = "collection_content_stats"
        const val LIST_TAG = "collection_content_list"
        const val BACK_BUTTON_TAG = "collection_content_back_button"
        const val MORE_BUTTON_TAG = "collection_content_more_button"
        const val EXPORT_ACTION_TAG = "collection_content_export_action"
        const val EXPORT_INCLUDE_IMAGES_TAG = "collection_content_export_include_images"
        const val EXPORT_CONFIRM_TAG = "collection_content_export_confirm"
        const val EXPORT_CANCEL_TAG = "collection_content_export_cancel"
    }
}
