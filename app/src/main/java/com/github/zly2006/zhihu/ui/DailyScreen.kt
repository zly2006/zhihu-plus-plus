package com.github.zly2006.zhihu.ui

import android.content.Intent
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import com.github.zly2006.zhihu.LocalNavigator
import com.github.zly2006.zhihu.MainActivity
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.DailyStoriesResponse
import com.github.zly2006.zhihu.data.DailyStory
import com.github.zly2006.zhihu.resolveContent
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonPrimitive
import org.jsoup.Jsoup
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Data class to hold stories grouped by date
data class DailySection(
    val date: String,
    val stories: List<DailyStory>,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyScreen() {
    val onNavigate = LocalNavigator.current
    val context = LocalActivity.current as MainActivity
    var sections by remember { mutableStateOf<List<DailySection>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var isRefreshing by remember { mutableStateOf(false) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var currentViewingDate by remember { mutableStateOf("") }
    var nextDate by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val json = remember {
        Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }
    }

    suspend fun loadLatestStories() {
        try {
            val response = context.httpClient.get("https://news-at.zhihu.com/api/4/stories/latest")
            val data = json.decodeFromString<DailyStoriesResponse>(response.body<String>())
            sections = listOf(DailySection(data.date, data.stories))
            nextDate = data.date
            currentViewingDate = formatDate(data.date)
            error = null
        } catch (e: Exception) {
            error = "加载失败: ${e.message}"
            e.printStackTrace()
        } finally {
            isLoading = false
            isRefreshing = false
        }
    }

    suspend fun loadMoreStories() {
        if (isLoadingMore || nextDate == null) return
        isLoadingMore = true
        try {
            val response = context.httpClient.get("https://news-at.zhihu.com/api/4/stories/before/$nextDate")
            val data = json.decodeFromString<DailyStoriesResponse>(response.body<String>())
            sections = sections + DailySection(data.date, data.stories)
            nextDate = data.date
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoadingMore = false
        }
    }

    // Update current viewing date based on scroll position
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { index ->
                var itemCount = 0
                for (section in sections) {
                    // +1 for the date header
                    if (index < itemCount + 1 + section.stories.size) {
                        currentViewingDate = formatDate(section.date)
                        break
                    }
                    itemCount += 1 + section.stories.size
                }
            }
    }

    // Load more when approaching the end
    LaunchedEffect(listState) {
        snapshotFlow {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val lastVisibleItem = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleItem to totalItems
        }.collect { (lastVisibleItem, totalItems) ->
            if (lastVisibleItem >= totalItems - 3 && !isLoadingMore && !isLoading) {
                loadMoreStories()
            }
        }
    }

    LaunchedEffect(Unit) {
        loadLatestStories()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "知乎日报",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                        if (currentViewingDate.isNotEmpty()) {
                            Text(
                                currentViewingDate,
                                style = MaterialTheme.typography.bodySmall.copy(
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                ),
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                isRefreshing = true
                                loadLatestStories()
                                listState.scrollToItem(0)
                            }
                        },
                    ) {
                        Icon(
                            Icons.Filled.Refresh,
                            contentDescription = "刷新",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
                windowInsets = WindowInsets(0.dp),
            )
        },
    ) { paddingValues ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                scope.launch {
                    isRefreshing = true
                    loadLatestStories()
                    listState.scrollToItem(0)
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "正在加载...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            )
                        }
                    }
                }

                error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            error ?: "未知错误",
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }

                sections.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "暂无内容",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp),
                    ) {
                        sections.forEach { section ->
                            // Date header
                            item(key = "header_${section.date}") {
                                DateHeader(date = formatDate(section.date))
                            }
                            // Stories for this date
                            items(section.stories, key = { "story_${it.id}" }) { story ->
                                DailyStoryCard(
                                    story = story,
                                    onClick = {
                                        scope.launch {
                                            val jojo = AccountData.fetchGet(context, "https://daily.zhihu.com/api/7/story/${story.id}")!!
                                            val body = Jsoup.parse(jojo["body"]!!.jsonPrimitive.content)
                                            val url = body.selectFirst("a")?.attr("href")
                                            val destination = runCatching {
                                                url?.let { resolveContent(url.toUri()) }
                                            }.getOrNull()
                                            if (destination != null) {
                                                onNavigate(destination)
                                            } else {
                                                val intent = Intent(Intent.ACTION_VIEW, story.url.toUri())
                                                context.startActivity(intent)
                                            }
                                        }
                                    },
                                )
                            }
                        }

                        // Loading indicator at the bottom
                        if (isLoadingMore) {
                            item(key = "loading_more") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DateHeader(date: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)),
            )
            Text(
                text = date,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                ),
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)),
            )
        }
    }
}

@Composable
fun DailyStoryCard(
    story: DailyStory,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 左侧图片区域
            if (story.images.isNotEmpty()) {
                AsyncImage(
                    model = story.images.first(),
                    contentDescription = story.title,
                    modifier = Modifier
                        .weight(0.3f, fill = true)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop,
                )
            }

            // 右侧内容区域
            Column(
                modifier = Modifier.weight(0.7f),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                // 标题
                Text(
                    text = story.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                    ),
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                // 底部信息
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Filled.AccessTime,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = story.hint,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 12.sp,
                        ),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    )
                }
            }
        }
    }
}

private fun formatDate(dateString: String): String = try {
    val inputFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    val outputFormat = SimpleDateFormat("yyyy年MM月dd日", Locale.getDefault())
    val date = inputFormat.parse(dateString)
    outputFormat.format(date ?: Date())
} catch (e: Exception) {
    dateString
}
