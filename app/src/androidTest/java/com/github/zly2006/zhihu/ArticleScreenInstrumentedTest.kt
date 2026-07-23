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
import android.content.Intent
import android.os.SystemClock
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.MutableState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.zly2006.zhihu.markdown.RenderMarkdownText
import com.github.zly2006.zhihu.navigation.AnswerNavigator
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.reading.AndroidReadingPlayerBridge
import com.github.zly2006.zhihu.reading.ContentReadingService
import com.github.zly2006.zhihu.reading.ReadingContentType
import com.github.zly2006.zhihu.reading.ReadingPlaybackStatus
import com.github.zly2006.zhihu.reading.ReadingPlayerState
import com.github.zly2006.zhihu.reading.ReadingQueueItem
import com.github.zly2006.zhihu.reading.ReadingQueueSourceRegistry
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.ui.AnswerDoubleTapAction
import com.github.zly2006.zhihu.test.MainActivityComposeRule
import com.github.zly2006.zhihu.test.resetAppPreferences
import com.github.zly2006.zhihu.test.setScreenContent
import com.github.zly2006.zhihu.ui.ARTICLE_USE_WEBVIEW_PREFERENCE_KEY
import com.github.zly2006.zhihu.ui.ArticleActionsMenu
import com.github.zly2006.zhihu.ui.ArticleScreen
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import com.github.zly2006.zhihu.ui.TtsState
import com.github.zly2006.zhihu.ui.rememberArticleTtsState
import com.github.zly2006.zhihu.viewmodel.ArticleViewModel
import com.github.zly2006.zhihu.viewmodel.ZhihuApiEnvironment
import io.ktor.client.HttpClient
import kotlinx.coroutines.CancellationException
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

@RunWith(AndroidJUnit4::class)
class ArticleScreenInstrumentedTest {
    @get:Rule
    val composeRule: MainActivityComposeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        AndroidReadingPlayerBridge.publish(ReadingPlayerState())
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

