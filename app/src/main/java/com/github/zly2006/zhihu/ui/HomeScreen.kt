package com.github.zly2006.zhihu.ui

import android.content.ClipData
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.BuildConfig
import com.github.zly2006.zhihu.LoginActivity
import com.github.zly2006.zhihu.MainActivity
import com.github.zly2006.zhihu.NavDestination
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.data.RecommendationMode
import com.github.zly2006.zhihu.ui.components.DraggableRefreshButton
import com.github.zly2006.zhihu.ui.components.FeedCard
import com.github.zly2006.zhihu.ui.components.FeedPullToRefresh
import com.github.zly2006.zhihu.ui.components.PaginatedList
import com.github.zly2006.zhihu.ui.components.ProgressIndicatorFooter
import com.github.zly2006.zhihu.viewmodel.feed.BaseFeedViewModel
import com.github.zly2006.zhihu.viewmodel.feed.HomeFeedViewModel
import com.github.zly2006.zhihu.viewmodel.local.LocalHomeFeedViewModel
import com.github.zly2006.zhihu.viewmodel.za.AndroidHomeFeedViewModel
import com.github.zly2006.zhihu.viewmodel.za.MixedHomeFeedViewModel
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlin.getValue

const val PREFERENCE_NAME = "com.github.zly2006.zhihu_preferences"

interface IHomeFeedViewModel {
    suspend fun recordContentInteraction(context: Context, feed: Feed)
}

@OptIn(ExperimentalMaterial3Api::class, DelicateCoroutinesApi::class)
@Composable
fun HomeScreen(
    onNavigate: (NavDestination) -> Unit,
) {
    val context = LocalActivity.current as MainActivity
    val preferences = remember {
        context.getSharedPreferences(PREFERENCE_NAME, MODE_PRIVATE)
    }

    // 获取当前推荐算法设置
    val currentRecommendationMode = remember {
        RecommendationMode.entries.find {
            it.key == preferences.getString("recommendationMode", RecommendationMode.MIXED.key)
        } ?: RecommendationMode.MIXED
    }

    // 根据设置选择对应的ViewModel
    val viewModel: BaseFeedViewModel by when (currentRecommendationMode) {
        RecommendationMode.WEB -> context.viewModels<HomeFeedViewModel>()
        RecommendationMode.ANDROID -> context.viewModels<AndroidHomeFeedViewModel>()
        RecommendationMode.LOCAL -> context.viewModels<LocalHomeFeedViewModel>()
        RecommendationMode.MIXED -> context.viewModels<MixedHomeFeedViewModel>() // 暂时使用在线推荐，因为相似度推荐还未实现
    }

    if (false) {
        // !preferences.getBoolean("developer", false)
        AlertDialog
            .Builder(context)
            .apply {
                setTitle("登录失败")
                setMessage("您当前的IP不在校园内，禁止使用！本应用仅供学习使用，使用责任由您自行承担。")
                setPositiveButton("OK") { _, _ ->
                }
            }.create()
            .show()
        return
    }

    // 初始加载
    LaunchedEffect(currentRecommendationMode) {
        if (!AccountData.data.login &&
            preferences.getBoolean("loginForRecommendation", true)
        ) {
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

    FeedPullToRefresh(viewModel) {
        PaginatedList(
            items = viewModel.displayItems,
            onLoadMore = { viewModel.loadMore(context) },
            footer = ProgressIndicatorFooter,
        ) { item ->
            FeedCard(item, onLike = {
                Toast.makeText(context, "收到喜欢，功能正在优化", Toast.LENGTH_SHORT).show()
            }, onDislike = {
                Toast.makeText(context, "收到反馈，功能正在优化", Toast.LENGTH_SHORT).show()
            }) {
                feed?.let {
                    DataHolder.putFeed(feed)
                    GlobalScope.launch {
                        (viewModel as IHomeFeedViewModel).recordContentInteraction(context, feed)
                    }
                }
                if (navDestination != null) {
                    onNavigate(navDestination)
                }
            }
        }

        if (BuildConfig.DEBUG) {
            DraggableRefreshButton(
                onClick = {
                    val data = Json.encodeToString(viewModel.debugData)
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clip = ClipData.newPlainText("data", data)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "已复制调试数据", Toast.LENGTH_SHORT).show()
                },
                preferenceName = "copyAll",
            ) {
                Icon(Icons.Default.CopyAll, contentDescription = "复制")
            }
        }

        DraggableRefreshButton(
            onClick = {
                viewModel.refresh(context)
            },
        ) {
            if (viewModel.isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(30.dp))
            } else {
                Icon(Icons.Default.Refresh, contentDescription = "刷新")
            }
        }

        DraggableRefreshButton(
            onClick = {
                onNavigate(
                    com.github.zly2006.zhihu
                        .Search(query = ""),
                )
            },
            preferenceName = "searchButton",
        ) {
            Icon(Icons.Default.Search, contentDescription = "搜索")
        }
    }
}
