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
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.core.content.edit
import androidx.navigation.compose.rememberNavController
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

    private fun MainActivityComposeRule.launchZhihuMainWithFakeArticle() {
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
                    navController = navController,
                    mainTabNavigationTarget = activity.mainTabNavigationTarget,
                    navigate = activity::navigate,
                    navigateContent = activity::navigateIn,
                    enableLandscapeListDetail = true,
                    setCurrentMainTabOpenFrom = activity::setCurrentMainTabOpenFrom,
                    consumeMainTabNavigationTarget = activity::consumeMainTabNavigationTarget,
                    preferenceState = rememberAndroidZhihuMainPreferenceState(),
                    isDarkTheme = ThemeManager.isDarkTheme,
                    articleContent = { _, _ ->
                        val navigator = LocalNavigator.current
                        Box(Modifier.fillMaxSize().testTag("article_content")) {
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