    @After
    fun tearDown() {
        composeRule.activity.stopService(Intent(composeRule.activity, ContentReadingService::class.java))
        AndroidReadingPlayerBridge.publish(ReadingPlayerState())
        ReadingQueueSourceRegistry.register(FULL_ORIGIN_SOURCE_ID, emptyList())
        ReadingQueueSourceRegistry.register(PARTIAL_ORIGIN_SOURCE_ID, emptyList())
        composeRule.runOnIdle {
            composeRule.activity.articleAnswerSwitchState.navigator = null
            composeRule.activity.articleAnswerSwitchState.pendingNavigator = null
        }
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
    fun issue495FirstFrameBenchmarkIncludesHtmlParsingLayoutAndDraw() {
        val warmupViewModel = seededAnswerViewModel(ANSWER)
        composeRule.activity.runOnUiThread {
            warmupViewModel.content =
                """<p>预热正文 <img src="https://www.zhihu.com/equation?tex=x%5E2" eeimg="1" /></p>"""
        }
        composeRule.setScreenContent {
            Scaffold(
                modifier = androidx.compose.ui.Modifier
                    .fillMaxSize(),
            ) { _ ->
                ArticleScreen(
                    article = ANSWER,
                    viewModel = warmupViewModel,
                )
            }
        }
        composeRule.onNodeWithText("预热正文", substring = true).assertIsDisplayed()
        composeRule.onRoot().captureToImage()

        val fullFixtureWarmupSamples = mutableListOf<Long>()
        val samples = buildList {
            repeat(7) { iteration ->
                val viewModel = issue495ViewModel()
                composeRule.activity.runOnUiThread {
                    viewModel.content += "<!-- benchmark-run-$iteration -->"
                }
                val startedAt = SystemClock.elapsedRealtime()
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
                composeRule.onNodeWithText("更新：", substring = true).assertIsDisplayed()
                composeRule.onRoot().captureToImage()
                val elapsedMillis = SystemClock.elapsedRealtime() - startedAt
                if (iteration < 2) {
                    fullFixtureWarmupSamples += elapsedMillis
                } else {
                    add(elapsedMillis)
                }
            }
        }
        val medianMillis = samples.sorted()[samples.size / 2]
        Log.i(
            ISSUE_495_BENCHMARK_TAG,
            "fullFixtureWarmupSamplesMs=$fullFixtureWarmupSamples firstFrameSamplesMs=$samples " +
                "medianMs=$medianMillis htmlChars=36460",
        )
        assertTrue(
            "Issue #495 median first frame took ${medianMillis}ms; benchmark includes HTML parsing, Compose layout, and draw",
            medianMillis < ISSUE_495_FIRST_FRAME_LIMIT_MS,
        )
    }

    @Test
    fun issue495MaterializesEstimatedOffscreenBlocksWhenScrolledIntoView() {
        val viewModel = issue495ViewModel()

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

        composeRule.onNodeWithText("更新：", substring = true).assertIsDisplayed()
        composeRule.onNodeWithText("再减去", substring = true).assertDoesNotExist()
        val scrollContainer = composeRule.onNode(
            SemanticsMatcher("has vertical scroll axis") { node ->
                node.config.contains(SemanticsProperties.VerticalScrollAxisRange)
            },
        )
        val initialMaxScroll = scrollContainer
            .fetchSemanticsNode()
            .config[SemanticsProperties.VerticalScrollAxisRange]
            .maxValue()
        var remainingScrolls = 30
        while (
            remainingScrolls-- > 0 &&
            composeRule.onAllNodesWithText("再减去", substring = true).fetchSemanticsNodes().isEmpty()
        ) {
            scrollContainer.performSemanticsAction(SemanticsActions.ScrollBy) { scrollBy ->
                scrollBy(0f, 4_000f)
            }
            composeRule.waitForIdle()
        }

        composeRule.onNodeWithText("再减去", substring = true).assertExists()
        composeRule.onNodeWithText("更新：", substring = true).assertDoesNotExist()
        composeRule.onNodeWithText("IP属地：上海").assertExists()

        var scrollToEndAttempts = 60
        while (scrollToEndAttempts-- > 0) {
            val range = scrollContainer
                .fetchSemanticsNode()
                .config[SemanticsProperties.VerticalScrollAxisRange]
            if (range.maxValue() - range.value() <= 1f) break
            scrollContainer.performSemanticsAction(SemanticsActions.ScrollBy) { scrollBy ->
                scrollBy(0f, 4_000f)
            }
            composeRule.waitForIdle()
        }
        val materializedMaxScroll = scrollContainer
            .fetchSemanticsNode()
            .config[SemanticsProperties.VerticalScrollAxisRange]
            .maxValue()
        assertTrue("The materialized document must remain scrollable", materializedMaxScroll > 0f)
        val estimateRatio = initialMaxScroll / materializedMaxScroll
        Log.i(
            ISSUE_495_BENCHMARK_TAG,
            "estimatedMaxScroll=$initialMaxScroll materializedMaxScroll=$materializedMaxScroll " +
                "ratio=$estimateRatio",
        )
        assertTrue(
            "Initial estimated scroll range should stay within 25% of the fully materialized range; " +
                "estimated=$initialMaxScroll materialized=$materializedMaxScroll",
            estimateRatio in 0.75f..1.25f,
        )
    }

    @Test
    fun footnoteReferenceAndBackLinkNavigateInsideOuterArticleScroll() {
        val markdown = buildString {
            appendLine("正文开头脚注[^note]")
            appendLine()
            repeat(60) { index ->
                appendLine("填充段落 $index：用于确保脚注定义位于当前屏幕之外。")
                appendLine()
            }
            appendLine("[^note]: 脚注内容")
        }

        composeRule.setScreenContent {
            val scrollState = rememberScrollState()
            Column(
                modifier = androidx.compose.ui.Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState),
            ) {
                RenderMarkdownText(
                    markdown = markdown,
                    scrollState = scrollState,
                    enableScroll = false,
                )
            }
        }

        composeRule
            .onAllNodesWithText("[1]", useUnmergedTree = true)[0]
            .assertIsDisplayed()
            .performClick()
        composeRule.onNodeWithText("脚注内容").assertIsDisplayed()
        composeRule.onNodeWithText("↩").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("正文开头脚注", substring = true).assertIsDisplayed()
    }

