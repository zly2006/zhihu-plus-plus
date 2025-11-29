package com.github.zly2006.zhihu.util

import androidx.compose.foundation.text.contextmenu.data.TextContextMenuItem
import androidx.compose.foundation.text.contextmenu.modifier.filterTextContextMenuComponents
import androidx.compose.ui.Modifier

fun Modifier.fuckHonorService(): Modifier = this.filterTextContextMenuComponents {
    // 过滤傻逼荣耀手机的 AI 帮写 选项
    it !is TextContextMenuItem || it.label != "AI 帮写"
}
