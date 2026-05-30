package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.fleeksoft.ksoup.Ksoup
import com.github.zly2006.zhihu.markdown.RenderMarkdown
import com.github.zly2006.zhihu.navigation.Question
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.platform.rememberSettingsStore
import com.github.zly2006.zhihu.shared.question.QuestionScreenUiState
import com.github.zly2006.zhihu.ui.components.CommentScreenComponent
import com.github.zly2006.zhihu.ui.components.handleShareAction
import com.github.zly2006.zhihu.ui.components.rememberShareDialogRuntime

data class LoadedQuestionScreenData(
    val uiState: QuestionScreenUiState,
    val historyDestination: Question,
)

data class QuestionScreenRuntime(
    val handleShareAction: (Question, () -> Unit) -> Unit,
)

fun questionDetailPreview(html: String): String = Ksoup.parse(html).text().trim()

internal fun loadedQuestionScreenData(
    question: Question,
    questionData: DataHolder.Question,
): LoadedQuestionScreenData {
    val historyDestination = Question(question.questionId, questionData.title)
    return LoadedQuestionScreenData(
        uiState = QuestionScreenUiState(
            questionContent = questionData.detail,
            answerCount = questionData.answerCount,
            visitCount = questionData.visitCount,
            commentCount = questionData.commentCount,
            followerCount = questionData.followerCount,
            title = questionData.title,
            isFollowing = questionData.relationship.isFollowing,
        ),
        historyDestination = historyDestination,
    )
}

@Composable
fun rememberQuestionScreenRuntime(): QuestionScreenRuntime {
    val settings = rememberSettingsStore()
    val shareRuntime = rememberShareDialogRuntime()
    return remember(settings, shareRuntime) {
        QuestionScreenRuntime(
            handleShareAction = { question, onShowDialog ->
                handleShareAction(question, settings, shareRuntime, onShowDialog)
            },
        )
    }
}

@Composable
fun QuestionDetailContent(
    questionId: Long,
    html: String,
) {
    if (rememberSettingsStore().getBoolean(ARTICLE_USE_WEBVIEW_PREFERENCE_KEY, false) &&
        supportsQuestionDetailWebView()
    ) {
        QuestionDetailWebViewContent(
            questionId = questionId,
            html = html,
        )
    } else {
        RenderMarkdown(
            html = html,
            modifier = Modifier.questionSelectionWorkaround(),
            selectable = true,
            enableScroll = false,
        )
    }
}

expect fun supportsQuestionDetailWebView(): Boolean

@Composable
expect fun QuestionDetailWebViewContent(
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