    @Test
    fun answerEndorsementsRenderOffline() {
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

        composeRule.onNodeWithText("话题收录 我的开源名片").assertIsDisplayed()
        composeRule.onNodeWithText("创作声明: 内容包含剧透").assertIsDisplayed()
        composeRule.onNodeWithText("收录于话题: 科技").assertIsDisplayed()
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
    fun pausedContinuousReadingOnAnotherQueueItemUsesStopActionInArticleMenu() {
        val viewModel = seededAnswerViewModel(ANSWER)
        AndroidReadingPlayerBridge.publish(
            ReadingPlayerState(
                status = ReadingPlaybackStatus.Paused,
                queue = listOf(
                    ReadingQueueItem(
                        contentType = ReadingContentType.Answer,
                        id = ANSWER.id,
                        title = "离线 Answer 标题",
                        author = "离线答主",
                    ),
                    ReadingQueueItem(
                        contentType = ReadingContentType.Answer,
                        id = NEXT_ANSWER.id,
                        title = "下一个离线回答",
                        author = "下一个作者",
                    ),
                ),
                currentIndex = 1,
            ),
        )
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

        composeRule.onNodeWithContentDescription("更多选项").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("停止朗读").assertIsDisplayed()
        composeRule.onNodeWithText("暂停朗读").assertDoesNotExist()
        composeRule.onNodeWithText("继续朗读").assertDoesNotExist()
        composeRule.onNodeWithText("停止朗读").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            !AndroidReadingPlayerBridge.state.value.hasSession
        }
    }

