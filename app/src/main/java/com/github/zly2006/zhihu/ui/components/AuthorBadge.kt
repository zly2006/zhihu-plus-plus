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

package com.github.zly2006.zhihu.ui.components

import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.github.zly2006.zhihu.R
import com.github.zly2006.zhihu.data.OfficialBadge
import com.github.zly2006.zhihu.shared.data.DataHolder

@Composable
fun AuthorBadge(
    badge: OfficialBadge?,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    if (badge == null) return
    if (badge.iconUrl.isBlank()) return
    AsyncImage(
        model = officialBadgeIconModel(badge.iconUrl),
        contentDescription = badge.description,
        modifier = modifier
            .size(if (compact) 16.dp else 18.dp)
            .semantics { contentDescription = badge.description },
    )
}

fun officialBadgeIconModel(iconUrl: String): Any = if (iconUrl == DataHolder.ZH_PLUS_AUTHOR_BADGE_ICON) {
    R.drawable.ic_zh_plus_author_badge
} else {
    iconUrl
}
