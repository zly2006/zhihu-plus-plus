package com.github.zly2006.zhihu.ui

import android.os.Parcelable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.zly2006.zhihu.Article
import com.github.zly2006.zhihu.ArticleType
import com.github.zly2006.zhihu.MainActivity
import com.github.zly2006.zhihu.NavDestination
import com.github.zly2006.zhihu.Navigator
import com.github.zly2006.zhihu.Person
import com.github.zly2006.zhihu.Pin
import com.github.zly2006.zhihu.Question
import com.github.zly2006.zhihu.viewmodel.ArticleViewModel
import kotlinx.parcelize.Parcelize

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private val ContentListDetailBackBehavior = BackNavigationBehavior.PopUntilContentChange

@Parcelize
data class ContentPaneDestination(
    val type: Type,
    val id: String,
    val articleType: String = "",
    val title: String = "",
    val urlToken: String = "",
    val jumpTo: String = "",
) : Parcelable {
    enum class Type {
        Answer,
        Article,
        Question,
        Pin,
        Person,
    }
}

private fun NavDestination.toContentPaneDestination(): ContentPaneDestination? = when (this) {
    is Article -> ContentPaneDestination(
        type = if (type == ArticleType.Answer) ContentPaneDestination.Type.Answer else ContentPaneDestination.Type.Article,
        id = id.toString(),
        articleType = type.name,
        title = title,
    )

    is Question -> ContentPaneDestination(
        type = ContentPaneDestination.Type.Question,
        id = questionId.toString(),
        title = title,
    )

    is Pin -> ContentPaneDestination(
        type = ContentPaneDestination.Type.Pin,
        id = id.toString(),
    )

    is Person -> ContentPaneDestination(
        type = ContentPaneDestination.Type.Person,
        id = id,
        urlToken = urlToken,
        title = name,
        jumpTo = jumpTo,
    )

    else -> null
}

private fun ContentPaneDestination.toNavDestination(): NavDestination? = when (type) {
    ContentPaneDestination.Type.Answer -> Article(
        type = ArticleType.Answer,
        id = id.toLongOrNull() ?: return null,
        title = title,
    )

    ContentPaneDestination.Type.Article -> Article(
        type = ArticleType.Article,
        id = id.toLongOrNull() ?: return null,
        title = title,
    )

    ContentPaneDestination.Type.Question -> Question(
        questionId = id.toLongOrNull() ?: return null,
        title = title,
    )

    ContentPaneDestination.Type.Pin -> Pin(id = id.toLongOrNull() ?: return null)

    ContentPaneDestination.Type.Person -> Person(
        id = id,
        urlToken = urlToken,
        name = title,
        jumpTo = jumpTo,
    )
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun ContentListDetailScreen(
    innerPadding: PaddingValues,
    onSinglePaneDetailChanged: (Boolean) -> Unit = {},
    listPane: @Composable (Navigator) -> Unit,
) {
    val activity = androidx.activity.compose.LocalActivity.current as MainActivity
    BaseListDetailScreen(
        backBehavior = ContentListDetailBackBehavior,
        toPaneDestination = { it.toContentPaneDestination() },
        emptyPane = {
            ListDetailEmptyPane(
                text = "请选择内容",
                icon = Icons.AutoMirrored.Outlined.Article,
            )
        },
        onSinglePaneDetailChanged = onSinglePaneDetailChanged,
        listPane = {
            listPane(it)
        },
        detailPane = { paneDestination, paneNavigator ->
            val destination = paneDestination.toNavDestination()
            if (destination == null) {
                ListDetailEmptyPane(
                    text = "暂不支持在详情窗格中打开该内容",
                    icon = Icons.AutoMirrored.Outlined.Article,
                )
                return@BaseListDetailScreen
            }
            when (destination) {
                is Article -> {
                    val viewModel: ArticleViewModel = viewModel(key = paneDestination.toString()) {
                        ArticleViewModel(destination, activity.httpClient, null)
                    }
                    ArticleScreen(
                        article = destination,
                        viewModel = viewModel,
                        innerPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding()),
                        paneNavigator = paneNavigator,
                    )
                }

                is Question -> {
                    QuestionScreen(
                        question = destination,
                        innerPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding()),
                    )
                }

                is Pin -> {
                    PinScreen(
                        innerPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding()),
                        pin = destination,
                    )
                }

                is Person -> {
                    PeopleScreen(
                        innerPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding()),
                        person = destination,
                    )
                }

                else -> {
                    ListDetailEmptyPane(
                        text = "暂不支持在详情窗格中打开该内容",
                        icon = Icons.AutoMirrored.Outlined.Article,
                    )
                }
            }
        },
    )
}
