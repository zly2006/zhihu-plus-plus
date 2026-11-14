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

import android.content.pm.ActivityInfo
import androidx.activity.BackEventCompat
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.navigation.Pin
import com.github.zly2006.zhihu.test.MainActivityComposeRule
import com.github.zly2006.zhihu.test.resetAppPreferences
import com.github.zly2006.zhihu.test.setZhihuMainContent
import com.github.zly2006.zhihu.ui.ArticleAnswerTransitionDirection
import com.github.zly2006.zhihu.ui.HOME_FEED_LIST_TAG
import com.github.zly2006.zhihu.ui.PIN_SCREEN_BACK_BUTTON_TAG
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import com.github.zly2006.zhihu.viewmodel.sharedArticleAnswerSwitchState
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

/**
 * 单栏布局下系统预测性返回在竖屏/横屏阅读页上的用户可见契约：回答页跟手位移、取消回位、松手回信息流，
 * 预览对象必须是上一页信息流。横屏用例显式关闭双栏布局，因此断言不依赖设备是否平板；横屏双栏的
 * 返回归属见 [LandscapeSplitBackGestureInstrumentedTest]。
 *
 * 手势通过 OnBackPressedDispatcher 的 `@VisibleForTesting` 接缝派发，与真实系统手势走同一条链路。
 */
@RunWith(AndroidJUnit4::class)
class PredictiveBackInstrumentedTest {
    @get:Rule
    val composeRule: MainActivityComposeRule = createAndroidComposeRule<MainActivity>()

    private val answer = Article(type = ArticleType.Answer, id = 318L)
    private val pin = Pin(id = 318L)

    private enum class ReadingPage(
        val label: String,
    ) {
        ANSWER("回答"),
        PIN("想法"),
    }

    @Before
    fun resetSharedState() {
        composeRule.resetAppPreferences()
        composeRule.runOnIdle {
            sharedArticleAnswerSwitchState.reset()
            sharedArticleAnswerSwitchState.answerTransitionDirection = ArticleAnswerTransitionDirection.DEFAULT
        }
    }

    @After
    fun cancelOngoingGesture() {
        runCatching {
            composeRule.runOnIdle {
                composeRule.activity.onBackPressedDispatcher.dispatchOnBackCancelled()
            }
        }
    }

    /**
     * Regression: https://github.com/zly2006/zhihu-plus-plus/issues/772
     * Fixed by: https://github.com/zly2006/zhihu-plus-plus/pull/777
     *
     * 竖屏从信息流打开回答后，返回手势进行中回答页必须跟手滑出，取消回位，松手回信息流且不退出应用。
     */
    @Test
    fun answerPageFollowsPredictiveBackGestureInPortrait() {
        assertReadingPageFollowsPredictiveBack(portrait = true, page = ReadingPage.ANSWER)
    }

    /**
     * Regression: https://github.com/zly2006/zhihu-plus-plus/issues/772
     * Fixed by: https://github.com/zly2006/zhihu-plus-plus/pull/777
     *
     * 横屏单栏（手机横屏，或平板关闭横屏双栏布局）同样要跟手滑出、取消回位、松手回信息流且不退出应用。
     */
    @Test
    fun answerPageFollowsPredictiveBackGestureInLandscape() {
        assertReadingPageFollowsPredictiveBack(portrait = false, page = ReadingPage.ANSWER)
    }

    /**
     * Control: https://github.com/zly2006/zhihu-plus-plus/issues/772
     * Fixed by: https://github.com/zly2006/zhihu-plus-plus/pull/777
     *
     * 想法页一直有返回动画，作为动画检测本身的对照组必须始终通过，否则说明测试基建失效而不是回答页回归。
     */
    @Test
    fun pinPageFollowsPredictiveBackGestureInPortrait() {
        assertReadingPageFollowsPredictiveBack(portrait = true, page = ReadingPage.PIN)
    }

    /**
     * Control: https://github.com/zly2006/zhihu-plus-plus/issues/772
     * Fixed by: https://github.com/zly2006/zhihu-plus-plus/pull/777
     *
     * 横屏单栏的想法页动画对照组；失败说明横屏返回手势没有生效。
     */
    @Test
    fun pinPageFollowsPredictiveBackGestureInLandscape() {
        assertReadingPageFollowsPredictiveBack(portrait = false, page = ReadingPage.PIN)
    }

    /**
     * Regression: https://github.com/zly2006/zhihu-plus-plus/issues/773
     * Fixed by: https://github.com/zly2006/zhihu-plus-plus/pull/777
     *
     * 竖屏返回手势的预览必须是主栈上一页信息流，不能是分屏占位页；松手落到信息流且不退出应用。
     */
    @Test
    fun predictiveBackPreviewShowsPreviousPageInPortrait() {
        assertPredictiveBackPreview(portrait = true)
    }

