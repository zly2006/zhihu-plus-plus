@file:Suppress("FunctionName")

package com.github.zly2006.zhihu.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.github.zly2006.zhihu.LocalNavigator
import com.github.zly2006.zhihu.Person
import com.github.zly2006.zhihu.Pin
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.ui.components.CommentScreenComponent
import com.github.zly2006.zhihu.ui.components.ShareDialog
import com.github.zly2006.zhihu.ui.components.WebviewComp
import com.github.zly2006.zhihu.ui.components.getShareText
import com.github.zly2006.zhihu.ui.components.handleShareAction
import com.github.zly2006.zhihu.ui.components.setupUpWebviewClient
import com.github.zly2006.zhihu.viewmodel.PinViewModel
import org.jsoup.Jsoup
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PinScreen(
    pin: Pin,
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    val httpClient = remember { AccountData.httpClient(context) }

    val viewModel = viewModel<PinViewModel>(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return PinViewModel(pin, httpClient) as T
            }
        },
    )

    LaunchedEffect(pin.id) {
        viewModel.loadPinDetail(context)
        AccountData.addReadHistory(context, pin.id.toString(), "pin")
    }

    var showShareDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
                Text(
                    "想法",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                IconButton(
                    onClick = {
                        val shareText = getShareText(pin)
                        if (shareText != null) {
                            handleShareAction(context, pin, shareText) {
                                showShareDialog = true
                            }
                        }
                    },
                ) {
                    Icon(Icons.Default.Share, contentDescription = "分享")
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                viewModel.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                viewModel.errorMessage != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "加载失败: ${viewModel.errorMessage}",
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }

                viewModel.pinContent != null -> {
                    var showComments by remember { mutableStateOf(false) }

                    PinContent(
                        pin = viewModel.pinContent!!,
                        isLiked = viewModel.isLiked,
                        likeCount = viewModel.likeCount,
                        onLikeClick = {
                            viewModel.toggleLike(context)
                        },
                        onCommentClick = {
                            showComments = true
                        },
                    )

                    // Comment sheet component
                    if (showComments) {
                        CommentScreenComponent(
                            showComments = showComments,
                            onDismiss = { showComments = false },
                            httpClient = httpClient,
                            content = pin,
                        )
                    }

                    // 分享对话框
                    val shareText = getShareText(pin)
                    if (shareText != null) {
                        ShareDialog(
                            content = pin,
                            shareText = shareText,
                            showDialog = showShareDialog,
                            onDismissRequest = { showShareDialog = false },
                            context = context,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PinContent(
    pin: DataHolder.Pin,
    isLiked: Boolean,
    likeCount: Int,
    onLikeClick: () -> Unit,
    onCommentClick: () -> Unit,
) {
    val onNavigate = LocalNavigator.current
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        // Author info
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    onNavigate(
                        Person(
                            id = pin.author.id,
                            urlToken = pin.author.urlToken,
                            name = pin.author.name,
                        ),
                    )
                },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = pin.author.avatarUrl,
                contentDescription = "头像",
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    pin.author.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                if (pin.author.headline.isNotEmpty()) {
                    Text(
                        pin.author.headline,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Time
        Text(
            dateFormat.format(Date(pin.created * 1000)),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Content
        WebviewComp {
            it.setupUpWebviewClient()
            val document = Jsoup.parse(pin.contentHtml)
            it.loadZhihu(
                "https://www.zhihu.com",
                document,
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Stats and actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            FilledTonalButton(
                onClick = onLikeClick,
            ) {
                Icon(
                    if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "赞",
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "$likeCount",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            FilledTonalButton(
                onClick = onCommentClick,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Comment,
                    contentDescription = "评论",
                    modifier = Modifier.size(20.dp),
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    "${pin.commentCount}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }

        // Topics
        if (pin.topics?.isNotEmpty() == true) {
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "话题",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(8.dp))
            pin.topics.forEach { topic ->
                Text(
                    "# ${topic.name}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
        }
    }
}
