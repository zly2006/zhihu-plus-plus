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

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.github.zly2006.zhihu.R
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.data.OfficialBadge

@Composable
fun AuthorBadge(
    badge: OfficialBadge?,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = 5.dp, vertical = 1.dp),
    compact: Boolean = false,
    expandGenericCertification: Boolean = false,
) {
    if (badge == null) return
    if (badge.iconUrl.isNotBlank()) {
        AsyncImage(
            model = officialBadgeIconModel(badge.iconUrl),
            contentDescription = badge.description,
            modifier = modifier
                .size(if (compact) 16.dp else 18.dp)
                .semantics { contentDescription = badge.description },
        )
        return
    }

    val shape = RoundedCornerShape(4.dp)
    val backgroundAlpha = if (compact) 0.12f else 0.45f

    Text(
        text = badge.displayTitle(expandGenericCertification),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = backgroundAlpha))
            .then(
                if (compact) {
                    Modifier.border(
                        width = 0.5.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                        shape = shape,
                    )
                } else {
                    Modifier
                },
            ).semantics { contentDescription = badge.description }
            .padding(contentPadding),
    )
}

fun officialBadgeIconModel(iconUrl: String): Any = if (iconUrl == DataHolder.ZH_PLUS_AUTHOR_BADGE_ICON) {
    R.drawable.ic_zh_plus_author_badge
} else {
    iconUrl
}
