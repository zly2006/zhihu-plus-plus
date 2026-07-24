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

enum class FeedAuthorBlockType {
    CONTENT_AUTHOR,
    QUESTION_AUTHOR,
}

data class FeedAuthorBlockRequest(
    val type: FeedAuthorBlockType,
    val userId: String,
    val userName: String,
)

/**
 * 信息流作者屏蔽确认弹窗。
 *
 * 普通作者和提问者共享确认交互，但保留不同文案和作者元数据来源；真正写入哪一类黑名单由 [onConfirmBlock] 决定。
 */
@Composable
fun FeedAuthorBlockConfirmDialogContent(
    request: FeedAuthorBlockRequest?,
    displayItems: List<FeedDisplayItem>,
    onDismiss: () -> Unit,
    onConfirmBlock: (BlockedFeedAuthor) -> Unit,
) {
    if (request == null) return

    val isQuestionAuthor = request.type == FeedAuthorBlockType.QUESTION_AUTHOR
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isQuestionAuthor) "屏蔽提问者" else "屏蔽用户") },
        text = {
            Column {
                Text("确定要屏蔽${if (isQuestionAuthor) "提问者" else "用户"} \"${request.userName}\" 吗？")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    if (isQuestionAuthor) {
                        "屏蔽后，该用户提出的问题将不会在推荐流中显示；该用户回答别人问题的内容不受影响。"
                    } else {
                        "屏蔽后，该用户的内容将不会在推荐流中显示。"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val author = if (isQuestionAuthor) {
                        displayItems
                            .asSequence()
                            .mapNotNull { it.feed?.target?.questionAuthor }
                            .firstOrNull { it.id == request.userId }
                    } else {
                        displayItems
                            .asSequence()
                            .mapNotNull { it.feed?.target?.author }
                            .firstOrNull { it.id == request.userId }
                    }
                    onConfirmBlock(
                        BlockedFeedAuthor(
                            id = author?.id ?: request.userId,
                            name = author?.name ?: request.userName,
                            urlToken = author?.urlToken,
                            avatarUrl = author?.avatarUrl,
                        ),
                    )
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

data class BlockedFeedAuthor(
    val id: String,
    val name: String,
    val urlToken: String?,
    val avatarUrl: String?,
)
