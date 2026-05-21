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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.navigation.CollectionContent
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.shared.data.Collection

data class CollectionScreenData(
    val collections: List<Collection>,
    val isEnd: Boolean,
    val isLoading: Boolean,
    val loadMore: () -> Unit,
)

@Composable
expect fun rememberCollectionScreenData(
    urlToken: String?,
    testCollections: List<Collection>? = null,
): CollectionScreenData

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionScreen(
    urlToken: String?,
    testCollections: List<Collection>? = null,
) {
    val navigator = LocalNavigator.current
    val data = rememberCollectionScreenData(urlToken, testCollections)
    val listState = rememberLazyListState()
    val shouldLoadMore by remember(data.collections.size, data.isEnd, data.isLoading) {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: return@derivedStateOf false
            lastVisibleIndex >= layoutInfo.totalItemsCount - 3 &&
                data.collections.isNotEmpty() &&
                !data.isEnd &&
                !data.isLoading
        }
    }

    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) {
            data.loadMore()
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "我的收藏夹",
                        modifier = Modifier.testTag(COLLECTION_SCREEN_TITLE_TAG),
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = navigator.onNavigateBack,
                        modifier = Modifier.testTag(COLLECTION_SCREEN_BACK_BUTTON_TAG),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .padding(innerPadding)
                .testTag(COLLECTION_SCREEN_LIST_TAG),
        ) {
            items(data.collections, key = { it.id }) { collection ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .testTag(collectionScreenItemTag(collection.id)),
                    elevation = CardDefaults.cardElevation(4.dp),
                    onClick = {
                        navigator.onNavigate(CollectionContent(collection.id))
                    },
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = collection.title, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (data.isEnd) {
                        Text("已经到底啦")
                    } else if (data.isLoading) {
                        CircularProgressIndicator()
                    }
                }
            }
        }
    }
}

private const val COLLECTION_SCREEN_TITLE_TAG = "collection_screen_title"
private const val COLLECTION_SCREEN_BACK_BUTTON_TAG = "collection_screen_back_button"
private const val COLLECTION_SCREEN_LIST_TAG = "collection_screen_list"

private fun collectionScreenItemTag(collectionId: String) = "collection_screen_item_$collectionId"
