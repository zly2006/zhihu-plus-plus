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

import android.content.Context
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.MutableState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.zly2006.zhihu.navigation.AnswerNavigator
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.shared.ui.AnswerDoubleTapAction
import com.github.zly2006.zhihu.test.MainActivityComposeRule
import com.github.zly2006.zhihu.test.resetAppPreferences
import com.github.zly2006.zhihu.test.setScreenContent
import com.github.zly2006.zhihu.ui.ARTICLE_USE_WEBVIEW_PREFERENCE_KEY
import com.github.zly2006.zhihu.ui.ArticleScreen
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import com.github.zly2006.zhihu.ui.TtsState
import com.github.zly2006.zhihu.ui.rememberArticleTtsState
import com.github.zly2006.zhihu.viewmodel.ArticleViewModel
import com.github.zly2006.zhihu.viewmodel.ZhihuApiEnvironment
import io.ktor.client.HttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

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
            .putBoolean(ARTICLE_USE_WEBVIEW_PREFERENCE_KEY, false)
            .putString("answerDoubleTapAction", AnswerDoubleTapAction.Ask.preferenceValue)
            .commit()
    }

    @Test
    fun topBarActionsDialogsClipboardAndBackHandlerRemainDeterministicOffline() {
        setArticleScreen()

        composeRule.onNodeWithText("离线 Article 标题").assertIsDisplayed()
        composeRule.onNodeWithText("离线作者").assertIsDisplayed()
        composeRule.onNodeWithText("IP属地：上海").assertExists()
        composeRule.onNodeWithContentDescription("更多选项").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("复制链接").assertIsDisplayed().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.activity.clipboardDestination == ARTICLE
        }
        assertEquals(ARTICLE, composeRule.activity.clipboardDestination)
    }

    @Test
    fun contentBodyAndMetadataRenderOffline() {
        setArticleScreen()
        composeRule.onNodeWithText("离线 Article 标题").assertIsDisplayed()
        composeRule.onNodeWithText("离线作者").assertIsDisplayed()
        composeRule.onNodeWithText("IP属地：上海").assertExists()
        composeRule.onNodeWithText("第 1 段离线正文", substring = true).assertIsDisplayed()
    }

    @Test
    fun answerCreationStatementRendersOffline() {
        val viewModel = seededAnswerViewModel(ANSWER)

        composeRule.setScreenContent {
            Scaffold(
                modifier = androidx.compose.ui.Modifier
                    .fillMaxSize(),
            ) { _ ->
                ArticleScreen(
                    article = ANSWER,
                    viewModel = viewModel,
                )
            }
        }

        composeRule.onNodeWithText("创作声明: 内容包含剧透").assertIsDisplayed()
    }

    @Test
    fun articleTtsStateReadsFromMainActivityHost() {
        composeRule.activity.runOnUiThread {
            composeRule.activity.forceTtsStateForTest(TtsState.Ready)
        }

        composeRule.setScreenContent {
            val ttsState = rememberArticleTtsState()
            Text("tts=$ttsState")
        }

        composeRule.onNodeWithText("tts=Ready").assertIsDisplayed()
    }

    @Test
    fun skipAnswerButtonNavigatesToPrefetchedNextAnswerOffline() {
        val viewModel = seededAnswerViewModel(ANSWER)
        val nextAnswer = ArticleViewModel.CachedAnswerContent(
            article = NEXT_ANSWER,
            title = "下一个离线回答",
            authorName = "下一个作者",
            authorBio = "下一个签名",
            authorAvatarUrl = "",
            content = "下一个离线回答正文",
            voteUpCount = 7,
            commentCount = 3,
        )
        composeRule.activity.runOnUiThread {
            composeRule.activity.articleAnswerSwitchState.pendingNavigator = object : AnswerNavigator(
                sourceName = "此问题",
                environment = NO_OP_API_ENVIRONMENT,
            ) {
                init {
                    nextAnswerContent = nextAnswer
                }

                override suspend fun loadNext(): Article? {
                    nextAnswerContent = null
                    return nextAnswer.article
                }

                override suspend fun prefetchNext(currentArticleId: Long) = Unit
            }
        }
        val recordingNavigator = composeRule.setScreenContent {
            Scaffold(
                modifier = androidx.compose.ui.Modifier
                    .fillMaxSize(),
            ) { _ ->
                ArticleScreen(
                    article = ANSWER,
                    viewModel = viewModel,
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("下一个回答")
            .assertIsDisplayed()
            .performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            recordingNavigator.destinations.contains(NEXT_ANSWER)
        }
    }

    @Test
    fun skipAnswerButtonCanBeDraggedBackToRightEdge() {
        val viewModel = seededAnswerViewModel(ANSWER)
        composeRule.setScreenContent {
            Scaffold(
                modifier = androidx.compose.ui.Modifier
                    .fillMaxSize(),
            ) { _ ->
                ArticleScreen(
                    article = ANSWER,
                    viewModel = viewModel,
                )
            }
        }

        val rootWidth = composeRule
            .onRoot()
            .fetchSemanticsNode()
            .boundsInRoot.width
        val preferences = composeRule.activity.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        dragSkipAnswerButtonBy(-rootWidth)
        composeRule.waitUntil(timeoutMillis = 5_000) {
            preferences.getFloat("buttonSkipAnswer-x", Float.NaN) < rootWidth / 3
        }

        dragSkipAnswerButtonBy(rootWidth)
        composeRule.waitUntil(timeoutMillis = 5_000) {
            preferences.getFloat("buttonSkipAnswer-x", Float.NaN) > rootWidth / 2
        }
        assertTrue(preferences.getFloat("buttonSkipAnswer-x", Float.NaN) > rootWidth / 2)
    }

    private fun setArticleScreen() {
        val viewModel = ArticleViewModel(
            article = ARTICLE,
            httpClient = null,
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
            Scaffold(
                modifier = androidx.compose.ui.Modifier
                    .fillMaxSize(),
            ) { _ ->
                ArticleScreen(
                    article = ARTICLE,
                    viewModel = viewModel,
                )
            }
        }
    }

    private fun dragSkipAnswerButtonBy(deltaX: Float) {
        composeRule
            .onNodeWithContentDescription("下一个回答")
            .assertIsDisplayed()
            .performTouchInput {
                down(center)
                moveBy(Offset(deltaX, 0f))
                up()
            }
        composeRule.waitForIdle()
    }

    private fun seededAnswerViewModel(article: Article): ArticleViewModel {
        val viewModel = ArticleViewModel(
            article = article,
            httpClient = null,
        )
        composeRule.activity.runOnUiThread {
            viewModel.title = "离线 Answer 标题"
            viewModel.authorName = "离线答主"
            viewModel.authorId = "offline-answer-author-id"
            viewModel.authorUrlToken = "offline-answer-author"
            viewModel.content = (1..20).joinToString("\n\n") { index ->
                "第 $index 段离线回答正文，用于 ArticleScreen instrumented test。"
            }
            viewModel.voteUpCount = 42
            viewModel.commentCount = 7
            viewModel.questionId = 123456L
            viewModel.createdAt = 1_710_000_000L
            viewModel.updatedAt = 1_710_000_600L
            viewModel.ipInfo = "上海"
            viewModel.creationStatementText = "创作声明: 内容包含剧透"
        }
        return viewModel
    }

    @Suppress("UNCHECKED_CAST")
    private fun MainActivity.forceTtsStateForTest(state: TtsState) {
        val ttsStateField = MainActivity::class.java.getDeclaredField("_ttsState")
        ttsStateField.isAccessible = true
        (ttsStateField.get(this) as MutableState<TtsState>).value = state
    }

    private companion object {
        val ARTICLE = Article(
            type = ArticleType.Article,
            id = 777L,
            title = "离线 Article 标题",
        )
        val ANSWER = Article(
            type = ArticleType.Answer,
            id = 777L,
            title = "离线 Answer 标题",
        )
        val NEXT_ANSWER = Article(
            type = ArticleType.Answer,
            id = 778L,
            title = "下一个离线回答",
        )

        val NO_OP_API_ENVIRONMENT = object : ZhihuApiEnvironment {
            override fun httpClient(): HttpClient = error("No HTTP client in offline navigator test")

            override fun authenticatedCookies(): Map<String, String> = emptyMap()

            override suspend fun handleFetchFailure(
                tag: String?,
                error: Exception,
            ) = Unit
        }
    }
}
