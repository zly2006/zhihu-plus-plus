package com.github.zly2006.zhihu.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.AnnotatedString
import com.fleeksoft.ksoup.Ksoup

@Composable
actual fun parseHtmlTextWithTheme(html: String): AnnotatedString =
    AnnotatedString(Ksoup.parseBodyFragment(html).text())
