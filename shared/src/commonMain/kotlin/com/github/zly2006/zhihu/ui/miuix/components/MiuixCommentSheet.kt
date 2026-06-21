/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * Licensed under AGPL-3.0-only.
 */

package com.github.zly2006.zhihu.ui.miuix.components

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.shared.viewmodel.CommentItem
import com.github.zly2006.zhihu.ui.commentViewModelKey
import com.github.zly2006.zhihu.ui.miuix.MiuixCommentScreen
import top.yukonga.miuix.kmp.window.WindowBottomSheet

/**
 * 评论弹层的 miuix 版本，对标 M3 的 `CommentScreenComponent`：
 * 根弹层显示评论列表，点击某条评论的「回复」时再叠加一层子评论弹层。
 * 复用 [MiuixCommentScreen] 与全部评论 ViewModel，仅把 `MyModalBottomSheet` 换成 miuix [WindowBottomSheet]。
 */
@Composable
fun MiuixCommentSheet(
    showComments: Boolean,
    onDismiss: () -> Unit,
    content: NavDestination,
) {
    var activeChildComment by remember { mutableStateOf<CommentItem?>(null) }
    val contentStateKey = commentViewModelKey(content)
    var rootListResetToken by rememberSaveable(contentStateKey) { mutableIntStateOf(0) }
    val rootListState = rememberSaveable(contentStateKey, rootListResetToken, saver = LazyListState.Saver) {
        LazyListState()
    }
    val childTarget = activeChildComment?.clickTarget

    WindowBottomSheet(
        show = showComments,
        title = "评论",
        // 评论列表自带 12dp 左右内边距；去掉弹层默认的 24dp 横向 insideMargin，
        // 否则两者叠加 36dp 会让评论挤成中间窄条。
        insideMargin = DpSize(0.dp, 0.dp),
        onDismissRequest = {
            activeChildComment = null
            rootListResetToken += 1
            onDismiss()
        },
    ) {
        MiuixCommentScreen(
            content = { content },
            onChildCommentClick = { activeChildComment = it },
            listState = rootListState,
        )
    }

    WindowBottomSheet(
        show = showComments && activeChildComment != null && childTarget != null,
        title = "回复",
        insideMargin = DpSize(0.dp, 0.dp),
        onDismissRequest = { activeChildComment = null },
    ) {
        if (childTarget != null) {
            MiuixCommentScreen(
                content = { childTarget },
                activeCommentItem = activeChildComment,
                onChildCommentClick = { },
            )
        }
    }
}
