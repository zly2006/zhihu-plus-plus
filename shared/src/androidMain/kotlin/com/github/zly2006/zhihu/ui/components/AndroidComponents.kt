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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.drawable.toDrawable
import androidx.core.net.toUri
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.shared.data.SegmentInfoMeta
import com.github.zly2006.zhihu.shared.nlp.KeywordAnalyzerCore
import com.github.zly2006.zhihu.shared.platform.androidUserMessageSink
import com.github.zly2006.zhihu.shared.util.SegmentHighlightSpan
import com.github.zly2006.zhihu.ui.articleHost
import com.github.zly2006.zhihu.util.clipboardManager
import com.github.zly2006.zhihu.util.luoTianYiUrlLauncher
import com.github.zly2006.zhihu.util.saveImageToGallery
import com.github.zly2006.zhihu.util.shareImage
import com.github.zly2006.zhihu.util.signFetchRequest
import com.github.zly2006.zhihu.viewmodel.feed.handleBlockByKeywords
import com.github.zly2006.zhihu.viewmodel.feed.handleBlockQuestionAuthor
import com.github.zly2006.zhihu.viewmodel.feed.handleBlockTopic
import com.github.zly2006.zhihu.viewmodel.feed.handleBlockUser
import com.github.zly2006.zhihu.viewmodel.filter.AndroidContentFilterRuntime
import com.github.zly2006.zhihu.viewmodel.filter.BlockedKeyword
import com.github.zly2006.zhihu.viewmodel.filter.KeywordType
import com.github.zly2006.zhihu.viewmodel.filter.getContentFilterDatabase
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import me.saket.telephoto.zoomable.coil3.ZoomableAsyncImage
import me.saket.telephoto.zoomable.rememberZoomableImageState
import me.saket.telephoto.zoomable.rememberZoomableState

class OpenImageDialog(
    context: Context,
    private val httpClient: HttpClient,
    urls: List<String>,
    initialIndex: Int = 0,
) : ComponentDialog(context) {
    constructor(
        context: Context,
        httpClient: HttpClient,
        url: String,
    ) : this(context, httpClient, listOf(url), 0)

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
                    val scope = rememberCoroutineScope()

                    OpenImagePreviewContent(
                        urls = imageUrls,
                        initialIndex = initialPage,
                        onDismiss = { dismiss() },
                        onSaveImage = { imageUrl ->
                            scope.launch {
                                saveImageToGallery(context, httpClient, imageUrl)
                            }
                        },
                        onShareImage = { imageUrl ->
                            scope.launch {
                                shareImage(context, httpClient, imageUrl)
                            }
                        },
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

@Composable
actual fun rememberSegmentedTextRuntime(): SegmentedTextRuntime {
    val context = LocalContext.current
    return remember(context) {
        SegmentedTextRuntime(
            toggleSegmentLike = { highlight ->
                toggleSegmentLike(context, highlight)
            },
        )
    }
}

private suspend fun toggleSegmentLike(
    context: android.content.Context,
    highlight: SegmentHighlightSpan,
): SegmentInfoMeta {
    val contentId = highlight.contentId ?: return highlight.meta
    val targetType = highlight.contentType ?: return highlight.meta
    val url = "https://www.zhihu.com/api/v4/reaction/${targetType}s/$contentId/segment_reaction"

    return if (highlight.meta.isLike) {
        val body = buildSegmentUnlikeBody(highlight)
        AccountData.httpClient(context).delete(url) {
            signFetchRequest()
            contentType(ContentType.Application.Json)
            setBody(body)
        }
        updateSegmentMetaAfterUnlike(highlight)
    } else {
        val body = buildSegmentLikeBody(highlight)
        val response = AccountData
            .httpClient(context)
            .post(url) {
                signFetchRequest()
                contentType(ContentType.Application.Json)
                setBody(body)
            }.let { response ->
                if (response.status == HttpStatusCode.NoContent) {
                    null
                } else {
                    response.body<JsonElement>() as? JsonObject
                }
            }
        updateSegmentMetaAfterLike(highlight, response)
    }
}

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
                val chooserIntent = Intent.createChooser(shareIntent, "分享到")
                chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooserIntent)
            },
            directShare = { content, shareText ->
                val shareIntent = Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareText)
                    putExtra(Intent.EXTRA_TITLE, getShareTitle(content))
                }
                val chooserIntent = Intent.createChooser(shareIntent, "分享到")
                chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooserIntent)
            },
            copyLink = { content, shareText ->
                context.articleHost()?.clipboardDestination = content
                context.clipboardManager.setPrimaryClip(ClipData.newPlainText("Link", shareText))
                androidUserMessageSink(context).showShortMessage("已复制链接")
            },
        )
    }
}

@Composable
actual fun rememberBlockByKeywordsRuntime(): BlockByKeywordsRuntime {
    val context = LocalContext.current
    val database = remember(context) { getContentFilterDatabase(context) }
    return remember(database) {
        BlockByKeywordsRuntime(
            extractKeywords = { title, excerpt ->
                KeywordAnalyzerCore.extractFromFeedWithWeight(
                    title = title,
                    excerpt = excerpt,
                    content = null,
                    topN = 10,
                    extractor = AndroidContentFilterRuntime.keywordWeightExtractor,
                )
            },
            addNlpPhrase = { phrase ->
                database.blockedKeywordDao().insertKeyword(
                    BlockedKeyword(
                        keyword = phrase.trim(),
                        keywordType = KeywordType.NLP_SEMANTIC.name,
                    ),
                )
            },
        )
    }
}

@Composable
actual fun rememberFeedBlockActions(): FeedBlockActions {
    val context = LocalContext.current
    return remember(context) {
        FeedBlockActions(
            handleBlockUser = { viewModel, feedItem, onShowDialog ->
                viewModel.handleBlockUser(context, feedItem, onShowDialog)
            },
            handleBlockQuestionAuthor = { viewModel, feedItem, onShowDialog ->
                viewModel.handleBlockQuestionAuthor(context, feedItem, onShowDialog)
            },
            handleBlockTopic = { viewModel, topicId, topicName ->
                viewModel.handleBlockTopic(context, topicId, topicName)
            },
            handleBlockByKeywords = { viewModel, feedItem, onShowDialog ->
                viewModel.handleBlockByKeywords(context, feedItem, onShowDialog)
            },
        )
    }
}
