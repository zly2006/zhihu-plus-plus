/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 */

package com.github.zly2006.zhihu.markdown

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.github.zly2006.zhihu.platform.rememberImageSaver
import com.github.zly2006.zhihu.platform.rememberImageSharer
import com.hrm.markdown.parser.ast.Document
import com.hrm.markdown.parser.ast.NativeBlock
import com.hrm.markdown.parser.ast.SegmentHighlight
import com.hrm.markdown.renderer.LocalOnSegmentHighlightClick
import com.hrm.markdown.renderer.MarkdownImageData
import org.tiqian.compose.material3.CjkText
import org.tiqian.compose.ruby
import org.tiqian.markdown.MarkdownCustomBlock
import org.tiqian.markdown.MarkdownImageBlock
import org.tiqian.markdown.MarkdownNodeKey
import org.tiqian.markdown.MarkdownRenderDocument
import org.tiqian.markdown.compose.MarkdownBlockSlots
import org.tiqian.markdown.compose.MarkdownCustomInlinePresentation
import org.tiqian.markdown.compose.MarkdownImageContent
import org.tiqian.markdown.compose.MarkdownImageLoadState
import org.tiqian.markdown.compose.MarkdownImageProvider
import org.tiqian.markdown.compose.MarkdownInlineContent
import org.tiqian.markdown.compose.MarkdownInlineDecoration
import org.tiqian.markdown.compose.MarkdownInlineMetrics
import org.tiqian.markdown.compose.MarkdownInlineSlots
import org.tiqian.markdown.compose.MarkdownMathFont
import org.tiqian.markdown.compose.MarkdownStyle
import org.tiqian.markdown.compose.TiqianMarkdownSurface
import org.tiqian.markdown.compose.rememberMarkdownStyle
import org.tiqian.markdown.compose.withBlockSpacingScale
import org.tiqian.markdown.compose.withMarkdownReadingScale

internal actual val isTiqianMarkdownRendererAvailable: Boolean = true

private data class ZhihuMarkdownDocument(
    val document: MarkdownRenderDocument,
    val nativeBlocks: Map<MarkdownNodeKey, @Composable () -> Unit>,
)

@Composable
internal actual fun PlatformTiqianMarkdown(
    document: Document,
    sourceMarkdown: String?,
    imageUrls: List<String>,
    scrollState: ScrollState,
    selectable: Boolean,
    enableScroll: Boolean,
    fontSizeScale: Float,
    lineHeightFromFontSize: Float,
    blockSpacingScale: Float,
    mathFontFamilyId: String?,
    onLinkClick: (String) -> Unit,
    header: (@Composable () -> Unit)?,
    footer: (@Composable () -> Unit)?,
) {
    // The renderer supports the full article block set, so the selected path never switches back
    // to the legacy renderer. Unknown nodes remain visible through the adapter's source-backed or
    // readable fallback text and are reported as capability issues.
    val compiled = remember(document, sourceMarkdown) {
        compileZhihuMarkdown(document, sourceMarkdown)
    }
    val style = rememberZhihuMarkdownStyle(
        fontSizeScale = fontSizeScale,
        lineHeightFromFontSize = lineHeightFromFontSize,
        blockSpacingScale = blockSpacingScale,
        mathFontFamilyId = mathFontFamilyId,
    )
    val inlineSlots = rememberZhihuInlineSlots(imageUrls)
    val imageProvider: MarkdownImageProvider = { block -> ZhihuMarkdownImageContent(block) }
    val saveImage = rememberImageSaver()
    val shareImage = rememberImageSharer()
    val slots = MarkdownBlockSlots(
        customBlock = { block, _ -> compiled.nativeBlocks[block.metadata.key]?.invoke() },
    )
    TiqianMarkdownSurface(
        document = compiled.document,
        modifier = Modifier.testTag("tiqian_markdown_content"),
        style = style,
        slots = slots,
        inlineSlots = inlineSlots,
        scrollState = scrollState,
        selectable = selectable,
        enableScroll = enableScroll,
        imageProvider = imageProvider,
        imageViewerActions = { image ->
            ZhihuImageViewerActionButton(onClick = { saveImage(image.destination) }) {
                Icon(
                    imageVector = Icons.Outlined.FileDownload,
                    contentDescription = "下载图片",
                    tint = Color.White,
                )
            }
            Spacer(Modifier.width(4.dp))
            ZhihuImageViewerActionButton(onClick = { shareImage(image.destination) }) {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = "分享图片",
                    tint = Color.White,
                )
            }
        },
        onLinkClick = onLinkClick,
        header = header,
        footer = footer,
    )
}

@Composable
private fun rememberZhihuMarkdownStyle(
    fontSizeScale: Float,
    lineHeightFromFontSize: Float,
    blockSpacingScale: Float,
    mathFontFamilyId: String?,
): MarkdownStyle =
    // App-side customization: body 字号/行高按阅读设置缩放、数学字体选择、块间距缩放，
    // 以及 display 公式滚动宿主 inset。作者色适配沿用 Markdown Material 3 默认。
    rememberMarkdownStyle(
        defaultStyle = MarkdownStyle()
            .let { base ->
                base.copy(
                    body = base.body.withMarkdownReadingScale(
                        fontSizeScale = fontSizeScale,
                        lineHeightFromFontSize = lineHeightFromFontSize,
                    ),
                )
            }.let { base ->
                base.copy(
                    math = base.math.copy(
                        font = mathFontFamilyId?.let(MarkdownMathFont::Packaged)
                            ?: MarkdownMathFont.LeteSansMath,
                        displayScrollHostInset = 16.dp,
                    ),
                )
            }.withBlockSpacingScale(blockSpacingScale),
    )