    /**
     * Regression: https://github.com/zly2006/zhihu-plus-plus/issues/773
     * Fixed by: https://github.com/zly2006/zhihu-plus-plus/pull/777
     *
     * 横屏单栏下返回手势的预览必须是上一页信息流，不能是「选择左侧内容」占位页。
     */
    @Test
    fun predictiveBackPreviewShowsPreviousPageInLandscape() {
        assertPredictiveBackPreview(portrait = false)
    }

    private fun assertReadingPageFollowsPredictiveBack(portrait: Boolean, page: ReadingPage) {
        openReadingPage(portrait = portrait, page = page)
        val screenWidth = rootWidth()
        val initialLeft = readingPageLeft(page)

        dispatchPredictiveBackStarted()
        dispatchPredictiveBackProgress(0.25f)
        val quarterLeft = awaitReadingPageLeft(
            page = page,
            message = "${page.label}页在 progress=0.25 时没有跟随预测性返回手势位移",
            minimum = initialLeft + screenWidth * 0.03f,
        )
        dispatchPredictiveBackProgress(0.6f)
        val laterLeft = awaitReadingPageLeft(
            page = page,
            message = "${page.label}页在 progress=0.6 时没有继续跟随手势位移",
            minimum = quarterLeft + screenWidth * 0.1f,
        )
        assertTrue(
            "${page.label}页位移应随预测性返回进度单调增加：0.25→$quarterLeft，0.6→$laterLeft，" +
                "屏幕宽度=$screenWidth",
            laterLeft > quarterLeft + screenWidth * 0.05f,
        )
        assertTrue("手势进行中${page.label}页不应被弹栈", readingPageNodes(page).isNotEmpty())

        cancelPredictiveBack()
        await("取消预测性返回后${page.label}页没有回到原位") {
            abs(readingPageLeftOrNull(page)?.minus(initialLeft) ?: Float.MAX_VALUE) <= 1f
        }

        dispatchPredictiveBackStarted()
        dispatchPredictiveBackProgress(0.6f)
        completePredictiveBack()
        runCatching { composeRule.waitForIdle() }
        assertTrue(
            "${page.label}页返回手势完成后应用被退出了（返回事件落到了 Activity 兜底）",
            activityIsAlive(),
        )
        await("返回手势完成后${page.label}页没有退出") { readingPageNodes(page).isEmpty() }
        await("返回手势完成后没有回到信息流") { homeFeedNodes().isNotEmpty() }
    }

    private fun assertPredictiveBackPreview(portrait: Boolean) {
        openReadingPage(portrait = portrait, page = ReadingPage.PIN)
        val initialLeft = readingPageLeft(ReadingPage.PIN)
        val screenWidth = rootWidth()

        dispatchPredictiveBackStarted()
        dispatchPredictiveBackProgress(0.4f)
        await("预测性返回进行中既没有预览目标出现，也没有页面跟手位移（返回手势可能没有生效）") {
            visiblePlaceholderCount() > 0 ||
                abs((readingPageLeftOrNull(ReadingPage.PIN) ?: initialLeft) - initialLeft) >= screenWidth * 0.03f
        }
        val placeholderCount = visiblePlaceholderCount()
        assertTrue(
            "预测性返回预览不应显示「$PLACEHOLDER_TEXT」占位页，应显示上一页信息流；" +
                "实际可见占位页节点=$placeholderCount",
            placeholderCount == 0,
        )
        assertTrue(
            "预测性返回预览中必须能看到上一页信息流（$HOME_FEED_LIST_TAG）",
            homeFeedNodes().isNotEmpty(),
        )

        cancelPredictiveBack()
        await("取消预测性返回后想法页没有回到原位") {
            abs(readingPageLeftOrNull(ReadingPage.PIN)?.minus(initialLeft) ?: Float.MAX_VALUE) <= 1f
        }

        dispatchPredictiveBackStarted()
        dispatchPredictiveBackProgress(0.6f)
        completePredictiveBack()
        runCatching { composeRule.waitForIdle() }
        assertTrue("想法页返回手势完成后应用被退出了（返回事件落到了 Activity 兜底）", activityIsAlive())
        await("返回手势完成后没有落到信息流") { homeFeedNodes().isNotEmpty() }
    }

    private fun openReadingPage(portrait: Boolean, page: ReadingPage) {
        if (!portrait) disableLandscapeSplitPane()
        composeRule.activity.requestedOrientation = if (portrait) {
            ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        } else {
            ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        }
        composeRule.setZhihuMainContent()
        await("模拟器没有进入${if (portrait) "竖屏" else "横屏"}") {
            val rootBounds = composeRule.onRoot().fetchSemanticsNode().boundsInRoot
            if (portrait) rootBounds.width < rootBounds.height else rootBounds.width > rootBounds.height
        }
        await("主壳没有显示主页") {
            composeRule.onAllNodesWithTag("nav_tab_home").fetchSemanticsNodes().isNotEmpty()
        }
        settleUi()
        composeRule.runOnIdle { composeRule.activity.navigate(readingDestination(page)) }
        await("${page.label}页没有打开") { readingPageNodes(page).isNotEmpty() }
        settleUi()
        assertTrue(
            "${page.label}页应占满单栏（本用例固定单栏布局；横屏双栏契约见 " +
                "LandscapeSplitBackGestureInstrumentedTest）：left=${readingPageLeft(page)}，屏宽=${rootWidth()}",
            readingPageLeft(page) < rootWidth() * 0.2f,
        )
    }

