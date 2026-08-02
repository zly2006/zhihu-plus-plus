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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.toRoute
import com.github.zly2006.zhihu.navigation.AnswerNavigator
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.Navigator
import com.github.zly2006.zhihu.viewmodel.ArticleViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Stable
internal class ArticleAnswerNavigationState(
    private val switchState: ArticleAnswerSwitchState?,
    private val viewModel: ArticleViewModel,
    private val navigator: Navigator,
    private val navController: NavHostController?,
    private val coroutineScope: CoroutineScope,
    answerSwitchMode: String,
) {
    var answerSwitchMode by mutableStateOf(answerSwitchMode)
        internal set
    var navigatingToNextAnswer by mutableStateOf(false)
        private set

    val answerNavigator: AnswerNavigator?
        get() = switchState?.navigator

    fun prepareArticle() {
        val state = switchState ?: return
        if (!state.navigatingFromAnswerSwitch) {
            state.reset()
        }
        state.navigatingFromAnswerSwitch = false
        state.answerTransitionDirection = ArticleAnswerTransitionDirection.DEFAULT

        val pending = state.pendingInitialContent ?: return
        viewModel.title = pending.title
        viewModel.authorName = pending.authorName
        viewModel.authorBio = pending.authorBio
        viewModel.authorAvatarSrc = pending.authorAvatarUrl
        viewModel.content = pending.content
        viewModel.voteUpCount = pending.voteUpCount
        viewModel.commentCount = pending.commentCount
        viewModel.endorsements = pending.endorsements
        state.pendingInitialContent = null
    }

    fun navigateToPrevious() {
        val state = switchState ?: return
        val answerNavigator = state.navigator ?: return
        state.answerTransitionDirection = if (answerSwitchMode == "horizontal") {
            ArticleAnswerTransitionDirection.HORIZONTAL_PREVIOUS
        } else {
            ArticleAnswerTransitionDirection.VERTICAL_PREVIOUS
        }
        state.navigatingFromAnswerSwitch = true
        answerNavigator.pushAnswer(viewModel.toCachedContent(sourceLabel = answerNavigator.sourceName))

        val previous = answerNavigator.goToPrevious()
        if (previous != null) {
            state.pendingInitialContent = previous
            state.promoteForNavigation(state.answerTransitionDirection)
            navigate(previous.article)
            return
        }

        state.pendingInitialContent = answerNavigator.previousAnswerPreview
        state.promoteForNavigation(state.answerTransitionDirection)
        coroutineScope.launch {
            val loaded = answerNavigator.loadPrevious() ?: return@launch
            state.pendingInitialContent = loaded
            navigate(loaded.article)
        }
    }

    fun navigateToNext() {
        if (navigatingToNextAnswer) return
        val state = switchState ?: return
        val answerNavigator = state.navigator ?: return
        state.answerTransitionDirection = if (answerSwitchMode == "horizontal") {
            ArticleAnswerTransitionDirection.HORIZONTAL_NEXT
        } else {
            ArticleAnswerTransitionDirection.VERTICAL_NEXT
        }
        state.navigatingFromAnswerSwitch = true
        answerNavigator.pushAnswer(viewModel.toCachedContent(sourceLabel = answerNavigator.sourceName))

        val historyNext = answerNavigator.goToNext()
        if (historyNext != null) {
            state.pendingInitialContent = historyNext
            state.promoteForNavigation(state.answerTransitionDirection)
            navigate(historyNext.article)
            return
        }

        state.pendingInitialContent = answerNavigator.nextAnswer
        state.promoteForNavigation(state.answerTransitionDirection)
        navigatingToNextAnswer = true
        coroutineScope.launch {
            try {
                answerNavigator.loadNext()?.let(::navigate)
            } finally {
                navigatingToNextAnswer = false
            }
        }
    }

    private fun navigate(article: Article) {
        if (navController?.currentBackStackEntry?.hasRoute(Article::class) == true &&
            navController.currentBackStackEntry?.toRoute<Article>()?.type == ArticleType.Answer
        ) {
            navController.popBackStack()
        }
        navigator.onNavigate(article)
    }
}

@Composable
internal fun rememberArticleAnswerNavigationState(
    switchState: ArticleAnswerSwitchState?,
    viewModel: ArticleViewModel,
    navigator: Navigator,
    navController: NavHostController?,
    answerSwitchMode: String,
): ArticleAnswerNavigationState {
    val coroutineScope = rememberCoroutineScope()
    val state = remember(switchState, viewModel, navigator, navController, coroutineScope) {
        ArticleAnswerNavigationState(
            switchState = switchState,
            viewModel = viewModel,
            navigator = navigator,
            navController = navController,
            coroutineScope = coroutineScope,
            answerSwitchMode = answerSwitchMode,
        )
    }
    SideEffect {
        state.answerSwitchMode = answerSwitchMode
    }
    return state
}
