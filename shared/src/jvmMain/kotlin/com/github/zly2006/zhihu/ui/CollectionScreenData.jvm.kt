package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable
import com.github.zly2006.zhihu.shared.data.Collection

@Composable
actual fun rememberCollectionScreenData(
    urlToken: String?,
    testCollections: List<Collection>?,
): CollectionScreenData = CollectionScreenData(
    collections = testCollections.orEmpty(),
    isEnd = true,
    isLoading = false,
    loadMore = {},
)
