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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.shared.data.FeedDisplayItem
import com.github.zly2006.zhihu.shared.data.toFeedDisplayItemNavDestinationJson
import com.github.zly2006.zhihu.shared.platform.PlatformBackHandler
import com.github.zly2006.zhihu.shared.platform.SettingsStore
import com.github.zly2006.zhihu.shared.platform.rememberSettingsStore
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.ui.components.FeedCard
import com.github.zly2006.zhihu.viewmodel.formatArticleDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import kotlin.time.Clock

const val READ_LATER_LIST_TAG = "readLater.list"
const val READ_LATER_EMPTY_TAG = "readLater.empty"
const val READ_LATER_OVERFLOW_TAG = "readLater.overflow"
const val READ_LATER_ITEM_TAG_PREFIX = "readLater.item"

private const val READ_LATER_ITEMS_KEY = "readLaterItems"
private const val MAX_READ_LATER_ITEMS = 500

private val readLaterJson = Json {
    ignoreUnknownKeys = true
}

@Serializable
data class ReadLaterItem(
    val article: Article,
    val savedAtEpochMillis: Long,
)

class ReadLaterStore(
    private val settings: SettingsStore,
) {
    var items by mutableStateOf(loadItems())
        private set

    fun contains(article: Article): Boolean = items.any { it.article == article }

    fun add(article: Article) {
        save(
            listOf(ReadLaterItem(article, Clock.System.now().toEpochMilliseconds())) +
                items.filterNot { it.article == article },
        )
    }

    fun remove(article: Article) {
        save(items.filterNot { it.article == article })
    }

    fun clear() {
        save(emptyList())
    }

    fun reload() {
        items = loadItems()
    }

    private fun save(nextItems: List<ReadLaterItem>) {
        items = nextItems.take(MAX_READ_LATER_ITEMS)
        if (items.isEmpty()) {
            settings.remove(READ_LATER_ITEMS_KEY)
        } else {
            settings.putString(
                READ_LATER_ITEMS_KEY,
                readLaterJson.encodeToString(ListSerializer(ReadLaterItem.serializer()), items),
            )
        }
    }

    private fun loadItems(): List<ReadLaterItem> =
        settings
            .getStringOrNull(READ_LATER_ITEMS_KEY)
            ?.let { raw ->
                runCatching {
                    readLaterJson.decodeFromString(ListSerializer(ReadLaterItem.serializer()), raw)
                }.getOrNull()
            }.orEmpty()
            .take(MAX_READ_LATER_ITEMS)
}

@Composable
fun rememberReadLaterStore(): ReadLaterStore {
    val settings = rememberSettingsStore()
    val store = remember(settings) { ReadLaterStore(settings) }
    DisposableEffect(settings) {
        val unregister = settings.observeKeyChanges { key ->
            if (key == READ_LATER_ITEMS_KEY) {
                store.reload()
            }
        }
        onDispose(unregister)
    }
    return store
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadLaterScreen() {
    val navigator = LocalNavigator.current
    val store = rememberReadLaterStore()
    val userMessages = rememberUserMessageSink()
    var showActionsMenu by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }

    PlatformBackHandler(enabled = showActionsMenu) {
        showActionsMenu = false
    }
    PlatformBackHandler(enabled = showClearDialog) {
        showClearDialog = false
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("稍后再看") },
                navigationIcon = {
                    IconButton(onClick = navigator.onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    Box {
                        IconButton(
                            onClick = { showActionsMenu = true },
                            modifier = Modifier.testTag(READ_LATER_OVERFLOW_TAG),
                        ) {
                            Icon(Icons.Filled.MoreVert, contentDescription = "更多选项")
                        }
                        DropdownMenu(
                            expanded = showActionsMenu,
                            onDismissRequest = { showActionsMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("清空稍后再看") },
                                leadingIcon = { Icon(Icons.Filled.DeleteSweep, contentDescription = null) },
                                enabled = store.items.isNotEmpty(),
                                onClick = {
                                    showActionsMenu = false
                                    showClearDialog = true
                                },
                            )
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        if (showClearDialog) {
            AlertDialog(
                onDismissRequest = { showClearDialog = false },
                title = { Text("清空稍后再看") },
                text = { Text("所有本地保存的稍后再看条目都会被移除。") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            showClearDialog = false
                            store.clear()
                            userMessages.showShortMessage("已清空稍后再看")
                        },
                    ) {
                        Text("清空")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showClearDialog = false }) {
                        Text("取消")
                    }
                },
            )
        }
        if (store.items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .testTag(READ_LATER_EMPTY_TAG),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "还没有保存稍后再看的内容",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .testTag(READ_LATER_LIST_TAG),
            ) {
                items(
                    items = store.items,
                    key = { "${it.article.type}:${it.article.id}" },
                ) { item ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .testTag("${READ_LATER_ITEM_TAG_PREFIX}.${item.article.type}.${item.article.id}"),
                    ) {
                        FeedCard(
                            item = FeedDisplayItem(
                                title = item.article.title,
                                summary = item.article.excerpt.orEmpty(),
                                details = "保存于 ${formatArticleDateTime(item.savedAtEpochMillis / 1000)}",
                                feed = null,
                                navDestinationJson = item.article.toFeedDisplayItemNavDestinationJson(),
                                avatarSrc = item.article.avatarSrc,
                                authorName = item.article.authorName,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}
