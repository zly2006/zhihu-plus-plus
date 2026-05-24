package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.zly2006.zhihu.shared.data.Collection
import com.github.zly2006.zhihu.viewmodel.CollectionsViewModel
import com.github.zly2006.zhihu.viewmodel.DesktopPaginationEnvironment

@Composable
actual fun rememberCollectionScreenData(
    urlToken: String?,
    testCollections: List<Collection>?,
): CollectionScreenData {
    val useTestCollections = testCollections != null || urlToken == null
    val viewModel: CollectionsViewModel = viewModel(key = urlToken) {
        CollectionsViewModel(urlToken.orEmpty())
    }
    val environment = remember { DesktopPaginationEnvironment() }

    LaunchedEffect(useTestCollections, viewModel, environment) {
        if (!useTestCollections && viewModel.allData.isEmpty()) {
            viewModel.refresh(environment)
        }
    }

    return CollectionScreenData(
        collections = testCollections ?: viewModel.allData,
        isEnd = useTestCollections || viewModel.isEnd,
        isLoading = viewModel.isLoading,
        loadMore = {
            if (!useTestCollections) {
                viewModel.loadMore(environment)
            }
        },
    )
}