@Composable
private fun ZhihuImageViewerActionButton(
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Surface(
        color = Color.Black.copy(alpha = 0.24f),
        shape = CircleShape,
    ) {
        IconButton(onClick = onClick, content = content)
    }
}

private fun compileZhihuMarkdown(document: Document, sourceMarkdown: String?): ZhihuMarkdownDocument {
    val nativeBlocks = mutableMapOf<MarkdownNodeKey, @Composable () -> Unit>()
    val compiler = TiqianRenderDocumentCompiler(
        customBlockAdapter = MarkdownCustomBlockAdapter { node, metadata ->
            if (node !is NativeBlock) return@MarkdownCustomBlockAdapter null
            nativeBlocks[metadata.key] = node.content
            MarkdownCustomBlock(kind = "zhihu.native", metadata = metadata)
        },
    )
    return ZhihuMarkdownDocument(
        document = compiler.compile(document, sourceMarkdown),
        nativeBlocks = nativeBlocks,
    )
}

@Composable
private fun rememberZhihuInlineSlots(
    imageUrls: List<String>,
): MarkdownInlineSlots {
    val onSegmentHighlightClick = LocalOnSegmentHighlightClick.current
    val segmentUnderlineColor = MaterialTheme.colorScheme.outlineVariant
    val density = LocalDensity.current

    return MarkdownInlineSlots(
        image = { mark, _, _ ->
            val width = (mark.widthPixels?.toFloat() ?: 200f).sp
            val height = (mark.heightPixels?.toFloat() ?: 150f).sp
            val widthPx = with(density) { width.toPx() }
            val heightPx = with(density) { height.toPx() }
            MarkdownInlineContent(
                alternateText = mark.title ?: mark.description.ifEmpty { mark.destination },
                placeholder = Placeholder(
                    width = width,
                    height = height,
                    placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter,
                ),
                // `ZhihuInlineImageBottomBaseline`: replaced inline images have no intrinsic text
                // baseline. Keep the existing host-owned box size and use the standard replaced-
                // element bottom edge as its explicit baseline for Tiqian layout.
                metrics = MarkdownInlineMetrics(
                    widthPx = widthPx,
                    heightPx = heightPx,
                    baselineFromTopPx = heightPx,
                ),
            ) {
                Box(Modifier.testTag("tiqian_inline_image")) {
                    RenderImage(
                        data = MarkdownImageData(
                            url = mark.destination,
                            altText = mark.description,
                            title = mark.title,
                            width = mark.widthPixels,
                            height = mark.heightPixels,
                            attributes = mark.attributes,
                        ),
                        modifier = Modifier,
                        imageUrls = imageUrls,
                    )
                }
            }
        },
        custom = { mark, _, _ ->
            if (mark.kind != ZHIHU_SEGMENT_HIGHLIGHT_KIND) {
                null
            } else {
                MarkdownCustomInlinePresentation(
                    decoration = MarkdownInlineDecoration.DashedUnderline(segmentUnderlineColor),
                    onClick = onSegmentHighlightClick?.let { callback ->
                        { text -> callback(SegmentHighlight(text, mark.attributes)) }
                    },
                    accessibilityLabel = { text -> "打开划线片段：$text" },
                )
            }
        },
    )
}

@Composable
private fun ZhihuMarkdownImageContent(block: MarkdownImageBlock): MarkdownImageContent {
    val widthPixels = block.widthPixels
    val heightPixels = block.heightPixels
    val retained = retainedImageOutcomes[block.destination]
    var intrinsicSize by remember(block.destination) {
        mutableStateOf(
            retained?.intrinsicSize
                ?: if (widthPixels != null && heightPixels != null) {
                    IntSize(widthPixels, heightPixels)
                } else {
                    null
                },
        )
    }
    var loadState by remember(block.destination) {
        mutableStateOf(retained?.loadState ?: MarkdownImageLoadState.Loading)
    }
    val model = rememberMarkdownImageModel(block.destination)
    val description = block.description.ifBlank { null }
    return MarkdownImageContent(
        intrinsicSize = intrinsicSize,
        loadState = loadState,
    ) { modifier ->
        AsyncImage(
            model = model,
            contentDescription = description,
            modifier = modifier,
            onLoading = {
                // A block scrolling back in replays coil's load; once an outcome is retained,
                // never regress the visible state to Loading — that height flip on every
                // re-entry is what oscillated the document and twitched the scroll position.
                if (retainedImageOutcomes[block.destination] == null) {
                    loadState = MarkdownImageLoadState.Loading
                }
            },
            onSuccess = { state ->
                val image = state.result.image
                if (image.width > 0 && image.height > 0) {
                    intrinsicSize = IntSize(image.width, image.height)
                }
                loadState = MarkdownImageLoadState.Success
                retainedImageOutcomes[block.destination] =
                    RetainedImageOutcome(MarkdownImageLoadState.Success, intrinsicSize)
            },
            onError = {
                loadState = MarkdownImageLoadState.Error
                retainedImageOutcomes[block.destination] =
                    RetainedImageOutcome(MarkdownImageLoadState.Error, intrinsicSize)
            },
            contentScale = ContentScale.Fit,
        )
    }
}

/**
 * Final load outcome per image URL, retained for the process lifetime so disposed blocks that
 * scroll back into view keep their settled geometry instead of replaying Loading→Error/Success.
 */
private data class RetainedImageOutcome(
    val loadState: MarkdownImageLoadState,
    val intrinsicSize: IntSize?,
)

private val retainedImageOutcomes = mutableMapOf<String, RetainedImageOutcome>()

@Composable
internal actual fun TiqianBrandTitle(prefix: String, suffix: String) {
    CjkText(
        text = buildAnnotatedString {
            append(prefix)
            ruby("提", "tí")
            ruby("椠", "qiàn")
            append(suffix)
        },
    )
}
