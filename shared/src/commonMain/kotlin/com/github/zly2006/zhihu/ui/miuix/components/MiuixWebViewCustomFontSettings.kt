/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 * Licensed under AGPL-3.0-only.
 */

package com.github.zly2006.zhihu.ui.miuix.components

import androidx.compose.runtime.Composable

@Composable
expect fun MiuixWebViewCustomFontSettings(
    customFontName: String?,
    onCustomFontNameChange: (String?) -> Unit,
)
