/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 */

package com.github.zly2006.zhihu.viewmodel

import android.content.Context
import com.github.zly2006.zhihu.ui.ArticleAnswerTransitionDirection
import com.github.zly2006.zhihu.ui.ArticlePreviewWebViewStore

class AndroidArticlesSharedData :
    ArticleViewModel.ArticlesSharedData(),
    ArticlePreviewWebViewStore {
    private val previewWebViews = AndroidArticlePreviewWebViewStore()

    fun getOrCreateMainWebView(context: Context, answerId: Long) =
        previewWebViews.getOrCreateMainWebView(context, answerId)

    /**
     * 导航时旋转三个 WebView：
     * NEXT: prev→destroy, main→prev, next→main
     * PREVIOUS: next→destroy, main→next, prev→main
     */
    override fun promoteForNavigation(direction: ArticleAnswerTransitionDirection) {
        super.promoteForNavigation(direction)
        previewWebViews.promoteForNavigation(direction)
    }

    override fun getOrCreatePreviewWebView(
        context: Context,
        isNext: Boolean,
        answerId: Long,
    ) = previewWebViews.getOrCreatePreviewWebView(context, isNext, answerId)

    override fun reset() {
        super.reset()
        previewWebViews.clearContentIds()
    }

    override fun onCleared() {
        previewWebViews.destroyAll()
        super.onCleared()
    }
}
