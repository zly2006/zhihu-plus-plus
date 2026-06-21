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

package com.github.zly2006.zhihu.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.theme.ThemeManager

private data class ZhihuEndorsementColorToken(
    val light: Color,
    val dark: Color,
)

private val ZhihuEndorsementColorTokens = mapOf(
    "GBK02A" to ZhihuEndorsementColorToken(Color(0xFF191B1F), Color.White),
    "GBK02B" to ZhihuEndorsementColorToken(Color.Black, Color.Black),
    "GBK03A" to ZhihuEndorsementColorToken(Color(0xFF373A40), Color(0xFFC2C6CF)),
    "GBK03B" to ZhihuEndorsementColorToken(Color(0xFF191B1F), Color.Black),
    "GBK03C" to ZhihuEndorsementColorToken(Color(0xFF535861), Color(0xFF535861)),
    "GBK04A" to ZhihuEndorsementColorToken(Color(0xFF535861), Color(0xFFC2C6CF)),
    "GBK04B" to ZhihuEndorsementColorToken(Color(0xFF9196A1), Color(0xFF9196A1)),
    "GBK05A" to ZhihuEndorsementColorToken(Color(0xFF81858F), Color(0xFF9196A1)),
    "GBK06A" to ZhihuEndorsementColorToken(Color(0xFF9196A1), Color(0xFF9196A1)),
    "GBK06B" to ZhihuEndorsementColorToken(Color(0xFF9196A1), Color(0xFF9196A1)),
    "GBK07A" to ZhihuEndorsementColorToken(Color(0xFFADB0B7), Color(0xFF5A5E66)),
    "GBK07B" to ZhihuEndorsementColorToken(Color(0xFFADB0B7), Color(0xFF5A5E66)),
    "GBK08A" to ZhihuEndorsementColorToken(Color(0xFFC4C7CE), Color(0xFF282B30)),
    "GBK08B" to ZhihuEndorsementColorToken(Color(0xFFC4C7CE), Color(0xFF282B30)),
    "GBK09A" to ZhihuEndorsementColorToken(Color(0xFFEBECED), Color(0xFF282B30)),
    "GBK09B" to ZhihuEndorsementColorToken(Color(0xFFEBECED), Color(0xFF212429)),
    "GBK09C" to ZhihuEndorsementColorToken(Color(0xFFE8E9ED), Color.Black),
    "GBK10A" to ZhihuEndorsementColorToken(Color(0xFFF8F8FA), Color(0xFF212429)),
    "GBK10B" to ZhihuEndorsementColorToken(Color.White, Color.White),
    "GBK10C" to ZhihuEndorsementColorToken(Color(0xFFF4F6F9), Color.Black),
    "GBK12A" to ZhihuEndorsementColorToken(Color(0xFFF8F8FA), Color(0xFF212429)),
    "GBK99A" to ZhihuEndorsementColorToken(Color.White, Color(0xFF191B1F)),
    "GBK99B" to ZhihuEndorsementColorToken(Color.White, Color.White),
    "GBK99C" to ZhihuEndorsementColorToken(Color.White, Color(0xFF282B30)),
    "GBL01A" to ZhihuEndorsementColorToken(Color(0xFF1772F6), Color(0xFF558EFF)),
    "GBL03A" to ZhihuEndorsementColorToken(Color(0xFF18AFFF), Color(0xFF5DBFFF)),
    "GBL05A" to ZhihuEndorsementColorToken(Color(0xFF8491A5), Color(0xFF929AAB)),
    "GBL07A" to ZhihuEndorsementColorToken(Color(0xFF09408E), Color(0xFF5271B0)),
    "GBL08A" to ZhihuEndorsementColorToken(Color(0xFF8491A5), Color(0xFF929AAB)),
    "GBL10A" to ZhihuEndorsementColorToken(Color(0xFFF8F8FA), Color(0xFF212429)),
    "GBL11A" to ZhihuEndorsementColorToken(Color(0xFF384263), Color(0xFF384263)),
    "GBL12A" to ZhihuEndorsementColorToken(Color(0xFF142457), Color(0xFF142457)),
    "GRD01A" to ZhihuEndorsementColorToken(Color(0xFFF05159), Color(0xFFF05159)),
    "GRD03A" to ZhihuEndorsementColorToken(Color(0xFFD95350), Color(0xFFD95350)),
    "GRD05A" to ZhihuEndorsementColorToken(Color(0xFFFF65C7), Color(0xFFFF65C7)),
    "GRD07A" to ZhihuEndorsementColorToken(Color(0xFF6A5FF3), Color(0xFF6A5FF3)),
    "GRD08A" to ZhihuEndorsementColorToken(Color(0xFFFF7D55), Color(0xFFFFA27F)),
    "GRD10A" to ZhihuEndorsementColorToken(Color(0xFFFF501A), Color(0xFFFF7744)),
    "GRD12A" to ZhihuEndorsementColorToken(Color(0xFFD95350), Color(0xFFD95350)),
    "GYL01A" to ZhihuEndorsementColorToken(Color(0xFFF77A31), Color(0xFFF77A31)),
    "GYL02A" to ZhihuEndorsementColorToken(Color(0xFFA5542F), Color(0xFFD28262)),
    "GYL04A" to ZhihuEndorsementColorToken(Color(0xFFF2BA6B), Color(0xFFF0B96C)),
    "GYL06A" to ZhihuEndorsementColorToken(Color(0xFFF7E2C4), Color(0xFFF5E0C4)),
    "GYL08A" to ZhihuEndorsementColorToken(Color(0xFFF2BA6B), Color(0xFFF0B96C)),
    "GYL10A" to ZhihuEndorsementColorToken(Color(0xFFCE994F), Color(0xFFCC9850)),
    "GYL12A" to ZhihuEndorsementColorToken(Color(0xFF75572D), Color(0xFF73552D)),
    "GYL12B" to ZhihuEndorsementColorToken(Color(0xFF916E3C), Color(0xFFF7EFE2)),
    "GYL16A" to ZhihuEndorsementColorToken(Color(0xFF75572D), Color(0xFF73552D)),
)

