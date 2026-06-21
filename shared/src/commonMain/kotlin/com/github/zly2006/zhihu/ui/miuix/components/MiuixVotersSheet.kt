/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * Licensed under AGPL-3.0-only.
 */

package com.github.zly2006.zhihu.ui.miuix.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.github.zly2006.zhihu.navigation.Person
import com.github.zly2006.zhihu.shared.data.DataHolder
import top.yukonga.miuix.kmp.basic.CircularProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowBottomSheet

/**
 * 赞同者列表弹层的 miuix 版本，对标 M3 [com.github.zly2006.zhihu.ui.components.VotersSheet]：
 * 标题用弹层自带 title，内容为头像+昵称+签名的分页列表，含加载/错误/重试/加载更多/空态。
 */
@Composable
fun MiuixVotersSheet(
    show: Boolean,
    title: String,
    voters: List<DataHolder.Author>,
    isLoading: Boolean,
    errorMessage: String?,
    canLoadMore: Boolean,
    onDismissRequest: () -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    onNavigate: (Person) -> Unit,
) {
    WindowBottomSheet(
        show = show,
        title = title,
        insideMargin = DpSize(0.dp, 0.dp),
        onDismissRequest = onDismissRequest,
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxWidth().height(520.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            items(items = voters, key = { it.id }) { voter ->
                MiuixVoterRow(voter) {
                    onNavigate(Person(id = voter.id, urlToken = voter.urlToken, name = voter.name))
                }
            }
            if (errorMessage != null) {
                item("error") {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text("加载失败：$errorMessage", color = MiuixTheme.colorScheme.primary)
                        TextButton(text = "重试", onClick = onRetry)
                    }
                }
            } else if (voters.isEmpty() && isLoading) {
                item("initial_loading") {
                    Box(Modifier.fillMaxWidth().padding(vertical = 32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (voters.isEmpty()) {
                item("empty") {
                    Text(
                        "暂无可查看的赞同者",
                        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                }
            } else if (canLoadMore || isLoading) {
                item("load_more") {
                    TextButton(
                        text = if (isLoading) "加载中" else "加载更多",
                        onClick = onLoadMore,
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

@Composable
private fun MiuixVoterRow(
    voter: DataHolder.Author,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(
                androidx.compose.foundation.shape
                    .RoundedCornerShape(12.dp),
            ).clickable(onClick = onClick)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = voter.avatarUrl,
            contentDescription = "${voter.name}的头像",
            modifier = Modifier.size(40.dp).clip(CircleShape),
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                voter.name,
                color = MiuixTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            voter.headline.takeIf { it.isNotBlank() }?.let { headline ->
                Text(
                    headline,
                    style = MiuixTheme.textStyles.body2,
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
