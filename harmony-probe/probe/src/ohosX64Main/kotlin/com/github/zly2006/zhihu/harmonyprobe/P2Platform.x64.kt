package com.github.zly2006.zhihu.harmonyprobe

import androidx.compose.foundation.Image
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlin.io.encoding.Base64
import org.jetbrains.skia.Image as SkiaImage

internal actual val usesNativeNetwork: Boolean = false

internal actual suspend fun loadNativeDaily(): Unit = error("x64 must use the NetworkKit bridge")

@Composable
internal actual fun P2Cover(url: String, description: String, modifier: Modifier) {
    val value = P2State.images[url]
    val bitmap = remember(value) {
        value?.let { runCatching { SkiaImage.makeFromEncoded(Base64.decode(it)).toComposeImageBitmap() }.getOrNull() }
    }
    if (bitmap != null) {
        Image(bitmap, contentDescription = description, modifier = modifier)
    } else {
        Text("图片加载中：$description", modifier)
    }
}
