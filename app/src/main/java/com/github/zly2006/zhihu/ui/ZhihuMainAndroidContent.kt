/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
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

import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.github.zly2006.zhihu.MainActivity
import com.github.zly2006.zhihu.navigation.Account
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.CollectionContent
import com.github.zly2006.zhihu.navigation.Collections
import com.github.zly2006.zhihu.navigation.Daily
import com.github.zly2006.zhihu.navigation.Follow
import com.github.zly2006.zhihu.navigation.History
import com.github.zly2006.zhihu.navigation.Home
import com.github.zly2006.zhihu.navigation.HotList
import com.github.zly2006.zhihu.navigation.Notification
import com.github.zly2006.zhihu.navigation.OnlineHistory
import com.github.zly2006.zhihu.navigation.Person
import com.github.zly2006.zhihu.navigation.Pin
import com.github.zly2006.zhihu.navigation.Question
import com.github.zly2006.zhihu.navigation.Search
import com.github.zly2006.zhihu.navigation.SentenceSimilarityTest
import com.github.zly2006.zhihu.theme.ThemeManager
import com.github.zly2006.zhihu.ui.subscreens.AppearanceSettingsScreen
import com.github.zly2006.zhihu.ui.subscreens.BlockedFeedHistoryScreen
import com.github.zly2006.zhihu.ui.subscreens.ColorSchemeScreen
import com.github.zly2006.zhihu.ui.subscreens.ContentFilterSettingsScreen
import com.github.zly2006.zhihu.ui.subscreens.DeveloperSettingsScreen
import com.github.zly2006.zhihu.ui.subscreens.OpenSourceLicensesScreen
import com.github.zly2006.zhihu.ui.subscreens.SystemAndUpdateSettingsScreen
import com.github.zly2006.zhihu.viewmodel.ArticleViewModel

@Composable
fun AndroidZhihuMain(navController: NavHostController) {
    val activity = rememberAndroidZhihuMainActivity()
    ZhihuMain(
        navController = navController,
        navigationState = rememberAndroidZhihuMainNavigationState(),
        preferenceState = rememberAndroidZhihuMainPreferenceState(),
        isDarkTheme = ThemeManager.isDarkTheme,
        mainTabContent = ::AndroidZhihuMainTabContent,
        routeContent = {
            androidZhihuMainRouteContent(
                activity = activity,
                scope = it,
            )
        },
    )
}

@Composable
private fun AndroidZhihuMainTabContent(scope: ZhihuMainTabContentScope) {
    when (scope.destination) {
        Home -> HomeScreen(
            scrollToTopTrigger = scope.scrollToTopTrigger,
            innerPadding = scope.innerPadding,
        )
        Follow -> FollowTopLevelPage(
            selectedTabIndex = scope.followTabIndex ?: 0,
            onTabSelected = scope.onFollowTabSelected,
            scrollToTopTrigger = scope.scrollToTopTrigger,
            innerPadding = scope.innerPadding,
            isActive = scope.isActive,
        )
        HotList -> HotListScreen(scope.innerPadding)
        Daily -> DailyScreen()
        OnlineHistory -> OnlineHistoryScreen()
        Account -> AccountSettingScreen(scope.innerPadding)
        else -> {}
    }
}

