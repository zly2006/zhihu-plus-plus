package com.github.zly2006.zhihu.ui

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.getContentDetail
import com.github.zly2006.zhihu.markdown.RenderMarkdown
import com.github.zly2006.zhihu.navigation.Question
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.platform.rememberSettingsStore
import com.github.zly2006.zhihu.shared.question.QuestionScreenUiState
import com.github.zly2006.zhihu.ui.components.CommentScreenComponent
import com.github.zly2006.zhihu.ui.components.ShareDialog
import com.github.zly2006.zhihu.ui.components.WebviewComp
import com.github.zly2006.zhihu.ui.components.handleShareAction
import com.github.zly2006.zhihu.util.fuckHonorService
import com.github.zly2006.zhihu.viewmodel.filter.ContentOpenEventSupport
import com.github.zly2006.zhihu.viewmodel.filter.ContentOpenFrom
import org.jsoup.Jsoup

private const val WEBVIEW_ACTIVITY_CLASS = "com.github.zly2006.zhihu.WebviewActivity"

@Composable
actual fun rememberQuestionScreenRuntime(): QuestionScreenRuntime {
    val context = LocalContext.current
    return remember(context) {
        QuestionScreenRuntime(
            loadQuestion = { question ->
                AccountData.addReadHistory(context, question.questionId.toString(), "question")
                val questionData = DataHolder.getContentDetail(context, question)
                if (questionData != null) {
                    val historyDestination = Question(question.questionId, questionData.title)
                    val articleHost = context.articleHost()
                    articleHost?.postHistoryDestination(historyDestination)
                    ContentOpenEventSupport.recordOpenEvent(
                        context = context,
                        destination = question,
                        questionId = question.questionId,
                        openFrom = articleHost?.consumePendingContentOpenFrom(question) ?: ContentOpenFrom.UNKNOWN,
                    )
                    LoadedQuestionScreenData(
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
                } else {
                    null
                }
            },
            openLog = { question ->
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = "https://www.zhihu.com/question/${question.questionId}/log".toUri()
                    setClassName(context, WEBVIEW_ACTIVITY_CLASS)
                }
                context.startActivity(intent)
            },
            handleShareAction = { question, onShowDialog ->
                handleShareAction(context, question, onShowDialog)
            },
            showShortMessage = { message ->
                android.widget.Toast
                    .makeText(context, message, android.widget.Toast.LENGTH_SHORT)
                    .show()
            },
        )
    }
}

@Composable
actual fun QuestionDetailContent(
    questionId: Long,
    html: String,
) {
    if (rememberSettingsStore().getBoolean(ARTICLE_USE_WEBVIEW_PREFERENCE_KEY, false)) {
        WebviewComp {
            it.loadZhihu(
                "https://www.zhihu.com/question/$questionId",
                Jsoup.parse(html),
            )
        }
    } else {
        RenderMarkdown(
            html = html,
            modifier = Modifier.fuckHonorService(),
            selectable = true,
            enableScroll = false,
        )
    }
}

@Composable
actual fun QuestionCommentsSheet(
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
actual fun QuestionShareDialog(
    content: Question,
    shareText: String,
    showDialog: Boolean,
    onDismissRequest: () -> Unit,
) {
    ShareDialog(
        content = content,
        shareText = shareText,
        showDialog = showDialog,
        onDismissRequest = onDismissRequest,
        context = LocalContext.current,
    )
}
