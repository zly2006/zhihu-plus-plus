package com.github.zly2006.zhihu.platform

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.dp

/** Shared across the main composition and Compose Dialog's separate macOS composition. */
val macosContentAreaInsetState: MutableState<androidx.compose.ui.unit.Dp> = mutableStateOf(0.dp)
