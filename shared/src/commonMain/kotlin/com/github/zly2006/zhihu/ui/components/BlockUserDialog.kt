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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.shared.data.FeedDisplayItem
import com.github.zly2006.zhihu.shared.data.target
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.viewmodel.feed.FeedBlockAuthorInfo
import com.github.zly2006.zhihu.viewmodel.filter.NoopMcnCompanyProvider
import com.github.zly2006.zhihu.viewmodel.filter.ZhihuMcnCompanyProvider
import com.github.zly2006.zhihu.viewmodel.filter.normalizeMcnCompany
import com.github.zly2006.zhihu.viewmodel.filter.rememberBlocklistManager
import com.github.zly2006.zhihu.viewmodel.rememberPaginationEnvironment
import kotlinx.coroutines.launch

/**
 * 屏蔽用户确认弹窗。
 *
 * 弹窗向用户确认即将屏蔽的作者，并在确认时从当前信息流条目中提取完整作者信息。它只处理确认 UI，
 * 真正写入屏蔽列表由 [onConfirmBlock] 完成。
 */
@Composable
fun BlockUserConfirmDialogContent(
    showDialog: Boolean,
    userToBlock: FeedBlockAuthorInfo?,
    displayItems: List<FeedDisplayItem>,
    onDismiss: () -> Unit,
    onConfirmBlock: (BlockedFeedAuthor) -> Unit,
    onConfirmBlockMcn: (String) -> Unit = {},
) {
    if (showDialog && userToBlock != null) {
        val coroutineScope = rememberCoroutineScope()
        val userMessages = rememberUserMessageSink()
        val author = remember(userToBlock, displayItems) {
            userToBlock.let { authorInfo ->
                val resolvedAuthor = displayItems
                    .find { item ->
                        item.feed
                            ?.target
                            ?.author
                            ?.id == authorInfo.id
                    }?.feed
                    ?.target
                    ?.author
                BlockedFeedAuthor(
                    id = resolvedAuthor?.id ?: authorInfo.id,
                    name = resolvedAuthor?.name ?: authorInfo.name,
                    urlToken = resolvedAuthor?.urlToken ?: authorInfo.urlToken,
                    avatarUrl = resolvedAuthor?.avatarUrl ?: authorInfo.avatarUrl,
                )
            }
        }
        val blocklistManager = rememberBlocklistManager()
        val paginationEnvironment = rememberPaginationEnvironment(allowGuestAccess = false)
        val mcnProvider = remember(paginationEnvironment) {
            runCatching {
                ZhihuMcnCompanyProvider(paginationEnvironment.httpClient()) { request ->
                    paginationEnvironment.configureSignedRequest(request)
                }
            }.getOrElse { NoopMcnCompanyProvider }
        }
        var isResolvingMcn by remember(author.urlToken) { mutableStateOf(true) }
        var mcnCompany by remember(author.urlToken) { mutableStateOf<String?>(null) }

        LaunchedEffect(author) {
            val urlToken = author.urlToken
            if (urlToken.isNullOrBlank()) {
                isResolvingMcn = false
                return@LaunchedEffect
            }
            blocklistManager.getCachedMcnAuthor(urlToken)?.let { cachedAuthor ->
                mcnCompany = cachedAuthor.mcnCompany.normalizeMcnCompany()
                isResolvingMcn = false
                return@LaunchedEffect
            }
            val lookupResult = runCatching { mcnProvider.getMcnCompany(urlToken).normalizeMcnCompany() }
            val resolvedCompany = lookupResult.getOrNull()
            if (lookupResult.isSuccess) {
                blocklistManager.cacheMcnCompany(urlToken, author.name, resolvedCompany)
            }
            if (resolvedCompany.isNullOrBlank()) {
                isResolvingMcn = false
            } else {
                mcnCompany = resolvedCompany
                isResolvingMcn = false
            }
        }

        val resolvedMcnCompany = mcnCompany
        if (!isResolvingMcn && !resolvedMcnCompany.isNullOrBlank()) {
            BlockRecommendationSourceDialog(
                authorName = author.name,
                mcnCompany = resolvedMcnCompany,
                isMcnBlocked = false,
                onDismiss = onDismiss,
                onBlockUser = { onConfirmBlock(author) },
                onBlockMcn = {
                    coroutineScope.launch {
                        if (blocklistManager.isMcnOrganizationBlocked(resolvedMcnCompany)) {
                            onDismiss()
                            userMessages.showShortMessage("该 MCN 已在屏蔽列表中")
                        } else {
                            onConfirmBlockMcn(resolvedMcnCompany)
                        }
                    }
                },
            )
            return
        }

        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("屏蔽用户") },
            text = {
                Column {
                    Text("确定要屏蔽用户 \"${userToBlock.name}\" 吗？")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "屏蔽后，该用户的内容将不会在推荐流中显示。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (isResolvingMcn) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "正在识别是否可按 MCN 机构屏蔽；你也可以直接屏蔽该用户。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onConfirmBlock(author)
                    },
                ) {
                    Text("确定屏蔽")
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            },
        )
    }
}

@Composable
fun BlockRecommendationSourceDialog(
    authorName: String,
    mcnCompany: String,
    isMcnBlocked: Boolean,
    onDismiss: () -> Unit,
    onBlockUser: () -> Unit,
    onBlockMcn: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("屏蔽推荐来源") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "检测到该作者属于 MCN 机构，你想屏蔽哪一类内容？",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                BlockSourceChoiceCard(
                    icon = { Icon(Icons.Default.PersonOff, contentDescription = null) },
                    title = "屏蔽该用户",
                    description = "仅隐藏「$authorName」的后续内容",
                    onClick = onBlockUser,
                )
                BlockSourceChoiceCard(
                    icon = { Icon(Icons.Default.Business, contentDescription = null) },
                    title = if (isMcnBlocked) "MCN 机构已屏蔽" else "屏蔽 MCN 机构",
                    description = if (isMcnBlocked) {
                        "「$mcnCompany」已在屏蔽列表中"
                    } else {
                        "隐藏「$mcnCompany」旗下作者内容"
                    },
                    enabled = !isMcnBlocked,
                    onClick = onBlockMcn,
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}

@Composable
private fun BlockSourceChoiceCard(
    icon: @Composable () -> Unit,
    title: String,
    description: String,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        border = if (enabled) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(
            containerColor = if (enabled) {
                MaterialTheme.colorScheme.surfaceContainerHighest
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        ),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon()
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

data class BlockedFeedAuthor(
    val id: String,
    val name: String,
    val urlToken: String?,
    val avatarUrl: String?,
)
