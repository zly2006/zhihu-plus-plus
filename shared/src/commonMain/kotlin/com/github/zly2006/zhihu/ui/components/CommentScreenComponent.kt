/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.github.zly2006.zhihu.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.CommentHolder
import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.navigation.Pin
import com.github.zly2006.zhihu.navigation.Question
import com.github.zly2006.zhihu.navigation.SegmentCommentHolder
import com.github.zly2006.zhihu.shared.viewmodel.CommentItem
import com.github.zly2006.zhihu.theme.Typography
import com.github.zly2006.zhihu.ui.CommentScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentScreenComponent(
    showComments: Boolean,
    onDismiss: () -> Unit,
    content: NavDestination,
) {
    var activeChildComment by remember { mutableStateOf<CommentItem?>(null) }
    val contentStateKey = commentContentStateKey(content)
    var rootListResetToken by rememberSaveable(contentStateKey) { mutableIntStateOf(0) }
    val rootListState = rememberSaveable(
        contentStateKey,
        rootListResetToken,
        saver = LazyListState.Saver,
    ) {
        LazyListState()
    }
    val rootSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val childSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val childTarget = activeChildComment?.clickTarget

    @Composable
    fun DragHandleTitle(text: String) {
        Column {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text,
                style = Typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                ),
                textAlign = TextAlign.Center,
                fontSize = 18.sp,
                lineHeight = 26.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    fun dismissRootComments() {
        activeChildComment = null
        rootListResetToken += 1
        onDismiss()
    }

    if (showComments) {
        MyModalBottomSheet(
            onDismissRequest = { dismissRootComments() },
            sheetState = rootSheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            properties = ModalBottomSheetProperties(
                shouldDismissOnBackPress = true,
                shouldDismissOnClickOutside = true,
            ),
            dragHandle = { DragHandleTitle("评论") },
            usePlatformWindow = false,
        ) {
            CommentScreen(
                content = { content },
                onChildCommentClick = { activeChildComment = it },
                listState = rootListState,
            )
        }
    }

    if (showComments && activeChildComment != null && childTarget != null) {
        MyModalBottomSheet(
            onDismissRequest = { activeChildComment = null },
            sheetState = childSheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            properties = ModalBottomSheetProperties(
                shouldDismissOnBackPress = true,
                shouldDismissOnClickOutside = true,
            ),
            dragHandle = { DragHandleTitle("回复") },
            usePlatformWindow = false,
        ) {
            CommentScreen(
                content = { childTarget },
                activeCommentItem = activeChildComment,
                onChildCommentClick = { },
            )
        }
    }
}

private fun commentContentStateKey(content: NavDestination): String = when (content) {
    is Article -> "article:${content.type}:${content.id}"
    is Question -> "question:${content.questionId}"
    is Pin -> "pin:${content.id}"
    is CommentHolder -> "comment:${commentContentStateKey(content.article)}:${content.commentId}"
    is SegmentCommentHolder -> "segment:${content.contentType}:${content.contentId}:${content.segmentId}"
    else -> content.toString()
}
