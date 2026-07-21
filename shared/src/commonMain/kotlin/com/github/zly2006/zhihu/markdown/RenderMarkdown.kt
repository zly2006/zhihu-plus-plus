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

package com.github.zly2006.zhihu.markdown

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.SegmentCommentHolder
import com.github.zly2006.zhihu.navigation.Video
import com.github.zly2006.zhihu.navigation.resolveContent
import com.github.zly2006.zhihu.shared.platform.rememberExternalUrlOpener
import com.github.zly2006.zhihu.shared.platform.rememberImageGalleryOpener
import com.github.zly2006.zhihu.shared.platform.rememberImageSaver
import com.github.zly2006.zhihu.shared.platform.rememberImageSharer
import com.github.zly2006.zhihu.shared.platform.rememberSettingsStore
import com.github.zly2006.zhihu.ui.components.CommentScreenComponent
import com.github.zly2006.zhihu.ui.components.LocalSegmentActionSheetHost
import com.github.zly2006.zhihu.ui.components.LocalSegmentCommentHost
import com.github.zly2006.zhihu.ui.components.SegmentActionSheet
import com.github.zly2006.zhihu.ui.components.SegmentActionSheetState
import com.github.zly2006.zhihu.ui.subscreens.PREF_BLOCK_SPACING
import com.github.zly2006.zhihu.ui.subscreens.PREF_FONT_SIZE
import com.github.zly2006.zhihu.ui.subscreens.PREF_LINE_HEIGHT
import com.hrm.markdown.parser.ast.Document
import com.hrm.markdown.renderer.Markdown
import com.hrm.markdown.renderer.MarkdownImageData
import com.hrm.markdown.renderer.MarkdownTheme

private const val HTML_PAGINATION_BLOCK_INCREMENT = 10
private const val HTML_PAGINATION_MINIMUM_BLOCK_COUNT = 40

@Composable
fun RenderImage(
    data: MarkdownImageData,
    modifier: Modifier,
    imageUrls: List<String> = listOf(data.url),
) {
    val openImageGallery = rememberImageGalleryOpener()
    val openExternalUrl = rememberExternalUrlOpener()
    val saveImage = rememberImageSaver()
    val shareImage = rememberImageSharer()
    var expanded by remember { mutableStateOf(false) }
    var pressOffset by remember { mutableStateOf(DpOffset.Zero) }
    val density = LocalDensity.current
    val hapticFeedback = LocalHapticFeedback.current
    val previewUrls = remember(imageUrls, data.url) {
        imageUrls.ifEmpty { listOf(data.url) }
    }

    fun openGallery() {
        val initialIndex = previewUrls.indexOf(data.url).takeIf { it >= 0 } ?: 0
        openImageGallery(previewUrls, initialIndex)
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = rememberMarkdownImageModel(data.url),
            contentDescription = data.altText,
            modifier = modifier
                .fillMaxWidth(0.8f)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            openGallery()
                        },
                        onLongPress = { offset ->
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            pressOffset = with(density) {
                                DpOffset(offset.x.toDp(), offset.y.toDp() - 20.dp)
                            }
                            expanded = true
                        },
                    )
                },
        )

        // DropdownMenu 在独立的 Popup 窗口中渲染，但其 Text 会注册到父级 SelectionRegistrar。
        // 当文本选择上下文菜单触发 isEntireContainerSelected → sort 时，
        // 跨窗口比较坐标会抛出 IllegalArgumentException: layouts are not part of the same hierarchy。
        DisableSelection {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                offset = pressOffset,
            ) {
                DropdownMenuItem(
                    text = { Text("查看图片") },
                    onClick = {
                        expanded = false
                        openGallery()
                    },
                )
                DropdownMenuItem(
                    text = { Text("在浏览器中打开") },
                    onClick = {
                        expanded = false
                        openExternalUrl(data.url)
                    },
                )
                DropdownMenuItem(
                    text = { Text("保存图片") },
                    onClick = {
                        expanded = false
                        saveImage(data.url)
                    },
                )
                DropdownMenuItem(
                    text = { Text("分享图片") },
                    onClick = {
                        expanded = false
                        shareImage(data.url)
                    },
                )
            }
        }
    }
}

