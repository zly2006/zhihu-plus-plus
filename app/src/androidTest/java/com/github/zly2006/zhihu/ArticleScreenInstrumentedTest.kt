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
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import android.view.InputDevice
import android.view.MotionEvent
import androidx.compose.foundation.ComposeFoundationFlags
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.NativeClipboard
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.markdown.RenderImage
import com.github.zly2006.zhihu.markdown.RenderMarkdown
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
import com.github.zly2006.zhihu.test.MainActivityComposeRule
import com.github.zly2006.zhihu.test.resetAppPreferences
import com.github.zly2006.zhihu.test.setScreenContent
import com.github.zly2006.zhihu.ui.ARTICLE_USE_WEBVIEW_PREFERENCE_KEY
import com.github.zly2006.zhihu.ui.AnswerDoubleTapAction
import com.github.zly2006.zhihu.ui.ArticleScreen
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import com.github.zly2006.zhihu.ui.TtsState
import com.github.zly2006.zhihu.ui.article.ArticleActionsMenu
import com.github.zly2006.zhihu.ui.rememberArticleTtsState
import com.github.zly2006.zhihu.viewmodel.ArticleViewModel
import com.github.zly2006.zhihu.viewmodel.ZhihuApiEnvironment
import com.hrm.markdown.renderer.Markdown
import com.hrm.markdown.renderer.MarkdownImageData
import io.ktor.client.HttpClient
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
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

    /**
     * Regression: https://github.com/zly2006/zhihu-plus-plus/issues/638
     * Fixed by: https://github.com/zly2006/zhihu-plus-plus/pull/640
     */
    @Test
    fun autoHideTitleRemainsResponsiveWhenDirectionChangesDuringHideAnimation() {
        setArticleScreen()

        val rootBounds = composeRule.onRoot().fetchSemanticsNode().boundsInRoot
        val instrumentation = InstrumentationRegistry.getInstrumentation()

        fun injectSwipe(
            startYFraction: Float,
            endYFraction: Float,
            durationMillis: Long,
        ) {
            val downTime = SystemClock.uptimeMillis()
            val x = rootBounds.center.x
            val startY = rootBounds.height * startYFraction
            val endY = rootBounds.height * endYFraction
            val steps = maxOf((durationMillis / 15).toInt(), 1)

            for (step in 0..steps) {
                val eventTime = SystemClock.uptimeMillis()
                val action = when (step) {
                    0 -> MotionEvent.ACTION_DOWN
                    steps -> MotionEvent.ACTION_UP
                    else -> MotionEvent.ACTION_MOVE
                }
                val fraction = step.toFloat() / steps
                val event = MotionEvent.obtain(
                    downTime,
                    eventTime,
                    action,
                    x,
                    startY + (endY - startY) * fraction,
                    0,
                )
                event.source = InputDevice.SOURCE_TOUCHSCREEN
                try {
                    assertTrue(instrumentation.uiAutomation.injectInputEvent(event, true))
                } finally {
                    event.recycle()
                }
                if (step < steps) SystemClock.sleep(durationMillis / steps)
            }
        }

        injectSwipe(0.64f, 0.49f, 600)
        repeat(4) {
            injectSwipe(0.57f, 0.51f, 90)
            injectSwipe(0.51f, 0.58f, 90)
        }

        val mainThreadResponsive = CountDownLatch(1)
        composeRule.activity.runOnUiThread { mainThreadResponsive.countDown() }
        assertTrue(
            "Main thread stopped responding after reversing scroll during title hide animation",
            mainThreadResponsive.await(3, TimeUnit.SECONDS),
        )

        injectSwipe(0.3f, 0.8f, 240)
        composeRule.waitForIdle()
        composeRule.onNodeWithContentDescription("更多选项").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("复制链接").assertIsDisplayed()
    }

    /**
     * Regression: https://github.com/zly2006/zhihu-plus-plus/issues/495
     * Fixed by: https://github.com/zly2006/zhihu-plus-plus/pull/556
     */
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

    /**
     * Regression: https://github.com/zly2006/zhihu-plus-plus/issues/495
     * Fixed by: https://github.com/zly2006/zhihu-plus-plus/pull/646
     */
    @Test
    fun markdownJvmToAvdCalibrationBenchmark() {
        assumeTrue(
            "Run explicitly with -e markdownPerformance true; normal functional suites should not occupy an AVD for calibration",
            InstrumentationRegistry.getArguments().getString("markdownPerformance") == "true",
        )
        val scenarios = linkedMapOf(
            "short-prose" to "一段普通正文，用于覆盖最常见的短回答。",
            "formatted-prose" to (1..30).joinToString("\n\n") { index ->
                "第 $index 段包含 **加粗**、*斜体*、~~删除线~~ 和 [链接](https://example.com/$index)。"
            },
            "block-math" to (1..80).joinToString("\n\n") { index ->
                "${'$'}${'$'}\\sum_{i=1}^{n} \\frac{x_i^{$index}}{1+x_i^2}${'$'}${'$'}"
            },
        )
        val markdown = mutableStateOf("calibration bootstrap")
        val scrollState = ScrollState(0)
        composeRule.setScreenContent {
            Markdown(
                markdown = markdown.value,
                modifier = androidx.compose.ui.Modifier
                    .fillMaxSize(),
                scrollState = scrollState,
                enableScroll = true,
                enableSelection = true,
            )
        }
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule
                .onAllNodesWithText("calibration bootstrap", substring = true, useUnmergedTree = true)
                .fetchSemanticsNodes(atLeastOneRootRequired = false)
                .isNotEmpty()
        }
        composeRule.onRoot().captureToImage()

        repeat(2) { warmup ->
            scenarios.forEach { (name, body) ->
                val marker = "$name warmup $warmup"
                composeRule.runOnUiThread { markdown.value = "$marker\n\n$body" }
                composeRule.waitUntil(timeoutMillis = 10_000) {
                    composeRule
                        .onAllNodesWithText(marker, substring = true, useUnmergedTree = true)
                        .fetchSemanticsNodes(atLeastOneRootRequired = false)
                        .isNotEmpty()
                }
                composeRule.onRoot().captureToImage()
            }
        }
        val medians = scenarios.mapValues { (name, body) ->
            val samples = List(7) { iteration ->
                val marker = "$name calibration $iteration"
                val startedAt = SystemClock.elapsedRealtimeNanos()
                composeRule.runOnUiThread { markdown.value = "$marker\n\n$body" }
                composeRule.waitUntil(timeoutMillis = 10_000) {
                    composeRule
                        .onAllNodesWithText(marker, substring = true, useUnmergedTree = true)
                        .fetchSemanticsNodes(atLeastOneRootRequired = false)
                        .isNotEmpty()
                }
                composeRule.waitForIdle()
                val elapsedMs = (SystemClock.elapsedRealtimeNanos() - startedAt) / 1_000_000.0
                composeRule.onRoot().captureToImage()
                elapsedMs
            }
            samples.sorted()[samples.size / 2].also { median ->
                Log.i(ISSUE_495_BENCHMARK_TAG, "calibrationScenario=$name samplesMs=$samples medianMs=$median")
            }
        }
        Log.i(ISSUE_495_BENCHMARK_TAG, "calibrationMediansMs=$medians")
    }

    /**
     * Regression: https://github.com/zly2006/zhihu-plus-plus/issues/495
     * Fixed by: https://github.com/zly2006/zhihu-plus-plus/pull/579
     */
    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun selectionSurvivesDeferredMarkdownViewDisposal() {
        val previousContextMenuFlag = ComposeFoundationFlags.isNewContextMenuEnabled
        ComposeFoundationFlags.isNewContextMenuEnabled = false
        try {
            val textToolbar = CapturingTextToolbar()
            val clipboard = RecordingClipboard()
            val selectionColor = Color.Magenta
            val firstParagraph = "FIRST_TARGET"
            val secondParagraph = "SECOND_TARGET"
            val codeBlock = "CODE_TARGET"
            val quoteBlock = "QUOTE_TARGET"
            val tableCell = "TABLE_TARGET"
            val thirdFromLastParagraph = "TAIL_THIRD_TARGET"
            val secondFromLastParagraph = "TAIL_PENULTIMATE_TARGET"
            val lastParagraph = "TAIL_FINAL_TARGET"
            val fillerParagraphs = (0 until 200).map { "FILLER_$it" }
            val markdown = buildString {
                appendLine(firstParagraph)
                appendLine()
                appendLine(secondParagraph)
                appendLine()
                fillerParagraphs.take(40).forEach {
                    appendLine(it)
                    appendLine()
                }
                appendLine("```text")
                appendLine(codeBlock)
                appendLine("```")
                appendLine()
                fillerParagraphs.drop(40).take(40).forEach {
                    appendLine(it)
                    appendLine()
                }
                appendLine("> $quoteBlock")
                appendLine()
                fillerParagraphs.drop(80).take(40).forEach {
                    appendLine(it)
                    appendLine()
                }
                appendLine("| 测试列 |")
                appendLine("| --- |")
                appendLine("| $tableCell |")
                appendLine()
                fillerParagraphs.drop(120).forEach {
                    appendLine(it)
                    appendLine()
                }
                appendLine(thirdFromLastParagraph)
                appendLine()
                appendLine(secondFromLastParagraph)
                appendLine()
                appendLine(lastParagraph)
            }
            composeRule.setScreenContent {
                CompositionLocalProvider(
                    LocalClipboard provides clipboard,
                    LocalTextToolbar provides textToolbar,
                    LocalTextSelectionColors provides TextSelectionColors(
                        handleColor = selectionColor,
                        backgroundColor = selectionColor,
                    ),
                ) {
                    RenderMarkdownText(markdown = markdown)
                }
            }

            val scrollContainer = composeRule.onNode(
                SemanticsMatcher("has vertical scroll axis") { node ->
                    node.config.contains(SemanticsProperties.VerticalScrollAxisRange)
                },
            )

            assertSelectionSurvivesDisposal(
                target = secondParagraph,
                additionallyDisposed = listOf(firstParagraph),
                awayToEnd = true,
                scrollContainer = scrollContainer,
                textToolbar = textToolbar,
                clipboard = clipboard,
            )

            // Visit the remaining renderer shapes once in document order. The paragraph above
            // exercises detach/reattach directly; the full-document selection below verifies that
            // code, quote, table and tail proxies retain their text after the same disposal.
            scrollToBoundary(scrollContainer, end = false)
            listOf(codeBlock, quoteBlock, tableCell, lastParagraph).forEach { target ->
                scrollForwardToText(scrollContainer, target)
            }

            // 逐项滚动已经让整篇文档的真实 BlockRenderer 至少注册过一次；全选必须覆盖全部
            // 内容，随后首部节点的销毁、重建和第二次销毁都不能改变高亮或复制结果。
            scrollToText(scrollContainer, secondParagraph)
            composeRule.onNodeWithText(secondParagraph).performTouchInput { longClick() }
            composeRule.runOnIdle {
                requireNotNull(textToolbar.onSelectAllRequested).invoke()
            }
            waitUntilSelectionHighlight(
                text = secondParagraph,
                failureMessage = "Select all did not highlight the second paragraph",
            )
            scrollToBoundary(scrollContainer, end = true)
            composeRule.onNodeWithText(firstParagraph).assertDoesNotExist()
            composeRule.onNodeWithText(secondParagraph).assertDoesNotExist()

            scrollToText(scrollContainer, secondParagraph)
            waitUntilSelectionHighlight(
                text = secondParagraph,
                failureMessage = "Full-document selection disappeared after the second paragraph was recreated",
            )
            val expectedUniqueTexts = listOf(
                firstParagraph,
                secondParagraph,
                codeBlock,
                quoteBlock,
                tableCell,
                thirdFromLastParagraph,
                secondFromLastParagraph,
                lastParagraph,
            )
            val fullDocumentCopy = copySelection(textToolbar, clipboard)
            assertCompleteDocumentSelection(fullDocumentCopy, expectedUniqueTexts, fillerParagraphs)

            composeRule.onNodeWithText(secondParagraph).performTouchInput { longClick() }
            composeRule.runOnIdle {
                requireNotNull(textToolbar.onSelectAllRequested).invoke()
            }
            waitUntilSelectionHighlight(
                text = secondParagraph,
                failureMessage = "Repeating select all did not highlight the second paragraph",
            )
            scrollToBoundary(scrollContainer, end = true)
            composeRule.onNodeWithText(secondParagraph).assertDoesNotExist()
            val detachedFullDocumentCopy = copySelection(textToolbar, clipboard)
            assertCompleteDocumentSelection(detachedFullDocumentCopy, expectedUniqueTexts, fillerParagraphs)
            assertEquals(fullDocumentCopy, detachedFullDocumentCopy)
        } finally {
            ComposeFoundationFlags.isNewContextMenuEnabled = previousContextMenuFlag
        }
    }

    /**
     * Regression: https://github.com/zly2006/zhihu-plus-plus/issues/495
     * Fixed by: https://github.com/zly2006/zhihu-plus-plus/pull/646
     */
    @Test
    fun deferredMarkdownSaveStateUsesBundleSafeKeys() {
        val markdown = (0 until 80).joinToString("\n\n") { index -> "SAVEABLE_BLOCK_$index" }
        composeRule.setScreenContent {
            RenderMarkdownText(markdown = markdown)
        }
        composeRule.onNodeWithText("SAVEABLE_BLOCK_0").assertIsDisplayed()

        composeRule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        composeRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        composeRule.activityRule.scenario.recreate()
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
    }

    private fun waitUntilSelectionHighlight(
        text: String,
        failureMessage: String,
    ) {
        composeRule.waitUntil(failureMessage, timeoutMillis = 5_000) {
            runCatching {
                composeRule
                    .onNodeWithText(text)
                    .captureToImage()
                    .toPixelMap()
                    .let { pixels ->
                        (0 until pixels.height).any { y ->
                            (0 until pixels.width).any { x ->
                                val pixel = pixels[x, y]
                                pixel.red > 0.9f && pixel.blue > 0.9f && pixel.green < 0.1f
                            }
                        }
                    }
            }.getOrDefault(false)
        }
    }

    private fun copySelection(
        textToolbar: CapturingTextToolbar,
        clipboard: RecordingClipboard,
    ): String {
        clipboard.clear()
        composeRule.runOnIdle {
            requireNotNull(textToolbar.onCopyRequested).invoke()
        }
        composeRule.waitUntil(
            "Selection did not publish text to the Compose clipboard",
            timeoutMillis = 5_000,
        ) {
            clipboard.text != null
        }
        return requireNotNull(clipboard.text)
    }

    private fun assertSelectionSurvivesDisposal(
        target: String,
        awayToEnd: Boolean,
        scrollContainer: SemanticsNodeInteraction,
        textToolbar: CapturingTextToolbar,
        clipboard: RecordingClipboard,
        additionallyDisposed: List<String> = emptyList(),
    ) {
        scrollToText(scrollContainer, target)
        composeRule.onNodeWithText(target).performTouchInput { longClick() }
        waitUntilSelectionHighlight(
            text = target,
            failureMessage = "Selection did not highlight $target before disposal",
        )

        scrollToBoundary(scrollContainer, end = awayToEnd)
        (listOf(target) + additionallyDisposed).forEach { disposedText ->
            composeRule.onNodeWithText(disposedText).assertDoesNotExist()
        }

        scrollToText(scrollContainer, target)
        waitUntilSelectionHighlight(
            text = target,
            failureMessage = "Selection on $target disappeared after its renderer was recreated",
        )
        val restoredCopy = copySelection(textToolbar, clipboard)
        assertTrue("Restored selection did not copy $target: $restoredCopy", restoredCopy.contains(target))

        composeRule.onNodeWithText(target).performTouchInput { longClick() }
        waitUntilSelectionHighlight(
            text = target,
            failureMessage = "Reselecting $target did not produce a visible selection",
        )

        scrollToBoundary(scrollContainer, end = awayToEnd)
        composeRule.onNodeWithText(target).assertDoesNotExist()
        val detachedCopy = copySelection(textToolbar, clipboard)
        assertTrue("Detached selection did not copy $target: $detachedCopy", detachedCopy.contains(target))
        assertEquals(restoredCopy, detachedCopy)
    }

    private fun scrollToText(
        scrollContainer: SemanticsNodeInteraction,
        text: String,
    ) {
        scrollToBoundary(scrollContainer, end = false)
        scrollForwardToText(scrollContainer, text)
    }

    private fun scrollForwardToText(
        scrollContainer: SemanticsNodeInteraction,
        text: String,
    ) {
        val scrollStep = scrollContainer
            .fetchSemanticsNode()
            .boundsInRoot
            .height
        repeat(80) {
            val target = composeRule.onNodeWithText(text)
            if (
                composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() &&
                runCatching { target.assertIsDisplayed() }.isSuccess
            ) {
                return
            }
            scrollContainer.performSemanticsAction(SemanticsActions.ScrollBy) { scrollBy ->
                scrollBy(0f, scrollStep)
            }
            composeRule.waitForIdle()
        }
        assertTrue("Markdown did not materialize $text while scrolling", false)
    }

    private fun scrollToBoundary(
        scrollContainer: SemanticsNodeInteraction,
        end: Boolean,
    ) {
        repeat(80) {
            val range = scrollContainer
                .fetchSemanticsNode()
                .config[SemanticsProperties.VerticalScrollAxisRange]
            val reachedBoundary = if (end) {
                range.maxValue() - range.value() <= 1f
            } else {
                range.value() <= 1f
            }
            if (reachedBoundary) return
            scrollContainer.performSemanticsAction(SemanticsActions.ScrollBy) { scrollBy ->
                scrollBy(0f, if (end) 4_000f else -4_000f)
            }
            composeRule.waitForIdle()
        }
        assertTrue("Markdown did not reach the ${if (end) "end" else "start"}", false)
    }

    private fun assertCompleteDocumentSelection(
        copiedText: String,
        expectedUniqueTexts: List<String>,
        fillerParagraphs: List<String>,
    ) {
        expectedUniqueTexts.forEach { expected ->
            assertEquals(
                "$expected must occur exactly once in the full-document copy",
                1,
                Regex(Regex.escape(expected)).findAll(copiedText).count(),
            )
        }
        val copiedFillerIndexes = Regex("FILLER_(\\d+)")
            .findAll(copiedText)
            .map { it.groupValues[1].toInt() }
            .toList()
        assertEquals(fillerParagraphs.indices.toList(), copiedFillerIndexes)
    }

    /**
     * Regression: https://github.com/zly2006/zhihu-plus-plus/issues/322
     * Fixed by: https://github.com/zly2006/zhihu-plus-plus/pull/584
     */
    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun highlightedParagraphRemainsSelectable() {
        val previousContextMenuFlag = ComposeFoundationFlags.isNewContextMenuEnabled
        ComposeFoundationFlags.isNewContextMenuEnabled = false
        try {
            val textToolbar = CapturingTextToolbar()
            val clipboard = RecordingClipboard()
            val selectionColor = Color.Magenta
            composeRule.setScreenContent {
                CompositionLocalProvider(
                    LocalClipboard provides clipboard,
                    LocalTextToolbar provides textToolbar,
                    LocalTextSelectionColors provides TextSelectionColors(
                        handleColor = selectionColor,
                        backgroundColor = selectionColor,
                    ),
                ) {
                    RenderMarkdown(
                        html = HIGHLIGHTED_PARAGRAPH_HTML,
                        modifier = androidx.compose.ui.Modifier
                            .width(280.dp)
                            .testTag("highlighted-selection-article"),
                        enableScroll = false,
                    )
                }
            }

            composeRule
                .onNodeWithText(HIGHLIGHTED_PARAGRAPH)
                .performTouchInput { longClick() }
            composeRule.onNodeWithText("划线片段").assertDoesNotExist()

            val selectionImage = composeRule
                .onNodeWithTag("highlighted-selection-article")
                .captureToImage()
            val selectedPixels = selectionImage.toPixelMap().let { pixels ->
                (0 until pixels.height).sumOf { y ->
                    (0 until pixels.width).count { x ->
                        val color = pixels[x, y]
                        color.red > 0.9f && color.green < 0.1f && color.blue > 0.9f
                    }
                }
            }
            assertTrue(
                "A long press on a highlighted paragraph must draw a visible selection background; found $selectedPixels selected pixels",
                selectedPixels >= 100,
            )
            composeRule.runOnIdle {
                requireNotNull(textToolbar.onSelectAllRequested).invoke()
            }

            val copiedText = copySelection(textToolbar, clipboard)
            assertEquals(HIGHLIGHTED_PARAGRAPH, copiedText)
        } finally {
            ComposeFoundationFlags.isNewContextMenuEnabled = previousContextMenuFlag
        }
    }

    /**
     * Regression: https://github.com/zly2006/zhihu-plus-plus/issues/322
     * Fixed by: https://github.com/zly2006/zhihu-plus-plus/pull/584
     */
    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun highlightedParagraphSelectionHandleCanExtendToFollowingParagraph() {
        val previousContextMenuFlag = ComposeFoundationFlags.isNewContextMenuEnabled
        ComposeFoundationFlags.isNewContextMenuEnabled = false
        try {
            val textToolbar = CapturingTextToolbar()
            val clipboard = RecordingClipboard()
            val selectionColor = Color.Magenta
            composeRule.setScreenContent {
                CompositionLocalProvider(
                    LocalClipboard provides clipboard,
                    LocalTextToolbar provides textToolbar,
                    LocalTextSelectionColors provides TextSelectionColors(
                        handleColor = selectionColor,
                        backgroundColor = selectionColor,
                    ),
                ) {
                    RenderMarkdown(
                        html = "$HIGHLIGHTED_PARAGRAPH_HTML<p>$HIGHLIGHT_SELECTION_TARGET</p>",
                        modifier = androidx.compose.ui.Modifier
                            .width(280.dp)
                            .testTag("highlighted-selection-drag-article"),
                        enableScroll = false,
                    )
                }
            }

            composeRule
                .onNodeWithText(HIGHLIGHTED_PARAGRAPH)
                .performTouchInput { longClick() }
            val endHandle = composeRule.onNode(
                SemanticsMatcher("是划线段落选区末端手柄") { node ->
                    node.config.any { (key, value) ->
                        key.name == "SelectionHandleInfo" && value.toString().contains("SelectionEnd")
                    }
                },
            )
            val targetBounds = composeRule
                .onNodeWithText(HIGHLIGHT_SELECTION_TARGET)
                .fetchSemanticsNode()
                .boundsInWindow
            val handleBounds = endHandle.fetchSemanticsNode().boundsInWindow
            endHandle.performTouchInput {
                val start = center
                val destination = Offset(
                    x = targetBounds.right - handleBounds.left - 1f,
                    y = targetBounds.bottom - handleBounds.top - 1f,
                )
                down(start)
                advanceEventTime(100)
                repeat(10) { step ->
                    val fraction = (step + 1) / 10f
                    moveTo(
                        start + (destination - start) * fraction,
                        delayMillis = 50,
                    )
                }
                up()
            }
            val selectedPixels = composeRule
                .onNodeWithTag("highlighted-selection-drag-article")
                .captureToImage()
                .toPixelMap()
                .let { pixels ->
                    (0 until pixels.height).sumOf { y ->
                        (0 until pixels.width).count { x ->
                            val color = pixels[x, y]
                            color.red > 0.9f && color.green < 0.1f && color.blue > 0.9f
                        }
                    }
                }
            assertTrue(
                "Dragging out of a highlighted paragraph must keep the cross-block selection visible; found $selectedPixels selected pixels",
                selectedPixels >= 1_000,
            )
            waitUntilSelectionHighlight(
                text = HIGHLIGHT_SELECTION_TARGET,
                failureMessage = "Selection did not reach the following block after dragging the handle",
            )
            val copiedText = copySelection(textToolbar, clipboard)
            assertTrue(
                "The standard selection handle must extend from a highlighted paragraph into the following block",
                copiedText.contains(HIGHLIGHT_SELECTION_TARGET),
            )
        } finally {
            ComposeFoundationFlags.isNewContextMenuEnabled = previousContextMenuFlag
        }
    }

    /**
     * Regression: https://github.com/zly2006/zhihu-plus-plus/issues/322
     * Fixed by: https://github.com/zly2006/zhihu-plus-plus/pull/584
     * Later tap regression: https://github.com/zly2006/zhihu-plus-plus/issues/595
     * Direct fix: https://github.com/zly2006/zhihu-plus-plus/commit/bc79ed18
     */
    @Test
    fun highlightedParagraphTapStillOpensActions() {
        composeRule.setScreenContent {
            RenderMarkdown(
                html = HIGHLIGHTED_PARAGRAPH_HTML,
                enableScroll = false,
            )
        }

        composeRule
            .onNodeWithText(HIGHLIGHTED_PARAGRAPH)
            .performTouchInput { click() }
        composeRule.onNodeWithText("划线片段").assertIsDisplayed()
        composeRule.onNodeWithText("“$HIGHLIGHTED_PARAGRAPH”").assertIsDisplayed()
        composeRule.onNodeWithTag("segment_action_sheet_top_divider").assertDoesNotExist()
        composeRule.onNodeWithTag("segment_action_sheet_bottom_divider").assertDoesNotExist()
    }

    /**
     * Regression: https://github.com/zly2006/zhihu-plus-plus/issues/595
     * Fixed by: https://github.com/zly2006/zhihu-plus-plus/pull/683
     */
    @Test
    fun spanningHighlightTapShowsTheCompleteSelection() {
        composeRule.setScreenContent {
            RenderMarkdown(
                html = SPANNING_HIGHLIGHT_HTML,
                enableScroll = false,
            )
        }

        composeRule
            .onNodeWithText(SPANNING_HIGHLIGHT_SECOND)
            .performTouchInput { click() }

        composeRule.onNodeWithText("划线片段").assertIsDisplayed()
        composeRule
            .onNodeWithText("“$SPANNING_HIGHLIGHT_FIRST\n\n$SPANNING_HIGHLIGHT_SECOND”")
            .assertIsDisplayed()
    }

    /**
     * Regression: https://github.com/zly2006/zhihu-plus-plus/issues/595
     * Fixed by: https://github.com/zly2006/zhihu-plus-plus/pull/683
     */
    @Test
    fun longSpanningHighlightShowsDirectionalDividersAndKeepsActionsVisible() {
        val repeatedParagraphs = List(24) { SPANNING_HIGHLIGHT_FIRST }
        val longDisplayText = repeatedParagraphs.joinToString("\n\n")
        val longDisplayTextAttribute = repeatedParagraphs.joinToString("&#10;&#10;")
        val html = SPANNING_HIGHLIGHT_HTML.replace(
            "$SPANNING_HIGHLIGHT_FIRST&#10;&#10;$SPANNING_HIGHLIGHT_SECOND",
            longDisplayTextAttribute,
        )
        composeRule.setScreenContent {
            RenderMarkdown(
                html = html,
                enableScroll = false,
            )
        }

        composeRule
            .onNodeWithText(SPANNING_HIGHLIGHT_SECOND)
            .performTouchInput { click() }

        val text = composeRule.onNodeWithText("“$longDisplayText”")
        text.fetchSemanticsNode()
        composeRule.onNodeWithTag("segment_action_sheet_top_divider").assertDoesNotExist()
        composeRule.onNodeWithTag("segment_action_sheet_bottom_divider").assertIsDisplayed()
        composeRule.onNodeWithText("15").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("复制内容").assertIsDisplayed()

        scrollToBoundary(text, end = true)
        val finalScrollRange = text
            .fetchSemanticsNode()
            .config[SemanticsProperties.VerticalScrollAxisRange]
        assertTrue("The expanded sheet must still have overflowing text", finalScrollRange.maxValue() > 0f)

        composeRule.onNodeWithTag("segment_action_sheet_top_divider").assertIsDisplayed()
        composeRule.onNodeWithTag("segment_action_sheet_bottom_divider").assertDoesNotExist()
        composeRule.onNodeWithText("15").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("复制内容").assertIsDisplayed()
    }

    /**
     * Regression: https://github.com/zly2006/zhihu-plus-plus/issues/595
     * Direct fix: https://github.com/zly2006/zhihu-plus-plus/commit/bc79ed18
     * Later audited by: https://github.com/zly2006/zhihu-plus-plus/pull/683
     */
    @Test
    fun highlightedTextUsesVisibleLayoutForTapTarget() {
        composeRule.setScreenContent {
            RenderMarkdown(
                html = FORMATTED_HIGHLIGHT_PARAGRAPH_HTML,
                modifier = androidx.compose.ui.Modifier
                    .width(240.dp),
                enableScroll = false,
            )
        }

        val paragraph = composeRule.onNodeWithText(FORMATTED_HIGHLIGHT_PARAGRAPH)
        var highlightCenter = Offset.Unspecified
        paragraph.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { getTextLayoutResult ->
            val results = mutableListOf<TextLayoutResult>()
            assertTrue(getTextLayoutResult(results))
            highlightCenter = results
                .single()
                .getBoundingBox(FORMATTED_HIGHLIGHT_PREFIX.length)
                .center
        }
        paragraph.performTouchInput { click(highlightCenter) }

        composeRule.onNodeWithText("划线片段").assertIsDisplayed()
        composeRule.onNodeWithText("“$FORMATTED_HIGHLIGHT”").assertIsDisplayed()
    }

    /**
     * Regression: https://github.com/zly2006/zhihu-plus-plus/issues/271
     * Fixed by: https://github.com/zly2006/zhihu-plus-plus/pull/629
     */
    @Test
    fun highlightedTextDrawsDashesAcrossEveryWrappedLine() {
        composeRule.setScreenContent {
            MaterialTheme(
                colorScheme = lightColorScheme(
                    outlineVariant = Color.Magenta,
                ),
            ) {
                RenderMarkdown(
                    html = WRAPPED_HIGHLIGHT_PARAGRAPH_HTML,
                    modifier = androidx.compose.ui.Modifier
                        .width(220.dp)
                        .testTag("wrapped-highlight-article"),
                    enableScroll = false,
                )
            }
        }

        val paragraph = composeRule.onNodeWithText(WRAPPED_HIGHLIGHT_PARAGRAPH)
        val layouts = mutableListOf<TextLayoutResult>()
        paragraph.performSemanticsAction(SemanticsActions.GetTextLayoutResult) { getTextLayoutResult ->
            assertTrue(getTextLayoutResult(layouts))
        }
        val layout = layouts.single()
        val highlightStart = WRAPPED_HIGHLIGHT_PREFIX.length
        val highlightEnd = highlightStart + WRAPPED_HIGHLIGHT.length
        val startLine = layout.getLineForOffset(highlightStart)
        val endLine = layout.getLineForOffset(highlightEnd - 1)
        assertTrue("Fixture must wrap the highlighted text onto at least three lines", endLine - startLine >= 2)

        val image = composeRule
            .onNodeWithTag("wrapped-highlight-article")
            .captureToImage()
        val output = File(
            requireNotNull(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null)),
            "segment-highlight-wrapped.png",
        )
        FileOutputStream(output).use { stream ->
            image.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, stream)
        }

        val pixels = image.toPixelMap()
        for (line in startLine..endLine) {
            val top = (layout.getLineBottom(line) - 6f).toInt().coerceAtLeast(0)
            val bottom = (layout.getLineBottom(line) + 2f).toInt().coerceAtMost(pixels.height - 1)
            val magentaPixels = (top..bottom).sumOf { y ->
                (0 until pixels.width).count { x ->
                    val color = pixels[x, y]
                    color.red > 0.8f && color.green < 0.2f && color.blue > 0.8f
                }
            }
            assertTrue(
                "Highlighted visual line $line must contain visible dash pixels; found $magentaPixels. Screenshot: ${output.absolutePath}",
                magentaPixels >= 4,
            )
        }
    }

    /**
     * Regression: https://github.com/zly2006/zhihu-plus-plus/issues/595
     * Direct fix: https://github.com/zly2006/zhihu-plus-plus/commit/bc79ed18
     * Later audited by: https://github.com/zly2006/zhihu-plus-plus/pull/683
     */
    @Test
    fun highlightedParagraphTapOpensActionsInsideAnswerScreen() {
        val viewModel = seededAnswerViewModel(ANSWER)
        composeRule.activity.runOnUiThread {
            viewModel.content = HIGHLIGHTED_PARAGRAPH_HTML
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

        composeRule
            .onNodeWithText(HIGHLIGHTED_PARAGRAPH)
            .performTouchInput { click() }
        composeRule.onNodeWithText("划线片段").assertIsDisplayed()
        composeRule.onNodeWithText("“$HIGHLIGHTED_PARAGRAPH”").assertIsDisplayed()
    }

    /**
     * Regression: https://github.com/zly2006/zhihu-plus-plus/issues/595
     * Direct fix: https://github.com/zly2006/zhihu-plus-plus/commit/bc79ed18
     * Later audited by: https://github.com/zly2006/zhihu-plus-plus/pull/683
     */
    @Test
    fun highlightedParagraphDragDoesNotOpenActions() {
        composeRule.setScreenContent {
            RenderMarkdown(
                html = HIGHLIGHTED_PARAGRAPH_HTML,
                enableScroll = false,
            )
        }

        composeRule
            .onNodeWithText(HIGHLIGHTED_PARAGRAPH)
            .performTouchInput {
                down(center)
                moveBy(Offset(0f, -100f))
                up()
            }
        composeRule.onNodeWithText("划线片段").assertDoesNotExist()
    }

    /**
     * Regression: https://github.com/zly2006/zhihu-plus-plus/issues/495
     * Fixed by: https://github.com/zly2006/zhihu-plus-plus/pull/579
     */
    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun selectAllHighlightsEveryVisualLineOfLongParagraph() {
        val previousContextMenuFlag = ComposeFoundationFlags.isNewContextMenuEnabled
        ComposeFoundationFlags.isNewContextMenuEnabled = false
        try {
            val textToolbar = CapturingTextToolbar()
            val selectionColor = Color.Magenta
            val paragraph = "长段落的选中背景必须跟随真实换行，".repeat(12)
            composeRule.setScreenContent {
                CompositionLocalProvider(
                    LocalTextToolbar provides textToolbar,
                    LocalTextSelectionColors provides TextSelectionColors(
                        handleColor = selectionColor,
                        backgroundColor = selectionColor,
                    ),
                ) {
                    RenderMarkdownText(
                        markdown = paragraph,
                        modifier = androidx.compose.ui.Modifier
                            .width(240.dp)
                            .testTag("multiline-selection-article"),
                        enableScroll = false,
                    )
                }
            }

            composeRule
                .onNodeWithText("长段落的选中背景", substring = true)
                .performTouchInput { longClick() }
            composeRule.runOnIdle {
                requireNotNull(textToolbar.onSelectAllRequested).invoke()
            }

            val selectionImage = composeRule
                .onNodeWithTag("multiline-selection-article")
                .captureToImage()
            val screenshot = File(
                requireNotNull(InstrumentationRegistry.getInstrumentation().targetContext.getExternalFilesDir(null)),
                "markdown-native-selection.png",
            )
            FileOutputStream(screenshot).use { stream ->
                selectionImage.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
            val pixels = selectionImage.toPixelMap()
            val highlightedRows = (0 until pixels.height).count { y ->
                var selectedPixels = 0
                for (x in 0 until pixels.width) {
                    val color = pixels[x, y]
                    if (color.red > 0.9f && color.green < 0.1f && color.blue > 0.9f) {
                        selectedPixels++
                    }
                }
                selectedPixels >= 100
            }
            Log.i("MarkdownSelection", "multilineSelectionHighlightedRows=$highlightedRows")
            assertTrue(
                "Select-all highlight only covered $highlightedRows pixel rows; a wrapped paragraph must highlight every line. " +
                    "Screenshot: ${screenshot.absolutePath}",
                highlightedRows >= 180,
            )
        } finally {
            ComposeFoundationFlags.isNewContextMenuEnabled = previousContextMenuFlag
        }
    }

    /**
     * Regression: https://github.com/zly2006/zhihu-plus-plus/issues/495
     * Fixed by: https://github.com/zly2006/zhihu-plus-plus/pull/579
     */
    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun draggingSelectionHandleUsesSameCompleteTextLayer() {
        val previousContextMenuFlag = ComposeFoundationFlags.isNewContextMenuEnabled
        ComposeFoundationFlags.isNewContextMenuEnabled = false
        try {
            val textToolbar = CapturingTextToolbar()
            val clipboard = RecordingClipboard()
            composeRule.setScreenContent {
                CompositionLocalProvider(
                    LocalClipboard provides clipboard,
                    LocalTextToolbar provides textToolbar,
                    LocalTextSelectionColors provides TextSelectionColors(
                        handleColor = Color.Magenta,
                        backgroundColor = Color.Magenta,
                    ),
                ) {
                    RenderMarkdownText(
                        markdown =
                            """
                            起始段落从这里开始拖动。

                            中间段落确保选区跨越多个文字块。

                            末段拖动必须到达这里。
                            """.trimIndent(),
                        modifier = androidx.compose.ui.Modifier
                            .width(280.dp),
                        enableScroll = false,
                    )
                }
            }

            composeRule
                .onNodeWithText("起始段落从这里开始拖动。")
                .performTouchInput { longClick() }

            // AndroidX 的手柄语义 key 仍是 internal，测试按 key 名匹配真实弹出层，
            // 避免另写一套选区计算来假装验证拖动。
            // https://cs.android.com/androidx/platform/frameworks/support/+/androidx-main:compose/foundation/foundation/src/commonMain/kotlin/androidx/compose/foundation/text/selection/SelectionHandles.kt
            val endHandle = composeRule.onNode(
                SemanticsMatcher("是选区末端手柄") { node ->
                    node.config.any { (key, value) ->
                        key.name == "SelectionHandleInfo" && value.toString().contains("SelectionEnd")
                    }
                },
            )
            val targetBounds = composeRule
                .onNodeWithText("末段拖动必须到达这里。")
                .fetchSemanticsNode()
                .boundsInWindow
            val handleBounds = endHandle.fetchSemanticsNode().boundsInWindow
            endHandle.performTouchInput {
                val start = center
                val destination = Offset(
                    x = targetBounds.right - handleBounds.left - 1f,
                    y = targetBounds.bottom - handleBounds.top - 1f,
                )
                down(start)
                advanceEventTime(100)
                repeat(10) { step ->
                    val fraction = (step + 1) / 10f
                    moveTo(
                        start + (destination - start) * fraction,
                        delayMillis = 50,
                    )
                }
                up()
            }
            waitUntilSelectionHighlight(
                text = "末段拖动必须到达这里。",
                failureMessage = "Selection did not reach the final block after dragging the handle",
            )
            val copiedText = copySelection(textToolbar, clipboard)
            assertTrue(
                "Dragging the standard selection handle must reach later blocks through the same selection layer",
                copiedText.contains("起始段落") && copiedText.contains("末段拖动必须到达这里"),
            )
        } finally {
            ComposeFoundationFlags.isNewContextMenuEnabled = previousContextMenuFlag
        }
    }

    /**
     * Regression: https://github.com/zly2006/zhihu-plus-plus/issues/495
     * Fixed by: https://github.com/zly2006/zhihu-plus-plus/pull/556
     */
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

    /**
     * Regression: https://github.com/zly2006/zhihu-plus-plus/issues/320
     * Fixed by: https://github.com/zly2006/zhihu-plus-plus/pull/576
     */
    @Test
    fun markdownImageReservesItsApiAspectRatioBeforeNetworkLoad() {
        composeRule.setScreenContent {
            RenderImage(
                data = MarkdownImageData(
                    url = "https://invalid.invalid/not-loaded.jpg",
                    altText = "未加载的比例图片",
                    width = 1200,
                    height = 880,
                ),
                modifier = androidx.compose.ui.Modifier,
            )
        }

        val imageBounds = composeRule
            .onNodeWithContentDescription("未加载的比例图片")
            .fetchSemanticsNode()
            .boundsInRoot
        assertTrue("Image width must be reserved before loading", imageBounds.width > 0f)
        assertTrue("Image height must be reserved before loading", imageBounds.height > 0f)
        assertEquals(
            1200.0 / 880.0,
            imageBounds.width.toDouble() / imageBounds.height.toDouble(),
            0.02,
        )
    }

    /**
     * Regression: https://github.com/zly2006/zhihu-plus-plus/issues/495
     * Fixed by: https://github.com/zly2006/zhihu-plus-plus/pull/556
     */
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

    /**
     * Contract: https://github.com/zly2006/zhihu-plus-plus/issues/493
     * Introduced by: https://github.com/zly2006/zhihu-plus-plus/pull/502
     */
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

    /**
     * Contract: https://github.com/zly2006/zhihu-plus-plus/issues/550
     * Introduced by: https://github.com/zly2006/zhihu-plus-plus/pull/552
     */
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

    /**
     * Contract: https://github.com/zly2006/zhihu-plus-plus/issues/550
     * Introduced by: https://github.com/zly2006/zhihu-plus-plus/pull/552
     */
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

    /**
     * Contract: https://github.com/zly2006/zhihu-plus-plus/issues/550
     * Introduced by: https://github.com/zly2006/zhihu-plus-plus/pull/552
     */
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

    /**
     * Regression: https://github.com/zly2006/zhihu-plus-plus/issues/213
     * Fixed by: https://github.com/zly2006/zhihu-plus-plus/pull/316
     */
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

    /**
     * Regression: https://github.com/zly2006/zhihu-plus-plus/issues/477
     * Fixed by: https://github.com/zly2006/zhihu-plus-plus/pull/480
     */
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
        const val HIGHLIGHTED_PARAGRAPH =
            "目前灰度机制是在OpenCode上，被选中的账号调用deepseek-v4-pro或deepseek-v4-flash有机会拿到GA版。"
        const val HIGHLIGHT_SELECTION_TARGET = "后续普通段落用于验证拖动手柄跨越文字块。"
        const val SPANNING_HIGHLIGHT_FIRST = "第一段跨段划线内容。"
        const val SPANNING_HIGHLIGHT_SECOND = "第二段跨段划线内容。"
        const val FORMATTED_HIGHLIGHT_PREFIX = "WWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWW"
        const val FORMATTED_HIGHLIGHT = "划线命中"
        const val FORMATTED_HIGHLIGHT_PARAGRAPH = "$FORMATTED_HIGHLIGHT_PREFIX$FORMATTED_HIGHLIGHT 后缀"
        const val WRAPPED_HIGHLIGHT_PREFIX = "普通前缀 "
        const val WRAPPED_HIGHLIGHT =
            "这是位于段落中间并且需要跨越多个视觉行的划线内容，用于验证每一行都能完整绘制虚线。"
        const val WRAPPED_HIGHLIGHT_SUFFIX = " 普通后缀"
        const val WRAPPED_HIGHLIGHT_PARAGRAPH =
            "$WRAPPED_HIGHLIGHT_PREFIX$WRAPPED_HIGHLIGHT$WRAPPED_HIGHLIGHT_SUFFIX"
        val HIGHLIGHTED_PARAGRAPH_HTML =
            """
            <p data-pid="WGd4cbq-"><span class="highlight-wrap other has-comments"
                data-highlight-id="2063081895399788604"
                data-highlight-like-count="9"
                data-highlight-comment-count="2"
                data-highlight-my-comment-count="0"
                data-highlight-is-like="false"
                data-highlight-is-span="false"
                data-highlight-content-id="2062174868112676264"
                data-highlight-content-type="answer"
                data-highlight-pid="WGd4cbq-"
                data-highlight-start-offset="0"
                data-highlight-end-offset="68">$HIGHLIGHTED_PARAGRAPH</span></p>
            """.trimIndent()
        val SPANNING_HIGHLIGHT_HTML =
            """
            <p data-pid="first"><span class="highlight-wrap other has-comments"
                data-highlight-id="shared-segment"
                data-highlight-like-count="806"
                data-highlight-comment-count="15"
                data-highlight-is-span="true"
                data-highlight-display-text="$SPANNING_HIGHLIGHT_FIRST&#10;&#10;$SPANNING_HIGHLIGHT_SECOND"
                data-highlight-content-id="1907864533831225689"
                data-highlight-content-type="answer"
                data-highlight-pid="first"
                data-highlight-start-offset="0"
                data-highlight-end-offset="${SPANNING_HIGHLIGHT_FIRST.length}">$SPANNING_HIGHLIGHT_FIRST</span></p>
            <p data-pid="second"><span class="highlight-wrap other has-comments"
                data-highlight-id="shared-segment"
                data-highlight-like-count="806"
                data-highlight-comment-count="15"
                data-highlight-is-span="true"
                data-highlight-content-id="1907864533831225689"
                data-highlight-content-type="answer"
                data-highlight-pid="second"
                data-highlight-start-offset="0"
                data-highlight-end-offset="${SPANNING_HIGHLIGHT_SECOND.length}">$SPANNING_HIGHLIGHT_SECOND</span></p>
            """.trimIndent()
        val FORMATTED_HIGHLIGHT_PARAGRAPH_HTML =
            """
            <p><strong>$FORMATTED_HIGHLIGHT_PREFIX</strong><span class="highlight-wrap other has-comments"
                data-highlight-id="formatted-highlight"
                data-highlight-like-count="1"
                data-highlight-comment-count="1"
                data-highlight-content-id="777"
                data-highlight-content-type="answer">$FORMATTED_HIGHLIGHT</span> 后缀</p>
            """.trimIndent()
        val WRAPPED_HIGHLIGHT_PARAGRAPH_HTML =
            """
            <p>$WRAPPED_HIGHLIGHT_PREFIX<span class="highlight-wrap other has-comments"
                data-highlight-id="wrapped-highlight"
                data-highlight-like-count="1"
                data-highlight-comment-count="1"
                data-highlight-content-id="778"
                data-highlight-content-type="answer">$WRAPPED_HIGHLIGHT</span>$WRAPPED_HIGHLIGHT_SUFFIX</p>
            """.trimIndent()

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

private class CapturingTextToolbar : TextToolbar {
    var onCopyRequested: (() -> Unit)? = null
    var onSelectAllRequested: (() -> Unit)? = null

    override var status = TextToolbarStatus.Hidden
        private set

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?,
    ) {
        this.onCopyRequested = onCopyRequested
        this.onSelectAllRequested = onSelectAllRequested
        status = TextToolbarStatus.Shown
    }

    override fun hide() {
        status = TextToolbarStatus.Hidden
    }
}

private class RecordingClipboard : Clipboard {
    private var clipEntry: ClipEntry? = null

    var text: String? = null
        private set

    override suspend fun getClipEntry(): ClipEntry? = clipEntry

    override suspend fun setClipEntry(clipEntry: ClipEntry?) {
        this.clipEntry = clipEntry
        text = clipEntry
            ?.clipData
            ?.takeIf { it.itemCount > 0 }
            ?.getItemAt(0)
            ?.text
            ?.toString()
    }

    override val nativeClipboard: NativeClipboard
        get() = error("RecordingClipboard does not expose a native clipboard")

    fun clear() {
        clipEntry = null
        text = null
    }
}
