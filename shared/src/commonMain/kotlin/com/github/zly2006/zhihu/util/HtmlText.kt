package com.github.zly2006.zhihu.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString

@Composable
expect fun parseHtmlTextWithTheme(html: String): AnnotatedString
