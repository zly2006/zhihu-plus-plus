package com.github.zly2006.zhihu.ui

import android.os.Parcelable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation.BackNavigationBehavior
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.zly2006.zhihu.Article
import com.github.zly2006.zhihu.ArticleType
import com.github.zly2006.zhihu.LocalNavigator
import com.github.zly2006.zhihu.MainActivity
import com.github.zly2006.zhihu.NavDestination
import com.github.zly2006.zhihu.Navigator
import com.github.zly2006.zhihu.Person
import com.github.zly2006.zhihu.Pin
import com.github.zly2006.zhihu.Question
import com.github.zly2006.zhihu.viewmodel.ArticleViewModel
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
private val ContentListDetailBackBehavior = BackNavigationBehavior.PopUntilContentChange
private val ContentListDetailPaneSpacer = 0.dp

@Composable
private fun EmptyDetailPane(
    text: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.Article,
            contentDescription = null,
            modifier = Modifier.size(40.dp).padding(bottom = 8.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Text(text = text, color = Color.Gray)
    }
}

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
    listPane: @Composable (Navigator) -> Unit,
) {
    val activity = androidx.activity.compose.LocalActivity.current as MainActivity
    val scaffoldDirective = calculatePaneScaffoldDirective(currentWindowAdaptiveInfo()).copy(
        horizontalPartitionSpacerSize = ContentListDetailPaneSpacer,
    )
    val paneNavigator = rememberListDetailPaneScaffoldNavigator<ContentPaneDestination>(
        scaffoldDirective = scaffoldDirective,
    )
    val coroutineScope = rememberCoroutineScope()
    val detailDestination = paneNavigator.currentDestination?.contentKey
    val rootNavigator = LocalNavigator.current

    val localNavigator = Navigator(
        onNavigate = { destination ->
            val paneDestination = destination.toContentPaneDestination()
            if (paneDestination != null) {
                coroutineScope.launch {
                    paneNavigator.navigateTo(ListDetailPaneScaffoldRole.Detail, paneDestination)
                }
            } else {
                rootNavigator.onNavigate(destination)
            }
        },
        onNavigateBack = {
            coroutineScope.launch {
                if (paneNavigator.canNavigateBack(ContentListDetailBackBehavior)) {
                    paneNavigator.navigateBack(ContentListDetailBackBehavior)
                } else {
                    rootNavigator.onNavigateBack()
                }
            }
        },
    )

    NavigableListDetailPaneScaffold(
        navigator = paneNavigator,
        defaultBackBehavior = ContentListDetailBackBehavior,
        listPane = {
            AnimatedPane {
                CompositionLocalProvider(LocalNavigator provides localNavigator) {
                    listPane(localNavigator)
                }
            }
        },
        detailPane = {
            AnimatedPane {
                val destination = detailDestination?.toNavDestination()
                if (destination == null) {
                    EmptyDetailPane(text = "请选择内容")
                    return@AnimatedPane
                }
                CompositionLocalProvider(LocalNavigator provides localNavigator) {
                    when (destination) {
                        is Article -> {
                            val viewModel: ArticleViewModel = viewModel(key = detailDestination.toString()) {
                                ArticleViewModel(destination, activity.httpClient, null)
                            }
                            ArticleScreen(
                                article = destination,
                                viewModel = viewModel,
                                innerPadding = PaddingValues(bottom = innerPadding.calculateBottomPadding()),
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
                            EmptyDetailPane(text = "暂不支持在详情窗格中打开该内容")
                        }
                    }
                }
            }
        },
    )
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun HomeListDetailScreen(
    scrollToTopTrigger: Int = 0,
    innerPadding: PaddingValues,
) {
    ContentListDetailScreen(
        innerPadding = innerPadding,
    ) { navigator ->
        HomeScreen(
            scrollToTopTrigger = scrollToTopTrigger,
            innerPadding = innerPadding,
            onContentNavigate = navigator.onNavigate,
        )
    }
}
