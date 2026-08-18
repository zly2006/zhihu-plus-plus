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
import androidx.compose.runtime.Composable
import com.hrm.markdown.parser.ast.Document

internal expect val isTiqianMarkdownRendererAvailable: Boolean

@Composable
internal expect fun PlatformTiqianMarkdown(
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
)

/**
 * 设置项里的「提椠」品牌标题：提椠前端可用的平台给「提椠」两字加拼音行间注（tí qiàn），
 * 其余平台回落为纯文本。文字整体为 prefix + 提椠 + suffix。
 */
@Composable
internal expect fun TiqianBrandTitle(prefix: String, suffix: String)
