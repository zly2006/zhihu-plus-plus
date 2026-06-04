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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.github.zly2006.zhihu.MainActivity
import com.github.zly2006.zhihu.shared.platform.androidUserMessageSink
import com.github.zly2006.zhihu.theme.ThemeManager
import com.github.zly2006.zhihu.theme.ThemeStyle
import com.github.zly2006.zhihu.ui.ArticleAnswerTransitionDirection
import com.github.zly2006.zhihu.ui.miuix.MiuixArticleScreen
import com.github.zly2006.zhihu.viewmodel.AndroidArticlesSharedData
import com.github.zly2006.zhihu.viewmodel.ArticleViewModel

@Composable
fun AndroidZhihuMain(navController: NavHostController) {
    val activity = rememberAndroidZhihuMainActivity()
    ZhihuMain(
        navController = navController,
        navigationState = rememberAndroidZhihuMainNavigationState(),
        preferenceState = rememberAndroidZhihuMainPreferenceState(),
        isDarkTheme = com.github.zly2006.zhihu.theme.ThemeManager.isDarkTheme,
        platformAdapter = androidZhihuMainPlatformAdapter(activity),
    )
}

private fun androidZhihuMainPlatformAdapter(activity: MainActivity) = ZhihuMainPlatformAdapter(
    articleEnterTransition = {
        val sharedData = try {
            ViewModelProvider(activity)[AndroidArticlesSharedData::class.java]
        } catch (_: Exception) {
            null
        }
        when (sharedData?.answerTransitionDirection) {
            ArticleAnswerTransitionDirection.VERTICAL_NEXT ->
                slideInVertically(tween(300)) { it } + fadeIn(tween(300))
            ArticleAnswerTransitionDirection.VERTICAL_PREVIOUS ->
                slideInVertically(tween(300)) { -it } + fadeIn(tween(300))
            ArticleAnswerTransitionDirection.HORIZONTAL_NEXT ->
                slideInHorizontally(tween(300)) { it } + fadeIn(tween(300))
            ArticleAnswerTransitionDirection.HORIZONTAL_PREVIOUS ->
                slideInHorizontally(tween(300)) { -it } + fadeIn(tween(300))
            else -> slideInHorizontally(tween(300)) { it }
        }
    },
    articleExitTransition = {
        val sharedData = try {
            ViewModelProvider(activity)[AndroidArticlesSharedData::class.java]
        } catch (_: Exception) {
            null
        }
        when (sharedData?.answerTransitionDirection) {
            ArticleAnswerTransitionDirection.VERTICAL_NEXT ->
                slideOutVertically(tween(300)) { -it } + fadeOut(tween(300))
            ArticleAnswerTransitionDirection.VERTICAL_PREVIOUS ->
                slideOutVertically(tween(300)) { it } + fadeOut(tween(300))
            ArticleAnswerTransitionDirection.HORIZONTAL_NEXT ->
                slideOutHorizontally(tween(300)) { -it } + fadeOut(tween(300))
            ArticleAnswerTransitionDirection.HORIZONTAL_PREVIOUS ->
                slideOutHorizontally(tween(300)) { it } + fadeOut(tween(300))
            else -> ExitTransition.None
        }
    },
    article = { article, navEntry ->
        val viewModel: ArticleViewModel = viewModel(navEntry) {
            ArticleViewModel(article, activity.httpClient, androidUserMessageSink(activity)) { onPause ->
                navEntry.lifecycle.addObserver(object : DefaultLifecycleObserver {
                    override fun onPause(owner: LifecycleOwner) {
                        onPause()
                    }
                })
            }
        }
        if (ThemeManager.getThemeStyle() == ThemeStyle.Miuix) {
            MiuixArticleScreen(article, viewModel)
        } else {
            ArticleScreen(article, viewModel)
        }
    },
    sentenceSimilarityTest = {
        SentenceSimilarityTestScreen()
    },
    blocklistSettingsNlpContent = { onNavigateBack ->
        NLPKeywordManagementScreen(
            innerPadding = PaddingValues(),
            onNavigateBack = onNavigateBack,
        )
    },
)
