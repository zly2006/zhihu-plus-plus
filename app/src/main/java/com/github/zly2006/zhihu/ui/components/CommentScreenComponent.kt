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
    val dismissEnabled = activeChildComment == null
    val childTarget = activeChildComment?.clickTarget
    val currentContent = childTarget ?: content
    val currentActiveComment = activeChildComment.takeIf { childTarget != null }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    MyModalBottomSheet(
        onDismissRequest = {
            activeChildComment = null
            onDismiss()
        },
        sheetState = sheetState,
        sheetGesturesEnabled = dismissEnabled,
        scrimColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
        properties = ModalBottomSheetProperties(
            shouldDismissOnBackPress = dismissEnabled,
            shouldDismissOnClickOutside = dismissEnabled,
        ),
        dragHandle = {
            Column {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    if (currentContent is CommentHolder) {
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
        },
    ) {
        CommentScreen(
            httpClient = httpClient,
            content = { currentContent },
            activeCommentItem = currentActiveComment,
            topPadding = 0.dp,
            onChildCommentClick = { activeChildComment = it },
        )
    }

    // 返回键处理
    BackHandler(enabled = activeChildComment != null) {
        activeChildComment = null
    }
}
