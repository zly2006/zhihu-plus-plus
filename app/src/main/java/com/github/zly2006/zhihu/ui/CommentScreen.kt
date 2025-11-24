@file:Suppress("FunctionName")

package com.github.zly2006.zhihu.ui

import android.content.Context.MODE_PRIVATE
import android.text.Html
import android.text.util.Linkify
import android.util.TypedValue
import android.widget.TextView
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Comment
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LocalPinnableContainer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.github.zly2006.zhihu.CommentHolder
import com.github.zly2006.zhihu.NavDestination
import com.github.zly2006.zhihu.Person
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.theme.Typography
import com.github.zly2006.zhihu.ui.components.WebviewComp
import com.github.zly2006.zhihu.util.LinkMovementMethod
import com.github.zly2006.zhihu.viewmodel.comment.BaseCommentViewModel
import com.github.zly2006.zhihu.viewmodel.comment.ChildCommentViewModel
import com.github.zly2006.zhihu.viewmodel.comment.RootCommentViewModel
import io.ktor.client.HttpClient
import kotlinx.coroutines.launch
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

typealias CommentModel = com.github.zly2006.zhihu.viewmodel.CommentItem

private val HMS = SimpleDateFormat("HH:mm:ss", Locale.ENGLISH)
private val MDHMS = SimpleDateFormat("MM-dd HH:mm:ss", Locale.ENGLISH)
val YMDHMS = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentScreen(
    httpClient: HttpClient,
    content: () -> NavDestination,
    activeCommentItem: CommentModel? = null,
    topPadding: Dp = 100.dp,
    onNavigate: (NavDestination) -> Unit,
    onChildCommentClick: (CommentModel) -> Unit,
) {
    val context = LocalContext.current
    var commentInput by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    val preferences = remember {
        context.getSharedPreferences(PREFERENCE_NAME, MODE_PRIVATE)
    }
    val useWebview = remember { preferences.getBoolean("commentsUseWebview", true) }
    val pinWebview = remember { preferences.getBoolean("commentsPinWebview", false) }

    // 根据内容类型选择合适的ViewModel
    val viewModel: BaseCommentViewModel = when (val content = content()) {
        is CommentHolder -> remember {
            // 子评论不进行状态保存
            ChildCommentViewModel(content)
        }

        else -> viewModel {
            RootCommentViewModel(content)
        }
    }
    val rootContent = when (val content = content()) {
        is CommentHolder -> content.article
        else -> content
    }

    val listState = rememberLazyListState()

    // 监控滚动位置以实现加载更多
    val loadMore = remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItemsCount = layoutInfo.totalItemsCount
            val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0

            lastVisibleItemIndex >= totalItemsCount - 3 && !viewModel.isLoading && !viewModel.isEnd
        }
    }

    // 监控滚动加载更多
    LaunchedEffect(loadMore.value) {
        if (loadMore.value && viewModel.errorMessage == null) {
            viewModel.loadMore(context)
        }
    }

    // 初始加载评论
    LaunchedEffect(content) {
        if (viewModel.article != content()) {
            error("Internal Error: Detected content mismatch")
        }
        if (viewModel.errorMessage == null) {
            viewModel.loadMore(context)
        }
    }
    val coroutineScope = rememberCoroutineScope()

    // 提交评论函数
    fun submitComment() {
        if (commentInput.isBlank() || isSending) return

        isSending = true
        viewModel.submitComment(content(), commentInput, httpClient, context) {
            commentInput = ""
            isSending = false
            coroutineScope.launch {
                listState.animateScrollToItem(
                    0,
                    0,
                )
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        // 评论内容区域
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = topPadding)
                .fillMaxHeight()
                .align(Alignment.BottomCenter),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
            color = MaterialTheme.colorScheme.surface,
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                CommentTopText(content())
                Box(modifier = Modifier.weight(1f)) {
                    when {
                        viewModel.isLoading && viewModel.allData.isEmpty() -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }

                        viewModel.errorMessage != null && viewModel.allData.isEmpty() -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(viewModel.errorMessage!!, color = MaterialTheme.colorScheme.error)
                            }
                        }

                        viewModel.allData.isEmpty() -> {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("暂无评论")
                            }
                        }

                        else -> {
                            @Composable
                            fun Comment(
                                commentItem: CommentModel,
                                modifier: Modifier = Modifier,
                                onChildCommentClick: (CommentModel) -> Unit,
                            ) {
                                var isLiked by remember { mutableStateOf(commentItem.item.liked) }
                                var likeCount by remember { mutableIntStateOf(commentItem.item.likeCount) }
                                var isLikeLoading by remember { mutableStateOf(false) }

                                Column(modifier = modifier) {
                                    CommentItem(
                                        comment = commentItem,
                                        useWebview = useWebview,
                                        pinWebview = pinWebview,
                                        isLiked = isLiked,
                                        likeCount = likeCount,
                                        isLikeLoading = isLikeLoading,
                                        toggleLike = {
                                            viewModel.toggleLikeComment(
                                                httpClient = httpClient,
                                                commentData = commentItem.item,
                                                context = context,
                                            ) {
                                                val newLikeState = !isLiked
                                                isLiked = newLikeState
                                                likeCount += if (newLikeState) 1 else -1
                                                commentItem.item.liked = newLikeState
                                                commentItem.item.likeCount = likeCount
                                            }
                                        },
                                        onNavigate = onNavigate,
                                        onChildCommentClick = onChildCommentClick,
                                    )

                                    // 在根评论区时 子评论
                                    if (activeCommentItem == null && commentItem.item.childCommentCount > 0) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(start = 40.dp, top = 8.dp),
                                        ) {
                                            if (commentItem.item.childComments.isNotEmpty()) {
                                                commentItem.item.childComments.forEach { childComment ->
                                                    var liked by remember { mutableStateOf(childComment.liked) }
                                                    var likeCount by remember { mutableIntStateOf(childComment.likeCount) }
                                                    val childCommentItem = CommentModel(
                                                        item = childComment,
                                                        clickTarget = null, // 子评论不需要点击跳转
                                                    )
                                                    CommentItem(
                                                        comment = childCommentItem,
                                                        isLiked = liked,
                                                        likeCount = likeCount,
                                                        toggleLike = {
                                                            viewModel.toggleLikeComment(
                                                                commentData = childCommentItem.item,
                                                                httpClient = httpClient,
                                                                context = context,
                                                            ) {
                                                                val newLikeState = !liked
                                                                liked = newLikeState
                                                                likeCount += if (newLikeState) 1 else -1
                                                                childCommentItem.item.liked = newLikeState
                                                                childCommentItem.item.likeCount = likeCount
                                                            }
                                                        },
                                                        useWebview = useWebview,
                                                        pinWebview = pinWebview,
                                                        onNavigate = onNavigate,
                                                        onChildCommentClick = onChildCommentClick,
                                                    )
                                                }
                                            }
                                            Button(
                                                onClick = { onChildCommentClick(commentItem) },
                                                modifier = Modifier
                                                    .height(28.dp),
                                                shape = RoundedCornerShape(50),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                                                    contentColor = MaterialTheme.colorScheme.onSurface,
                                                ),
                                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                                            ) {
                                                Icon(
                                                    Icons.AutoMirrored.Outlined.Comment,
                                                    contentDescription = "查看子评论",
                                                    modifier = Modifier.size(16.dp),
                                                    tint = MaterialTheme.colorScheme.surfaceTint,
                                                )
                                                Text(
                                                    "查看 ${commentItem.item.childCommentCount} 条子评论",
                                                    fontSize = 12.sp,
                                                    modifier = Modifier.padding(vertical = 1.dp, horizontal = 4.dp),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            LazyColumn(
                                state = listState,
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                if (activeCommentItem != null) {
                                    item(
                                        key = "active_${activeCommentItem.item.id}",
                                    ) {
                                        Column(
                                            modifier = Modifier.animateItem(
                                                fadeInSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                                fadeOutSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                                placementSpec = spring(
                                                    stiffness = Spring.StiffnessMediumLow,
                                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                                ),
                                            ),
                                        ) {
                                            Comment(activeCommentItem) { }
                                            HorizontalDivider()
                                        }
                                    }
                                }

                                items(
                                    items = viewModel.allData,
                                    key = { it.id },
                                ) { commentItem ->
                                    Comment(
                                        viewModel.createCommentItem(commentItem, article = rootContent),
                                        modifier = Modifier.animateItem(
                                            fadeInSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                            fadeOutSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                            placementSpec = spring(
                                                stiffness = Spring.StiffnessMediumLow,
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                            ),
                                        ),
                                    ) { comment ->
                                        if (comment.clickTarget != null) {
                                            onChildCommentClick(comment)
                                        }
                                    }
                                }

                                if (viewModel.isLoading && viewModel.allData.isNotEmpty()) {
                                    item(key = "loading_indicator") {
                                        Box(
                                            modifier = Modifier.fillMaxWidth().padding(8.dp),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // 评论输入框
                Surface(
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 40.dp, max = 140.dp)
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BasicTextField(
                            value = commentInput,
                            onValueChange = { commentInput = it },
                            modifier = Modifier.weight(1f),
                            decorationBox = { inner ->
                                Box {
                                    if (commentInput.isEmpty()) {
                                        Text(
                                            "写下你的评论...",
                                            fontSize = 16.sp,
                                        )
                                    }
                                    inner()
                                }
                            },
                            textStyle = TextStyle.Default.copy(
                                fontSize = 16.sp,
                                lineHeight = 18.sp,
                            ),
                        )

                        IconButton(
                            onClick = { submitComment() },
                            modifier = Modifier.size(24.dp),
                            enabled = !isSending && commentInput.isNotBlank(),
                        ) {
                            if (isSending) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            } else {
                                Icon(
                                    Icons.AutoMirrored.Outlined.Send,
                                    contentDescription = "发送评论",
                                    tint = if (commentInput.isNotBlank()) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
@Preview(showBackground = true)
fun CommentTopText(content: NavDestination? = null) {
    Text(
        if (content is CommentHolder) {
            "回复"
        } else {
            "评论"
        },
        style = Typography.bodyMedium.copy(
            fontWeight = FontWeight.Bold,
        ),
        modifier = Modifier.fillMaxWidth().height(26.dp),
        textAlign = TextAlign.Center,
        fontSize = 18.sp,
    )
}

@Composable
private fun CommentItem(
    comment: CommentModel,
    useWebview: Boolean,
    pinWebview: Boolean,
    isLiked: Boolean = false,
    likeCount: Int = 0,
    isLikeLoading: Boolean = false,
    toggleLike: () -> Unit = {},
    onNavigate: (NavDestination) -> Unit,
    onChildCommentClick: (CommentModel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val commentData = comment.item

    Column(modifier = modifier.fillMaxWidth()) {
        // 作者信息
        Row(
            modifier = Modifier.fillMaxWidth(),
        ) {
            // 头像
            AsyncImage(
                model = commentData.author.avatarUrl,
                contentDescription = "头像",
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(
                verticalArrangement = Arrangement.Top,
                modifier = Modifier.fillMaxHeight(),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // 作者名
                    Text(
                        text = commentData.author.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        modifier = Modifier.clickable {
                            onNavigate(
                                Person(
                                    id = commentData.author.id,
                                    name = commentData.author.name,
                                    urlToken = commentData.author.urlToken,
                                ),
                            )
                        },
                    )

                    val authorTag = comment.item.authorTag
                        .firstOrNull()
                        ?.get("text")
                        ?.jsonPrimitive
                        ?.contentOrNull

                    if (authorTag != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        AuthorTag(authorTag)
                    }

                    if (commentData.replyToAuthor != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "回复",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = commentData.replyToAuthor.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            modifier = Modifier.clickable {
                                onNavigate(
                                    Person(
                                        id = commentData.replyToAuthor.id,
                                        name = commentData.replyToAuthor.name,
                                        urlToken = commentData.replyToAuthor.urlToken,
                                    ),
                                )
                            },
                        )
                    }
                }

                if (pinWebview) {
                    LocalPinnableContainer.current?.pin()
                }
                if (useWebview) {
                    WebviewComp {
                        it.isVerticalScrollBarEnabled = false
                        it.isHorizontalScrollBarEnabled = false
                        it.loadZhihu(
                            "",
                            Jsoup.parse(commentData.content).processCommentImages(),
                            additionalStyle =
                                """
                                body { margin: 0; }
                                p { margin: 0; margin-block: 0; }
                                """.trimIndent(),
                        )
                    }
                } else {
                    // 评论内容
                    val context = LocalContext.current
                    val contentColor = LocalContentColor.current
                    AndroidView({
                        TextView(context).apply {
                            autoLinkMask = Linkify.EMAIL_ADDRESSES or Linkify.WEB_URLS
                            text = Html
                                .fromHtml(
                                    Jsoup
                                        .parse(commentData.content)
                                        .processCommentImages()
                                        .body()
                                        .html(),
                                    Html.FROM_HTML_MODE_COMPACT,
                                ).let {
                                    it
                                }
                            if (text.endsWith("\n")) {
                                text = text.subSequence(0, text.length - 1)
                            }
                            setTextIsSelectable(true)
                            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                            setTextColor(contentColor.toArgb())
                            movementMethod = LinkMovementMethod.getInstance()
                        }
                    })
                }
            }
        }

        // 底部信息栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 44.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // 时间
            val formattedTime = remember(commentData.createdTime) {
                val time = commentData.createdTime * 1000
                val now = System.currentTimeMillis()
                val dateTime = Date(time)
                val nowDate = Date(now)

                when {
                    isSameDay(dateTime, nowDate) -> HMS.format(time)
                    isSameYear(dateTime, nowDate) -> MDHMS.format(time)
                    else -> YMDHMS.format(time)
                }
            }

            Text(
                text = formattedTime,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            val ipInfo = comment.item.commentTag
                .firstOrNull {
                    it.type == "ip_info"
                }?.text
            if (ipInfo != null) {
                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = ipInfo,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // 回复按钮
            if (comment.clickTarget != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onChildCommentClick(comment) },
                ) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        Icons.AutoMirrored.Outlined.Comment,
                        contentDescription = "回复",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = comment.item.childCommentCount.toString(),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }

                Spacer(modifier = Modifier.width(8.dp))
            }

            // 点赞
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable(enabled = !isLikeLoading) { toggleLike() },
            ) {
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    if (isLiked) {
                        Icons.Filled.ThumbUp
                    } else {
                        Icons.Outlined.ThumbUp
                    },
                    contentDescription = "点赞",
                    modifier = Modifier.size(16.dp),
                    tint = if (isLiked) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = likeCount.toString(),
                    fontSize = 12.sp,
                    color = if (isLiked) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
        }
    }
}

fun Document.processCommentImages(): Document = apply {
    select("a.comment_img").forEach {
        it.tagName("img")
        it.text("")
        it.attr("src", it.attr("href"))
    }
}

private fun isSameDay(date1: Date, date2: Date): Boolean {
    val cal1 = Calendar.getInstance().apply { time = date1 }
    val cal2 = Calendar.getInstance().apply { time = date2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
        cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

private fun isSameYear(date1: Date, date2: Date): Boolean {
    val cal1 = Calendar.getInstance().apply { time = date1 }
    val cal2 = Calendar.getInstance().apply { time = date2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR)
}

@Preview(backgroundColor = 0xFFFFFF, showBackground = true, heightDp = 100)
@Composable
@Suppress("SpellCheckingInspection")
private fun CommentItemPreview() {
    val comment = CommentModel(
        item = DataHolder.Comment(
            id = "123",
            content = "<p>这是一条评论<br/>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Vestibulum eleifend nisl vitae est tincidunt, non rhoncus magna cursus. Donec non elit non urna dignissim dapibus. Curabitur tempus magna quis dui pellentesque, in venenatis leo mollis. Duis ornare turpis in fermentum mollis. In at fringilla odio. Morbi elementum cursus purus, ut mollis libero facilisis ac. Sed eu mattis ante, ac aliquet purus. Quisque non eros ut ligula tincidunt elementum in ac sem. Praesent diam metus, bibendum vitae mollis ut, vehicula eget ante. Quisque efficitur, odio at ornare commodo, nibh dui eleifend enim, eget consequat quam tortor sit amet arcu. Aliquam mollis auctor ligula, placerat sodales leo malesuada eu. Donec porta nisl at congue laoreet. Duis vel tellus tincidunt, malesuada urna in, maximus nisl. Maecenas rhoncus augue eros, non aliquet eros eleifend ut. Mauris dignissim quis nisi id suscipit. In imperdiet, odio id ornare pretium, eros ipsum faucibus felis, at accumsan mi ex vitae mi.</p>",
            createdTime = System.currentTimeMillis() / 1000,
            author = DataHolder.Comment.Author(
                name = "作者",
                avatarUrl = "https://i1.hdslb.com/bfs/face/b93b6ff0c1d434ae8026a4bedc82d0d883b5da95.jpg",
                isOrg = false,
                type = "people",
                url = "",
                urlToken = "",
                id = "",
                headline = "个人介绍",
                avatarUrlTemplate = "",
                isAdvertiser = false,
                gender = 0,
                userType = "",
            ),
            likeCount = 10,
            childCommentCount = 5,
            type = "",
            url = "",
            resourceType = "",
            collapsed = false,
            top = false,
            isDelete = false,
            reviewing = false,
            isAuthor = false,
            canCollapse = false,
            childComments = emptyList(),
        ),
        clickTarget = null,
    )
    CommentItem(
        comment,
        useWebview = true,
        pinWebview = true,
        onNavigate = { },
        onChildCommentClick = { },
    )
}

@Composable
fun AuthorTag(authorTag: String) {
    Box(
        modifier = Modifier
            .border(
                width = 0.5.dp,
                color = Color.Gray,
                shape = RoundedCornerShape(3.dp),
            ).padding(horizontal = 3.dp),
    ) {
        Text(
            text = authorTag,
            fontSize = 12.sp,
            lineHeight = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Preview(backgroundColor = 0xFFFFFF, showBackground = true)
@Composable
fun CommentAuthorTagPreview() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "作者名",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.clickable { },
        )

        Spacer(modifier = Modifier.width(4.dp))
        AuthorTag("作者")

        Spacer(modifier = Modifier.width(4.dp))
        Text(
            "回复",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = "zly2006",
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            modifier = Modifier.clickable { },
        )
    }
}

@Preview(backgroundColor = 0xFFFFFF, showBackground = true, heightDp = 100)
@Composable
@Suppress("SpellCheckingInspection")
private fun NestedCommentPreview() {
    val comment = CommentModel(
        item = DataHolder.Comment(
            id = "123",
            content = "<p>这是一条评论<br/>Lorem ipsum dolor sit amet, consectetur adipiscing elit. Vestibulum eleifend nisl vitae est tincidunt, non rhoncus magna cursus. Donec non elit non urna dignissim dapibus. Curabitur tempus magna quis dui pellentesque, in venenatis leo mollis. Duis ornare turpis in fermentum mollis. In at fringilla odio. Morbi elementum cursus purus, ut mollis libero facilisis ac. Sed eu mattis ante, ac aliquet purus. Quisque non eros ut ligula tincidunt elementum in ac sem. Praesent diam metus, bibendum vitae mollis ut, vehicula eget ante. Quisque efficitur, odio at ornare commodo, nibh dui eleifend enim, eget consequat quam tortor sit amet arcu. Aliquam mollis auctor ligula, placerat sodales leo malesuada eu. Donec porta nisl at congue laoreet. Duis vel tellus tincidunt, malesuada urna in, maximus nisl. Maecenas rhoncus augue eros, non aliquet eros eleifend ut. Mauris dignissim quis nisi id suscipit. In imperdiet, odio id ornare pretium, eros ipsum faucibus felis, at accumsan mi ex vitae mi.</p>",
            createdTime = System.currentTimeMillis() / 1000,
            author = DataHolder.Comment.Author(
                name = "作者",
                avatarUrl = "https://i1.hdslb.com/bfs/face/b93b6ff0c1d434ae8026a4bedc82d0d883b5da95.jpg",
                isOrg = false,
                type = "people",
                url = "",
                urlToken = "",
                id = "",
                headline = "个人介绍",
                avatarUrlTemplate = "",
                isAdvertiser = false,
                gender = 0,
                userType = "",
            ),
            likeCount = 10,
            childCommentCount = 5,
            type = "",
            url = "",
            resourceType = "",
            collapsed = false,
            top = false,
            isDelete = false,
            reviewing = false,
            isAuthor = false,
            canCollapse = false,
            childComments = listOf(
                DataHolder.Comment(
                    id = "千早爱音",
                    content = "<p>我喜欢你</p>",
                    createdTime = System.currentTimeMillis() / 1000,
                    author = DataHolder.Comment.Author(
                        name = "长期素食",
                        avatarUrl = "",
                        isOrg = false,
                        type = "people",
                        url = "",
                        urlToken = "",
                        id = "",
                        headline = "个人介绍",
                        avatarUrlTemplate = "",
                        isAdvertiser = false,
                        gender = 0,
                        userType = "people",
                    ),
                    type = "",
                    isDelete = false,
                    url = "",
                    resourceType = "",
                    collapsed = false,
                    reviewing = false,
                ),
            ),
        ),
        clickTarget = null,
    )
    CommentItem(
        comment,
        useWebview = true,
        pinWebview = true,
        onNavigate = { },
        onChildCommentClick = { },
    )
}
