/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * Licensed under AGPL-3.0-only.
 */

package com.github.zly2006.zhihu.ui.miuix.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import com.github.zly2006.zhihu.ui.loadSearchHistory
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

private data class MiuixHotQuery(val query: String, val hotShow: String)

/**
 * miuix 首页搜索默认页：搜索历史（chip）+ 热搜榜。数据层与 M3 SearchScreen 互通
 * （历史 key [com.github.zly2006.zhihu.ui.SEARCH_HISTORY_KEY]，热搜走 /api/v4/search/hot_search）。
 * [onQueryClick] 点击某条建议时回调（由调用方写入 searchText 触发搜索）。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MiuixSearchSuggestions(
    onQueryClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE) }
    val showHistory = remember { preferences.getBoolean("showSearchHistory", true) }
    val showHotSearch = remember { preferences.getBoolean("showSearchHotSearch", true) }
    val historyItems = remember { if (showHistory) loadSearchHistory(preferences) else emptyList() }
    val hotItems = remember { mutableStateListOf<MiuixHotQuery>() }

    LaunchedEffect(showHotSearch) {
        if (!showHotSearch) return@LaunchedEffect
        runCatching {
            val json = AccountData.fetchGet(context, "https://www.zhihu.com/api/v4/search/hot_search") ?: return@runCatching
            val queries = json["hot_search_queries"] as? JsonArray ?: return@runCatching
            hotItems.clear()
            queries.take(15).forEach { item ->
                val obj = item.jsonObject
                val query = obj["query"]?.jsonPrimitive?.contentOrNull ?: return@forEach
                hotItems.add(MiuixHotQuery(query, obj["hot_show"]?.jsonPrimitive?.contentOrNull.orEmpty()))
            }
        }
    }

    val hasHistory = historyItems.isNotEmpty()
    val hasHot = hotItems.isNotEmpty()
    if (!hasHistory && !hasHot) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("输入关键词搜索", color = MiuixTheme.colorScheme.onSurfaceVariantSummary)
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    ) {
        if (hasHistory) {
            item { SmallTitle(text = "搜索历史") }
            item {
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    historyItems.forEach { query ->
                        Text(
                            text = query,
                            color = MiuixTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(MiuixTheme.colorScheme.secondaryContainer)
                                .clickable { onQueryClick(query) }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                        )
                    }
                }
            }
        }

        if (hasHot) {
            item { SmallTitle(text = "热搜") }
            itemsIndexed(hotItems) { index, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onQueryClick(item.query) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${index + 1}",
                        color = if (index < 3) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.onSurfaceVariantSummary,
                        modifier = Modifier.width(28.dp),
                    )
                    Text(
                        text = item.query,
                        color = MiuixTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                    )
                    if (item.hotShow.isNotEmpty()) {
                        Text(
                            text = item.hotShow,
                            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
                            style = MiuixTheme.textStyles.footnote1,
                        )
                    }
                }
            }
        }
    }
}
