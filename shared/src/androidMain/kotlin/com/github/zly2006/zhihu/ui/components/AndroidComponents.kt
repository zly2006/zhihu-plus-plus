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

package com.github.zly2006.zhihu.ui.components

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Color.BLACK
import android.os.Bundle
import android.view.ViewGroup
import android.view.Window
import androidx.activity.ComponentDialog
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toDrawable
import androidx.core.net.toUri
import com.github.zly2006.zhihu.nlp.KeywordAnalyzerCore
import com.github.zly2006.zhihu.nlp.KeywordWithWeight
import com.github.zly2006.zhihu.platform.androidUserMessageSink
import com.github.zly2006.zhihu.ui.articleHost
import com.github.zly2006.zhihu.util.clipboardManager
import com.github.zly2006.zhihu.util.luoTianYiUrlLauncher
import com.github.zly2006.zhihu.viewmodel.filter.AndroidContentFilterRuntime
import me.saket.telephoto.zoomable.coil3.ZoomableAsyncImage
import me.saket.telephoto.zoomable.rememberZoomableImageState
import me.saket.telephoto.zoomable.rememberZoomableState

class OpenImageDialog(
    context: Context,
    urls: List<String>,
    initialIndex: Int = 0,
) : ComponentDialog(context) {
    constructor(
        context: Context,
        url: String,
    ) : this(context, listOf(url), 0)

    private val imageUrls = urls
        .filter { it.isNotBlank() && !it.startsWith("data") }
        .distinct()
        .ifEmpty { listOf("") }
    private val initialPage = initialIndex.coerceIn(0, imageUrls.lastIndex)

    init {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setCanceledOnTouchOutside(true)
        setContentView(
            ComposeView(context).apply {
                setContent {
                    OpenImagePreviewContent(
                        urls = imageUrls,
                        initialIndex = initialPage,
                        onDismiss = { dismiss() },
                        onOpenInBrowser = { imageUrl ->
                            luoTianYiUrlLauncher(context, imageUrl.toUri())
                        },
                    ) { imageUrl, onClick, onLongClick, onPageSwipeEnabledChange ->
                        val imageState = rememberZoomableImageState(rememberZoomableState())
                        LaunchedEffect(imageState) {
                            snapshotFlow { imageState.zoomableState.zoomFraction }
                                .collect { zoomFraction ->
                                    onPageSwipeEnabledChange((zoomFraction ?: 0f) <= 0.01f)
                                }
                        }
                        ZoomableAsyncImage(
                            model = imageUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            state = imageState,
                            onClick = { onClick() },
                            onLongClick = onLongClick,
                        )
                    }
                }
            },
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
        window?.setBackgroundDrawable(BLACK.toDrawable())
    }
}

actual suspend fun extractFeedKeywords(
    title: String,
    excerpt: String?,
): List<KeywordWithWeight> = KeywordAnalyzerCore.extractFromFeedWithWeight(
    title = title,
    excerpt = excerpt,
    content = null,
    topN = 10,
    extractor = AndroidContentFilterRuntime.keywordWeightExtractor,
)

@Composable
actual fun rememberShareDialogRuntime(): ShareDialogRuntime {
    val context = LocalContext.current
    return remember(context) {
        ShareDialogRuntime(
            share = { _, shareText ->
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareText)
                }
                context.startActivity(
                    Intent.createChooser(shareIntent, "分享到").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            },
            directShare = { content, shareText ->
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareText)
                    putExtra(Intent.EXTRA_TITLE, getShareTitle(content))
                }
                context.startActivity(
                    Intent.createChooser(shareIntent, "分享到").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            },
            copyLink = { content, shareText ->
                context.articleHost()?.clipboardDestination = content
                context.clipboardManager.setPrimaryClip(ClipData.newPlainText("Link", shareText))
                androidUserMessageSink(context).showShortMessage("已复制链接")
            },
        )
    }
}
