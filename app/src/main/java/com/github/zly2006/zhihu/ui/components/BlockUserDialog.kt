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

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.R
import com.github.zly2006.zhihu.data.target
import com.github.zly2006.zhihu.viewmodel.feed.BaseFeedViewModel
import com.github.zly2006.zhihu.viewmodel.filter.BlocklistManager
import kotlinx.coroutines.launch

@Composable
fun BlockUserConfirmDialog(
    showDialog: Boolean,
    userToBlock: Pair<String, String>?, // Pair of userId and userName
    displayItems: List<BaseFeedViewModel.FeedDisplayItem>,
    context: Context,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    if (showDialog && userToBlock != null) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(context.getString(R.string.block_user)) },
            text = {
                Column {
                    Text(context.getString(R.string.block_user_confirm_message, userToBlock.second))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        context.getString(R.string.block_user_confirm_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        userToBlock.let { (userId, userName) ->
                            coroutineScope.launch {
                                try {
                                    val blocklistManager = BlocklistManager.getInstance(context)
                                    displayItems
                                        .find { item ->
                                            item.feed
                                                ?.target
                                                ?.author
                                                ?.id == userId
                                        }?.feed
                                        ?.target
                                        ?.author
                                        ?.let { author ->
                                            blocklistManager.addBlockedUser(
                                                userId = author.id,
                                                userName = author.name,
                                                urlToken = author.urlToken,
                                                avatarUrl = author.avatarUrl,
                                            )
                                            onConfirm()
                                            Toast.makeText(context, context.getString(R.string.block_user_success, author.name), Toast.LENGTH_SHORT).show()
                                        }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Toast.makeText(context, context.getString(R.string.block_user_failed, e.message), Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    },
                ) {
                    Text(context.getString(R.string.block_user_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = onDismiss) {
                    Text(context.getString(R.string.cancel))
                }
            },
        )
    }
}