    private fun disableLandscapeSplitPane() {
        composeRule.activity
            .getSharedPreferences(PREFERENCE_NAME, android.content.Context.MODE_PRIVATE)
            .edit()
            .putBoolean(LANDSCAPE_SPLIT_PREFERENCE_KEY, false)
            .commit()
    }

    private fun readingDestination(page: ReadingPage): NavDestination = when (page) {
        ReadingPage.ANSWER -> answer
        ReadingPage.PIN -> pin
    }

    private fun readingPageNodes(page: ReadingPage) = when (page) {
        ReadingPage.ANSWER -> composeRule.onAllNodesWithTag(ARTICLE_SCREEN_ROOT_TAG).fetchSemanticsNodes()
        ReadingPage.PIN -> composeRule.onAllNodesWithTag(PIN_SCREEN_BACK_BUTTON_TAG).fetchSemanticsNodes()
    }

    private fun readingPageLeftOrNull(page: ReadingPage): Float? =
        readingPageNodes(page).firstOrNull()?.boundsInRoot?.left

    private fun readingPageLeft(page: ReadingPage): Float = readingPageLeftOrNull(page)
        ?: throw AssertionError("${page.label}页不在组合中")

    private fun awaitReadingPageLeft(page: ReadingPage, message: String, minimum: Float): Float {
        await(message) {
            readingPageLeftOrNull(page)?.let { it >= minimum } == true
        }
        return readingPageLeft(page)
    }

    private fun dispatchPredictiveBackStarted() {
        composeRule.runOnIdle {
            val decorView = composeRule.activity.window.decorView
            composeRule.activity.onBackPressedDispatcher.dispatchOnBackStarted(
                backEvent(progress = 0f, decorWidth = decorView.width, decorHeight = decorView.height),
            )
        }
    }

    private fun dispatchPredictiveBackProgress(progress: Float) {
        composeRule.runOnIdle {
            val decorView = composeRule.activity.window.decorView
            composeRule.activity.onBackPressedDispatcher.dispatchOnBackProgressed(
                backEvent(progress = progress, decorWidth = decorView.width, decorHeight = decorView.height),
            )
        }
    }

    private fun cancelPredictiveBack() {
        composeRule.runOnIdle {
            composeRule.activity.onBackPressedDispatcher.dispatchOnBackCancelled()
        }
    }

    private fun completePredictiveBack() {
        composeRule.runOnIdle {
            composeRule.activity.onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun backEvent(progress: Float, decorWidth: Int, decorHeight: Int) = BackEventCompat(
        touchX = decorWidth * progress,
        touchY = decorHeight / 2f,
        progress = progress,
        swipeEdge = BackEventCompat.EDGE_LEFT,
    )

    private fun settleUi() {
        composeRule.waitForIdle()
        composeRule.mainClock.advanceTimeBy(300)
        composeRule.waitForIdle()
    }

    private fun await(message: String, condition: () -> Boolean) {
        try {
            composeRule.waitUntil(WAIT_TIMEOUT_MILLIS, condition)
        } catch (error: Throwable) {
            throw AssertionError("$message（等待 ${WAIT_TIMEOUT_MILLIS}ms 后仍不满足）", error)
        }
    }

    private fun activityIsAlive(): Boolean = runCatching {
        val activity = composeRule.activity
        !activity.isFinishing && !activity.isDestroyed
    }.getOrDefault(false)

    private fun homeFeedNodes() = composeRule.onAllNodesWithTag(HOME_FEED_LIST_TAG).fetchSemanticsNodes()

    private fun visiblePlaceholderCount(): Int = composeRule
        .onAllNodes(hasText(PLACEHOLDER_TEXT, substring = true))
        .fetchSemanticsNodes()
        .count { node -> node.size.width > 0 && node.size.height > 0 }

    private fun rootWidth(): Float = composeRule
        .onRoot()
        .fetchSemanticsNode()
        .boundsInRoot
        .width

    private companion object {
        const val ARTICLE_SCREEN_ROOT_TAG = "article_screen_root"
        const val PLACEHOLDER_TEXT = "选择左侧内容"
        const val LANDSCAPE_SPLIT_PREFERENCE_KEY = "landscapeListDetail"
        const val WAIT_TIMEOUT_MILLIS = 10_000L
    }
}
