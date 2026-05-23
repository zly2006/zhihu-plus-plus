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
import android.webkit.WebView
import com.github.zly2006.zhihu.ui.ArticleAnswerTransitionDirection
import com.github.zly2006.zhihu.ui.ArticlePreviewWebViewStore
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import com.github.zly2006.zhihu.ui.components.CustomWebView
import com.github.zly2006.zhihu.ui.components.setupUpWebviewClient

class AndroidArticlePreviewWebViewStore : ArticlePreviewWebViewStore {
    var mainWebView: CustomWebView? = null
        private set
    var previousPreviewWebView: CustomWebView? = null
        private set
    var nextPreviewWebView: CustomWebView? = null
        private set

    var mainTag: String? = null
        private set
    var prevTag: String? = null
        private set
    var nextTag: String? = null
        private set

    fun getOrCreateMainWebView(context: Context, answerId: Long): CustomWebView {
        mainWebView?.let { return it }
        return createCachedWebView(context)
            .also {
                mainWebView = it
                mainTag = "wv_main_$answerId"
                it.tag = mainTag
            }
    }

    fun promoteForNavigation(direction: ArticleAnswerTransitionDirection) {
        when (direction) {
            ArticleAnswerTransitionDirection.HORIZONTAL_NEXT, ArticleAnswerTransitionDirection.VERTICAL_NEXT -> {
                previousPreviewWebView?.destroy()
                previousPreviewWebView = mainWebView
                prevTag = mainTag
                mainWebView = nextPreviewWebView
                mainTag = nextTag
                nextPreviewWebView = null
                nextTag = null
            }
            ArticleAnswerTransitionDirection.HORIZONTAL_PREVIOUS, ArticleAnswerTransitionDirection.VERTICAL_PREVIOUS -> {
                nextPreviewWebView?.destroy()
                nextPreviewWebView = mainWebView
                nextTag = mainTag
                mainWebView = previousPreviewWebView
                mainTag = prevTag
                previousPreviewWebView = null
                prevTag = null
            }
            else -> {}
        }
    }

    override fun getOrCreatePreviewWebView(
        context: Context,
        isNext: Boolean,
        answerId: Long,
    ): CustomWebView {
        val existing = if (isNext) nextPreviewWebView else previousPreviewWebView
        if (existing != null) return existing
        return createCachedWebView(context)
            .also {
                if (isNext) {
                    nextPreviewWebView = it
                    nextTag = "wv_next_$answerId"
                    it.tag = nextTag
                } else {
                    previousPreviewWebView = it
                    prevTag = "wv_prev_$answerId"
                    it.tag = prevTag
                }
            }
    }

    fun clearContentIds() {
        mainWebView?.contentId = null
        previousPreviewWebView?.contentId = null
        nextPreviewWebView?.contentId = null
    }

    fun destroyAll() {
        mainWebView?.destroy()
        mainWebView = null
        mainTag = null
        previousPreviewWebView?.destroy()
        previousPreviewWebView = null
        prevTag = null
        nextPreviewWebView?.destroy()
        nextPreviewWebView = null
        nextTag = null
    }

    private fun createCachedWebView(context: Context): CustomWebView {
        val preferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        val useHardwareAcceleration = preferences.getBoolean("webviewHardwareAcceleration", true)
        return CustomWebView(context)
            .apply {
                if (useHardwareAcceleration) {
                    setLayerType(WebView.LAYER_TYPE_HARDWARE, null)
                } else {
                    setLayerType(WebView.LAYER_TYPE_SOFTWARE, null)
                }
                setupUpWebviewClient()
            }
    }
}
