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
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onRoot
import androidx.navigation.compose.rememberNavController
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.Home
import com.github.zly2006.zhihu.navigation.Question
import com.github.zly2006.zhihu.test.MainActivityComposeRule
import com.github.zly2006.zhihu.test.resetAppPreferences
import com.github.zly2006.zhihu.theme.ThemeManager
import com.github.zly2006.zhihu.theme.ZhihuTheme
import com.github.zly2006.zhihu.ui.HOME_FEED_LIST_TAG
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import com.github.zly2006.zhihu.ui.QUESTION_SCREEN_LIST_TAG
import com.github.zly2006.zhihu.ui.ZhihuMain
import com.github.zly2006.zhihu.ui.rememberAndroidZhihuMainPreferenceState
import com.github.zly2006.zhihu.ui.subscreens.BOTTOM_BAR_ITEMS_PREFERENCE_KEY
import com.github.zly2006.zhihu.ui.subscreens.START_DESTINATION_PREFERENCE_KEY
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

/**
 * 横屏分屏（左列表 / 右详情）下系统返回手势的归属契约：返回手势只作用于左栏主栈，
 * 右栏详情不参与手势返回。左栏已在根页时完成手势会由 Activity 兜底退出应用，属预期行为，
 * 根态用例只验证取消路径。
 *
 * 生产入口在手机上不进入分屏，这里用 `isListDetailCapable = true` 的 [ZhihuMain] 组合根固定分屏，
 * 使同一条契约在 CI 手机与平板模拟器上都能执行；阅读页用最小可控内容替代，契约只关心两个返回栈的归属。
 */
@RunWith(AndroidJUnit4::class)
class LandscapeSplitBackGestureInstrumentedTest {
    @get:Rule
    val composeRule: MainActivityComposeRule = createAndroidComposeRule<MainActivity>()

    private val article = Article(type = ArticleType.Answer, id = 318L)

