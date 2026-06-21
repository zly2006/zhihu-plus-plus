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

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.zly2006.zhihu.MainActivity
import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.shared.platform.androidUserMessageSink
import com.github.zly2006.zhihu.theme.ThemeManager
import com.github.zly2006.zhihu.theme.ThemeStyle
import com.github.zly2006.zhihu.ui.miuix.MiuixArticleScreen
import com.github.zly2006.zhihu.viewmodel.ArticleViewModel
import top.yukonga.miuix.kmp.nav.core.NavController

/**
 * Android 平台的 Zhihu++ 主界面入口。
 *
 * 这里把 [MainActivity] 持有的导航、偏好设置、文章页 ViewModel、回答切换转场和 NLP 页面适配到共享 [ZhihuMain]。
 * UI 结构仍由 common 主壳负责，Android 只提供生命周期、Activity、ViewModel 和平台专属页面实现。
 */
@Composable
fun AndroidZhihuMain(navController: NavController<NavDestination>) {
    val activity = rememberAndroidZhihuMainActivity()
    ZhihuMain(
        navController = navController,
        navigationState = rememberAndroidZhihuMainNavigationState(),
        preferenceState = rememberAndroidZhihuMainPreferenceState(),
        isDarkTheme = com.github.zly2006.zhihu.theme.ThemeManager.isDarkTheme,
        platformAdapter = androidZhihuMainPlatformAdapter(activity),
    )
}

private fun androidZhihuMainPlatformAdapter(activity: MainActivity): ZhihuMainPlatformAdapter = ZhihuMainPlatformAdapter(
    article = { article ->
        val lifecycleOwner = LocalLifecycleOwner.current
        // 同一回答链共用一个导航 entry 的 store，故按回答 id 区分 ViewModel（见 ArticleAnswerSlot）。
        val viewModel: ArticleViewModel = viewModel(key = "article-${article.id}") {
            ArticleViewModel(article, activity.httpClient, androidUserMessageSink(activity)) { onPause ->
                lifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
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
