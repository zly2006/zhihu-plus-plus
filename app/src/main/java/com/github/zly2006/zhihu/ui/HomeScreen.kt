package com.github.zly2006.zhihu.ui

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CopyAll
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

const val PREFERENCE_NAME = "com.github.zly2006.zhihu_preferences"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigate: (NavDestination) -> Unit,
) {
    val context = LocalContext.current as MainActivity
    val viewModel: HomeFeedViewModel by context.viewModels()
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
    LaunchedEffect(Unit) {
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
                val data = Json.encodeToString(viewModel.debugData)
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText(
                    "data",
                    data
                )
                clipboard.setPrimaryClip(clip)
                Toast.makeText(context, "已复制调试数据", Toast.LENGTH_SHORT).show()
            },
            preferenceName = "copyAll"
        ) {
            Icon(Icons.Default.CopyAll, contentDescription = "复制")
        }
    }
}

