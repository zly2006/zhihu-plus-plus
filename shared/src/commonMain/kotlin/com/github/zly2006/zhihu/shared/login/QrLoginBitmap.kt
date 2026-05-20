package com.github.zly2006.zhihu.shared.login

import androidx.compose.ui.graphics.ImageBitmap

expect fun generateQrLoginBitmap(content: String): ImageBitmap
