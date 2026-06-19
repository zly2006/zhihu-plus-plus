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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.shared.data.FeedDisplayItem
import com.github.zly2006.zhihu.shared.data.questionAuthor
import com.github.zly2006.zhihu.shared.data.target

/**
 * 屏蔽用户确认弹窗。
 *
 * 弹窗向用户确认即将屏蔽的作者，并在确认时从当前信息流条目中提取完整作者信息。它只处理确认 UI，
 * 真正写入屏蔽列表由 [onConfirmBlock] 完成。
 */
@Composable
fun BlockUserConfirmDialogContent(
    showDialog: Boolean,
    userToBlock: Pair<String, String>?, // userId 和 userName
    displayItems: List<FeedDisplayItem>,
    onDismiss: () -> Unit,
    onConfirmBlock: (BlockedFeedAuthor) -> Unit,
) {
    if (showDialog && userToBlock != null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("屏蔽用户") },
            text = {
                Column {
                    Text("确定要屏蔽用户 \"${userToBlock.second}\" 吗？")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "屏蔽后，该用户的内容将不会在推荐流中显示。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        userToBlock.let { (userId, userName) ->
                            val author = displayItems
                                .find { item ->
                                    item.feed
                                        ?.target
                                        ?.author
                                        ?.id == userId
                                }?.feed
                                ?.target
                                ?.author
                            onConfirmBlock(
                                BlockedFeedAuthor(
                                    id = author?.id ?: userId,
                                    name = author?.name ?: userName,
                                    urlToken = author?.urlToken,
                                    avatarUrl = author?.avatarUrl,
                                ),
                            )
                        }
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
fun BlockQuestionAuthorConfirmDialogContent(
    showDialog: Boolean,
    userToBlock: Pair<String, String>?,
    displayItems: List<FeedDisplayItem>,
    onDismiss: () -> Unit,
    onConfirmBlock: (BlockedFeedAuthor) -> Unit,
) {
    if (showDialog && userToBlock != null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("屏蔽提问者") },
            text = {
                Column {
                    Text("确定要屏蔽提问者 \"${userToBlock.second}\" 吗？")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "屏蔽后，该用户提出的问题将不会在推荐流中显示；该用户回答别人问题的内容不受影响。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        userToBlock.let { (userId, userName) ->
                            val author = displayItems
                                .asSequence()
                                .mapNotNull { it.feed?.target?.questionAuthor }
                                .firstOrNull { it.id == userId }
                            onConfirmBlock(
                                BlockedFeedAuthor(
                                    id = author?.id ?: userId,
                                    name = author?.name ?: userName,
                                    urlToken = author?.urlToken,
                                    avatarUrl = author?.avatarUrl,
                                ),
                            )
                        }
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

data class BlockedFeedAuthor(
    val id: String,
    val name: String,
    val urlToken: String?,
    val avatarUrl: String?,
)
