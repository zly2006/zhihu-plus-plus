package com.github.zly2006.zhihu.shared.platform

import androidx.compose.runtime.Composable

@Composable
actual fun PlatformBackHandler(
    enabled: Boolean,
    onBack: () -> Unit,
) = Unit // TODO: desktop back handler