    @Before
    fun resetPreferences() {
        composeRule.resetAppPreferences()
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
     * Contract: https://github.com/zly2006/zhihu-plus-plus/issues/680
     * Introduced by: https://github.com/zly2006/zhihu-plus-plus/pull/777
     *
     * 左栏有历史时，返回手势只预览/返回左栏页面，右栏详情位置不变；取消后左栏回位；
     * 松手后左栏回信息流、右栏详情仍在、应用不退出。修复前手势会被右栏 NavHost 抢走。
     */
    @Test
    fun backGestureReturnsLeftPaneAndKeepsDetailPane() {
        launchLandscapeSplit()
        composeRule.runOnIdle { composeRule.activity.navigate(article) }
        await("阅读页没有进入右栏") { detailPageLeftOrNull()?.let { it > rootWidth() * 0.2f } == true }
        settleUi()
        val detailLeft = detailPageLeft()

        composeRule.runOnIdle { composeRule.activity.navigate(Question(318L, "测试问题")) }
        await("问题页没有进入左栏") { questionListLeftOrNull() != null }
        settleUi()
        val initialQuestionLeft = questionListLeft()
        assertTrue(
            "问题页应显示在左栏：left=$initialQuestionLeft，屏宽=${rootWidth()}",
            initialQuestionLeft < rootWidth() * 0.2f,
        )

        dispatchPredictiveBackStarted()
        dispatchPredictiveBackProgress(0.25f)
        val quarterLeft = awaitQuestionLeft(
            message = "左栏问题页在 progress=0.25 时没有跟随预测性返回手势位移",
            minimum = initialQuestionLeft + rootWidth() * 0.03f,
        )
        dispatchPredictiveBackProgress(0.6f)
        val laterLeft = awaitQuestionLeft(
            message = "左栏问题页在 progress=0.6 时没有继续跟随手势位移",
            minimum = quarterLeft + rootWidth() * 0.1f,
        )
        assertTrue(
            "左栏页面位移应随预测性返回进度单调增加：0.25→$quarterLeft，0.6→$laterLeft，" +
                "屏幕宽度=${rootWidth()}",
            laterLeft > quarterLeft + rootWidth() * 0.05f,
        )
        assertTrue(
            "返回手势过程中右栏详情不应跟随位移：手势前 left=$detailLeft，progress=0.6 时 left=${detailPageLeft()}",
            abs(detailPageLeft() - detailLeft) <= 1f,
        )

        cancelPredictiveBack()
        await("取消预测性返回后左栏问题页没有回到原位") {
            abs((questionListLeftOrNull() ?: Float.MAX_VALUE) - initialQuestionLeft) <= 1f
        }

        dispatchPredictiveBackStarted()
        dispatchPredictiveBackProgress(0.6f)
        completePredictiveBack()
        runCatching { composeRule.waitForIdle() }
        assertTrue("左栏返回后应用被退出了（返回事件落到了 Activity 兜底）", activityIsAlive())
        await("返回手势完成后左栏问题页没有退出") { questionListNodes().isEmpty() }
        await("返回手势完成后左栏没有回到信息流") { homeFeedNodes().isNotEmpty() }
        assertTrue("返回手势完成后右栏详情页消失了", readingPageNodes().isNotEmpty())
    }

    /**
     * Regression: https://github.com/zly2006/zhihu-plus-plus/issues/773
     * Fixed by: https://github.com/zly2006/zhihu-plus-plus/pull/777
     *
     * 左栏已在根页时，返回手势不得预览或移除右栏详情：手势中不出现「选择左侧内容」占位预览、
     * 右栏位置不变；取消后详情仍显示、应用仍存活。
     */
    @Test
    fun rootBackGestureLeavesDetailPaneUntouched() {
        launchLandscapeSplit()
        composeRule.runOnIdle { composeRule.activity.navigate(article) }
        await("阅读页没有进入右栏") { detailPageLeftOrNull()?.let { it > rootWidth() * 0.2f } == true }
        settleUi()
        val detailLeft = detailPageLeft()

        dispatchPredictiveBackStarted()
        dispatchPredictiveBackProgress(0.4f)
        assertTrue(
            "右栏详情不应跟随返回手势位移：手势前 left=$detailLeft，progress=0.4 时 left=${detailPageLeft()}",
            abs(detailPageLeft() - detailLeft) <= 1f,
        )
        assertTrue(
            "返回手势不应把分屏占位页当预览；实际可见占位页节点=${visibleDetailPlaceholderCount()}",
            visibleDetailPlaceholderCount() == 0,
        )
        assertTrue("返回手势进行中用户应仍停留在应用内", activityIsAlive())

        cancelPredictiveBack()
        await("取消预测性返回后右栏详情消失了") { readingPageNodes().isNotEmpty() }
        assertTrue("取消预测性返回后应用被退出了", activityIsAlive())
    }

    private fun launchLandscapeSplit() {
        composeRule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        composeRule.activity
            .getSharedPreferences(PREFERENCE_NAME, android.content.Context.MODE_PRIVATE)
            .edit()
            .putString(START_DESTINATION_PREFERENCE_KEY, Home.name)
            .putStringSet(BOTTOM_BAR_ITEMS_PREFERENCE_KEY, setOf(Home.name))
            .putBoolean("duo3_home_account", false)
            .putBoolean("bottomBarTapScrollToTop", false)
            .putBoolean("autoHideBottomBar", false)
            .commit()
        composeRule.activity.setContent { }
        composeRule.waitForIdle()
        composeRule.activity.setContent {
            ZhihuTheme {
                val navController = rememberNavController()
                LaunchedEffect(navController) {
                    composeRule.activity.navController = navController
                }
                ZhihuMain(
                    navController = navController,
                    mainTabNavigationTarget = composeRule.activity.mainTabNavigationTarget,
                    navigate = { destination -> composeRule.activity.navigate(destination) },
                    navigateContent = { destination, controller ->
                        composeRule.activity.navigateIn(destination, controller)
                    },
                    enableLandscapeListDetail = true,
                    isListDetailCapable = true,
                    setCurrentMainTabOpenFrom = composeRule.activity::setCurrentMainTabOpenFrom,
                    consumeMainTabNavigationTarget = composeRule.activity::consumeMainTabNavigationTarget,
                    preferenceState = rememberAndroidZhihuMainPreferenceState(),
                    isDarkTheme = ThemeManager.isDarkTheme,
                    articleContent = { _, _ -> Box(Modifier.fillMaxSize().testTag(READING_PAGE_TAG)) },
                )
            }
        }
        composeRule.waitForIdle()
        await("模拟器没有进入横屏") {
            val rootBounds = composeRule.onRoot().fetchSemanticsNode().boundsInRoot
            rootBounds.width > rootBounds.height
        }
        await("横屏分屏没有生效（左栏不应再占满整个窗口）") {
            composeRule.onAllNodesWithTag("list_pane").fetchSemanticsNodes().firstOrNull()?.let { node ->
                node.boundsInRoot.width < rootWidth() * 0.8f
            } == true
        }
        await("主壳没有显示信息流") { homeFeedNodes().isNotEmpty() }
        settleUi()
    }

    private fun readingPageNodes() = composeRule.onAllNodesWithTag(READING_PAGE_TAG).fetchSemanticsNodes()

    private fun detailPageLeftOrNull(): Float? = readingPageNodes().firstOrNull()?.boundsInRoot?.left

    private fun detailPageLeft(): Float = detailPageLeftOrNull() ?: throw AssertionError("右栏阅读页不在组合中")

    private fun questionListNodes() = composeRule.onAllNodesWithTag(QUESTION_SCREEN_LIST_TAG).fetchSemanticsNodes()

    private fun questionListLeftOrNull(): Float? = questionListNodes().firstOrNull()?.boundsInRoot?.left

    private fun questionListLeft(): Float = questionListLeftOrNull() ?: throw AssertionError("左栏问题页不在组合中")

    private fun awaitQuestionLeft(message: String, minimum: Float): Float {
        await(message) {
            questionListLeftOrNull()?.let { it >= minimum } == true
        }
        return questionListLeft()
    }

    private fun visibleDetailPlaceholderCount(): Int = composeRule
        .onAllNodesWithTag(DETAIL_PANE_EMPTY_TAG)
        .fetchSemanticsNodes()
        .count { node -> node.size.width > 0 && node.size.height > 0 }

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

    private fun rootWidth(): Float = composeRule
        .onRoot()
        .fetchSemanticsNode()
        .boundsInRoot
        .width

    private companion object {
        const val READING_PAGE_TAG = "predictive_back_split_reading_page"
        const val DETAIL_PANE_EMPTY_TAG = "detail_pane_empty"
        const val WAIT_TIMEOUT_MILLIS = 10_000L
    }
}
