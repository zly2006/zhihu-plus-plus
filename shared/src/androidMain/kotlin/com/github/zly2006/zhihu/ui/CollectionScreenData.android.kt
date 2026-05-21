package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.zly2006.zhihu.shared.data.Collection
import com.github.zly2006.zhihu.viewmodel.CollectionsViewModel

@Composable
actual fun rememberCollectionScreenData(
    urlToken: String?,
    testCollections: List<Collection>?,
): CollectionScreenData {
    val context = LocalContext.current
    val useTestCollections = testCollections != null || urlToken == null
    val viewModel = viewModel(urlToken) {
        CollectionsViewModel(urlToken.orEmpty())
    }

    LaunchedEffect(useTestCollections, viewModel) {
        if (!useTestCollections && viewModel.allData.isEmpty()) {
            viewModel.refresh(context)
        }
    }

    return CollectionScreenData(
        collections = testCollections ?: viewModel.allData,
        isEnd = useTestCollections || viewModel.isEnd,
        isLoading = viewModel.isLoading,
        loadMore = {
            if (!useTestCollections) {
                viewModel.loadMore(context)
            }
        },
    )
}
