/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * Licensed under AGPL-3.0-only.
 */

package com.github.zly2006.zhihu.ui.miuix

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.input.nestedscroll.nestedScroll
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
import com.github.zly2006.zhihu.shared.data.DailyStory
import com.github.zly2006.zhihu.shared.platform.rememberSettingBoolean
import com.github.zly2006.zhihu.shared.platform.rememberSettingsStore
import com.github.zly2006.zhihu.shared.util.formatDailyDate
import com.github.zly2006.zhihu.shared.viewmodel.DailyViewModel
import com.github.zly2006.zhihu.theme.getMiuixAppBarColor
import com.github.zly2006.zhihu.theme.installerMiuixBlurEffect
import com.github.zly2006.zhihu.theme.rememberMiuixBlurBackdrop
import com.github.zly2006.zhihu.ui.DailyScreenUiState
import com.github.zly2006.zhihu.ui.components.AutoHideTopBar
import com.github.zly2006.zhihu.ui.miuix.components.MiuixIconsEmbedded
import com.github.zly2006.zhihu.ui.rememberZhihuHttpClient
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
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalMaterial3Api::class, ExperimentalTime::class)
@Composable
fun MiuixDailyScreen(
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
    var showDatePicker by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val uiState = testState ?: DailyScreenUiState(
        sections = viewModel.sections,
        isLoading = viewModel.isLoading,
        isLoadingMore = viewModel.isLoadingMore,
        error = viewModel.error,
    )
    val settings = rememberSettingsStore()
    val blurEnabled = rememberSettingBoolean("blurEnabled", true, settings)
    val backdrop = rememberMiuixBlurBackdrop(blurEnabled)
    val scrollBehavior = MiuixScrollBehavior()

    LaunchedEffect(listState, isTestMode, onTestLoadMore) {
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
                    modifier = Modifier.installerMiuixBlurEffect(backdrop),
                    color = backdrop.getMiuixAppBarColor(),
                    title = "知乎日报",
                    actions = {
                        IconButton(
                            onClick = { showDatePicker = true },
                            modifier = Modifier.testTag(DAILY_SCREEN_DATE_PICKER_BUTTON_TAG),
                        ) {
                            Icon(MiuixIconsEmbedded.Months, contentDescription = "选择日期")
                        }
                    },
                    scrollBehavior = scrollBehavior,
                )
            }
        },
    ) { padding ->
        PullToRefresh(
            isRefreshing = isRefreshing,
            onRefresh = {
                scope.launch {
                    isRefreshing = true
                    if (!isTestMode) {
                        viewModel.loadLatest(httpClient)
                        listState.scrollToItem(0)
                    }
                    isRefreshing = false
                }
            },
            contentPadding = PaddingValues(top = padding.calculateTopPadding() + 6.dp),
            refreshTexts = listOf("下拉刷新", "释放刷新", "正在刷新...", "刷新完成"),
        ) {
            Box(
                modifier = if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier,
            ) {
                when {
                    // 下拉刷新时不显示中心转圈，避免与刷新动画叠加（旧内容仍展示，刷新成功后替换）
                    uiState.isLoading && !isRefreshing -> {
                        Box(
                            modifier = Modifier.fillMaxSize().testTag(DAILY_SCREEN_LOADING_TAG),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("正在加载...", color = MiuixTheme.colorScheme.onSurfaceSecondary)
                            }
                        }
                    }

                    uiState.error != null -> {
                        Box(
                            modifier = Modifier.fillMaxSize().testTag(DAILY_SCREEN_ERROR_TAG),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(uiState.error, color = MiuixTheme.colorScheme.error)
                        }
                    }

                    uiState.sections.isEmpty() -> {
                        Box(
                            modifier = Modifier.fillMaxSize().testTag(DAILY_SCREEN_EMPTY_TAG),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("暂无内容", fontSize = 16.sp, color = MiuixTheme.colorScheme.onSurfaceSecondary)
                        }
                    }

                    else -> {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxSize()
                                .fillMaxHeight()
                                .overScrollVertical()
                                .scrollEndHaptic()
                                .nestedScroll(scrollBehavior.nestedScrollConnection)
                                .testTag(DAILY_SCREEN_LIST_TAG),
                            contentPadding = PaddingValues(
                                top = padding.calculateTopPadding() + 8.dp,
                                bottom = padding.calculateBottomPadding() + 8.dp,
                            ),
                        ) {
                            uiState.sections.forEach { section ->
                                item(key = "header_${section.date}") {
                                    MiuixDateHeader(
                                        date = formatDailyDate(section.date),
                                        modifier = Modifier.testTag(dailySectionHeaderTag(section.date)),
                                    )
                                }
                                items(section.stories, key = { "story_${it.id}" }) { story ->
                                    MiuixDailyStoryCard(
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

                            if (uiState.isLoadingMore) {
                                item(key = "loading_more") {
                                    Box(
                                        modifier = Modifier.fillMaxWidth().padding(16.dp),
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
        }
    }
}

@Composable
private fun MiuixDateHeader(date: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(MiuixTheme.colorScheme.onSurface.copy(alpha = 0.2f)),
            )
            Text(
                text = date,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MiuixTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(1.dp)
                    .background(MiuixTheme.colorScheme.onSurface.copy(alpha = 0.2f)),
            )
        }
    }
}

@Composable
private fun MiuixDailyStoryCard(
    story: DailyStory,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (story.images.isNotEmpty()) {
                AsyncImage(
                    model = story.images.first(),
                    contentDescription = story.title,
                    modifier = Modifier.weight(0.3f, fill = true).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop,
                )
            }

            Column(
                modifier = Modifier.weight(0.7f),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = story.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    color = MiuixTheme.colorScheme.onSurface,
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = MiuixIconsEmbedded.Timer,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MiuixTheme.colorScheme.onSurfaceSecondary,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = story.hint,
                        fontSize = 12.sp,
                        color = MiuixTheme.colorScheme.onSurfaceSecondary,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalTime::class)
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
private const val DAILY_SCREEN_DATE_PICKER_BUTTON_TAG = "daily_screen_date_picker_button"
private const val DAILY_SCREEN_LOADING_TAG = "daily_screen_loading"
private const val DAILY_SCREEN_ERROR_TAG = "daily_screen_error"
private const val DAILY_SCREEN_EMPTY_TAG = "daily_screen_empty"
private const val DAILY_SCREEN_LIST_TAG = "daily_screen_list"