@Composable
private fun zhihuEndorsementColor(
    color: DataHolder.AnswerEndorsementColor?,
    fallback: Color,
): Color {
    val token = ZhihuEndorsementColorTokens[color?.group] ?: return fallback
    val base = if (ThemeManager.isDarkTheme()) token.dark else token.light
    return base.copy(alpha = (color?.alpha ?: 1f).coerceIn(0f, 1f))
}

@Composable
internal fun AnswerEndorsementChip(
    endorsement: DataHolder.AnswerEndorsementDisplay,
    modifier: Modifier = Modifier,
) {
    val contentColor = zhihuEndorsementColor(
        color = endorsement.textColor,
        fallback = MaterialTheme.colorScheme.primary,
    )
    val backgroundColor = zhihuEndorsementColor(
        color = endorsement.backgroundColor,
        fallback = contentColor.copy(alpha = 0.12f),
    )
    val leadingIcon = when (endorsement.leadingIconKey) {
        "zhicon_icon_24_chat_bubble_hash_fill" -> Icons.Filled.Tag
        else -> null
    }
    val trailingIcon = when (endorsement.trailingIconKey) {
        "zhicon_icon_16_arrow_right" -> Icons.AutoMirrored.Filled.KeyboardArrowRight
        "zhicon_icon_16_arrow_down" -> Icons.Filled.KeyboardArrowDown
        else -> null
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(50),
        color = backgroundColor,
        contentColor = contentColor,
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, top = 5.dp, end = 8.dp, bottom = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = zhihuEndorsementColor(
                        color = endorsement.leadingIconColor,
                        fallback = contentColor,
                    ),
                    modifier = Modifier.size(16.dp),
                )
            }
            Text(
                text = endorsement.text,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (trailingIcon != null) {
                Icon(
                    imageVector = trailingIcon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}
