package com.github.zly2006.zhihu.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.github.zly2006.zhihu.Article
import com.github.zly2006.zhihu.ArticleType
import com.github.zly2006.zhihu.LocalNavigator
import com.github.zly2006.zhihu.NavDestination
import com.github.zly2006.zhihu.Person
import com.github.zly2006.zhihu.Pin
import com.github.zly2006.zhihu.Question
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.markdown.MarkdownRenderContext
import com.github.zly2006.zhihu.markdown.Render
import com.github.zly2006.zhihu.markdown.htmlToMdAst
import com.github.zly2006.zhihu.resolveContent
import com.github.zly2006.zhihu.ui.components.CommentScreenComponent
import com.github.zly2006.zhihu.ui.components.ShareDialog
import com.github.zly2006.zhihu.ui.components.WebviewComp
import com.github.zly2006.zhihu.ui.components.getShareText
import com.github.zly2006.zhihu.ui.components.handleShareAction
import com.github.zly2006.zhihu.ui.components.setupUpWebviewClient
import com.github.zly2006.zhihu.util.fuckHonorService
import com.github.zly2006.zhihu.util.luoTianYiUrlLauncher
import com.github.zly2006.zhihu.viewmodel.PinViewModel
import org.jsoup.Jsoup
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun PinScreen(
    innerPadding: PaddingValues,
    pin: Pin,
) {
    val navigator = LocalNavigator.current
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
        modifier = Modifier
            .padding(innerPadding)
            .padding(top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = navigator.onNavigateBack) {
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
                            handleShareAction(context, pin) {
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
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val preferences = context.getSharedPreferences(PREFERENCE_NAME, android.content.Context.MODE_PRIVATE)
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
                    navigator.onNavigate(
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
        if (preferences.getBoolean("articleUseWebview", true)) {
            WebviewComp {
                it.isVerticalScrollBarEnabled = false
                it.setupUpWebviewClient()
                val document = Jsoup.parse(pin.contentHtml)
                it.loadZhihu(
                    "https://www.zhihu.com",
                    document,
                )
            }
        } else {
            val astNode = remember(pin.contentHtml) {
                htmlToMdAst(pin.contentHtml)
            }
            val mdContext = MarkdownRenderContext()
            Spacer(Modifier.height(10.dp))
            SelectionContainer(
                modifier = Modifier.fuckHonorService(),
            ) {
                Column {
                    for (ast in astNode) {
                        ast.Render(mdContext)
                        Spacer(Modifier.height(12.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        val linkCard = pin.content.firstOrNull {
            it is DataHolder.Pin.ContentLinkCard
        } as? DataHolder.Pin.ContentLinkCard
        if (linkCard != null) {
            var relatedTitle by remember(linkCard.dataContentType, linkCard.dataContentId, linkCard.url) { mutableStateOf<String?>(null) }
            var relatedPreview by remember(linkCard.dataContentType, linkCard.dataContentId, linkCard.url) { mutableStateOf<String?>(null) }
            var isRelatedLoading by remember(linkCard.dataContentType, linkCard.dataContentId, linkCard.url) { mutableStateOf(true) }

            LaunchedEffect(linkCard.dataContentType, linkCard.dataContentId, linkCard.url) {
                isRelatedLoading = true
                val preview = fetchLinkCardPreview(context, linkCard)
                relatedTitle = preview?.title
                relatedPreview = preview?.preview
                isRelatedLoading = false
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val targetUrl = linkCard.url.takeIf { it.isNotBlank() }
                        val destination = targetUrl?.toUri()?.let(::resolveContent)

                        if (destination != null) {
                            navigator.onNavigate(destination)
                        } else {
                            targetUrl?.let {
                                luoTianYiUrlLauncher(context, it.toUri())
                            }
                        }
                    },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                ),
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                ) {
                    Text(
                        "关联内容",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    if (isRelatedLoading) {
                        Text(
                            text = "正在加载关联内容...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else if (!relatedTitle.isNullOrBlank()) {
                        Text(
                            text = relatedTitle!!,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (!relatedPreview.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = relatedPreview!!,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 3,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    } else {
                        Text(
                            text = "${linkCardTypeLabel(linkCard.dataContentType)} · ${linkCard.dataContentId}",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                    if (relatedTitle.isNullOrBlank() && linkCard.url.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = linkCard.url,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

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

private fun linkCardTypeLabel(dataContentType: String): String = when (dataContentType.lowercase(Locale.ROOT)) {
    "answer" -> "回答"
    "article" -> "文章"
    "question" -> "问题"
    "pin" -> "想法"
    "people" -> "用户"
    "video", "zvideo" -> "视频"
    else -> dataContentType
}

private data class LinkCardPreview(
    val title: String,
    val preview: String,
)

private suspend fun fetchLinkCardPreview(
    context: Context,
    linkCard: DataHolder.Pin.ContentLinkCard,
): LinkCardPreview? {
    val destination = resolveLinkCardDestination(linkCard) ?: return null
    return when (destination) {
        is Article -> {
            when (val detail = DataHolder.getContentDetail(context, destination)) {
                is DataHolder.Article -> LinkCardPreview(
                    title = compactTitle(detail.title),
                    preview = compactPreview(detail.excerpt.ifBlank { detail.content }),
                )

                is DataHolder.Answer -> LinkCardPreview(
                    title = compactTitle(detail.question.title),
                    preview = compactPreview(detail.excerpt.ifBlank { detail.content }),
                )

                else -> null
            }
        }

        is Question -> {
            DataHolder.getContentDetail(context, destination)?.let { detail ->
                LinkCardPreview(
                    title = compactTitle(detail.title),
                    preview = compactPreview(detail.detail),
                )
            }
        }

        is Pin -> {
            DataHolder.getContentDetail(context, destination)?.let { detail ->
                LinkCardPreview(
                    title = "${detail.author.name} 的想法",
                    preview = compactPreview(detail.contentHtml),
                )
            }
        }

        else -> null
    }
}

private fun resolveLinkCardDestination(linkCard: DataHolder.Pin.ContentLinkCard): NavDestination? {
    val byUrl = linkCard.url
        .takeIf { it.isNotBlank() }
        ?.toUri()
        ?.let(::resolveContent)
    if (byUrl != null) return byUrl

    val contentId = linkCard.dataContentId
    return when (linkCard.dataContentType.lowercase(Locale.ROOT)) {
        "answer" -> contentId.toLongOrNull()?.let { Article(type = ArticleType.Answer, id = it) }
        "article" -> contentId.toLongOrNull()?.let { Article(type = ArticleType.Article, id = it) }
        "question" -> contentId.toLongOrNull()?.let { Question(questionId = it) }
        "pin" -> contentId.toLongOrNull()?.let { Pin(id = it) }
        else -> null
    }
}

private fun compactPreview(raw: String, maxLength: Int = 120): String {
    val plainText = Jsoup
        .parse(raw)
        .text()
        .replace(Regex("\\s+"), " ")
        .trim()
    if (plainText.length <= maxLength) return plainText
    return plainText.take(maxLength).trimEnd() + "..."
}

private fun compactTitle(raw: String, maxLength: Int = 56): String = compactPreview(raw, maxLength)
