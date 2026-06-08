/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
