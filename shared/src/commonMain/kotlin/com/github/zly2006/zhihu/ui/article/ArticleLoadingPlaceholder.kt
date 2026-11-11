/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
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

package com.github.zly2006.zhihu.ui.article

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.unit.dp

/** 标题、作者和正文共用的轻微呼吸占位；动画只使绘制失效，不重组详情页。 */
@Composable
internal fun ArticleLoadingPlaceholder(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "articleSkeleton")
    val opacity = transition.animateFloat(
        initialValue = 0.06f,
        targetValue = 0.12f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "articleSkeletonOpacity",
    )
    val color = MaterialTheme.colorScheme.onSurface
    Spacer(
        modifier.drawBehind {
            drawRoundRect(
                color = color.copy(alpha = opacity.value),
                cornerRadius = CornerRadius(4.dp.toPx()),
            )
        },
    )
}
