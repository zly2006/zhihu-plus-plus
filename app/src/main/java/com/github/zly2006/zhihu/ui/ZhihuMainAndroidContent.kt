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

import androidx.activity.compose.LocalActivity
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.github.zly2006.zhihu.MainActivity
import com.github.zly2006.zhihu.shared.platform.androidUserMessageSink
import com.github.zly2006.zhihu.ui.ArticleAnswerTransitionDirection
import com.github.zly2006.zhihu.viewmodel.AndroidArticlesSharedData
import com.github.zly2006.zhihu.viewmodel.ArticleViewModel

/**
 * Android 平台的 Zhihu++ 主界面入口。
 *
 * 这里把 [MainActivity] 持有的导航、偏好设置、文章页 ViewModel、回答切换转场和 NLP 页面适配到共享 [ZhihuMain]。
 * UI 结构仍由 common 主壳负责，Android 只提供生命周期、Activity、ViewModel 和平台专属页面实现。
 */
@Composable
fun AndroidZhihuMain(navController: NavHostController) {
    val activity = LocalActivity.current as MainActivity
    CompositionLocalProvider(LocalMainTabSelectionRequester provides activity::navigateMainTab) {
        ZhihuMain(
            navController = navController,
            navigationState = rememberAndroidZhihuMainNavigationState(),
            preferenceState = rememberAndroidZhihuMainPreferenceState(),
            isDarkTheme = com.github.zly2006.zhihu.theme.ThemeManager.isDarkTheme,
            platformAdapter = androidZhihuMainPlatformAdapter(activity),
        )
    }
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
        ArticleScreen(article, viewModel)
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
