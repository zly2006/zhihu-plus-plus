/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 * Licensed under AGPL-3.0-only.
 *
 * 内嵌 Search/Close 两个 miuix 图标（PathData 提取自 miuix-icons 0.9.1，Apache-2.0）。
 * Back 改为委托官方 MiuixIcons.Back —— 之前手写内嵌的样式与官方不一致。
 */
package com.github.zly2006.zhihu.ui.miuix.components

import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.Back as OfficialBack

@Suppress("MagicNumber")
object MiuixIconsEmbedded {
    val Back: ImageVector get() = MiuixIcons.OfficialBack

    val Search: ImageVector by lazy {
        ImageVector.Builder("Search", 24.0.dp, 24.0.dp, 24.0f, 24.0f).apply {
            group(scaleX = 1.0f, scaleY = 1.0f, translationX = 2.5f, translationY = 2.5f) {
                path(fill = SolidColor(androidx.compose.ui.graphics.Color.Black), pathFillType = PathFillType.NonZero) {
                    moveTo(8.25f, 0.0f)
                    curveTo(3.694f, 0.0f, 0.0f, 3.694f, 0.0f, 8.25f)
                    curveToRelative(0.0f, 4.556f, 3.694f, 8.25f, 8.25f, 8.25f)
                    curveToRelative(4.556f, 0.0f, 8.25f, -3.694f, 8.25f, -8.25f)
                    reflectiveCurveTo(12.806f, 0.0f, 8.25f, 0.0f)
                    close()
                    moveTo(8.25f, 15.0f)
                    curveToRelative(-3.728f, 0.0f, -6.75f, -3.022f, -6.75f, -6.75f)
                    reflectiveCurveTo(4.522f, 1.5f, 8.25f, 1.5f)
                    reflectiveCurveTo(15.0f, 4.522f, 15.0f, 8.25f)
                    reflectiveCurveTo(11.978f, 15.0f, 8.25f, 15.0f)
                    close()
                    moveTo(12.78f, 11.72f)
                    lineToRelative(-1.84f, -1.84f)
                    lineToRelative(-1.06f, 1.06f)
                    lineToRelative(1.84f, 1.84f)
                    lineToRelative(6.56f, 6.56f)
                    curveToRelative(0.293f, 0.293f, 0.767f, 0.293f, 1.06f, 0.0f)
                    reflectiveCurveToRelative(0.293f, -0.767f, 0.0f, -1.06f)
                    close()
                }
            }
        }.build()
    }

    val Close: ImageVector by lazy {
        ImageVector.Builder("Close", 24.0.dp, 24.0.dp, 24.0f, 24.0f).apply {
            group(scaleX = 1.0f, scaleY = 1.0f, translationX = 4.5f, translationY = 4.5f) {
                path(fill = SolidColor(androidx.compose.ui.graphics.Color.Black), pathFillType = PathFillType.NonZero) {
                    moveTo(0.22f, 0.22f)
                    curveToRelative(0.293f, -0.293f, 0.767f, -0.293f, 1.06f, 0.0f)
                    lineTo(7.5f, 6.44f)
                    lineTo(13.72f, 0.22f)
                    curveToRelative(0.293f, -0.293f, 0.767f, -0.293f, 1.06f, 0.0f)
                    reflectiveCurveToRelative(0.293f, 0.767f, 0.0f, 1.06f)
                    lineTo(8.56f, 7.5f)
                    lineToRelative(6.22f, 6.22f)
                    curveToRelative(0.293f, 0.293f, 0.293f, 0.767f, 0.0f, 1.06f)
                    reflectiveCurveToRelative(-0.767f, 0.293f, -1.06f, 0.0f)
                    lineTo(7.5f, 8.56f)
                    lineToRelative(-6.22f, 6.22f)
                    curveToRelative(-0.293f, 0.293f, -0.767f, 0.293f, -1.06f, 0.0f)
                    reflectiveCurveToRelative(-0.293f, -0.767f, 0.0f, -1.06f)
                    lineTo(6.44f, 7.5f)
                    lineTo(0.22f, 1.28f)
                    curveToRelative(-0.293f, -0.293f, -0.293f, -0.767f, 0.0f, -1.06f)
                    close()
                }
            }
        }.build()
    }
}
