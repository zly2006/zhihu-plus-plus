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
import android.os.SystemClock
import android.util.Log
import androidx.compose.foundation.ComposeFoundationFlags
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
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
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.zly2006.zhihu.markdown.RenderImage
import com.github.zly2006.zhihu.markdown.RenderMarkdown
import com.github.zly2006.zhihu.markdown.RenderMarkdownText
import com.github.zly2006.zhihu.navigation.AnswerNavigator
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.shared.data.DataHolder
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
import com.hrm.markdown.renderer.MarkdownImageData
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

    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun selectAllIncludesDeferredMarkdownBlocks() {
        val previousContextMenuFlag = ComposeFoundationFlags.isNewContextMenuEnabled
        ComposeFoundationFlags.isNewContextMenuEnabled = false
        try {
            val textToolbar = CapturingTextToolbar()
            val markdown = buildString {
                appendLine("第一段可见正文")
                appendLine()
                repeat(120) { index ->
                    appendLine("第 $index 段长文填充正文，用于把末段推到视口之外。")
                    appendLine()
                }
                appendLine("末段必须被全选")
            }
            composeRule.setScreenContent {
                CompositionLocalProvider(LocalTextToolbar provides textToolbar) {
                    RenderMarkdownText(markdown = markdown)
                }
            }

            composeRule
                .onNodeWithText("第一段可见正文")
                .performTouchInput { longClick() }
            composeRule.runOnIdle {
                requireNotNull(textToolbar.onSelectAllRequested).invoke()
            }
            // 全选后滚到底部，覆盖离屏投影与真实 Markdown 块互换时的选择稳定性。
            val scrollContainer = composeRule.onNode(
                SemanticsMatcher("has vertical scroll axis") { node ->
                    node.config.contains(SemanticsProperties.VerticalScrollAxisRange)
                },
            )
            repeat(40) {
                val range = scrollContainer
                    .fetchSemanticsNode()
                    .config[SemanticsProperties.VerticalScrollAxisRange]
                if (range.maxValue() - range.value() <= 1f) return@repeat
                scrollContainer.performSemanticsAction(SemanticsActions.ScrollBy) { scrollBy ->
                    scrollBy(0f, 4_000f)
                }
                composeRule.waitForIdle()
            }
            composeRule.runOnIdle {
                requireNotNull(textToolbar.onCopyRequested).invoke()
            }

            val clipboard = composeRule.activity.getSystemService(android.content.ClipboardManager::class.java)
            val copiedText = clipboard.primaryClip
                ?.getItemAt(0)
                ?.coerceToText(composeRule.activity)
                ?.toString()
                .orEmpty()
            val copiedParagraphIndexes = Regex("第 (\\d+) 段长文填充正文")
                .findAll(copiedText)
                .map { it.groupValues[1].toInt() }
                .toList()
            assertEquals((0 until 120).toList(), copiedParagraphIndexes)
            assertEquals(1, Regex(Regex.escape("第一段可见正文")).findAll(copiedText).count())
            assertEquals(1, Regex(Regex.escape("末段必须被全选")).findAll(copiedText).count())
            assertTrue(
                "Select all must include markdown blocks that are deferred outside the viewport",
                copiedText.contains("第一段可见正文") && copiedText.contains("末段必须被全选"),
            )
        } finally {
            ComposeFoundationFlags.isNewContextMenuEnabled = previousContextMenuFlag
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun highlightedParagraphRemainsSelectable() {
        val previousContextMenuFlag = ComposeFoundationFlags.isNewContextMenuEnabled
        ComposeFoundationFlags.isNewContextMenuEnabled = false
        try {
            val textToolbar = CapturingTextToolbar()
            val selectionColor = Color.Magenta
            composeRule.setScreenContent {
                CompositionLocalProvider(
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
                requireNotNull(textToolbar.onCopyRequested).invoke()
            }

            val copiedText = composeRule.activity
                .getSystemService(android.content.ClipboardManager::class.java)
                .primaryClip
                ?.getItemAt(0)
                ?.coerceToText(composeRule.activity)
                ?.toString()
                .orEmpty()
            assertEquals(HIGHLIGHTED_PARAGRAPH, copiedText)
        } finally {
            ComposeFoundationFlags.isNewContextMenuEnabled = previousContextMenuFlag
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun highlightedParagraphSelectionHandleCanExtendToFollowingParagraph() {
        val previousContextMenuFlag = ComposeFoundationFlags.isNewContextMenuEnabled
        ComposeFoundationFlags.isNewContextMenuEnabled = false
        try {
            val textToolbar = CapturingTextToolbar()
            val selectionColor = Color.Magenta
            composeRule.setScreenContent {
                CompositionLocalProvider(
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
                .boundsInRoot
            val handleBounds = endHandle.fetchSemanticsNode().boundsInRoot
            endHandle.performTouchInput {
                down(center)
                advanceEventTime(100)
                moveTo(
                    Offset(
                        x = targetBounds.right - handleBounds.left - 1f,
                        y = targetBounds.bottom - handleBounds.top - 1f,
                    ),
                    delayMillis = 500,
                )
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
            composeRule.runOnIdle {
                requireNotNull(textToolbar.onCopyRequested).invoke()
            }

            val copiedText = composeRule.activity
                .getSystemService(android.content.ClipboardManager::class.java)
                .primaryClip
                ?.getItemAt(0)
                ?.coerceToText(composeRule.activity)
                ?.toString()
                .orEmpty()
            assertTrue(
                "The standard selection handle must extend from a highlighted paragraph into the following block",
                copiedText.contains(HIGHLIGHT_SELECTION_TARGET),
            )
        } finally {
            ComposeFoundationFlags.isNewContextMenuEnabled = previousContextMenuFlag
        }
    }

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
    }

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

            val pixels = composeRule
                .onNodeWithTag("multiline-selection-article")
                .captureToImage()
                .toPixelMap()
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
                "Select-all highlight only covered $highlightedRows pixel rows; a wrapped paragraph must highlight every line",
                highlightedRows >= 180,
            )
        } finally {
            ComposeFoundationFlags.isNewContextMenuEnabled = previousContextMenuFlag
        }
    }

    @OptIn(ExperimentalFoundationApi::class)
    @Test
    fun draggingSelectionHandleUsesSameCompleteTextLayer() {
        val previousContextMenuFlag = ComposeFoundationFlags.isNewContextMenuEnabled
        ComposeFoundationFlags.isNewContextMenuEnabled = false
        try {
            val textToolbar = CapturingTextToolbar()
            composeRule.setScreenContent {
                CompositionLocalProvider(LocalTextToolbar provides textToolbar) {
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
                .boundsInRoot
            val handleBounds = endHandle.fetchSemanticsNode().boundsInRoot
            endHandle.performTouchInput {
                down(center)
                advanceEventTime(100)
                moveTo(
                    Offset(
                        x = targetBounds.right - handleBounds.left - 1f,
                        y = targetBounds.bottom - handleBounds.top - 1f,
                    ),
                    delayMillis = 500,
                )
                up()
            }
            composeRule.runOnIdle {
                requireNotNull(textToolbar.onCopyRequested).invoke()
            }

            val copiedText = composeRule.activity
                .getSystemService(android.content.ClipboardManager::class.java)
                .primaryClip
                ?.getItemAt(0)
                ?.coerceToText(composeRule.activity)
                ?.toString()
                .orEmpty()
            assertTrue(
                "Dragging the standard selection handle must reach later blocks through the same selection layer",
                copiedText.contains("起始段落") && copiedText.contains("末段拖动必须到达这里"),
            )
        } finally {
            ComposeFoundationFlags.isNewContextMenuEnabled = previousContextMenuFlag
        }
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

    private companion object {
        const val ISSUE_495_BENCHMARK_TAG = "Issue495Benchmark"
        const val ISSUE_495_FIRST_FRAME_LIMIT_MS = 5_000L
        const val HIGHLIGHTED_PARAGRAPH =
            "目前灰度机制是在OpenCode上，被选中的账号调用deepseek-v4-pro或deepseek-v4-flash有机会拿到GA版。"
        const val HIGHLIGHT_SELECTION_TARGET = "后续普通段落用于验证拖动手柄跨越文字块。"
        const val FORMATTED_HIGHLIGHT_PREFIX = "WWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWWW"
        const val FORMATTED_HIGHLIGHT = "划线命中"
        const val FORMATTED_HIGHLIGHT_PARAGRAPH = "$FORMATTED_HIGHLIGHT_PREFIX$FORMATTED_HIGHLIGHT 后缀"
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
        val FORMATTED_HIGHLIGHT_PARAGRAPH_HTML =
            """
            <p><strong>$FORMATTED_HIGHLIGHT_PREFIX</strong><span class="highlight-wrap other has-comments"
                data-highlight-id="formatted-highlight"
                data-highlight-like-count="1"
                data-highlight-comment-count="1"
                data-highlight-content-id="777"
                data-highlight-content-type="answer">$FORMATTED_HIGHLIGHT</span> 后缀</p>
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
