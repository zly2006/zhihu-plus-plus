package com.github.zly2006.zhihu.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.zly2006.zhihu.CommentHolder
import com.github.zly2006.zhihu.NavDestination
import com.github.zly2006.zhihu.theme.Typography
import com.github.zly2006.zhihu.ui.CommentScreen
import com.github.zly2006.zhihu.viewmodel.CommentItem
import io.ktor.client.HttpClient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentScreenComponent(
    showComments: Boolean,
    onDismiss: () -> Unit,
    httpClient: HttpClient,
    content: NavDestination,
) {
    if (!showComments) {
        return
    }

    var activeChildComment by remember { mutableStateOf<CommentItem?>(null) }
    val rootSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val childSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val childTarget = activeChildComment?.clickTarget

    @Composable
    fun DragHandleTitle(targetContent: NavDestination) {
        Column {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                if (targetContent is CommentHolder) {
                    "回复"
                } else {
                    "评论"
                },
                style = Typography.bodyMedium.copy(
                    fontWeight = FontWeight.Bold,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp),
                textAlign = TextAlign.Center,
                fontSize = 18.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    if (activeChildComment == null) {
        MyModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = rootSheetState,
            scrimColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            properties = ModalBottomSheetProperties(
                shouldDismissOnBackPress = true,
                shouldDismissOnClickOutside = true,
            ),
            dragHandle = { DragHandleTitle(content) },
        ) {
            CommentScreen(
                httpClient = httpClient,
                content = { content },
                topPadding = 0.dp,
                onChildCommentClick = { activeChildComment = it },
            )
        }
    }

    if (activeChildComment != null && childTarget != null) {
        MyModalBottomSheet(
            onDismissRequest = { activeChildComment = null },
            sheetState = childSheetState,
            scrimColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            properties = ModalBottomSheetProperties(
                shouldDismissOnBackPress = true,
                shouldDismissOnClickOutside = true,
            ),
            dragHandle = { DragHandleTitle(childTarget) },
        ) {
            CommentScreen(
                httpClient = httpClient,
                content = { childTarget },
                activeCommentItem = activeChildComment,
                topPadding = 0.dp,
                onChildCommentClick = { },
            )
        }
    }

    // 返回键处理
    BackHandler(enabled = activeChildComment != null) {
        activeChildComment = null
    }
}
