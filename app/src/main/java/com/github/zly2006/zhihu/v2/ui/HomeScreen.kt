package com.github.zly2006.zhihu.v2.ui

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
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
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigate: (NavDestination) -> Unit,
) {
    val viewModel: HomeFeedViewModel = viewModel()
    val context = LocalContext.current
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
                        null -> {}
                    }
                }
            }
        }

        var offsetX by remember { mutableStateOf(preferences.getFloat("homeRefresh_x", 0f)) }
        var offsetY by remember { mutableStateOf(preferences.getFloat("homeRefresh_y", 0f)) }
        var pressing by remember { mutableStateOf(false) }

        val configuration = LocalConfiguration.current
        val density = LocalDensity.current
        val screenWidth = with(density) { configuration.screenWidthDp.dp.toPx() }

        val animatedOffsetX by animateFloatAsState(
            targetValue = offsetX,
            animationSpec = tween(if (pressing) 1 else 300), // 如果按下，立即完成动画
            label = "offsetX"
        )
        val animatedOffsetY by animateFloatAsState(
            targetValue = offsetY,
            animationSpec = tween(if (pressing) 1 else 300),
            label = "offsetY"
        )

        FloatingActionButton(
            onClick = { viewModel.refresh(context) },
            shape = CircleShape,
            modifier = Modifier
                .offset { IntOffset(animatedOffsetX.roundToInt(), animatedOffsetY.roundToInt()) }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            pressing = true
                            // 震动反馈
                            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
                            } else {
                                @Suppress("DEPRECATION")
                                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                vibrator.vibrate(
                                    VibrationEffect.createOneShot(
                                        50,
                                        VibrationEffect.DEFAULT_AMPLITUDE
                                    )
                                )
                            } else {
                                @Suppress("DEPRECATION")
                                vibrator.vibrate(50)
                            }
                        },
                        onDragEnd = {
                            pressing = false
                            offsetX =
                                if (offsetX < screenWidth / 2) 0f
                                else screenWidth - 150
                            preferences.edit()
                                .putFloat("homeRefresh_x", offsetX)
                                .putFloat("homeRefresh_y", offsetY)
                                .apply()
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()
                            if (pressing) {
                                offsetX = (offsetX + dragAmount.x).coerceIn(0f, screenWidth - 150)
                                offsetY = (offsetY + dragAmount.y).coerceIn(
                                    0f,
                                    configuration.screenHeightDp * density.density - 150
                                )
                            }
                        }
                    )
                }
        ) {
            Icon(Icons.Default.Refresh, contentDescription = "刷新")
        }
    }
}

