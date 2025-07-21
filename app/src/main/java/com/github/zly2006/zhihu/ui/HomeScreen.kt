package com.github.zly2006.zhihu.ui

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.LoginActivity
import com.github.zly2006.zhihu.MainActivity
import com.github.zly2006.zhihu.NavDestination
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.ui.components.DraggableRefreshButton
import com.github.zly2006.zhihu.ui.components.FeedCard
import com.github.zly2006.zhihu.ui.components.PaginatedList
import com.github.zly2006.zhihu.ui.components.ProgressIndicatorFooter
import com.github.zly2006.zhihu.viewmodel.feed.HomeFeedViewModel
import com.github.zly2006.zhihu.viewmodel.local.LocalHomeFeedViewModel
import com.github.zly2006.zhihu.viewmodel.feed.BaseFeedViewModel
import kotlinx.coroutines.delay

const val PREFERENCE_NAME = "com.github.zly2006.zhihu_preferences"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigate: (NavDestination) -> Unit,
) {
    val context = LocalContext.current as MainActivity
    val onlineViewModel: HomeFeedViewModel by context.viewModels()
    val localViewModel: LocalHomeFeedViewModel by context.viewModels()

    // 本地模式状态
    var isLocalMode by remember { mutableStateOf(false) }

    // 当前使用的ViewModel
    val viewModel: BaseFeedViewModel = if (isLocalMode) localViewModel else onlineViewModel

    val preferences = remember {
        context.getSharedPreferences(PREFERENCE_NAME, MODE_PRIVATE)
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
    LaunchedEffect(isLocalMode) {
        if (!AccountData.data.login) {
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
            FeedCard(item) {
                feed?.let {
                    DataHolder.putFeed(feed)
                }
                if (navDestination != null) {
                    onNavigate(navDestination)
                }
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
                isLocalMode = !isLocalMode
                Toast.makeText(context, if (isLocalMode) "已切换到本地推荐" else "已切换到在线模式", Toast.LENGTH_SHORT).show()
            },
            preferenceName = "toggleMode"
        ) {
            Icon(if (isLocalMode) Icons.Default.CloudOff else Icons.Default.Cloud, contentDescription = "切换模式")
        }
    }
}
