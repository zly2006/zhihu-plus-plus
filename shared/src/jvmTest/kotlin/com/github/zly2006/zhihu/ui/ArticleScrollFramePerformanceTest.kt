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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.markdown.RenderMarkdown
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.Navigator
import com.github.zly2006.zhihu.theme.ZhihuTheme
import com.github.zly2006.zhihu.ui.components.AppScaffold
import com.github.zly2006.zhihu.ui.components.ZhihuTwoRowsTopAppBar
import com.github.zly2006.zhihu.ui.components.rememberPreferCollapsedExitUntilCollapsedScrollBehavior
import java.io.File
import kotlin.math.ceil
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class, ExperimentalMaterial3Api::class)
class ArticleScrollFramePerformanceTest {
    @Test
    fun articleTopBarCollapseAndMarkdownScroll_p95FrameTime() = runComposeUiTest {
        val html = File("../app/src/androidTest/assets/issue-495-answer.html").readText()
        var activeHtml by mutableStateOf("")

        setContent {
            ZhihuTheme {
                CompositionLocalProvider(LocalNavigator provides Navigator(onNavigate = {}, onNavigateBack = {})) {
                    val scrollState = rememberScrollState()
                    val scrollBehavior = rememberPreferCollapsedExitUntilCollapsedScrollBehavior()
                    AppScaffold(
                        modifier = Modifier
                            .fillMaxSize()
                            .nestedScroll(scrollBehavior.nestedScrollConnection),
                        topBar = {
                            ZhihuTwoRowsTopAppBar(
                                title = { expanded ->
                                    Text(
                                        text = "如何在 60Hz 设备上排查 Compose 回答页滚动卡顿问题？",
                                        maxLines = if (expanded) 2 else 1,
                                    )
                                },
                                subtitle = { expanded ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth().padding(vertical = if (expanded) 12.dp else 4.dp),
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(if (expanded) 40.dp else 20.dp)
                                                .clip(CircleShape),
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = "测试答主",
                                            style = if (expanded) MaterialTheme.typography.titleSmall else MaterialTheme.typography.labelMedium,
                                        )
                                    }
                                },
                                scrollBehavior = scrollBehavior,
                            )
                        },
                    ) { innerPadding ->
                        val density = LocalDensity.current
                        val stableTopPadding = (
                            innerPadding.calculateTopPadding() -
                                with(density) { scrollBehavior.state.heightOffset.toDp() }
                        ).coerceAtLeast(0.dp)
                        val stableBottomPadding = (
                            innerPadding.calculateBottomPadding() +
                                with(density) { scrollBehavior.state.heightOffsetLimit.toDp() }
                        ).coerceAtLeast(0.dp)
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    translationY = scrollBehavior.state.heightOffset
                                }.padding(horizontal = 16.dp)
                                .verticalScroll(scrollState)
                                .padding(top = stableTopPadding, bottom = stableBottomPadding)
                                .testTag("article_scroll_column"),
                        ) {
                            if (activeHtml.isNotEmpty()) {
                                RenderMarkdown(
                                    html = activeHtml,
                                    scrollState = scrollState,
                                    selectable = true,
                                    enableScroll = false,
                                )
                            }
                        }
                    }
                }
            }
        }

        // Warmup initial composition
        waitForIdle()

        // Measure first-frame cost when long HTML arrives (off-main-thread AST parse)
        mainClock.autoAdvance = false
        val loadStartNs = System.nanoTime()
        runOnUiThread { activeHtml = html }
        mainClock.advanceTimeByFrame()
        val firstLoadFrameMs = (System.nanoTime() - loadStartNs) / 1_000_000.0
        mainClock.autoAdvance = true
        waitUntil(timeoutMillis = 5_000) {
            onAllNodesWithText("知乎", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        waitForIdle()
        mainClock.autoAdvance = false

        // Warmup 1 collapse/expand cycle before collecting p95 samples
        onRoot().performTouchInput { down(Offset(200f, 700f)) }
        repeat(15) {
            onRoot().performTouchInput { moveBy(Offset(0f, -24f)) }
            mainClock.advanceTimeByFrame()
        }
        onRoot().performTouchInput { up() }
        onRoot().performTouchInput { down(Offset(200f, 300f)) }
        repeat(15) {
            onRoot().performTouchInput { moveBy(Offset(0f, 24f)) }
            mainClock.advanceTimeByFrame()
        }
        onRoot().performTouchInput { up() }
        repeat(5) { mainClock.advanceTimeByFrame() }

        // Now measure per-frame cost during repeated scroll/collapse gestures
        val frameTimesMs = mutableListOf<Double>()
        repeat(3) {
            onRoot().performTouchInput {
                down(Offset(200f, 700f))
            }
            repeat(15) { step ->
                val t0 = System.nanoTime()
                onRoot().performTouchInput {
                    moveBy(Offset(0f, -24f))
                }
                mainClock.advanceTimeByFrame()
                frameTimesMs += (System.nanoTime() - t0) / 1_000_000.0
            }
            onRoot().performTouchInput { up() }
            repeat(5) {
                val t0 = System.nanoTime()
                mainClock.advanceTimeByFrame()
                frameTimesMs += (System.nanoTime() - t0) / 1_000_000.0
            }
            // Scroll back down to expand top bar again
            onRoot().performTouchInput {
                down(Offset(200f, 300f))
            }
            repeat(15) {
                val t0 = System.nanoTime()
                onRoot().performTouchInput {
                    moveBy(Offset(0f, 24f))
                }
                mainClock.advanceTimeByFrame()
                frameTimesMs += (System.nanoTime() - t0) / 1_000_000.0
            }
            onRoot().performTouchInput { up() }
            repeat(5) {
                val t0 = System.nanoTime()
                mainClock.advanceTimeByFrame()
                frameTimesMs += (System.nanoTime() - t0) / 1_000_000.0
            }
        }

        val sorted = frameTimesMs.sorted()
        val p50 = sorted[sorted.size / 2]
        val p95Index = (ceil(sorted.size * 0.95).toInt() - 1).coerceIn(0, sorted.lastIndex)
        val p95 = sorted[p95Index]
        val max = sorted.last()
        val slowFrames = sorted.count { it > 16.67 }
        val slowDetails = frameTimesMs.mapIndexedNotNull { idx, ms ->
            if (ms > 16.67) "$idx=${"%.1f".format(ms)}" else null
        }
        println(
            "ArticleScrollFramePerf firstLoadFrameMs=${"%.2f".format(firstLoadFrameMs)} " +
                "frames=${sorted.size} p50Ms=${"%.2f".format(p50)} " +
                "p95Ms=${"%.2f".format(p95)} maxMs=${"%.2f".format(max)} slowFramesOver16_67ms=$slowFrames slowDetails=$slowDetails",
        )
        assertTrue(sorted.isNotEmpty())
        if (System.getenv("MARKDOWN_PERFORMANCE") == "1") {
            assertTrue(firstLoadFrameMs < 50.0, "Async RenderMarkdown first frame exceeded 50ms: $firstLoadFrameMs")
            assertTrue(p95 < 16.67, "Article scroll p95 frame time exceeded 16.67ms: $p95 ($slowDetails)")
        }
    }
}
