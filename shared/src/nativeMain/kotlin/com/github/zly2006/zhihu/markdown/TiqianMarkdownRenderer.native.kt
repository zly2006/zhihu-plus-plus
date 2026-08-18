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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.hrm.markdown.parser.ast.Document

internal actual val isTiqianMarkdownRendererAvailable: Boolean = false

@Suppress("UNUSED_PARAMETER")
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
    error("Tiqian Markdown renderer is unavailable on Native")
}

@Composable
internal actual fun TiqianBrandTitle(prefix: String, suffix: String) {
    Text("$prefix提椠$suffix")
}
