package com.github.zly2006.zhihu.v2.ui

import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.zly2006.zhihu.Article
import com.github.zly2006.zhihu.NavDestination
import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.data.target
import com.github.zly2006.zhihu.v2.ui.components.FeedCard
import com.github.zly2006.zhihu.v2.ui.components.PaginatedList
import com.github.zly2006.zhihu.v2.ui.components.ProgressIndicatorFooter
import com.github.zly2006.zhihu.v2.viewmodel.FollowViewModel

@Composable
fun FollowScreen(
    onNavigate: (NavDestination) -> Unit
) {
    val viewModel: FollowViewModel = viewModel()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        if (viewModel.displayItems.isEmpty()) {
            viewModel.refresh(context)
        }
    }

    LaunchedEffect(viewModel.errorMessage) {
        viewModel.errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        PaginatedList(
            items = viewModel.displayItems,
            onLoadMore = { viewModel.loadMore(context) },
            footer = ProgressIndicatorFooter
        ) { item ->
            FeedCard(item) { feed ->
                feed?.let {
                    when (val target = it.target) {
                        is Feed.AnswerTarget -> {
                            onNavigate(
                                Article(
                                    target.question.title,
                                    "answer",
                                    target.id,
                                    target.author.name,
                                    target.author.headline,
                                    target.author.avatar_url,
                                    target.excerpt
                                )
                            )
                        }
                        is Feed.ArticleTarget -> {
                            onNavigate(
                                Article(
                                    target.title,
                                    "article",
                                    target.id,
                                    target.author.name,
                                    target.author.headline,
                                    target.author.avatar_url,
                                    target.excerpt
                                )
                            )
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}
