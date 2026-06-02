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

package com.github.zly2006.zhihu.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.fleeksoft.ksoup.Ksoup
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.navigation.resolveContent
import com.github.zly2006.zhihu.shared.data.DailySection
import com.github.zly2006.zhihu.shared.data.DailyStory
import com.github.zly2006.zhihu.shared.util.formatDailyDate
import com.github.zly2006.zhihu.shared.viewmodel.DailyViewModel
import com.github.zly2006.zhihu.ui.components.AutoHideTopBar
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
fun DailyScreen(
    testState: DailyScreenUiState? = null,
    onTestDateSelected: ((String) -> Unit)? = null,
    onTestLoadMore: (() -> Unit)? = null,
) {
    val navigator = LocalNavigator.current
    val httpClient = rememberZhihuHttpClient()
    val uriHandler = LocalUriHandler.current
    val viewModel = viewModel { DailyViewModel() }
    val isTestMode = testState != null
    var isRefreshing by remember { mutableStateOf(false) }
    var currentViewingDate by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val uiState = testState ?: DailyScreenUiState(
        sections = viewModel.sections,
        isLoading = viewModel.isLoading,
        isLoadingMore = viewModel.isLoadingMore,
        error = viewModel.error,
    )

    LaunchedEffect(listState, uiState.sections) {
        currentViewingDate = resolveViewingDate(
            firstVisibleItemIndex = listState.firstVisibleItemIndex,
            sections = uiState.sections,
        )
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { index ->
                currentViewingDate = resolveViewingDate(
                    firstVisibleItemIndex = index,
                    sections = uiState.sections,
                )
            }
    }

    LaunchedEffect(listState, isTestMode, onTestLoadMore) {
        // 滚动到底部时加载更多
        snapshotFlow {
            val info = listState.layoutInfo
            (info.visibleItemsInfo.lastOrNull()?.index ?: 0) to info.totalItemsCount
        }.collect { (last, total) ->
            if (total > 0 && last >= total - 3) {
                if (isTestMode) {
                    onTestLoadMore?.invoke()
                } else {
                    viewModel.loadMore(httpClient)
                }
            }
        }
    }

    LaunchedEffect(isTestMode) {
        if (!isTestMode && viewModel.sections.isEmpty()) {
            viewModel.loadLatest(httpClient)
        }
    }

    val doRefresh: () -> Unit = {
        scope.launch {
            isRefreshing = true
            if (!isTestMode) {
                viewModel.loadLatest(httpClient)
                listState.scrollToItem(0)
            }
            isRefreshing = false
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = Clock.System.now().toEpochMilliseconds(),
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showDatePicker = false
                    datePickerState.selectedDateMillis?.let { millis ->
                        val dateStr = formatDailyDatePickerSelection(millis)
                        scope.launch {
                            if (isTestMode) {
                                onTestDateSelected?.invoke(dateStr)
                            } else {
                                viewModel.loadDate(httpClient, dateStr)
                                listState.scrollToItem(0)
                            }
                        }
                    }
                }) { Text("确认") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("取消") }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            AutoHideTopBar {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "知乎日报",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                ),
                                modifier = Modifier.testTag(DAILY_SCREEN_TITLE_TAG),
                            )
                            if (currentViewingDate.isNotEmpty()) {
                                Text(
                                    currentViewingDate,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    ),
                                    modifier = Modifier.testTag(DAILY_SCREEN_CURRENT_DATE_TAG),
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showDatePicker = true },
                            modifier = Modifier.testTag(DAILY_SCREEN_DATE_PICKER_BUTTON_TAG),
                        ) {
                            Icon(Icons.Filled.DateRange, contentDescription = "选择日期")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface,
                    ),
                )
            }
        },
    ) { scaffoldPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = doRefresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = scaffoldPadding.calculateTopPadding()),
        ) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag(DAILY_SCREEN_LOADING_TAG),
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

                uiState.error != null -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag(DAILY_SCREEN_ERROR_TAG),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            uiState.error,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }

                uiState.sections.isEmpty() -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag(DAILY_SCREEN_EMPTY_TAG),
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
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag(DAILY_SCREEN_LIST_TAG),
                        contentPadding = PaddingValues(vertical = 8.dp),
                    ) {
                        uiState.sections.forEach { section ->
                            // Date header
                            item(key = "header_${section.date}") {
                                DateHeader(
                                    date = formatDailyDate(section.date),
                                    modifier = Modifier.testTag(dailySectionHeaderTag(section.date)),
                                )
                            }
                            // Stories for this date
                            items(section.stories, key = { "story_${it.id}" }) { story ->
                                DailyStoryCard(
                                    story = story,
                                    modifier = Modifier.testTag(dailyStoryCardTag(story.id)),
                                    onClick = {
                                        if (!isTestMode) {
                                            scope.launch {
                                                val destination = fetchDailyStoryDestination(httpClient, story.id)
                                                if (destination != null) {
                                                    navigator.onNavigate(destination)
                                                } else {
                                                    uriHandler.openUri(story.url)
                                                }
                                            }
                                        }
                                    },
                                )
                            }
                        }

                        // Loading indicator at the bottom
                        if (uiState.isLoadingMore) {
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
fun DateHeader(
    date: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
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
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier
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

private fun resolveViewingDate(
    firstVisibleItemIndex: Int,
    sections: List<DailySection>,
): String {
    var count = 0
    for (section in sections) {
        if (firstVisibleItemIndex < count + 1 + section.stories.size) {
            return formatDailyDate(section.date)
        }
        count += 1 + section.stories.size
    }
    return ""
}

private fun formatDailyDatePickerSelection(millis: Long): String {
    val date = kotlin.time.Instant
        .fromEpochMilliseconds(millis)
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date
    return date.year.toString().padStart(4, '0') +
        (date.month.ordinal + 1).toString().padStart(2, '0') +
        date.day.toString().padStart(2, '0')
}

private suspend fun fetchDailyStoryDestination(
    httpClient: HttpClient,
    storyId: Long,
): NavDestination? = withContext(Dispatchers.Default) {
    val response: JsonObject = httpClient
        .get("https://daily.zhihu.com/api/7/story/$storyId")
        .body()
    val body = response["body"]?.jsonPrimitive?.content ?: return@withContext null
    val url = Ksoup.parse(body).selectFirst("a")?.attr("href")
    url?.let(::resolveContent)
}

private fun dailySectionHeaderTag(date: String) = "daily_screen_section_$date"

private fun dailyStoryCardTag(storyId: Long) = "daily_screen_story_$storyId"

private const val DAILY_SCREEN_TITLE_TAG = "daily_screen_title"
private const val DAILY_SCREEN_CURRENT_DATE_TAG = "daily_screen_current_date"
private const val DAILY_SCREEN_DATE_PICKER_BUTTON_TAG = "daily_screen_date_picker_button"
private const val DAILY_SCREEN_LOADING_TAG = "daily_screen_loading"
private const val DAILY_SCREEN_ERROR_TAG = "daily_screen_error"
private const val DAILY_SCREEN_EMPTY_TAG = "daily_screen_empty"
private const val DAILY_SCREEN_LIST_TAG = "daily_screen_list"

data class DailyScreenUiState(
    val sections: List<DailySection> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
)
