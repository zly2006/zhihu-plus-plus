@file:Suppress("FunctionName")

package com.github.zly2006.zhihu.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * 左右 HorizontalPager 切换回答容器。
 * 类似 ViewPager 的水平滑动切换，展示完整的回答内容。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AnswerHorizontalPager(
    canGoPrevious: Boolean,
    canGoNext: Boolean,
    onNavigatePrevious: () -> Unit,
    onNavigateNext: () -> Unit,
    currentContent: @Composable () -> Unit,
    previousContent: (@Composable () -> Unit)?,
    nextContent: (@Composable () -> Unit)?,
) {
    val hapticFeedback = LocalHapticFeedback.current

    // 计算页面数和初始页面索引
    val pageCount = 1 + (if (canGoPrevious) 1 else 0) + (if (canGoNext) 1 else 0)
    val initialPage = if (canGoPrevious) 1 else 0

    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { pageCount },
    )

    var lastSettledPage by remember { mutableIntStateOf(initialPage) }

    // 监听页面切换完成
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .distinctUntilChanged()
            .collect { settledPage ->
                if (settledPage != lastSettledPage) {
                    val navigatedToPrevious = settledPage < lastSettledPage
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (navigatedToPrevious && canGoPrevious) {
                        onNavigatePrevious()
                    } else if (!navigatedToPrevious && canGoNext) {
                        onNavigateNext()
                    }
                    lastSettledPage = settledPage
                }
            }
    }

    // 监听拖拽过程中跨越半页阈值时的震动反馈
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPageOffsetFraction }
            .collect { offset ->
                if (kotlin.math.abs(offset) > 0.3f) {
                    // 只在首次跨越时震动，通过 distinctUntilChanged 避免重复
                }
            }
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        beyondViewportPageCount = 1,
        flingBehavior = PagerDefaults.flingBehavior(
            state = pagerState,
            snapAnimationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow,
            ),
        ),
    ) { page ->
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                canGoPrevious && page == 0 -> previousContent?.invoke()
                page == initialPage -> currentContent()
                else -> nextContent?.invoke()
            }
        }
    }
}
