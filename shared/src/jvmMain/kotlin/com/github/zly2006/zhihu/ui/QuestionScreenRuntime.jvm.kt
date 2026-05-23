package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable
import com.github.zly2006.zhihu.markdown.RenderMarkdown
import com.github.zly2006.zhihu.navigation.Question

@Composable
actual fun rememberQuestionScreenRuntime(): QuestionScreenRuntime = QuestionScreenRuntime(
    loadQuestion = { null },
    openLog = {},
    handleShareAction = { _, onShowDialog -> onShowDialog() },
    showShortMessage = {},
)

@Composable
actual fun QuestionDetailContent(
    questionId: Long,
    html: String,
) {
    RenderMarkdown(html)
}

@Composable
actual fun QuestionCommentsSheet(
    showComments: Boolean,
    onDismiss: () -> Unit,
    content: Question,
) {
}

@Composable
actual fun QuestionShareDialog(
    content: Question,
    shareText: String,
    showDialog: Boolean,
    onDismissRequest: () -> Unit,
) {
}
