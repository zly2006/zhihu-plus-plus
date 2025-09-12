package com.github.zly2006.zhihu.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.NavDestination
import com.github.zly2006.zhihu.ui.CommentScreen
import com.github.zly2006.zhihu.viewmodel.CommentItem
import io.ktor.client.HttpClient

@Composable
fun CommentScreenComponent(
    showComments: Boolean,
    onDismiss: () -> Unit,
    onNavigate: (NavDestination) -> Unit,
    httpClient: HttpClient,
    content: NavDestination,
) {
    var activeChildComment by remember { mutableStateOf<CommentItem?>(null) }
    val commentTopPadding =
        if (LocalConfiguration.current.screenHeightDp > 500) {
            100.dp
        } else {
            0.dp
        }
    val commentPaddingPixels = LocalDensity.current.run {
        commentTopPadding.toPx()
    }

    // 主评论区
    AnimatedVisibility(
        visible = showComments && activeChildComment == null,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        if (offset.y < commentPaddingPixels) {
                            onDismiss()
                        }
                    }
                },
        )
    }

    AnimatedVisibility(
        visible = showComments,
        enter = slideInVertically(
            animationSpec = tween(300),
        ) { it },
        exit = slideOutVertically(
            animationSpec = tween(300),
        ) { it },
    ) {
        CommentScreen(
            httpClient = httpClient,
            content = { content },
            topPadding = commentTopPadding,
            onNavigate = onNavigate,
            onChildCommentClick = {
                activeChildComment = it
            },
        )
    }

    // 子评论区
    AnimatedVisibility(
        visible = showComments && activeChildComment != null,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300)),
    ) {
        // 半透明背景,点击上方区域关闭
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        if (offset.y < commentPaddingPixels) {
                            activeChildComment = null
                        }
                    }
                },
        )
    }

    AnimatedVisibility(
        visible = activeChildComment != null,
        enter = slideInVertically(
            animationSpec = tween(300),
        ) { it },
        exit = slideOutVertically(
            animationSpec = tween(300),
        ) { it },
    ) {
        var notNullActiveChildComment by remember { mutableStateOf(activeChildComment) }
        LaunchedEffect(activeChildComment) {
            if (activeChildComment != null) {
                notNullActiveChildComment = activeChildComment
            }
        }
        CommentScreen(
            httpClient = httpClient,
            topPadding = commentTopPadding,
            content = { notNullActiveChildComment!!.clickTarget!! },
            activeCommentItem = notNullActiveChildComment,
            onNavigate = onNavigate,
            onChildCommentClick = { },
        )
    }

    // 返回键处理
    BackHandler(
        enabled = activeChildComment != null,
    ) {
        activeChildComment = null
    }

    BackHandler(
        enabled = showComments && activeChildComment == null,
    ) {
        onDismiss()
    }
}
