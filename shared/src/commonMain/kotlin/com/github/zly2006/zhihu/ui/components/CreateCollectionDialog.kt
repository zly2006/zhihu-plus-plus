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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun CreateCollectionDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (title: String, description: String, Boolean) -> Unit,
    isSubmitting: Boolean = false,
    errorMessage: String? = null,
) {
    if (showDialog) {
        var title by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }

        Dialog(
            onDismissRequest = {
                if (!isSubmitting) {
                    onDismiss()
                }
            },
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .testTag(CREATE_COLLECTION_DIALOG_TAG),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // 标题
                    Text(
                        text = "新建收藏夹",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )

                    // 收藏夹名称输入框
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("收藏夹名称") },
                        placeholder = { Text("请输入收藏夹名称") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag(CREATE_COLLECTION_TITLE_INPUT_TAG),
                        enabled = !isSubmitting,
                        singleLine = true,
                    )

                    // 描述输入框（可选）
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("描述（可选）") },
                        placeholder = { Text("请输入收藏夹描述") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSubmitting,
                        maxLines = 3,
                    )

                    var isPublic by remember { mutableStateOf(false) }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = isPublic,
                            onCheckedChange = { isPublic = it },
                            enabled = !isSubmitting,
                        )
                        Text(
                            text = "公开收藏夹",
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                    }

                    errorMessage?.let {
                        Text(
                            text = it,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }

                    // 按钮行
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(
                            onClick = onDismiss,
                            enabled = !isSubmitting,
                        ) {
                            Text("取消")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (title.isNotBlank()) {
                                    onConfirm(title.trim(), description.trim(), isPublic)
                                }
                            },
                            enabled = title.isNotBlank() && !isSubmitting,
                            modifier = Modifier.testTag(CREATE_COLLECTION_CONFIRM_TAG),
                        ) {
                            Text(if (isSubmitting) "创建中…" else "创建")
                        }
                    }
                }
            }
        }
    }
}

private const val CREATE_COLLECTION_DIALOG_TAG = "create_collection_dialog"
private const val CREATE_COLLECTION_TITLE_INPUT_TAG = "create_collection_title_input"
private const val CREATE_COLLECTION_CONFIRM_TAG = "create_collection_confirm"
