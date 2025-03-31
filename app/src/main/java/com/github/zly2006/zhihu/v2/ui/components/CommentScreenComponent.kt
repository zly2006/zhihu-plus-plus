package com.github.zly2006.zhihu.v2.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.github.zly2006.zhihu.NavDestination
import com.github.zly2006.zhihu.v2.ui.CommentScreen
import com.github.zly2006.zhihu.v2.viewmodel.CommentItem
import io.ktor.client.*

@Composable
fun CommentScreenComponent(
    showComments: Boolean,
    onDismiss: () -> Unit,
    httpClient: HttpClient,
    content: NavDestination
) {
    var activeChildComment by remember { mutableStateOf<CommentItem?>(null) }
    
    // 主评论区
    AnimatedVisibility(
        visible = showComments && activeChildComment == null,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300)),
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                .clickable { onDismiss() }
        )
    }
    CommentScreen(
        visible = showComments,
        httpClient = httpClient,
        content = content,
        onChildCommentClick = {
            activeChildComment = it
        }
    )
    
    // 子评论区
    AnimatedVisibility(
        visible = showComments && activeChildComment != null,
        enter = fadeIn(animationSpec = tween(300)),
        exit = fadeOut(animationSpec = tween(300)),
    ) {
        // 半透明背景,点击上方区域关闭
        Box(
            modifier = Modifier.fillMaxSize()
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                .clickable { activeChildComment = null }
        )
    }
    CommentScreen(
        visible = activeChildComment != null,
        httpClient = httpClient,
        content = activeChildComment?.clickTarget,
        activeCommentItem = activeChildComment,
        onChildCommentClick = { }
    )

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
