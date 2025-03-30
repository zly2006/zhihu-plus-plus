@file:Suppress("FunctionName")

package com.github.zly2006.zhihu.v2.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Comment
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.LocalPinnableContainer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.github.zly2006.zhihu.Article
import com.github.zly2006.zhihu.CommentHolder
import com.github.zly2006.zhihu.NavDestination
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.signFetchRequest
import com.github.zly2006.zhihu.v2.theme.Typography
import com.github.zly2006.zhihu.v2.ui.components.WebviewComp
import com.github.zly2006.zhihu.v2.ui.components.loadZhihu
import com.github.zly2006.zhihu.v2.viewmodel.BaseFeedViewModel
import com.github.zly2006.zhihu.v2.viewmodel.CommentItem
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.text.SimpleDateFormat
import java.util.*

private val HMS = SimpleDateFormat("HH:mm:ss")
private val MDHMS = SimpleDateFormat("MM-dd HH:mm:ss")
private val YMDHMS = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")

private fun rootCommentUrl(content: NavDestination?) = when (content) {
    is Article -> {
        if (content.type == "answer") {
            "https://www.zhihu.com/api/v4/comment_v5/answers/${content.id}/root_comment"
        } else if (content.type == "article") {
            "https://www.zhihu.com/api/v4/comment_v5/articles/${content.id}/root_comment"
        } else null
    }

    is CommentHolder -> {
        "https://www.zhihu.com/api/v4/comment_v5/comment/${content.commentId}/child_comment"
    }

    else -> null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentScreen(
    visible: Boolean,
    httpClient: HttpClient,
    content: NavDestination?,
    activeCommentItem: CommentItem? = null,
    onChildCommentClick: (CommentItem) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var commentsList = remember { mutableStateListOf<CommentItem>() }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var commentInput by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }

    @Serializable
    class CommentResponse(
        val data: List<DataHolder.Comment>,
        val paging: BaseFeedViewModel.Paging,
    )

    // 加载评论
    LaunchedEffect(content) {
        isLoading = true
        errorMessage = null
        try {
            commentsList.clear()
            val url = rootCommentUrl(content)
            if (url == null) {
                errorMessage = "不支持在此内容下评论"
                return@LaunchedEffect
            }
            val response = httpClient.get(url) {
                signFetchRequest(context)
            }
            if (response.status.isSuccess()) {
                val comments = response.body<JsonObject>()
                val parsedComments = AccountData.decodeJson<CommentResponse>(comments)
                commentsList.addAll(
                    parsedComments.data.map {
                        CommentItem(
                            it,
                            if (it.childCommentCount == 0) null
                            else CommentHolder(it.id, content!!)
                        )
                    }
                )
            } else {
                errorMessage = "加载评论失败: ${response.status} ${response.bodyAsText()}"
            }
        } catch (e: Exception) {
            errorMessage = "加载评论异常: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    // 提交评论
    fun submitComment() {
        if (commentInput.isBlank()) return

        scope.launch {
            isSending = true
            try {
                val url = when (content) {
                    is Article -> {
                        if (content.type == "answer") {
                            "https://www.zhihu.com/api/v4/comment_v5/answers/${content.id}/root_comment"
                        } else if (content.type == "article") {
                            "https://www.zhihu.com/api/v4/comment_v5/articles/${content.id}/root_comment"
                        } else null
                    }

                    is CommentHolder -> {
                        "https://www.zhihu.com/api/v4/comment_v5/comment/${content.commentId}/child_comment"
                    }

                    else -> null
                }

                if (url == null) {
                    errorMessage = "不支持在此内容下评论"
                    return@launch
                }

                val response = httpClient.post(url) {
                    signFetchRequest(context)
                    contentType(ContentType.Application.Json)
                    setBody("""{"content":"$commentInput"}""")
                }

                if (response.status.isSuccess()) {
                    commentInput = ""
                    // 刷新评论列表
                    // todo
                } else {
                    errorMessage = "评论发送失败: ${response.status}"
                }
            } catch (e: Exception) {
                errorMessage = "评论发送异常: ${e.message}"
            } finally {
                isSending = false
            }
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            animationSpec = tween(300)
        ) { it },
        exit = slideOutVertically(
            animationSpec = tween(300)
        ) { it },
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // 评论内容区域
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 100.dp)
                    .fillMaxHeight() // 保留上方空间
                    .align(Alignment.BottomCenter), // 底部对齐
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    CommentTopText(content)
                    Box(modifier = Modifier.weight(1f)) {
                        when {
                            isLoading -> {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator()
                                }
                            }

                            errorMessage != null -> {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text(errorMessage!!, color = MaterialTheme.colorScheme.error)
                                }
                            }

                            commentsList.isEmpty() -> {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Text("暂无评论")
                                }
                            }

                            else -> {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    if (activeCommentItem != null) {
                                        item(0) {
                                            Column {
                                                CommentItem(
                                                    comment = activeCommentItem,
                                                    httpClient = httpClient,
                                                    onChildCommentClick = { }
                                                )
                                                HorizontalDivider()
                                            }
                                        }
                                    }
                                    items(commentsList) { commentItem ->
                                        CommentItem(
                                            comment = commentItem,
                                            httpClient = httpClient,
                                            onChildCommentClick = { comment ->
                                                if (comment.clickTarget != null) {
                                                    onChildCommentClick(comment)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 评论输入框
                    Surface(
                        tonalElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextField(
                                value = commentInput,
                                onValueChange = { commentInput = it },
                                modifier = Modifier.weight(1f),
                                placeholder = { Text("写下你的评论...") },
                                singleLine = false,
                                maxLines = 3,
                                colors = TextFieldDefaults.colors()
                            )

                            IconButton(
                                onClick = { submitComment() },
                                enabled = !isSending && commentInput.isNotBlank()
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Outlined.Send,
                                    contentDescription = "发送评论",
                                    tint = if (!isSending && commentInput.isNotBlank())
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
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
        if (content is CommentHolder) "回复"
        else "评论",
        style = Typography.bodyMedium.copy(
            fontWeight = FontWeight.Bold,
        ),
        modifier = Modifier.fillMaxWidth().height(26.dp),
        textAlign = TextAlign.Center,
        fontSize = 18.sp
    )
}

@Composable
private fun CommentItem(
    comment: CommentItem,
    httpClient: HttpClient,
    onChildCommentClick: (CommentItem) -> Unit
) {
    val commentData = comment.item

    Column(modifier = Modifier.fillMaxWidth()) {
        // 作者信息
        Row(
            modifier = Modifier.fillMaxWidth()
        ) {
            // 头像
            AsyncImage(
                model = commentData.author.avatarUrl,
                contentDescription = "头像",
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(
                verticalArrangement = Arrangement.Top,
                modifier = Modifier.fillMaxHeight()
            ) {
                // 作者名
                Text(
                    text = commentData.author.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                LocalPinnableContainer.current?.pin()
                WebviewComp(httpClient) {
                    it.loadZhihu(
                        "",
                        Jsoup.parse(commentData.content).processCommentImages().body().html(),
                        additionalStyle = """
                            body {
                              margin: 0;
                            }
                            p {
                              margin: 0;
                              margin-block: 0;
                            }
                        """.trimIndent()
                    )
                }
            }
        }

        // 底部信息栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 44.dp),
            horizontalArrangement = Arrangement.SpaceBetween
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
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.weight(1f))

            // 回复按钮
            if (comment.clickTarget != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onChildCommentClick(comment) }
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.Comment,
                        contentDescription = "回复",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = comment.item.childCommentCount.toString(),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))
            }

            // 点赞
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.ThumbUp,
                    contentDescription = "点赞",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = commentData.likeCount.toString(),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

fun Document.processCommentImages(): Document = apply {
    select("a.comment_img").forEach {
        it.tagName("img")
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
private fun CommentItemPreview() {
    val comment = CommentItem(
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
                userType = ""
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
        clickTarget = null
    )
    val context = LocalContext.current
    CommentItem(comment, AccountData.httpClient(context)) {
    }
}