    @Test
    fun emptyAnswerQueueProviderDoesNotFallBackToPaginationIds() {
        val viewModel = seededAnswerViewModel(ANSWER)
        composeRule.runOnIdle {
            viewModel.forceAnswerNextIdsForTest(listOf(901L, 902L))
        }
        setArticleActionsMenu(
            viewModel = viewModel,
            answerQueueFallbackProvider = { _ -> emptyList() },
        )

        composeRule.onNodeWithText("开始连续朗读").assertIsDisplayed().performClick()
        waitForReadingQueue(listOf(ANSWER.id))

        composeRule.onNodeWithText("停止朗读").assertIsDisplayed().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            !AndroidReadingPlayerBridge.state.value.hasSession
        }
    }

    @Test
    fun answerQueueProviderKeepsCollectionOrderWithoutPaginationItems() {
        val viewModel = seededAnswerViewModel(ANSWER)
        composeRule.runOnIdle {
            viewModel.forceAnswerNextIdsForTest(listOf(901L, 902L))
        }
        setArticleActionsMenu(
            viewModel = viewModel,
            answerQueueFallbackProvider = { _ ->
                listOf(
                    Article(type = ArticleType.Answer, id = 801L, title = "收藏回答一"),
                    Article(type = ArticleType.Answer, id = 802L, title = "收藏回答二"),
                )
            },
        )

        composeRule.onNodeWithText("开始连续朗读").assertIsDisplayed().performClick()
        waitForReadingQueue(listOf(ANSWER.id, 801L, 802L))

        composeRule.onNodeWithText("停止朗读").assertIsDisplayed().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            !AndroidReadingPlayerBridge.state.value.hasSession
        }
    }

    @Test
    fun failingAnswerQueueProviderStillStartsCurrentAnswer() {
        val viewModel = seededAnswerViewModel(ANSWER)
        setArticleActionsMenu(
            viewModel = viewModel,
            answerQueueFallbackProvider = { error("离线分页失败") },
        )

        composeRule.onNodeWithText("开始连续朗读").assertIsDisplayed().performClick()
        waitForReadingQueue(listOf(ANSWER.id))
    }

    @Test
    fun matchingOriginAtQueueLimitDoesNotLoadQuestionFallback() {
        val viewModel = seededAnswerViewModel(ANSWER)
        val sourceId = FULL_ORIGIN_SOURCE_ID
        val sourceAnswer = ANSWER.copy(readingQueueSourceId = sourceId)
        var providerCalls = 0
        ReadingQueueSourceRegistry.register(
            sourceId = sourceId,
            items = listOf(
                ReadingQueueItem(ReadingContentType.Answer, id = ANSWER.id),
                ReadingQueueItem(ReadingContentType.Answer, id = NEXT_ANSWER.id),
                ReadingQueueItem(ReadingContentType.Answer, id = 779L),
                ReadingQueueItem(ReadingContentType.Answer, id = 780L),
                ReadingQueueItem(ReadingContentType.Answer, id = 781L),
            ),
        )
        setArticleActionsMenu(
            viewModel = viewModel,
            article = sourceAnswer,
            answerQueueFallbackProvider = {
                providerCalls++
                error("来源队列足够时不应加载 fallback")
            },
        )

        composeRule.onNodeWithText("开始连续朗读").assertIsDisplayed().performClick()
        waitForReadingQueue(listOf(ANSWER.id, NEXT_ANSWER.id, 779L, 780L, 781L))
        composeRule.runOnIdle { assertEquals(0, providerCalls) }
    }

    @Test
    fun partialMatchingOriginLoadsRequestedRemainderAndFillsQueue() {
        val viewModel = seededAnswerViewModel(ANSWER)
        val sourceId = PARTIAL_ORIGIN_SOURCE_ID
        val sourceAnswer = ANSWER.copy(readingQueueSourceId = sourceId)
        var requestedLimit = 0
        ReadingQueueSourceRegistry.register(
            sourceId = sourceId,
            items = listOf(
                ReadingQueueItem(ReadingContentType.Answer, id = ANSWER.id),
                ReadingQueueItem(ReadingContentType.Answer, id = NEXT_ANSWER.id),
            ),
        )
        setArticleActionsMenu(
            viewModel = viewModel,
            article = sourceAnswer,
            answerQueueFallbackProvider = { limit ->
                requestedLimit = limit
                listOf(
                    NEXT_ANSWER,
                    Article(type = ArticleType.Answer, id = 779L),
                    Article(type = ArticleType.Answer, id = 780L),
                    Article(type = ArticleType.Answer, id = 781L),
                )
            },
        )

        composeRule.onNodeWithText("开始连续朗读").assertIsDisplayed().performClick()
        waitForReadingQueue(listOf(ANSWER.id, NEXT_ANSWER.id, 779L, 780L, 781L))
        composeRule.runOnIdle { assertEquals(4, requestedLimit) }
    }

    @Test
    fun cancelledAnswerQueueProviderDoesNotStartReadingSession() {
        val viewModel = seededAnswerViewModel(ANSWER)
        val providerCalled = AtomicBoolean(false)
        setArticleActionsMenu(
            viewModel = viewModel,
            answerQueueFallbackProvider = {
                providerCalled.set(true)
                throw CancellationException("测试取消")
            },
        )

        composeRule.onNodeWithText("开始连续朗读").assertIsDisplayed().performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) { providerCalled.get() }

        assertFalse(AndroidReadingPlayerBridge.state.value.hasSession)
    }

    @Test
    fun articleScreenUsesSharedAnswerNavigatorSnapshotForReadingQueue() {
        val viewModel = seededAnswerViewModel(ANSWER)
        val snapshotCurrentId = AtomicLong(-1L)
        val snapshotLimit = AtomicInteger(0)
        val sharedNavigator = object : AnswerNavigator(
            sourceName = "此问题",
            environment = NO_OP_API_ENVIRONMENT,
        ) {
            override suspend fun loadNext(): Article? = null

            override suspend fun prefetchNext(currentArticleId: Long) = Unit

            override suspend fun remainingAnswersSnapshot(
                currentArticleId: Long,
                limit: Int,
            ): List<Article> {
                snapshotCurrentId.set(currentArticleId)
                snapshotLimit.set(limit)
                return listOf(
                    NEXT_ANSWER,
                    Article(type = ArticleType.Answer, id = 779L),
                ).take(limit)
            }
        }
        composeRule.activity.runOnUiThread {
            composeRule.activity.articleAnswerSwitchState.pendingNavigator = sharedNavigator
        }
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

        composeRule.onNodeWithContentDescription("更多选项").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("开始连续朗读").assertIsDisplayed().performClick()

        waitForReadingQueue(listOf(ANSWER.id, NEXT_ANSWER.id, 779L))
        assertEquals(ANSWER.id, snapshotCurrentId.get())
        assertEquals(4, snapshotLimit.get())
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

    private fun setArticleActionsMenu(
        viewModel: ArticleViewModel,
        article: Article = ANSWER,
        answerQueueFallbackProvider: suspend (limit: Int) -> List<Article>,
    ) {
        composeRule.setScreenContent {
            Scaffold(
                modifier = androidx.compose.ui.Modifier
                    .fillMaxSize(),
            ) { _ ->
                ArticleActionsMenu(
                    article = article,
                    viewModel = viewModel,
                    answerQueueFallbackProvider = answerQueueFallbackProvider,
                    showMenu = true,
                    onDismissRequest = {},
                    onSummaryRequest = {},
                    onAigcFlagRequest = {},
                    onExportRequest = {},
                )
            }
        }
    }

    private fun waitForReadingQueue(expectedIds: List<Long>) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            AndroidReadingPlayerBridge.state.value.queue
                .map(ReadingQueueItem::id) == expectedIds
        }
        assertEquals(
            expectedIds,
            AndroidReadingPlayerBridge.state.value.queue
                .map(ReadingQueueItem::id),
        )
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

    private fun issue495ViewModel(): ArticleViewModel {
        val viewModel = seededAnswerViewModel(ANSWER)
        val html = InstrumentationRegistry
            .getInstrumentation()
            .context
            .assets
            .open("issue-495-answer.html")
            .bufferedReader()
            .use { it.readText() }
        composeRule.activity.runOnUiThread {
            viewModel.content = html
        }
        return viewModel
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
            viewModel.endorsements = listOf(
                DataHolder.AnswerEndorsementDisplay(
                    text = "话题收录 我的开源名片",
                    backgroundColor = DataHolder.AnswerEndorsementColor(alpha = 0.1f, group = "GYL02A"),
                    textColor = DataHolder.AnswerEndorsementColor(group = "GYL02A"),
                    leadingIconKey = "zhicon_icon_24_chat_bubble_hash_fill",
                    leadingIconColor = DataHolder.AnswerEndorsementColor(group = "GYL02A"),
                    trailingIconKey = "zhicon_icon_16_arrow_right",
                ),
                DataHolder.AnswerEndorsementDisplay(
                    text = "创作声明: 内容包含剧透",
                    backgroundColor = DataHolder.AnswerEndorsementColor(alpha = 0.1f, group = "GBL01A"),
                    textColor = DataHolder.AnswerEndorsementColor(group = "GBL07A"),
                    trailingIconKey = "zhicon_icon_16_arrow_down",
                ),
                DataHolder.AnswerEndorsementDisplay(
                    text = "收录于话题: 科技",
                ),
            )
        }
        return viewModel
    }

    @Suppress("UNCHECKED_CAST")
    private fun MainActivity.forceTtsStateForTest(state: TtsState) {
        val ttsStateField = MainActivity::class.java.getDeclaredField("_ttsState")
        ttsStateField.isAccessible = true
        (ttsStateField.get(this) as MutableState<TtsState>).value = state
    }

    private fun ArticleViewModel.forceAnswerNextIdsForTest(ids: List<Long>) {
        val setter = ArticleViewModel::class.java.getDeclaredMethod("setAnswerNextIds", List::class.java)
        setter.isAccessible = true
        setter.invoke(this, ids)
    }

    private companion object {
        const val FULL_ORIGIN_SOURCE_ID = "instrumented:reading-origin"
        const val PARTIAL_ORIGIN_SOURCE_ID = "instrumented:partial-reading-origin"
        const val ISSUE_495_BENCHMARK_TAG = "Issue495Benchmark"
        const val ISSUE_495_FIRST_FRAME_LIMIT_MS = 5_000L

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
