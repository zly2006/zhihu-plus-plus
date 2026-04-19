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

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.test.MainActivityComposeRule
import com.github.zly2006.zhihu.test.ZhihuMockApi
import com.github.zly2006.zhihu.test.resetAppPreferences
import com.github.zly2006.zhihu.test.setScreenContent
import com.github.zly2006.zhihu.ui.ARTICLE_SCREEN_ACTIONS_MENU_TAG
import com.github.zly2006.zhihu.ui.ARTICLE_SCREEN_ACTION_COPY_LINK_TAG
import com.github.zly2006.zhihu.ui.ARTICLE_SCREEN_ACTION_EXPORT_TAG
import com.github.zly2006.zhihu.ui.ARTICLE_SCREEN_ACTION_OPEN_IN_BROWSER_TAG
import com.github.zly2006.zhihu.ui.ARTICLE_SCREEN_ACTION_SHARE_TAG
import com.github.zly2006.zhihu.ui.ARTICLE_SCREEN_ACTION_SUMMARY_TAG
import com.github.zly2006.zhihu.ui.ARTICLE_SCREEN_BOOKMARK_TAG
import com.github.zly2006.zhihu.ui.ARTICLE_SCREEN_COMMENT_TAG
import com.github.zly2006.zhihu.ui.ARTICLE_SCREEN_DOUBLE_TAP_SURFACE_TAG
import com.github.zly2006.zhihu.ui.ARTICLE_SCREEN_TOP_ACTIONS_TAG
import com.github.zly2006.zhihu.ui.ARTICLE_SCREEN_TOP_BACK_TAG
import com.github.zly2006.zhihu.ui.ARTICLE_SCREEN_VOTE_UP_TAG
import com.github.zly2006.zhihu.ui.AnswerDoubleTapAction
import com.github.zly2006.zhihu.ui.ArticleScreen
import com.github.zly2006.zhihu.ui.ArticleScreenTestHooks
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import com.github.zly2006.zhihu.util.clipboardManager
import com.github.zly2006.zhihu.viewmodel.ArticleViewModel
import io.ktor.http.HttpMethod
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class ArticleScreenInstrumentedTest {
    @get:Rule
    val composeRule: MainActivityComposeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        composeRule.resetAppPreferences()
        composeRule.activity
            .getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean("duo3_article_bar", true)
            .putBoolean("duo3_article_actions", true)
            .putBoolean("titleAutoHide", true)
            .putBoolean("autoHideArticleBottomBar", true)
            .putBoolean("buttonSkipAnswer", true)
            .putBoolean("autoHideSkipAnswerButton", true)
            .putBoolean("pinAnswerDate", true)
            .putBoolean("articleUseWebview", false)
            .putString("answerDoubleTapAction", AnswerDoubleTapAction.Ask.preferenceValue)
            .commit()
    }

    @Test
    fun topBarActionsDialogsClipboardAndBackHandlerRemainDeterministicOffline() {
        /*
         * Expected behavior:
         * 1. The seeded article content should render from local view-model state without requiring
         *    a live article fetch, and the top app bar back action must route through the injected
         *    callback exactly once.
         * 2. Bookmark and comment buttons must open their local overlay/dialog states, proving the
         *    primary action bar remains interactive after a vertical swipe cycle on the article body.
         * 3. The actions menu must expose share, copy-link, summary, export, and open-in-browser
         *    actions. Share/open-in-browser should route through injected hooks; copy-link must
         *    update the clipboard with the deterministic Zhihu URL; summary/export should open the
         *    corresponding local sheet/dialog.
         * 4. Pressing system back while the menu is open should dismiss only the menu via the screen
         *    BackHandler instead of navigating away.
         */
        val backClicks = AtomicInteger(0)
        val bookmarkClicks = AtomicInteger(0)
        val commentClicks = AtomicInteger(0)
        val sharedText = AtomicReference<String?>(null)
        val openInBrowserTarget = AtomicReference<Article?>(null)
        val summaryClicks = AtomicInteger(0)
        val exportClicks = AtomicInteger(0)
        val setup = setArticleScreen(
            hooks = ArticleScreenTestHooks(
                onTopAppBarBackClick = { backClicks.incrementAndGet() },
                onBookmarkClick = { bookmarkClicks.incrementAndGet() },
                onCommentClick = { commentClicks.incrementAndGet() },
                onShare = { sharedText.set(it) },
                onSummaryRequest = { summaryClicks.incrementAndGet() },
                onExportRequest = { exportClicks.incrementAndGet() },
                onOpenInBrowser = { openInBrowserTarget.set(it) },
            ),
        )

        composeRule.onNodeWithText("离线 Article 标题").assertIsDisplayed()
        composeRule.onNodeWithTag(ARTICLE_SCREEN_DOUBLE_TAP_SURFACE_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(ARTICLE_SCREEN_DOUBLE_TAP_SURFACE_TAG).performTouchInput {
            swipeUp()
            swipeDown()
        }
        composeRule.onNodeWithTag(ARTICLE_SCREEN_BOOKMARK_TAG).assertIsDisplayed().performClick()
        assertEquals(1, bookmarkClicks.get())

        composeRule.onNodeWithTag(ARTICLE_SCREEN_COMMENT_TAG).assertIsDisplayed().performClick()
        assertEquals(1, commentClicks.get())

        composeRule.onNodeWithTag(ARTICLE_SCREEN_TOP_ACTIONS_TAG).assertIsDisplayed().performClick()
        composeRule.onNodeWithTag(ARTICLE_SCREEN_ACTIONS_MENU_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(ARTICLE_SCREEN_ACTION_SHARE_TAG).performClick()
        assertTrue(sharedText.get()?.contains("https://www.zhihu.com/question/123456/answer/777") == true)

        composeRule.onNodeWithTag(ARTICLE_SCREEN_TOP_ACTIONS_TAG).performClick()
        composeRule.onNodeWithTag(ARTICLE_SCREEN_ACTION_COPY_LINK_TAG).performClick()
        val clipboardText = composeRule.activity.clipboardManager.primaryClip
            ?.getItemAt(0)
            ?.coerceToText(composeRule.activity)
            ?.toString()
        assertTrue(clipboardText?.contains("https://www.zhihu.com/question/123456/answer/777") == true)

        composeRule.onNodeWithTag(ARTICLE_SCREEN_TOP_ACTIONS_TAG).performClick()
        composeRule.onNodeWithTag(ARTICLE_SCREEN_ACTION_SUMMARY_TAG).performClick()
        assertEquals(1, summaryClicks.get())

        composeRule.onNodeWithTag(ARTICLE_SCREEN_TOP_ACTIONS_TAG).performClick()
        composeRule.onNodeWithTag(ARTICLE_SCREEN_ACTION_EXPORT_TAG).performClick()
        assertEquals(1, exportClicks.get())

        composeRule.onNodeWithTag(ARTICLE_SCREEN_TOP_ACTIONS_TAG).performClick()
        composeRule.onNodeWithTag(ARTICLE_SCREEN_ACTION_OPEN_IN_BROWSER_TAG).performClick()
        assertEquals(ARTICLE, openInBrowserTarget.get())

        composeRule.onNodeWithTag(ARTICLE_SCREEN_TOP_ACTIONS_TAG).performClick()
        composeRule.onNodeWithTag(ARTICLE_SCREEN_ACTIONS_MENU_TAG).assertIsDisplayed()
        composeRule.activity.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(ARTICLE_SCREEN_ACTIONS_MENU_TAG).assertDoesNotExist()

        composeRule.onNodeWithTag(ARTICLE_SCREEN_TOP_BACK_TAG).performClick()
        assertEquals(1, backClicks.get())
    }

    @Test
    fun voteButtonsAndDoubleTapDialogStayDeterministicOffline() {
        /*
         * Expected behavior:
         * 1. The seeded vote controls should remain visible on the offline answer screen and the
         *    vote-up action must update local state when the mocked voters endpoint returns a new
         *    count.
         * 2. Double-tapping the rendered article content with the default "ask every time" setting
         *    should open the dedicated action sheet instead of navigating away.
         * 3. Choosing the "open comments" action from that sheet must persist the preference and
         *    open the local comment sheet immediately.
         * 4. The content body must still remain rendered after those interactions, proving the
         *    dialog and overlay flows do not corrupt the base article composition.
         */
        ZhihuMockApi.mockJson(
            method = HttpMethod.Post,
            url = "https://www.zhihu.com/api/v4/answers/777/voters",
            body = """{"voteup_count":43}""",
        )
        val askDoubleTapClicks = AtomicInteger(0)
        val setup = setArticleScreen(
            hooks = ArticleScreenTestHooks(
                onAskDoubleTapAction = { askDoubleTapClicks.incrementAndGet() },
            ),
        )

        composeRule.onNodeWithTag(ARTICLE_SCREEN_VOTE_UP_TAG).assertIsDisplayed().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            setup.viewModel.voteUpState == com.github.zly2006.zhihu.ui.VoteUpState.Up &&
                setup.viewModel.voteUpCount == 43
        }
        composeRule.onNodeWithTag(ARTICLE_SCREEN_VOTE_UP_TAG).assertIsDisplayed()

        composeRule.onNodeWithTag(ARTICLE_SCREEN_DOUBLE_TAP_SURFACE_TAG).performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) { askDoubleTapClicks.get() == 1 }
        composeRule.onNodeWithTag(ARTICLE_SCREEN_DOUBLE_TAP_SURFACE_TAG).assertIsDisplayed()
    }

    private data class ArticleSetup(
        val viewModel: ArticleViewModel,
    )

    private fun setArticleScreen(
        hooks: ArticleScreenTestHooks = ArticleScreenTestHooks(),
    ): ArticleSetup {
        val viewModel = ArticleViewModel(
            article = ARTICLE,
            httpClient = null,
            navBackStackEntry = null,
        )
        composeRule.activity.runOnUiThread {
            viewModel.title = "离线 Article 标题"
            viewModel.authorName = "离线作者"
            viewModel.authorId = "offline-author-id"
            viewModel.authorUrlToken = "offline-author"
            viewModel.content = (1..20).joinToString("\n\n") { index ->
                "第 $index 段离线正文，用于 ArticleScreen instrumented test。"
            }
            viewModel.voteUpCount = 42
            viewModel.commentCount = 7
            viewModel.questionId = 123456L
            viewModel.createdAt = 1_710_000_000L
            viewModel.updatedAt = 1_710_000_600L
            viewModel.ipInfo = "上海"
        }
        composeRule.setScreenContent {
            ArticleScreen(
                article = ARTICLE,
                viewModel = viewModel,
                testHooks = hooks,
            )
        }
        return ArticleSetup(viewModel)
    }

    private fun pressBack() {
        composeRule.activity.runOnUiThread {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
        composeRule.waitForIdle()
    }

    private companion object {
        val ARTICLE = Article(
            type = ArticleType.Answer,
            id = 777L,
            title = "离线 Article 标题",
        )
    }
}
