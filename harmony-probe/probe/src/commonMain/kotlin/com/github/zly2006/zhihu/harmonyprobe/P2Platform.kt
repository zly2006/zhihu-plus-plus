package com.github.zly2006.zhihu.harmonyprobe

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

internal expect val usesNativeNetwork: Boolean
internal expect suspend fun loadNativeDaily()

@Composable
internal expect fun P2Cover(url: String, description: String, modifier: Modifier)
