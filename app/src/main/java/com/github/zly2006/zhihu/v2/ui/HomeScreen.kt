@file:Suppress("FunctionName")

package com.github.zly2006.zhihu.v2.ui

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.zly2006.zhihu.Article
import com.github.zly2006.zhihu.LoginActivity
import com.github.zly2006.zhihu.NavDestination
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.v2.ui.components.FeedCard
import com.github.zly2006.zhihu.v2.ui.components.PaginatedList
import com.github.zly2006.zhihu.v2.ui.components.ProgressIndicatorFooter
import com.github.zly2006.zhihu.v2.viewmodel.HomeFeedViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigate: (NavDestination) -> Unit,
) {
    val viewModel: HomeFeedViewModel = viewModel()
    val context = LocalContext.current
    val listState = rememberLazyListState()
    val preferences = remember {
        context.getSharedPreferences(
            "com.github.zly2006.zhihu_preferences",
            MODE_PRIVATE
        )
    }
    if (!preferences.getBoolean("developer", false)) {
        AlertDialog.Builder(context).apply {
            setTitle("登录失败")
            setMessage("您当前的IP不在校园内，禁止使用！本应用仅供学习使用，使用责任由您自行承担。")
            setPositiveButton("OK") { _, _ ->
            }
        }.create().show()
        return
    }

    // 初始加载
    LaunchedEffect(Unit) {
        val data = AccountData.getData(context)
        if (!data.login) {
            val myIntent = Intent(context, LoginActivity::class.java)
            context.startActivity(myIntent)
        } else if (viewModel.displayItems.isEmpty()) {
            // 只在第一次加载时刷新，这样可以避免在返回时刷新
            viewModel.refresh(context)
        }
    }

    // 显示错误信息
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
                    DataHolder.putFeed(it)
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

                        is Feed.AdvertTarget -> {}
                        is Feed.PinTarget -> {}
                        is Feed.VideoTarget -> {}
                    }
                }
            }
        }
    }
}

