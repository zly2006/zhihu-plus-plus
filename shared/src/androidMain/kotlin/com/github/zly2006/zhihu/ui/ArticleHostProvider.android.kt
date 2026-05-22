package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
actual fun rememberArticleHost(): ArticleHost? = LocalContext.current.articleHost()