@Composable
fun RenderVideoBox(
    videoId: Long,
    thumbnailUrl: String?,
    modifier: Modifier = Modifier,
) {
    val navigator = LocalNavigator.current
    val shape = RoundedCornerShape(8.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable {
                navigator.onNavigate(Video(videoId))
            },
    ) {
        if (thumbnailUrl != null) {
            AsyncImage(
                model = rememberMarkdownImageModel(thumbnailUrl),
                contentDescription = "视频封面",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Surface(
            modifier = Modifier.align(Alignment.Center),
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.56f),
            onClick = {
                navigator.onNavigate(Video(videoId))
            },
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "播放视频",
                tint = Color.White,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

/**
 * 禁用谷歌的双击选择文字功能。
 *
 * 比较hack的做法，但目前没有更好的方案了。
 */
@Composable
private fun NoDoubleClickSelectionScope(content: @Composable () -> Unit) {
    val current = LocalViewConfiguration.current
    val patched =
        remember(current) {
            object : ViewConfiguration by current {
                override val doubleTapTimeoutMillis: Long = 0L
            }
        }

    CompositionLocalProvider(LocalViewConfiguration provides patched) {
        content()
    }
}

@Composable
fun RenderMarkdown(
    html: String,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    selectable: Boolean = true,
    enablePagination: Boolean = false,
    enableScroll: Boolean = true,
    initialBlockCount: Int = 100,
    header: (@Composable () -> Unit)? = null,
    footer: (@Composable () -> Unit)? = null,
) {
    val htmlBlocks = remember(html, enablePagination) {
        if (enablePagination) splitTopLevelHtmlBlocks(html) else listOf(html)
    }
    val effectivePagination = enablePagination && htmlBlocks.size >= HTML_PAGINATION_MINIMUM_BLOCK_COUNT
    var visibleHtmlBlockCount by remember(html, enablePagination, initialBlockCount) {
        mutableIntStateOf(
            if (effectivePagination) {
                initialBlockCount.coerceAtLeast(0).coerceAtMost(htmlBlocks.size)
            } else {
                htmlBlocks.size
            },
        )
    }
    val parsedDocuments = remember(html) { mutableListOf<Document>() }
    val document = remember(htmlBlocks, visibleHtmlBlockCount) {
        while (parsedDocuments.size < visibleHtmlBlockCount) {
            parsedDocuments += htmlToMdAst(htmlBlocks[parsedDocuments.size])
        }
        combineMdAstDocuments(parsedDocuments.take(visibleHtmlBlockCount))
    }
    val hasMoreHtmlBlocks = visibleHtmlBlockCount < htmlBlocks.size
    val imageUrls = remember(html, effectivePagination) {
        if (effectivePagination) htmlPreviewImageUrls(html) else null
    }

    LaunchedEffect(scrollState, effectivePagination, htmlBlocks.size) {
        if (!effectivePagination) return@LaunchedEffect
        snapshotFlow { scrollState.value to scrollState.maxValue }
            .collect { (value, maximum) ->
                if (maximum > 0 &&
                    visibleHtmlBlockCount < htmlBlocks.size &&
                    value.toFloat() / maximum.toFloat() > 0.8f
                ) {
                    visibleHtmlBlockCount =
                        (visibleHtmlBlockCount + HTML_PAGINATION_BLOCK_INCREMENT).coerceAtMost(htmlBlocks.size)
                }
            }
    }
    LaunchedEffect(scrollState, effectivePagination, visibleHtmlBlockCount, htmlBlocks.size) {
        if (!effectivePagination || !hasMoreHtmlBlocks) return@LaunchedEffect
        withFrameNanos { }
        // 顶部块太短时逐帧补足到可滚动，避免永远等不到滚动触发后续加载。
        if (scrollState.maxValue == 0) {
            visibleHtmlBlockCount =
                (visibleHtmlBlockCount + HTML_PAGINATION_BLOCK_INCREMENT).coerceAtMost(htmlBlocks.size)
        }
    }

    RenderMarkdownDocument(
        document = document,
        imageUrls = imageUrls,
        modifier = modifier,
        scrollState = scrollState,
        selectable = selectable,
        enableScroll = enableScroll,
        header = header,
        footer = footer.takeUnless { hasMoreHtmlBlocks },
    )
}

@Composable
fun RenderMarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    selectable: Boolean = true,
    enableScroll: Boolean = true,
    header: (@Composable () -> Unit)? = null,
    footer: (@Composable () -> Unit)? = null,
) {
    val document = remember(markdown) { markdownToMdAst(markdown) }
    RenderMarkdownDocument(
        document = document,
        imageUrls = null,
        modifier = modifier,
        scrollState = scrollState,
        selectable = selectable,
        enableScroll = enableScroll,
        header = header,
        footer = footer,
    )
}

@Composable
private fun RenderMarkdownDocument(
    document: Document,
    imageUrls: List<String>?,
    modifier: Modifier,
    scrollState: ScrollState,
    selectable: Boolean,
    enableScroll: Boolean,
    header: (@Composable () -> Unit)?,
    footer: (@Composable () -> Unit)?,
) {
    val documentImageUrls = remember(document) { document.previewImageUrls() }
    val previewImageUrls = imageUrls ?: documentImageUrls
    val navigator = LocalNavigator.current
    val runtime = rememberMarkdownRuntime()
    val openExternalUrl = rememberExternalUrlOpener()
    val settings = rememberSettingsStore()
    val fontSize = settings.getInt(PREF_FONT_SIZE, 100)
    val lineHeight = settings.getInt(PREF_LINE_HEIGHT, 160)
    val blockSpacing = settings.getInt(PREF_BLOCK_SPACING, 100)
    val defaultTheme = MarkdownTheme.material3()

    val theme = defaultTheme.copy(
        bodyStyle = defaultTheme.bodyStyle.copy(
            fontSize = 16.sp * fontSize / 100,
            lineHeight = 16.sp * fontSize / 100 * lineHeight / 100,
        ),
        blockSpacing = defaultTheme.blockSpacing * (blockSpacing / 100f),
        mathFontSize = 18f * fontSize / 100,
        mathFont = runtime.mathFont ?: defaultTheme.mathFont,
    )
    var segmentCommentTarget by remember { mutableStateOf<SegmentCommentHolder?>(null) }
    var segmentActionSheetState by remember { mutableStateOf<SegmentActionSheetState?>(null) }
    CompositionLocalProvider(
        LocalSegmentCommentHost provides { target ->
            segmentCommentTarget = target
        },
        LocalSegmentActionSheetHost provides { state -> segmentActionSheetState = state },
    ) {
        Box(modifier = modifier) {
            NoDoubleClickSelectionScope {
                Markdown(
                    document = document,
                    imageContent = { data, imageModifier ->
                        RenderImage(
                            data = data,
                            modifier = imageModifier,
                            imageUrls = previewImageUrls,
                        )
                    },
                    scrollState = scrollState,
                    enableScroll = enableScroll,
                    enableSelection = selectable,
                    onLinkClick = { url ->
                        resolveContent(url)?.let { navigator.onNavigate(it) }
                            ?: openExternalUrl(url)
                    },
                    header = header,
                    footer = footer,
                    theme = theme,
                )
            }
        }
    }
    CommentScreenComponent(
        showComments = segmentCommentTarget != null,
        onDismiss = { segmentCommentTarget = null },
        content = segmentCommentTarget ?: SegmentCommentHolder("dummy", "dummy", "dummy"),
    )
    segmentActionSheetState?.let { state ->
        SegmentActionSheet(state)
    }
}
