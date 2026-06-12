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

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import com.github.zly2006.zhihu.navigation.Article

/**
 * 回答页内部的“切换到某个回答”入口。
 *
 * 由 [ArticleAnswerSlot] 提供；[ArticleScreen] / [MiuixArticleScreen] 的上一/下一回答按钮调用它，
 * 在**同一个导航 entry 内**用 [AnimatedContent] 切换答案，而不是往返回栈 push 新页面。这样返回键一次性
 * pop 整个回答页回到问题/来源，且切换动画两层互补满屏滑动，不会透出上一个回答。
 */
val LocalArticleAnswerSwitcher = staticCompositionLocalOf<((Article, ArticleAnswerTransitionDirection) -> Unit)?> { null }

/**
 * 把同一回答链承载在单个导航 entry 内的容器。
 *
 * [initialArticle] 是该 entry 的路由参数（从信息流/问题/收藏夹等 push 进来的那个回答）。之后“上一/下一回答”
 * 只更新内部的 [current] 状态并触发 [AnimatedContent] 的方向性转场，返回栈深度保持不变——返回键直接回到来源页。
 * 每个 entry 实例持有自己的状态，因此通过链接进入的另一篇回答是独立 entry，不会被这里折叠。
 *
 * @param content 渲染单个回答的内容（平台注入：创建按 [Article.id] 区分的 ViewModel 并绘制文章页）。
 */
@Composable
fun ArticleAnswerSlot(
    initialArticle: Article,
    content: @Composable (Article) -> Unit,
) {
    var current by remember { mutableStateOf(initialArticle) }
    var direction by remember { mutableStateOf(ArticleAnswerTransitionDirection.DEFAULT) }
    val switcher: (Article, ArticleAnswerTransitionDirection) -> Unit = remember {
        { next, dir ->
            direction = dir
            current = next
        }
    }
    // 不透明背景兜底：回答页内容若有未铺满/透明区域，切换时也只露出本主题背景色，
    // 绝不透出下方的主页/信息流（NavDisplay 会保留被覆盖层）。
    // 用与正文页 Scaffold 相同的 MaterialTheme.colorScheme.background（miuix 派生），保证同色（暗色=黑）。
    val background = MaterialTheme.colorScheme.background
    CompositionLocalProvider(LocalArticleAnswerSwitcher provides switcher) {
        AnimatedContent(
            targetState = current,
            transitionSpec = { articleAnswerContentTransform(direction) },
            contentKey = { it.id },
            modifier = Modifier.fillMaxSize().background(background),
            label = "article-answer-switch",
        ) { answer ->
            content(answer)
        }
    }
}

/**
 * 按 [direction] 给出回答切换的进出场转场：进入层与离开层向相反方向**满屏纯位移**，两层互补覆盖整屏。
 *
 * 刻意不加 fade：回答页（尤其 miuix 的磨砂顶栏走 textureBlur + 透明容器）一旦被 alpha 淡化，整页会半透明，
 * 看起来“不是 miuix 形式”，待 fade 结束才突变回磨砂样式。纯位移让进入页全程不透明，miuix 磨砂顶栏从第一帧就在。
 * [ArticleAnswerTransitionDirection.DEFAULT] 仅初始/异常路径出现，退化为淡入淡出。
 */
private fun AnimatedContentTransitionScope<Article>.articleAnswerContentTransform(
    direction: ArticleAnswerTransitionDirection,
): ContentTransform {
    val intSpec = tween<androidx.compose.ui.unit.IntOffset>(300)
    val transform = when (direction) {
        ArticleAnswerTransitionDirection.VERTICAL_NEXT ->
            slideInVertically(intSpec) { it } togetherWith slideOutVertically(intSpec) { -it }

        ArticleAnswerTransitionDirection.VERTICAL_PREVIOUS ->
            slideInVertically(intSpec) { -it } togetherWith slideOutVertically(intSpec) { it }

        ArticleAnswerTransitionDirection.HORIZONTAL_NEXT ->
            slideInHorizontally(intSpec) { it } togetherWith slideOutHorizontally(intSpec) { -it }

        ArticleAnswerTransitionDirection.HORIZONTAL_PREVIOUS ->
            slideInHorizontally(intSpec) { -it } togetherWith slideOutHorizontally(intSpec) { it }

        ArticleAnswerTransitionDirection.DEFAULT ->
            fadeIn(tween(300)) togetherWith fadeOut(tween(300))
    }
    // 同尺寸全屏页面，关闭尺寸形变裁剪，避免切换时出现裁切。
    return transform.using(SizeTransform(clip = false))
}
