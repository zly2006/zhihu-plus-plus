package com.github.zly2006.zhihu.shared.login

import androidx.compose.ui.graphics.ImageBitmap

actual fun generateQrLoginBitmap(content: String): ImageBitmap =
    error("QR login bitmap is not implemented for iOS: ${content.length}")
