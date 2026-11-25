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
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.test.MainActivityComposeRule
import com.github.zly2006.zhihu.test.resetAppPreferences
import com.github.zly2006.zhihu.test.setScreenContent
import com.github.zly2006.zhihu.theme.LocalUiBackdrop
import com.github.zly2006.zhihu.theme.ThemeManager
import com.github.zly2006.zhihu.theme.ThemeMode
import com.github.zly2006.zhihu.theme.UI_STYLE_PREFERENCE_KEY
import com.github.zly2006.zhihu.theme.UiStyle
import com.github.zly2006.zhihu.theme.rememberUiBackdrop
import com.github.zly2006.zhihu.ui.ARTICLE_USE_WEBVIEW_PREFERENCE_KEY
import com.github.zly2006.zhihu.ui.ArticleScreen
import com.github.zly2006.zhihu.ui.HOME_NOTIFICATION_BUTTON_TAG
import com.github.zly2006.zhihu.ui.HOME_SEARCH_BUTTON_TAG
import com.github.zly2006.zhihu.ui.HomeScreen
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import com.github.zly2006.zhihu.ui.components.AppIconButton
import com.github.zly2006.zhihu.ui.components.AppIconButtonStyle
import com.github.zly2006.zhihu.ui.components.AppScaffold
import com.github.zly2006.zhihu.ui.components.ZhihuTwoRowsTopAppBar
import com.github.zly2006.zhihu.ui.components.rememberPreferCollapsedExitUntilCollapsedScrollBehavior
import com.github.zly2006.zhihu.ui.liquidglass.components.AndroidLiquidSlider
import com.github.zly2006.zhihu.viewmodel.ArticleViewModel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs
import kotlin.math.roundToInt

@RunWith(AndroidJUnit4::class)
class LiquidGlassInstrumentedTest {
    @get:Rule
    val composeRule: MainActivityComposeRule = createAndroidComposeRule<MainActivity>()

    private lateinit var previousUiStyle: UiStyle

    @Before
    fun setUp() {
        previousUiStyle = ThemeManager.currentUiStyle()
        composeRule.resetAppPreferences()
        composeRule.activity
            .getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
            .edit(commit = true) {
                putString(UI_STYLE_PREFERENCE_KEY, UiStyle.LiquidGlass.prefValue)
            }
        composeRule.runOnIdle {
            ThemeManager.setThemeMode(ThemeMode.LIGHT)
            ThemeManager.setUiStyle(UiStyle.LiquidGlass)
        }
    }

    @After
    fun tearDown() {
        composeRule.runOnIdle {
            ThemeManager.setUiStyle(previousUiStyle)
            ThemeManager.setThemeMode(ThemeMode.SYSTEM)
        }
    }

