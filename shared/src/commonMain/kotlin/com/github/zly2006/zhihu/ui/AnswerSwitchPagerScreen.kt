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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.zly2006.zhihu.navigation.AnswerNavigator
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.shared.platform.UserMessageSink
import com.github.zly2006.zhihu.ui.components.DEFAULT_ANSWER_SWITCH_SENSITIVITY
import com.github.zly2006.zhihu.ui.components.LocalVerticalPagerScrollGate
import com.github.zly2006.zhihu.ui.components.normalizedAnswerSwitchSensitivity
import com.github.zly2006.zhihu.ui.components.rememberVerticalPagerScrollGate
import com.github.zly2006.zhihu.viewmodel.ArticleViewModel
import com.github.zly2006.zhihu.viewmodel.PaginationEnvironment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class AnswerPagerOrientation {
    Horizontal,
    Vertical,
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AnswerSwitchPagerScreen(
    orientation: AnswerPagerOrientation,
    initialArticle: Article,
    initialViewModel: ArticleViewModel,
    navigator: AnswerNavigator,
    environment: PaginationEnvironment,
    userMessages: UserMessageSink,
    answerSwitchSensitivity: Float = DEFAULT_ANSWER_SWITCH_SENSITIVITY,
) {
    @Suppress("UNUSED_VARIABLE")
    val queueRevision = navigator.queueRevision
    val answerIds = navigator.session.orderedIds.toList()
    if (answerIds.isEmpty()) {
        ArticleScreen(
            article = initialArticle,
            viewModel = initialViewModel,
            answerSwitchPagerEnabled = false,
            isPagerPageActive = true,
        )
        return
    }

    val coroutineScope = rememberCoroutineScope()
    val initialPage = answerIds
        .indexOf(initialArticle.id)
        .takeIf { it >= 0 }
        ?: navigator.session.cursor.coerceIn(0, answerIds.lastIndex)
    val pagerState = rememberPagerState(initialPage = initialPage) { answerIds.size }
    val normalizedSensitivity = normalizedAnswerSwitchSensitivity(answerSwitchSensitivity)
    val snapPositionalThreshold = 0.3f / normalizedSensitivity
    val flingBehavior = PagerDefaults.flingBehavior(
        state = pagerState,
        snapPositionalThreshold = snapPositionalThreshold,
    )

    suspend fun loadPageAtEdge(isNext: Boolean): Int? {
        val cached = withContext(Dispatchers.Default) {
            if (isNext) {
                navigator.loadNextCached()
            } else {
                navigator.loadPreviousCached()
            }
        } ?: return null
        return navigator.session.orderedIds
            .indexOf(cached.article.id)
            .takeIf { it >= 0 }
    }

    fun navigatePager(isNext: Boolean) {
        coroutineScope.launch {
            val directTarget = pagerState.currentPage + if (isNext) 1 else -1
            val target = directTarget.takeIf { it in navigator.session.orderedIds.indices }
                ?: loadPageAtEdge(isNext)
                ?: return@launch
            pagerState.animateScrollToPage(target)
        }
    }

    LaunchedEffect(pagerState.settledPage, answerIds) {
        val currentAnswerId = answerIds.getOrNull(pagerState.settledPage) ?: return@LaunchedEffect
        withContext(Dispatchers.Default) {
            navigator.onPageSettled(
                articleId = currentAnswerId,
                direction = null,
                paginationInfo = null,
            ) { id ->
                navigator.prefetchPrevious(id)
                navigator.prefetchNext(id)
            }
        }
    }

    val verticalScrollGate = if (orientation == AnswerPagerOrientation.Vertical) {
        rememberVerticalPagerScrollGate(
            onNavigatePrevious = { navigatePager(isNext = false) },
            onNavigateNext = { navigatePager(isNext = true) },
            canGoPrevious = { navigator.hasPreviousCandidate },
            canGoNext = { navigator.hasNextCandidate },
            answerSwitchSensitivity = answerSwitchSensitivity,
        )
    } else {
        null
    }

    @Composable
    fun PagerPage(page: Int) {
        val answerId = answerIds[page]
        val pageArticle = remember(answerId) {
            Article(
                id = answerId,
                type = ArticleType.Answer,
                answerSessionId = initialArticle.answerSessionId,
            )
        }
        val initialCachedContent = navigator.session.entryById[answerId]?.cached
        val pageViewModel: ArticleViewModel = if (answerId == initialArticle.id) {
            initialViewModel
        } else {
            viewModel(key = "answer_pager_$answerId") {
                ArticleViewModel(
                    article = pageArticle,
                    httpClient = environment.httpClient(),
                    userMessages = userMessages,
                    initialCachedContent = initialCachedContent,
                )
            }
        }
        ArticleScreen(
            article = pageArticle,
            viewModel = pageViewModel,
            answerSwitchPagerEnabled = false,
            pagerNavigateToPrevious = { navigatePager(isNext = false) },
            pagerNavigateToNext = { navigatePager(isNext = true) },
            isPagerPageActive = page == pagerState.settledPage,
        )
    }

    when (orientation) {
        AnswerPagerOrientation.Horizontal -> {
            HorizontalPager(
                state = pagerState,
                key = { page -> answerIds[page] },
                flingBehavior = flingBehavior,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                PagerPage(page)
            }
        }
        AnswerPagerOrientation.Vertical -> {
            CompositionLocalProvider(LocalVerticalPagerScrollGate provides verticalScrollGate) {
                VerticalPager(
                    state = pagerState,
                    key = { page -> answerIds[page] },
                    flingBehavior = flingBehavior,
                    beyondViewportPageCount = 1,
                    userScrollEnabled = false,
                    modifier = Modifier.fillMaxSize(),
                ) { page ->
                    PagerPage(page)
                }
            }
        }
    }
}
