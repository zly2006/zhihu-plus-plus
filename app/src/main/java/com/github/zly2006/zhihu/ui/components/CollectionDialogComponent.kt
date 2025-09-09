package com.github.zly2006.zhihu.ui.components

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.ui.Collection
import com.github.zly2006.zhihu.viewmodel.ArticleViewModel

@Composable
fun CollectionDialogComponent(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    viewModel: ArticleViewModel,
    context: Context,
) {
    val dialogTopPadding = if (LocalConfiguration.current.screenHeightDp > 500) {
        100.dp
    } else {
        60.dp
    }
    val dialogPaddingPixels = LocalDensity.current.run {
        dialogTopPadding.toPx()
    }

    // 半透明背景遮罩
    AnimatedVisibility(
        visible = showDialog,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        if (offset.y < dialogPaddingPixels) {
                            onDismiss()
                        }
                    }
                },
        )
    }

    // 对话框内容
    AnimatedVisibility(
        visible = showDialog,
        enter = slideInVertically(
            animationSpec = tween(300),
        ) { it },
        exit = slideOutVertically(
            animationSpec = tween(300),
        ) { it },
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = dialogTopPadding),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            ) {
                // 顶部拖拽指示器
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .background(
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                RoundedCornerShape(2.dp),
                            ),
                    )
                }

                // 标题
                Text(
                    text = "选择收藏夹",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp),
                )

                // 收藏夹列表
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // 新建收藏夹按钮
                    item {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    // TODO: 打开新建收藏夹对话框
                                    viewModel.createNewCollection("新收藏夹", "")
                                },
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = "新建收藏夹",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(24.dp),
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                Text(
                                    text = "新建收藏夹",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                )
                            }
                        }
                    }

                    items(viewModel.collections) { collection ->
                        CollectionItem(
                            collection = collection,
                            onToggle = {
                                viewModel.toggleFavorite(collection.id, collection.isFavorited, context)
                                viewModel.loadCollections()
                            },
                        )
                    }
                }

                // 底部按钮
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("关闭")
                    }
                }
            }
        }
    }

    // 返回键处理
    BackHandler(
        enabled = showDialog,
    ) {
        onDismiss()
    }
}

@Composable
private fun CollectionItem(
    collection: Collection,
    onToggle: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = collection.title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                if (collection.description.isNotEmpty()) {
                    Text(
                        text = collection.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
                Text(
                    text = "${collection.itemCount} 篇内容",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 2.dp),
                )
            }

            Icon(
                imageVector = if (collection.isFavorited) Icons.Filled.Bookmark else Icons.Filled.BookmarkBorder,
                contentDescription = if (collection.isFavorited) "已收藏" else "未收藏",
                tint = if (collection.isFavorited) Color(0xFFF57C00) else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp),
            )
        }
    }
}
