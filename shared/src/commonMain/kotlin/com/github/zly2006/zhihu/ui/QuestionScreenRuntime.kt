package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable
import com.fleeksoft.ksoup.Ksoup
import com.github.zly2006.zhihu.navigation.Question
import com.github.zly2006.zhihu.shared.question.QuestionScreenUiState
import com.github.zly2006.zhihu.ui.components.CommentScreenComponent

data class LoadedQuestionScreenData(
    val uiState: QuestionScreenUiState,
    val historyDestination: Question,
)

data class QuestionScreenRuntime(
    val loadQuestion: suspend (Question) -> LoadedQuestionScreenData?,
    val openLog: (Question) -> Unit,
    val handleShareAction: (Question, () -> Unit) -> Unit,
    val showShortMessage: (String) -> Unit,
)

fun questionDetailPreview(html: String): String = Ksoup.parse(html).text().trim()

@Composable
expect fun rememberQuestionScreenRuntime(): QuestionScreenRuntime

@Composable
expect fun QuestionDetailContent(
    questionId: Long,
    html: String,
)

@Composable
fun QuestionCommentsSheet(
    showComments: Boolean,
    onDismiss: () -> Unit,
    content: Question,
) {
    CommentScreenComponent(
        showComments = showComments,
        onDismiss = onDismiss,
        content = content,
    )
}

@Composable
expect fun QuestionShareDialog(
    content: Question,
    shareText: String,
    showDialog: Boolean,
    onDismissRequest: () -> Unit,
)