private fun NavGraphBuilder.androidZhihuMainRouteContent(
    activity: MainActivity,
    scope: ZhihuMainRouteContentScope,
) {
    composable<Question> { navEntry ->
        val question: Question = navEntry.toRoute()
        QuestionScreen(question)
    }
    composable<Article>(
        enterTransition = {
            val sharedData = try {
                ViewModelProvider(activity)[ArticleViewModel.ArticlesSharedData::class.java]
            } catch (_: Exception) {
                null
            }
            when (sharedData?.answerTransitionDirection) {
                ArticleViewModel.AnswerTransitionDirection.VERTICAL_NEXT ->
                    slideInVertically(tween(300)) { it } + fadeIn(tween(300))
                ArticleViewModel.AnswerTransitionDirection.VERTICAL_PREVIOUS ->
                    slideInVertically(tween(300)) { -it } + fadeIn(tween(300))
                ArticleViewModel.AnswerTransitionDirection.HORIZONTAL_NEXT ->
                    slideInHorizontally(tween(300)) { it } + fadeIn(tween(300))
                ArticleViewModel.AnswerTransitionDirection.HORIZONTAL_PREVIOUS ->
                    slideInHorizontally(tween(300)) { -it } + fadeIn(tween(300))
                else -> slideInHorizontally(tween(300)) { it }
            }
        },
        exitTransition = {
            val sharedData = try {
                (activity as? androidx.activity.ComponentActivity)
                    ?.let { ViewModelProvider(it)[ArticleViewModel.ArticlesSharedData::class.java] }
            } catch (_: Exception) {
                null
            }
            when (sharedData?.answerTransitionDirection) {
                ArticleViewModel.AnswerTransitionDirection.VERTICAL_NEXT ->
                    slideOutVertically(tween(300)) { -it } + fadeOut(tween(300))
                ArticleViewModel.AnswerTransitionDirection.VERTICAL_PREVIOUS ->
                    slideOutVertically(tween(300)) { it } + fadeOut(tween(300))
                ArticleViewModel.AnswerTransitionDirection.HORIZONTAL_NEXT ->
                    slideOutHorizontally(tween(300)) { -it } + fadeOut(tween(300))
                ArticleViewModel.AnswerTransitionDirection.HORIZONTAL_PREVIOUS ->
                    slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300))
                else -> ExitTransition.None
            }
        },
    ) { navEntry ->
        val article: Article = navEntry.toRoute()
        val viewModel: ArticleViewModel = viewModel(navEntry) {
            ArticleViewModel(article, activity.httpClient, navEntry)
        }
        ArticleScreen(article, viewModel)
    }
    composable<HotList> {
        HotListScreen(scope.innerPadding)
    }
    composable<Follow> {
        FollowScreen(
            scrollToTopTrigger = scope.scrollToTopTrigger,
            innerPadding = scope.innerPadding,
        )
    }
    composable<Daily> {
        DailyScreen()
    }
    composable<History> {
        LegacyLocalHistoryScreen(scope.innerPadding)
    }
    composable<OnlineHistory> {
        OnlineHistoryScreen()
    }
    composable<Account> {
        AccountSettingScreen(scope.innerPadding)
    }
    composable<Search> { navEntry ->
        val search: Search = navEntry.toRoute()
        SearchScreen(search)
    }
    composable<Collections> {
        val data: Collections = it.toRoute()
        CollectionScreen(data.userToken)
    }
    composable<CollectionContent> {
        val content: CollectionContent = it.toRoute()
        CollectionContentScreen(content.collectionId)
    }
    composable<Person> {
        val person: Person = it.toRoute()
        PeopleScreen(person)
    }
    composable<Pin> {
        val pin = it.toRoute<Pin>()
        PinScreen(pin)
    }
    composable<Account.RecommendSettings.Blocklist> {
        BlocklistSettingsScreen()
    }
    composable<Account.RecommendSettings.BlockedFeedHistory> {
        BlockedFeedHistoryScreen()
    }
    composable<Notification> {
        NotificationScreen()
    }
    composable<Notification.NotificationSettings> {
        NotificationSettingsScreen()
    }
    composable<SentenceSimilarityTest> {
        SentenceSimilarityTestScreen()
    }
    composable<Account.AppearanceSettings> {
        val args = it.toRoute<Account.AppearanceSettings>()
        AppearanceSettingsScreen(
            setting = args.setting,
            onExit = scope.reloadBottomBarPreferences,
        )
    }
    composable<Account.RecommendSettings> {
        val args = it.toRoute<Account.RecommendSettings>()
        ContentFilterSettingsScreen(args.setting)
    }
    composable<Account.SystemAndUpdateSettings> {
        SystemAndUpdateSettingsScreen()
    }
    composable<Account.OpenSourceLicenses> {
        OpenSourceLicensesScreen()
    }
    composable<Account.DeveloperSettings> {
        DeveloperSettingsScreen()
    }
    composable<Account.DeveloperSettings.ColorScheme> {
        ColorSchemeScreen()
    }
}
