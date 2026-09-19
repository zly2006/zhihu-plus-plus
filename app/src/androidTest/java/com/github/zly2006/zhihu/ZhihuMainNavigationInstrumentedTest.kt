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
import android.graphics.Bitmap
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.zly2006.zhihu.filter.ContentOpenFrom
import com.github.zly2006.zhihu.navigation.Account
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.Daily
import com.github.zly2006.zhihu.navigation.Follow
import com.github.zly2006.zhihu.navigation.Home
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.MainTabs
import com.github.zly2006.zhihu.navigation.MyCollections
import com.github.zly2006.zhihu.navigation.OnlineHistory
import com.github.zly2006.zhihu.navigation.Question
import com.github.zly2006.zhihu.navigation.Video
import com.github.zly2006.zhihu.reading.AndroidReadingPlayerBridge
import com.github.zly2006.zhihu.reading.ReadingContentType
import com.github.zly2006.zhihu.reading.ReadingPlaybackStatus
import com.github.zly2006.zhihu.reading.ReadingPlayerState
import com.github.zly2006.zhihu.reading.ReadingQueueItem
import com.github.zly2006.zhihu.test.MainActivityComposeRule
import com.github.zly2006.zhihu.test.resetAppPreferences
import com.github.zly2006.zhihu.test.setZhihuMainContent
import com.github.zly2006.zhihu.theme.ThemeManager
import com.github.zly2006.zhihu.theme.ZhihuTheme
import com.github.zly2006.zhihu.ui.AndroidArticleNavigationHandoff
import com.github.zly2006.zhihu.ui.FOLLOW_SCREEN_PAGER_TAG
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import com.github.zly2006.zhihu.ui.QUESTION_SCREEN_LIST_TAG
import com.github.zly2006.zhihu.ui.ZhihuMain
import com.github.zly2006.zhihu.ui.rememberAndroidZhihuMainPreferenceState
import com.github.zly2006.zhihu.ui.subscreens.BOTTOM_BAR_ITEMS_PREFERENCE_KEY
import com.github.zly2006.zhihu.ui.subscreens.COLLECTION_DIRECT_BROWSE_PREFERENCE_KEY
import com.github.zly2006.zhihu.ui.subscreens.START_DESTINATION_PREFERENCE_KEY
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ZhihuMainNavigationInstrumentedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val deterministicBottomBarItems = linkedSetOf(
        Home.name,
        Follow.name,
        Daily.name,
        OnlineHistory.name,
        Account.name,
    )

    @Before
    fun resetPreferences() {
        composeRule.resetAppPreferences()
    }

    /**
     * Contract: https://github.com/zly2006/zhihu-plus-plus/issues/318
     * Introduced by: https://github.com/zly2006/zhihu-plus-plus/pull/326
     */
    @Test
    fun startDestinationAndHiddenBottomTabsRemainCompatibleWithFlattenedPager() {
        // The flattened main pager now treats Follow as a single main tab with an internal pager.
        // Startup still opens the configured visible tab, hidden tabs stay hidden, and entering
        // Follow from the main pager lands on the Follow page while its inner pager starts from
        // the default recommendation tab.
        composeRule.launchZhihuMain(
            startDestination = Daily.name,
            bottomBarItems = linkedSetOf(Follow.name, Daily.name, Account.name),
        )

        composeRule.waitUntilTabSelected("nav_tab_daily")
        composeRule.onNodeWithTag("nav_tab_daily").assertIsSelected()
        composeRule.onNodeWithTag("nav_tab_follow").assertIsNotSelected()
        composeRule.onNodeWithTag("nav_tab_account").assertIsNotSelected()
        composeRule.onNodeWithTag("nav_tab_home").assertDoesNotExist()
        composeRule.onNodeWithTag("nav_tab_hotlist").assertDoesNotExist()
        composeRule.onNodeWithTag("nav_tab_onlinehistory").assertDoesNotExist()

        composeRule.onRoot().performTouchInput { swipeRight() }

        composeRule.waitUntilTabSelected("follow_screen_tab_0")
        composeRule.onNodeWithTag("nav_tab_follow").assertIsSelected()
        composeRule.onNodeWithTag("follow_screen_tab_0").assertIsSelected()
        composeRule.onNodeWithTag("follow_screen_tab_1").assertIsNotSelected()

        composeRule.onNodeWithTag(FOLLOW_SCREEN_PAGER_TAG).performTouchInput { swipeLeft() }

        composeRule.waitUntilTabSelected("follow_screen_tab_1")
        composeRule.onNodeWithTag("nav_tab_follow").assertIsSelected()
        composeRule.onNodeWithTag("follow_screen_tab_1").assertIsSelected()

        composeRule.activity.runOnUiThread {
            composeRule.activity.navigate(MainTabs, popup = true)
        }

        composeRule.waitUntilTabSelected("nav_tab_daily")
        composeRule.onNodeWithTag("nav_tab_daily").assertIsSelected()
        composeRule.onNodeWithTag("nav_tab_home").assertDoesNotExist()

        composeRule.onNodeWithTag("nav_tab_follow").performClick()

        composeRule.waitUntilTabSelected("nav_tab_follow")
        composeRule.onNodeWithTag("nav_tab_follow").assertIsSelected()
        composeRule.waitUntilTabSelected("follow_screen_tab_1")
    }

    /**
     * Contract: https://github.com/zly2006/zhihu-plus-plus/issues/318
     * Introduced by: https://github.com/zly2006/zhihu-plus-plus/pull/326
     */
    @Test
    fun homeTabOpenContent_recordsHomeFeedOpenFrom() {
        composeRule.launchZhihuMain(startDestination = Home.name)

        composeRule.waitUntilTabSelected("nav_tab_home")

        val article = Article(type = ArticleType.Answer, id = 318L)
        var openFrom: String? = null
        composeRule.runOnIdle {
            composeRule.activity.navigate(article)
            openFrom = AndroidArticleNavigationHandoff.consumeContentOpenFrom(article)
        }

        assertEquals(ContentOpenFrom.HOME_FEED, openFrom)
    }

    /**
     * Contract: https://github.com/zly2006/zhihu-plus-plus/issues/680
     * Introduced by: https://github.com/zly2006/zhihu-plus-plus/pull/725
     * 右侧设置进入子页面后返回，必须恢复上级设置；通过真实 Compose 导航验证两个返回栈的归属。
     */
    @Test
    fun secondarySettingsBackRestoresParent() {
        composeRule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        composeRule.activity
            .getSharedPreferences(PREFERENCE_NAME, android.content.Context.MODE_PRIVATE)
            .edit(commit = true) { putBoolean("developer", true) }
        composeRule.launchZhihuMain(startDestination = Account.name)
        composeRule.waitUntilTabSelected("nav_tab_account")
        composeRule.onNodeWithTag("accountSettings.developer").performScrollTo().performClick()
        composeRule.onNodeWithTag("developerSettings/colorScheme").performScrollTo().performClick()
        composeRule.onNodeWithText("Primary").assertIsDisplayed()
        composeRule.onRoot().captureToImage().asAndroidBitmap().let { bitmap ->
            composeRule.activity.openFileOutput("split-settings-child.png", 0).use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }
        composeRule.runOnIdle { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.onNodeWithTag("developerSettings/colorScheme").assertIsDisplayed()
        composeRule.onRoot().captureToImage().asAndroidBitmap().let { bitmap ->
            composeRule.activity.openFileOutput("split-settings-return.png", 0).use {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
            }
        }
    }

    @Test
    fun questionOpenedFromArticleRemainsInLandscapeListPane() {
        composeRule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        composeRule.launchZhihuMainWithFakeArticle()

        val article = Article(type = ArticleType.Answer, id = 318L)
        composeRule.runOnIdle {
            composeRule.activity.navigate(article)
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule
                .onAllNodes(hasTestTag("article_question_link"))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        composeRule.onNodeWithTag("article_question_link").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule
                .onAllNodes(hasTestTag(QUESTION_SCREEN_LIST_TAG))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        val listPaneBounds = composeRule
            .onNodeWithTag("list_pane")
            .fetchSemanticsNode()
            .boundsInRoot
        val questionBounds = composeRule
            .onNodeWithTag(QUESTION_SCREEN_LIST_TAG)
            .fetchSemanticsNode()
            .boundsInRoot
        val articleBounds = composeRule
            .onNodeWithTag("article_content")
            .fetchSemanticsNode()
            .boundsInRoot
        assertTrue(
            "Question answer list should be in the left pane: question=$questionBounds list=$listPaneBounds",
            questionBounds.right <= listPaneBounds.right + 0.5f,
        )
        assertTrue(
            "Article content should start in the right pane: article=$articleBounds list=$listPaneBounds",
            articleBounds.left >= listPaneBounds.right,
        )
        composeRule.onNodeWithTag("article_content").assertIsDisplayed()
    }

    @Test
    fun questionUsesSinglePaneWhenPortrait() {
        composeRule.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        composeRule.launchZhihuMainWithFakeArticle()

        composeRule.runOnIdle {
            composeRule.activity.navigate(Article(type = ArticleType.Answer, id = 318L))
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule
                .onAllNodes(hasTestTag("article_question_link"))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithTag("article_question_link").performClick()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule
                .onAllNodes(hasTestTag(QUESTION_SCREEN_LIST_TAG))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }

        val listPaneBounds = composeRule
            .onNodeWithTag("list_pane")
            .fetchSemanticsNode()
            .boundsInRoot
        val questionBounds = composeRule
            .onNodeWithTag(QUESTION_SCREEN_LIST_TAG)
            .fetchSemanticsNode()
            .boundsInRoot
        assertEquals(listPaneBounds.left, questionBounds.left, 0.5f)
        assertEquals(listPaneBounds.right, questionBounds.right, 0.5f)
    }

    /**
     * Related tablet request: https://github.com/zly2006/zhihu-plus-plus/issues/680
     * Regression found reviewing: https://github.com/zly2006/zhihu-plus-plus/pull/754
     * 从窄屏开始阅读再展开时，也必须保留原详情的保存状态，不能重新创建导航项。
     */
    @Test
    fun expandingInitiallyNarrowArticlePreservesState() {
        val width = mutableStateOf(400.dp)
        composeRule.launchZhihuMainWithFakeArticle(width = { width.value })
        composeRule.runOnIdle { composeRule.activity.navigate(Article(type = ArticleType.Answer, id = 318L)) }
        composeRule.onNodeWithTag("article_state").performClick()
        composeRule.onNodeWithText("状态已保留").assertIsDisplayed()
        composeRule.runOnIdle { width.value = 1000.dp }
        composeRule.onNodeWithText("状态已保留").assertIsDisplayed()
    }

    /**
     * Related tablet request: https://github.com/zly2006/zhihu-plus-plus/issues/680
     * Regression found reviewing: https://github.com/zly2006/zhihu-plus-plus/pull/754
     * 页面隐藏时独立窗口必须同步隐藏，返回后恢复窗口，避免评论窗口遮住跳转目标。
     */
    @Test
    fun hiddenDetailDismissesWindowAndRestoresItOnBack() {
        val width = mutableStateOf(1000.dp)
        composeRule.launchZhihuMainWithFakeArticle(width = { width.value })
        composeRule.runOnIdle { composeRule.activity.navigate(Article(type = ArticleType.Answer, id = 318L)) }
        composeRule.runOnIdle { width.value = 400.dp }
        composeRule.onNodeWithTag("article_dialog").performClick()
        composeRule.onNodeWithText("从弹窗打开问题").performClick()
        composeRule.onNodeWithText("从弹窗打开问题").assertDoesNotExist()
        composeRule.onNodeWithTag(QUESTION_SCREEN_LIST_TAG).assertIsDisplayed()
        composeRule.runOnIdle { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.onNodeWithText("从弹窗打开问题").assertIsDisplayed()
    }

    /**
     * Related tablet request: https://github.com/zly2006/zhihu-plus-plus/issues/680
     * Regression found reviewing: https://github.com/zly2006/zhihu-plus-plus/pull/754
     * 阅读中收窄窗口后打开问题必须显示问题，返回时恢复原文章，覆盖真实双栈切换。
     */
    @Test
    fun narrowArticleOpensQuestionAndBackRestoresArticle() {
        val width = mutableStateOf(1000.dp)
        composeRule.launchZhihuMainWithFakeArticle(width = { width.value })
        composeRule.runOnIdle { composeRule.activity.navigate(Article(type = ArticleType.Answer, id = 318L)) }
        composeRule.onNodeWithTag("article_question_link").assertIsDisplayed()
        composeRule.runOnIdle { width.value = 400.dp }
        composeRule.onNodeWithTag("article_question_link").performClick()
        composeRule.onNodeWithTag("article_content").assertIsNotDisplayed()
        composeRule.onNodeWithTag(QUESTION_SCREEN_LIST_TAG).assertIsDisplayed()
        composeRule.runOnIdle { composeRule.activity.onBackPressedDispatcher.onBackPressed() }
        composeRule.onNodeWithTag("article_content").assertIsDisplayed()
    }

    /**
     * Related tablet request: https://github.com/zly2006/zhihu-plus-plus/issues/680
     * Regression found reviewing: https://github.com/zly2006/zhihu-plus-plus/pull/754
     * 保留同一详情导航项内的可保存状态，避免窗口尺寸改变重建详情宿主。
     */
    @Test
    fun resizingPreservesDetailSaveableState() {
        val width = mutableStateOf(1000.dp)
        composeRule.launchZhihuMainWithFakeArticle(width = { width.value })
        composeRule.runOnIdle { composeRule.activity.navigate(Article(type = ArticleType.Answer, id = 318L)) }
        composeRule.onNodeWithTag("article_state").performClick()
        composeRule.onNodeWithText("状态已保留").assertIsDisplayed()
        composeRule.runOnIdle { width.value = 400.dp }
        composeRule.onNodeWithText("状态已保留").assertIsDisplayed()
        composeRule.runOnIdle { width.value = 1000.dp }
        composeRule.onNodeWithText("状态已保留").assertIsDisplayed()
    }

    /**
     * Related tablet request: https://github.com/zly2006/zhihu-plus-plus/issues/680
     * Regression found reviewing: https://github.com/zly2006/zhihu-plus-plus/pull/754
     * 视频平台入口必须收到发起操作的文章控制器，不能从左侧首页推导内容身份。
     */
    @Test
    fun detailVideoUsesArticleController() {
        var videoSource: Article? = null
        composeRule.launchZhihuMainWithFakeArticle(width = { 1000.dp }, onVideo = { controller ->
            videoSource = runCatching { controller.currentBackStackEntry?.toRoute<Article>() }.getOrNull()
        })
        val article = Article(type = ArticleType.Answer, id = 318L)
        composeRule.runOnIdle { composeRule.activity.navigate(article) }
        composeRule.onNodeWithTag("article_video_link").performClick()
        composeRule.runOnIdle { assertEquals(article, videoSource) }
    }

    /**
     * Related tablet request: https://github.com/zly2006/zhihu-plus-plus/issues/680
     * Regression found reviewing: https://github.com/zly2006/zhihu-plus-plus/pull/754
     * 混合听读队列从回答切换到问题时，必须打开问题而不是跳入未注册该路由的详情图。
     */
    @Test
    fun readingQueueCanSwitchFromDetailToQuestion() {
        val width = mutableStateOf(1000.dp)
        val player = ReadingPlayerState(
            status = ReadingPlaybackStatus.Playing,
            queue = listOf(
                ReadingQueueItem(ReadingContentType.Answer, 318L),
                ReadingQueueItem(ReadingContentType.Question, 318L, title = "测试问题"),
            ),
            currentIndex = 0,
        )
        try {
            AndroidReadingPlayerBridge.publish(player)
            composeRule.launchZhihuMainWithFakeArticle(width = { width.value })
            composeRule.runOnIdle { composeRule.activity.navigate(Article(type = ArticleType.Answer, id = 318L)) }
            composeRule.onNodeWithTag("article_content").assertIsDisplayed()
            composeRule.runOnIdle { width.value = 400.dp }
            composeRule.onNodeWithTag("reading_player_queue").performTouchInput { click() }
            composeRule.onNodeWithTag("reading_queue_sheet").assertIsDisplayed()
            Espresso.pressBack()
            composeRule.waitUntil(timeoutMillis = 5_000) {
                composeRule.onAllNodesWithTag("reading_queue_sheet")
                    .fetchSemanticsNodes(atLeastOneRootRequired = false)
                    .isEmpty()
            }
            composeRule.runOnIdle { AndroidReadingPlayerBridge.publish(player.copy(currentIndex = 1)) }
            composeRule.onNodeWithTag(QUESTION_SCREEN_LIST_TAG).assertIsDisplayed()
            composeRule.runOnIdle {
                width.value = 1000.dp
                AndroidReadingPlayerBridge.publish(player)
            }
            composeRule.onNodeWithTag("article_content").assertIsDisplayed()
            composeRule.onNodeWithTag(QUESTION_SCREEN_LIST_TAG).assertIsDisplayed()
        } finally {
            AndroidReadingPlayerBridge.publish(ReadingPlayerState())
        }
    }

    /**
     * Contract: https://github.com/zly2006/zhihu-plus-plus/issues/609
     * Introduced by: https://github.com/zly2006/zhihu-plus-plus/pull/611
     */
    @Test
    fun collectionsTabKeepsLegacyListByDefault() {
        composeRule.launchZhihuMain(
            startDestination = MyCollections.name,
            bottomBarItems = collectionBottomBarItems,
        )

        composeRule.waitUntilTabSelected("nav_tab_mycollections")
        composeRule.onNodeWithTag("collection_screen_title").assertIsDisplayed()
        composeRule.onNodeWithTag("collection_browse_title").assertDoesNotExist()
    }

    /**
     * Contract: https://github.com/zly2006/zhihu-plus-plus/issues/609
     * Introduced by: https://github.com/zly2006/zhihu-plus-plus/pull/611
     */
    @Test
    fun collectionsTabUsesDirectBrowseOnlyWhenEnabled() {
        composeRule.launchZhihuMain(
            startDestination = MyCollections.name,
            bottomBarItems = collectionBottomBarItems,
            collectionDirectBrowseEnabled = true,
        )

        composeRule.waitUntilTabSelected("nav_tab_mycollections")
        composeRule.onNodeWithTag("collection_browse_title").assertIsDisplayed()
        composeRule.onNodeWithTag("collection_screen_title").assertDoesNotExist()
    }

    private fun MainActivityComposeRule.launchZhihuMain(
        startDestination: String,
        bottomBarItems: Set<String> = deterministicBottomBarItems,
        collectionDirectBrowseEnabled: Boolean = false,
    ) {
        activity.getSharedPreferences(PREFERENCE_NAME, android.content.Context.MODE_PRIVATE).edit(commit = true) {
            putString(START_DESTINATION_PREFERENCE_KEY, startDestination)
            putStringSet(BOTTOM_BAR_ITEMS_PREFERENCE_KEY, bottomBarItems)
            putBoolean("duo3_home_account", false)
            putBoolean("bottomBarTapScrollToTop", false)
            putBoolean("autoHideBottomBar", false)
            putBoolean(COLLECTION_DIRECT_BROWSE_PREFERENCE_KEY, collectionDirectBrowseEnabled)
        }
        setZhihuMainContent()
    }

    private fun MainActivityComposeRule.launchZhihuMainWithFakeArticle(
        width: (() -> Dp)? = null,
        onVideo: ((NavHostController) -> Unit)? = null,
    ) {
        activity.getSharedPreferences(PREFERENCE_NAME, android.content.Context.MODE_PRIVATE).edit(commit = true) {
            putString(START_DESTINATION_PREFERENCE_KEY, Home.name)
            putStringSet(BOTTOM_BAR_ITEMS_PREFERENCE_KEY, deterministicBottomBarItems)
            putBoolean("duo3_home_account", false)
            putBoolean("bottomBarTapScrollToTop", false)
            putBoolean("autoHideBottomBar", false)
            putBoolean(COLLECTION_DIRECT_BROWSE_PREFERENCE_KEY, false)
        }
        activity.setContent { }
        waitForIdle()
        activity.setContent {
            ZhihuTheme {
                val navController = rememberNavController()
                LaunchedEffect(navController) {
                    activity.navController = navController
                }
                ZhihuMain(
                    modifier = width?.let { Modifier.requiredSize(it(), 600.dp) } ?: Modifier,
                    navController = navController,
                    mainTabNavigationTarget = activity.mainTabNavigationTarget,
                    navigate = { destination ->
                        if (destination is Video && onVideo != null) onVideo(navController) else activity.navigate(destination)
                    },
                    navigateContent = { destination, controller ->
                        if (destination is Video && onVideo != null) onVideo(controller) else activity.navigateIn(destination, controller)
                    },
                    enableLandscapeListDetail = true,
                    setCurrentMainTabOpenFrom = activity::setCurrentMainTabOpenFrom,
                    consumeMainTabNavigationTarget = activity::consumeMainTabNavigationTarget,
                    preferenceState = rememberAndroidZhihuMainPreferenceState(),
                    isDarkTheme = ThemeManager.isDarkTheme,
                    articleContent = { _, _ ->
                        val navigator = LocalNavigator.current
                        var expanded by rememberSaveable { mutableStateOf(false) }
                        var dialogVisible by rememberSaveable { mutableStateOf(false) }
                        if (dialogVisible) {
                            AlertDialog(
                                onDismissRequest = { dialogVisible = false },
                                text = { Text("详情窗口") },
                                confirmButton = {
                                    Button(onClick = { navigator.onNavigate(Question(318L, "测试问题")) }) {
                                        Text("从弹窗打开问题")
                                    }
                                },
                            )
                        }
                        Column(Modifier.fillMaxSize().testTag("article_content")) {
                            Button(onClick = { dialogVisible = true }, modifier = Modifier.testTag("article_dialog")) {
                                Text("打开窗口")
                            }
                            Button(onClick = { expanded = true }, modifier = Modifier.testTag("article_state")) {
                                Text(if (expanded) "状态已保留" else "更改状态")
                            }
                            Button(onClick = { navigator.onNavigate(Video(1L)) }, modifier = Modifier.testTag("article_video_link")) {
                                Text("播放视频")
                            }
                            Button(
                                onClick = {
                                    navigator.onNavigate(Question(318L, "测试问题"))
                                },
                                modifier = Modifier.testTag("article_question_link"),
                            ) {
                                Text("打开问题")
                            }
                        }
                    },
                )
            }
        }
        waitForIdle()
    }

    private val collectionBottomBarItems = linkedSetOf(
        Home.name,
        Follow.name,
        MyCollections.name,
        Account.name,
    )

    private fun MainActivityComposeRule.waitUntilTabSelected(tag: String) {
        waitUntil(timeoutMillis = 5_000) {
            onAllNodes(hasTestTag(tag).and(isSelectedMatcher()))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    private fun isSelectedMatcher(): SemanticsMatcher = SemanticsMatcher.expectValue(
        SemanticsProperties.Selected,
        true,
    )
}
