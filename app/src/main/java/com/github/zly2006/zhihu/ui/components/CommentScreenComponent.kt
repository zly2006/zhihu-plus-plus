package com.github.zly2006.zhihu.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.zly2006.zhihu.NavDestination
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.theme.Typography
import com.github.zly2006.zhihu.ui.CommentScreen
import com.github.zly2006.zhihu.viewmodel.CommentItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommentScreenComponent(
    showComments: Boolean,
    onDismiss: () -> Unit,
    content: NavDestination,
) {
    val context = LocalContext.current
    val httpClient = remember { AccountData.httpClient(context) }
    var activeChildComment by remember { mutableStateOf<CommentItem?>(null) }
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

    if (showComments) {
        MyModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = rootSheetState,
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            properties = ModalBottomSheetProperties(
                shouldDismissOnBackPress = true,
                shouldDismissOnClickOutside = true,
            ),
            dragHandle = { DragHandleTitle("评论") },
        ) {
            CommentScreen(
                httpClient = httpClient,
                content = { content },
                onChildCommentClick = { activeChildComment = it },
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
        ) {
            CommentScreen(
                httpClient = httpClient,
                content = { childTarget },
                activeCommentItem = activeChildComment,
                onChildCommentClick = { },
            )
        }
    }
}
