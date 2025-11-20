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
                                            Toast.makeText(context, "已屏蔽用户：${author.name}", Toast.LENGTH_SHORT).show()
                                        }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Toast.makeText(context, "屏蔽用户失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
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
