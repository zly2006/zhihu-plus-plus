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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.github.zly2006.zhihu.markdown.RenderMarkdown
import com.github.zly2006.zhihu.ui.AnswerEndorsementChip
import com.github.zly2006.zhihu.ui.components.AuthorBadge
import com.github.zly2006.zhihu.viewmodel.ArticleViewModel.CachedAnswerContent
import org.jetbrains.compose.resources.painterResource
import zhihu.shared.generated.resources.Res
import zhihu.shared.generated.resources.ic_vote_up_24dp

/** 渲染水平回答切换时的缓存内容预览。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CachedAnswerPreview(cached: CachedAnswerContent, useTiqianMarkdown: Boolean) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(
                color = MaterialTheme.colorScheme.background,
                shape = RectangleShape,
            ),
        topBar = {
            // 与正文页 ZhihuTwoRowsTopAppBar 展开态对齐：整宽，上行图标(返回/分享) + 下方完整大标题(headlineMedium)。
            // 预览是占位骨架，图标不接任何动作，只为让切换到真正正文页时顶部不跳变。
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .statusBarsPadding(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(56.dp).padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = {}) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                    Spacer(Modifier.weight(1f))
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Share, contentDescription = "分享")
                    }
                }
                Text(
                    text = cached.title,
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                )
            }
        },
        bottomBar = {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(color = Color(0xFF40B6F6)),
                        horizontalArrangement = Arrangement.Start,
                    ) {
                        Button(
                            onClick = {},
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF40B6F6),
                                contentColor = Color.Black,
                            ),
                            shape = RectangleShape,
                            contentPadding = PaddingValues(horizontal = 0.dp),
                        ) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(painterResource(Res.drawable.ic_vote_up_24dp), "赞同")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = cached.voteUpCount.toString())
                        }
                    }
                    Button(
                        onClick = {},
                        contentPadding = PaddingValues(horizontal = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        ),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Comment, contentDescription = "评论")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "${cached.commentCount}")
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = innerPadding.calculateStartPadding(LocalLayoutDirection.current),
                    end = innerPadding.calculateEndPadding(LocalLayoutDirection.current),
                ),
        ) {
            Spacer(modifier = Modifier.height(innerPadding.calculateTopPadding()))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (cached.authorAvatarUrl.isNotEmpty()) {
                    AsyncImage(
                        model = cached.authorAvatarUrl,
                        contentDescription = "作者头像",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.LightGray),
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = cached.authorName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false),
                        )
                        cached.authorBadge?.let { badge ->
                            Spacer(modifier = Modifier.width(4.dp))
                            AuthorBadge(badge = badge)
                        }
                    }
                    if (cached.authorBio.isNotEmpty()) {
                        Text(
                            text = cached.authorBio,
                            fontSize = 12.sp,
                            color = Color.Gray,
                        )
                    }
                }
            }
            if (cached.endorsements.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    cached.endorsements.forEach { endorsement ->
                        AnswerEndorsementChip(endorsement)
                    }
                }
            }
            if (cached.content.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                RenderMarkdown(
                    html = cached.content,
                    modifier = Modifier,
                    selectable = true,
                    useTiqianRenderer = useTiqianMarkdown,
                    enableScroll = false,
                    header = {},
                    footer = {},
                )
            }
            Spacer(modifier = Modifier.height((16 + 36).dp))
        }
    }
}
