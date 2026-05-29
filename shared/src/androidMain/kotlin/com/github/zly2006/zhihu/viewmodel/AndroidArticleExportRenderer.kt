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
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Handler
import android.os.Looper
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebView
import com.github.zly2006.zhihu.shared.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class AndroidPreparedExportWebView(
    val webView: WebView,
    val viewportWidthPx: Int,
    val contentHeightPx: Int,
) : PreparedArticleExportContent

class AndroidArticleExportRenderer(
    private val context: Context,
    private val loadAssetText: (String) -> String,
) : ArticleImageExportRenderer {
    override suspend fun prepareExportWebView(
        htmlContent: String,
        timeoutMs: Long,
    ): PreparedArticleExportContent = withContext(Dispatchers.Main) {
        suspendCancellableCoroutine { continuation ->
            val webView = createExportWebView()
            val mainHandler = Handler(Looper.getMainLooper())
            val viewportWidthPx = resolveExportViewportWidthPx()
            var isFinished = false
            var timeoutRunnable = Runnable {}

            fun fail(error: Throwable) {
                if (isFinished) return
                isFinished = true
                mainHandler.removeCallbacks(timeoutRunnable)
                runCatching { webView.stopLoading() }
                runCatching { webView.destroy() }
                if (continuation.isActive) {
                    continuation.resumeWithException(error)
                }
            }

            fun finish(contentHeightPx: Int) {
                if (isFinished) return
                isFinished = true
                mainHandler.removeCallbacks(timeoutRunnable)
                if (continuation.isActive) {
                    continuation.resume(
                        AndroidPreparedExportWebView(
                            webView = webView,
                            viewportWidthPx = viewportWidthPx,
                            contentHeightPx = contentHeightPx,
                        ),
                    )
                }
            }

            fun scheduleReadinessCheck(
                attempt: Int = 0,
                lastHeightPx: Int = -1,
                stablePasses: Int = 0,
            ) {
                mainHandler.postDelayed({
                    if (isFinished) return@postDelayed

                    val contentHeightPx = computeExportContentHeightPx(webView)
                    if (contentHeightPx <= 1 && attempt >= 24) {
                        fail(IllegalStateException("内容为空"))
                        return@postDelayed
                    }

                    measureAndLayoutExportWebView(
                        webView = webView,
                        widthPx = viewportWidthPx,
                        heightPx = contentHeightPx.coerceAtLeast(1),
                    )

                    val nextStablePasses = if (contentHeightPx == lastHeightPx) stablePasses + 1 else 0
                    if (contentHeightPx > 1 && (nextStablePasses >= 2 || attempt >= 24)) {
                        finish(contentHeightPx)
                    } else {
                        scheduleReadinessCheck(
                            attempt = attempt + 1,
                            lastHeightPx = contentHeightPx,
                            stablePasses = nextStablePasses,
                        )
                    }
                }, if (attempt == 0) 450L else 180L)
            }

            timeoutRunnable = Runnable {
                fail(IllegalStateException("超时"))
            }

            webView.webViewClient = object : android.webkit.WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    super.onPageFinished(view, url)
                    if (!isFinished) {
                        injectExportFootnoteScript(webView) {
                            if (!isFinished) {
                                scheduleReadinessCheck()
                            }
                        }
                    }
                }

                override fun onReceivedError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    error: android.webkit.WebResourceError?,
                ) {
                    super.onReceivedError(view, request, error)
                    if (request?.isForMainFrame != false) {
                        fail(IllegalStateException("加载错误"))
                    }
                }
            }

            measureAndLayoutExportWebView(
                webView = webView,
                widthPx = viewportWidthPx,
                heightPx = 1,
            )
            mainHandler.postDelayed(timeoutRunnable, timeoutMs)
            webView.loadDataWithBaseURL(
                "https://www.zhihu.com",
                htmlContent,
                "text/html",
                "UTF-8",
                null,
            )

            continuation.invokeOnCancellation {
                if (!isFinished) {
                    isFinished = true
                    mainHandler.removeCallbacks(timeoutRunnable)
                    runCatching { webView.stopLoading() }
                    runCatching { webView.destroy() }
                }
            }
        }
    }

    fun createExportWebView(): WebView = WebView(context).apply {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = false
        settings.useWideViewPort = true
        settings.loadWithOverviewMode = false
        settings.builtInZoomControls = false
        settings.displayZoomControls = false
        setBackgroundColor(android.graphics.Color.WHITE)
        isVerticalScrollBarEnabled = false
        isHorizontalScrollBarEnabled = false
    }

    fun injectExportFootnoteScript(webView: WebView, onInjected: () -> Unit) {
        val jsCode = loadAssetText("footnotes.js")
        if (jsCode.isBlank()) {
            onInjected()
            return
        }

        runCatching {
            webView.evaluateJavascript(jsCode) {
                onInjected()
            }
        }.onFailure { error ->
            Log.e("ArticleViewModel", "Failed to inject export footnotes", error)
            onInjected()
        }
    }

    fun measureAndLayoutExportWebView(webView: WebView, widthPx: Int, heightPx: Int) {
        val safeHeight = heightPx.coerceAtLeast(1)
        webView.measure(
            View.MeasureSpec.makeMeasureSpec(widthPx.coerceAtLeast(1), View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(safeHeight, View.MeasureSpec.EXACTLY),
        )
        webView.layout(0, 0, widthPx.coerceAtLeast(1), safeHeight)
    }

    fun resolveExportViewportWidthPx(): Int = context.resources.displayMetrics.widthPixels
        .coerceAtLeast(1)

    fun computeExportContentHeightPx(webView: WebView): Int {
        val density = webView.resources.displayMetrics.density
        val contentHeightPx = (webView.contentHeight * density).roundToInt()
        return maxOf(contentHeightPx, webView.measuredHeight, webView.height, 1)
    }

    override suspend fun captureExportBitmap(preparedWebView: PreparedArticleExportContent): Bitmap = withContext(Dispatchers.Main) {
        preparedWebView as AndroidPreparedExportWebView
        val rawWidth = preparedWebView.viewportWidthPx.coerceAtLeast(1)
        val rawHeight = preparedWebView.contentHeightPx.coerceAtLeast(1)
        val maxPixels = 24_000_000.0
        val rawPixels = rawWidth.toDouble() * rawHeight.toDouble()
        val scale = if (rawPixels > maxPixels) sqrt(maxPixels / rawPixels) else 1.0
        val bitmapWidth = (rawWidth * scale).roundToInt().coerceAtLeast(1)
        val bitmapHeight = (rawHeight * scale).roundToInt().coerceAtLeast(1)

        Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888).also { bitmap ->
            val canvas = Canvas(bitmap)
            canvas.drawColor(android.graphics.Color.WHITE)
            canvas.scale(
                bitmapWidth.toFloat() / rawWidth.toFloat(),
                bitmapHeight.toFloat() / rawHeight.toFloat(),
            )
            preparedWebView.webView.draw(canvas)
        }
    }

    suspend fun destroyExportWebView(webView: WebView) {
        withContext(Dispatchers.Main) {
            runCatching {
                webView.stopLoading()
                webView.destroy()
            }
        }
    }

    override suspend fun destroyExportWebView(preparedWebView: PreparedArticleExportContent) {
        preparedWebView as AndroidPreparedExportWebView
        destroyExportWebView(preparedWebView.webView)
    }

    override fun recycleExportBitmap(bitmap: Any) {
        (bitmap as Bitmap).recycle()
    }
}