    /**
     * Regression: https://github.com/zly2006/zhihu-plus-plus/issues/833
     * Fixed by: https://github.com/zly2006/zhihu-plus-plus/pull/839
     * Also covers PR #838 (https://github.com/zly2006/zhihu-plus-plus/pull/838).
     * Target state: dragging AndroidLiquidSlider horizontally locks horizontal adjustment even when
     * the finger drifts vertically out of the slider bounds, consumes the UP event so a clickable
     * parent row does not fire, accepts a 45-degree diagonal drag, and still yields vertical swipes
     * to an enclosing verticalScroll container.
     */
    @Test
    fun liquidSliderLocksHorizontalDragAgainstVerticalDriftAndProtectsParentRow() {
        var sliderValue by mutableFloatStateOf(0.2f)
        var parentClicks by mutableIntStateOf(0)
        var touchSlopPx = 0f
        lateinit var scrollState: ScrollState

        composeRule.setScreenContent {
            touchSlopPx = LocalViewConfiguration.current.touchSlop
            scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { parentClicks += 1 }
                        .padding(16.dp),
                ) {
                    AndroidLiquidSlider(
                        value = { sliderValue },
                        onValueChange = { sliderValue = it },
                        valueRange = 0f..1f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(SLIDER_TAG),
                    )
                }
                repeat(40) { index ->
                    Text(
                        text = "滑块下方列表项 $index",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                    )
                }
            }
        }

        val sliderNode = composeRule.onNodeWithTag(SLIDER_TAG, useUnmergedTree = true)
        val initialValue = sliderValue

        // 1. 横向锁定后即使大幅纵向离开滑块区域，仍持续调节且不触发父级滚动或点击。
        sliderNode.performTouchInput {
            val start = Offset(visibleSize.width * 0.25f, visibleSize.height * 0.5f)
            val lockPoint = Offset(start.x + touchSlopPx + 12f, start.y)
            val driftedPoint = Offset(visibleSize.width * 0.78f, start.y + visibleSize.height * 3f)
            down(start)
            moveTo(lockPoint)
            moveTo(driftedPoint)
            up()
        }
        composeRule.waitForIdle()
        assertTrue(
            "Horizontal drag with vertical drift must keep updating slider: initial=$initialValue current=$sliderValue",
            sliderValue > initialValue + 0.25f,
        )
        assertEquals("Locked horizontal slider drag must not scroll parent column", 0, scrollState.value)
        assertEquals("Locked horizontal slider drag must consume UP and not trigger parent row click", 0, parentClicks)

        // 2. 精确 45° 对角线起手（absX == absY >= touchSlop）也应进入横向拖动而不是卡住。
        val beforeDiagonal = sliderValue
        sliderNode.performTouchInput {
            val start = Offset(visibleSize.width * 0.75f, visibleSize.height * 0.5f)
            val diagStep = touchSlopPx + 4f
            down(start)
            moveTo(Offset(start.x - diagStep, start.y + diagStep))
            moveTo(Offset(visibleSize.width * 0.30f, start.y + diagStep))
            up()
        }
        composeRule.waitForIdle()
        assertTrue(
            "45-degree diagonal drag must engage horizontal slider drag: before=$beforeDiagonal after=$sliderValue",
            sliderValue < beforeDiagonal - 0.15f,
        )
        assertEquals("45-degree horizontal drag must not trigger parent row click", 0, parentClicks)

        // 3. 在滑块上明确纵向滑动时，应交给父级列表滚动且不误改滑块数值。
        val beforeVerticalScroll = sliderValue
        sliderNode.performTouchInput {
            val start = Offset(visibleSize.width * 0.5f, visibleSize.height * 0.5f)
            down(start)
            moveTo(Offset(start.x, start.y - touchSlopPx - 20f))
            moveTo(Offset(start.x, start.y - 260f))
            up()
        }
        composeRule.waitForIdle()
        assertTrue(
            "Vertical swipe starting on slider must scroll enclosing Column: scroll=${scrollState.value}",
            scrollState.value > 0,
        )
        assertEquals(
            "Vertical swipe must not mutate slider value",
            beforeVerticalScroll,
            sliderValue,
            0.02f,
        )
    }

    /**
     * Regression: https://github.com/zly2006/zhihu-plus-plus/issues/833
     * Fixed by: https://github.com/zly2006/zhihu-plus-plus/pull/839
     * Target state: after AndroidLiquidSlider's container width grows (e.g. rotation or split-screen),
     * subsequent taps and drags calculate progress against the updated trackWidth rather than the
     * stale initial width captured in pointerInput / DampedDragAnimation closures.
     */
    @Test
    fun liquidSliderUsesUpdatedTrackWidthAfterResize() {
        var sliderValue by mutableFloatStateOf(0.5f)
        var widthDp by mutableStateOf(160.dp)
        var touchSlopPx = 0f

        composeRule.setScreenContent {
            touchSlopPx = LocalViewConfiguration.current.touchSlop
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            ) {
                AndroidLiquidSlider(
                    value = { sliderValue },
                    onValueChange = { sliderValue = it },
                    valueRange = 0f..1f,
                    modifier = Modifier
                        .width(widthDp)
                        .testTag(SLIDER_TAG),
                )
            }
        }

        // 将轨道宽度翻倍（160dp -> 320dp），随后点按新宽度的 75%（240dp）。
        // 若闭包仍持有旧宽度 160dp，240/160 会被钳制到 1.0f 而非 0.75f。
        composeRule.runOnIdle { widthDp = 320.dp }
        composeRule.waitForIdle()

        val sliderNode = composeRule.onNodeWithTag(SLIDER_TAG, useUnmergedTree = true)
        sliderNode.performTouchInput {
            click(Offset(visibleSize.width * 0.75f, visibleSize.height * 0.5f))
        }
        composeRule.waitForIdle()
        assertEquals(
            "Tap at 75% of resized 320dp slider must yield ~0.75f instead of clamping to 1.0f from stale 160dp width",
            0.75f,
            sliderValue,
            0.08f,
        )

        // 从 0.75f 向左拖动 25% 宽度（80dp）；若仍按旧 160dp 换算会减少 0.50f 落到 0.25f，而非 0.50f。
        val beforeDrag = sliderValue
        sliderNode.performTouchInput {
            val start = Offset(visibleSize.width * 0.75f, visibleSize.height * 0.5f)
            val lock = Offset(start.x - touchSlopPx - 6f, start.y)
            val end = Offset(visibleSize.width * 0.50f, start.y)
            down(start)
            moveTo(lock)
            moveTo(end)
            up()
        }
        composeRule.waitForIdle()
        val delta = beforeDrag - sliderValue
        assertEquals(
            "Dragging 25% of resized 320dp width must shift value by ~0.25f (delta=$delta)",
            0.25f,
            delta,
            0.08f,
        )
    }

    /**
     * Regression: https://github.com/zly2006/zhihu-plus-plus/issues/810
     * Fixed by: https://github.com/zly2006/zhihu-plus-plus/pull/838
     * Target state: under Liquid Glass, the Home screen search bar pill matches the height of the
     * adjacent notification floating button (48dp) so their glass capsules align visually.
     * Fails if the search bar reverts to 44dp while the notification button stays 48dp.
     */
    @Test
    fun liquidGlassHomeSearchBarMatchesNotificationButtonHeight() {
        composeRule.activity
            .getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
            .edit(commit = true) {
                putBoolean("duo3_home_account", false)
                putBoolean("showRefreshFab", false)
            }
        composeRule.setScreenContent {
            HomeScreen(
                scrollToTopTrigger = 0,
                innerPadding = PaddingValues(),
            )
        }

        val searchNode = composeRule.onNodeWithTag(HOME_SEARCH_BUTTON_TAG, useUnmergedTree = true)
        val notificationNode = composeRule.onNodeWithTag(HOME_NOTIFICATION_BUTTON_TAG, useUnmergedTree = true)
        searchNode.assertIsDisplayed()
        notificationNode.assertIsDisplayed()

        val searchHeight = searchNode.fetchSemanticsNode().boundsInRoot.height
        val notificationHeight = notificationNode.fetchSemanticsNode().boundsInRoot.height
        assertTrue(
            "Liquid Glass Home search bar height ($searchHeight) must match notification button height ($notificationHeight)",
            abs(searchHeight - notificationHeight) <= 2f,
        )
    }

    /**
     * Regression: https://github.com/zly2006/zhihu-plus-plus/issues/833
     * Fixed by: https://github.com/zly2006/zhihu-plus-plus/pull/839
     * Target state: floating AppIconButton under Liquid Glass compensates for LiquidButton's 0.55f
     * surface alpha multiplier so its rendered glass tint over a dark backdrop stays at ~0.42f
     * instead of suffering double alpha attenuation (~0.23f).
     */
    @Test
    fun liquidGlassFloatingIconButtonAvoidsDoubleAlphaAttenuation() {
        composeRule.setScreenContent {
            val backdrop = rememberUiBackdrop(style = UiStyle.LiquidGlass)
            CompositionLocalProvider(LocalUiBackdrop provides backdrop) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(backdrop.source)
                            .background(Color.Black),
                    )
                    AppIconButton(
                        onClick = {},
                        style = AppIconButtonStyle.Floating,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(64.dp)
                            .testTag(FLOATING_ICON_BUTTON_TAG),
                    ) {
                        Spacer(Modifier.size(1.dp))
                    }
                }
            }
        }

        val buttonNode = composeRule.onNodeWithTag(FLOATING_ICON_BUTTON_TAG)
        buttonNode.assertIsDisplayed()
        val pixelMap = buttonNode.captureToImage().toPixelMap()
        val centerX = (pixelMap.width * 0.5f).roundToInt().coerceIn(0, pixelMap.width - 1)
        val centerY = (pixelMap.height * 0.5f).roundToInt().coerceIn(0, pixelMap.height - 1)
        val centerColor = pixelMap[centerX, centerY]
        val luminance = (centerColor.red + centerColor.green + centerColor.blue) / 3f
        assertTrue(
            "Floating AppIconButton over black backdrop must avoid double alpha attenuation " +
                "(expected luminance >= 0.34f, actual=$luminance, color=$centerColor)",
            luminance >= 0.34f,
        )
    }

    /**
     * Regression: https://github.com/zly2006/zhihu-plus-plus/issues/833
     * Fixed by: https://github.com/zly2006/zhihu-plus-plus/pull/839
     * Target state: when bright content scrolls behind a collapsed Liquid Glass ZhihuTwoRowsTopAppBar
     * in dark mode, the TopAppBar's blurred scroll scrim strongly attenuates the underlying pixels
     * across the status-bar, title, and subtitle bands so body text cannot bleed through and collide
     * with the collapsed header text.
     */
    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun liquidGlassScrolledTopBarIsolatesTitleFromUnderlyingContent() {
        composeRule.runOnIdle {
            ThemeManager.setThemeMode(ThemeMode.DARK)
        }
        lateinit var scrollState: ScrollState
        composeRule.setScreenContent {
            val scrollBehavior = rememberPreferCollapsedExitUntilCollapsedScrollBehavior()
            scrollState = rememberScrollState()
            AppScaffold(
                modifier = Modifier
                    .fillMaxSize()
                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                containerColor = Color.Black,
                topBar = {
                    ZhihuTwoRowsTopAppBar(
                        modifier = Modifier.testTag(SCROLLED_TOP_BAR_TAG),
                        title = { Text("短标题") },
                        subtitle = { Text("作者") },
                        scrollBehavior = scrollBehavior,
                    )
                },
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(innerPadding)
                        .testTag(SCROLLED_BODY_TAG),
                ) {
                    // 高亮纯白区块滚到顶栏背后并延伸至顶栏下方，确保状态栏/标题/副标题三段后方均有纯白正文。
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1800.dp)
                            .background(Color.White),
                    )
                    Spacer(Modifier.height(1200.dp))
                }
            }
        }

        val bodyNode = composeRule.onNodeWithTag(SCROLLED_BODY_TAG)
        // 先收起顶栏并继续上滑一段距离，使白色正文区块位于收起态顶栏正后方。
        repeat(3) {
            bodyNode.performTouchInput {
                val x = visibleSize.width * 0.5f
                down(Offset(x, visibleSize.height * 0.72f))
                moveTo(Offset(x, visibleSize.height * 0.42f))
                up()
            }
            composeRule.waitForIdle()
        }
        assertTrue("Expected scrolled body under top bar (scroll=${scrollState.value})", scrollState.value > 80)

        val topBarBounds = composeRule.onNodeWithTag(SCROLLED_TOP_BAR_TAG).fetchSemanticsNode().boundsInRoot
        val pixels = composeRule.onRoot().captureToImage().toPixelMap()
        // 采样顶栏右侧无文字区域（状态栏带、标题带、副标题带），下方正好覆盖纯白正文区块。
        val sampleX = (topBarBounds.right - 24f).roundToInt().coerceIn(0, pixels.width - 1)
        // 先确认顶栏下方紧邻的正文区仍是纯白，证明高亮正文确实横跨在收起态顶栏正后方。
        val belowTopBarY = (topBarBounds.bottom + 120f).roundToInt().coerceIn(0, pixels.height - 1)
        val belowColor = pixels[sampleX, belowTopBarY]
        val belowLuminance = (belowColor.red + belowColor.green + belowColor.blue) / 3f
        assertTrue(
            "Expected bright white body block to still span behind and below the collapsed TopAppBar " +
                "(belowLuminance=$belowLuminance, belowColor=$belowColor)",
            belowLuminance >= 0.90f,
        )
        val bands = listOf(
            "statusBar(20%)" to 0.20f,
            "title(65%)" to 0.65f,
            "subtitle(88%)" to 0.88f,
        )
        bands.forEach { (label, fractionY) ->
            val y = (topBarBounds.top + topBarBounds.height * fractionY)
                .roundToInt()
                .coerceIn(0, pixels.height - 1)
            val color = pixels[sampleX, y]
            val luminance = (color.red + color.green + color.blue) / 3f
            assertTrue(
                "Scrolled Liquid Glass TopAppBar must isolate $label band from bright underlying content " +
                    "(expected luminance <= 0.18f, actual=$luminance, color=$color, bounds=$topBarBounds)",
                luminance <= 0.18f,
            )
        }
    }

    /**
     * Contract: https://github.com/zly2006/zhihu-plus-plus/issues/833
     * Introduced by: https://github.com/zly2006/zhihu-plus-plus/pull/839
     * Target state: under Liquid Glass, ArticleScreen's bottom bar splits controls into grouped
     * floating glass pills (vote pill on leading side, actions pill on trailing side, and a
     * standalone circular more button when duo3_article_actions is disabled) with an open center
     * gap, unified 52dp height/baseline, and zero glass-on-glass nesting.
     */
    @Test
    fun liquidGlassArticleBottomBarUsesGroupedFloatingPillsWithAlignedHeight() {
        composeRule.activity
            .getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
            .edit(commit = true) {
                putBoolean("duo3_article_actions", false)
                putBoolean("autoHideArticleBottomBar", false)
                putBoolean(ARTICLE_USE_WEBVIEW_PREFERENCE_KEY, false)
            }
        composeRule.runOnIdle {
            ThemeManager.setThemeMode(ThemeMode.DARK)
        }
        val article = Article(type = ArticleType.Answer, id = 839900L)
        val viewModel = ArticleViewModel(article = article, httpClient = null)
        composeRule.activity.runOnUiThread {
            viewModel.title = "孩子用AI写小说遭父亲质疑非原创，版权到底归谁？"
            viewModel.authorName = "Cecilia"
            viewModel.content =
                """
                <h2>红果上那么多AI漫剧，难道版权都归AI吗？</h2>
                <p>当然不是。AI是创作工具，不是法律意义上的作者。至少在我国现行著作权法框架下，AI不能因为参与了创作，就自动成为作品的著作权人。</p>
                <p>但这并不意味着，所有使用AI生成的内容，都天然属于使用者。这里其实涉及一个很重要的区别：谁提供了创意，谁完成了表达。</p>
                <p>我国《著作权法》第三条规定，作品是文学、艺术和科学领域内具有独创性并能以一定形式表现的智力成果。</p>
                <p>继续阅读下面的法律条款分析，观察底部浮空玻璃分组胶囊在正文上方滚动时的折射与留白效果。</p>
                <p>第一，提示词与多轮迭代修改是否体现作者个性化表达选择；第二，最终生成成果是否经过人类实质性编排与取舍。</p>
                """.trimIndent()
            viewModel.voteUpCount = 35
            viewModel.commentCount = 12
            viewModel.createdAt = 1_710_000_000L
            viewModel.updatedAt = 1_710_000_600L
        }

        composeRule.setScreenContent {
            ArticleScreen(article = article, viewModel = viewModel)
        }

        val votePill = composeRule.onNodeWithTag("article_liquid_vote_pill")
        val actionsPill = composeRule.onNodeWithTag("article_liquid_actions_pill")
        val moreButton = composeRule.onNodeWithTag("article_liquid_more_button")
        votePill.assertIsDisplayed()
        actionsPill.assertIsDisplayed()
        moreButton.assertIsDisplayed()

        val voteBounds = votePill.fetchSemanticsNode().boundsInRoot
        val actionsBounds = actionsPill.fetchSemanticsNode().boundsInRoot
        val moreBounds = moreButton.fetchSemanticsNode().boundsInRoot

        assertTrue(
            "Grouped floating pills must leave an open breathing gap in the center: vote=$voteBounds actions=$actionsBounds",
            actionsBounds.left > voteBounds.right + 16f,
        )
        assertTrue(
            "Standalone more circle must sit to the right of actions pill with a clear gap: actions=$actionsBounds more=$moreBounds",
            moreBounds.left > actionsBounds.right + 8f,
        )
        assertEquals("Vote and actions pills must share identical height", voteBounds.height, actionsBounds.height, 2f)
        assertEquals("Standalone more circle must match toolbar pill height", actionsBounds.height, moreBounds.height, 2f)
        assertEquals("Standalone more button must be circular", moreBounds.width, moreBounds.height, 2f)
        assertEquals("All bottom floating controls must share the same vertical center", voteBounds.center.y, moreBounds.center.y, 2f)

        composeRule.onNodeWithContentDescription("更多选项").performClick()
        composeRule.onNodeWithText("复制链接").assertIsDisplayed()
    }

    companion object {
        private const val SLIDER_TAG = "liquid_glass_regression_slider"
        private const val FLOATING_ICON_BUTTON_TAG = "liquid_glass_floating_icon_button"
        private const val SCROLLED_TOP_BAR_TAG = "liquid_glass_scrolled_top_bar"
        private const val SCROLLED_BODY_TAG = "liquid_glass_scrolled_body"
    }
}
