package com.github.zly2006.zhihu.util

import android.content.pm.ResolveInfo
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuItem
import androidx.compose.foundation.text.contextmenu.modifier.filterTextContextMenuComponents
import androidx.compose.ui.Modifier

val blacklist = listOf(
    "com.baidu.BaiduMap",
    "com.hihonor.",
    "com.madness.collision",
)

fun Modifier.fuckHonorService(): Modifier = this.filterTextContextMenuComponents {
    // 过滤傻逼荣耀手机的 AI 帮写 选项
    if (it !is TextContextMenuItem) {
        return@filterTextContextMenuComponents true
    }
    try {
        val fields = it.onClick.javaClass.declaredFields
        val resolveInfo = fields.firstOrNull { field -> field.type === ResolveInfo::class.java }
        resolveInfo ?: return@filterTextContextMenuComponents true
        resolveInfo.isAccessible = true
        val info = resolveInfo.get(it.onClick) as ResolveInfo
        val packageName = info.activityInfo.packageName
        if (blacklist.any { it in packageName }) {
            // 黑名单过滤
            return@filterTextContextMenuComponents false
        }
        return@filterTextContextMenuComponents true
    } catch (_: Exception) {
        return@filterTextContextMenuComponents true
    }
}
