package com.github.zly2006.zhihu.ui

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.github.zly2006.zhihu.Article
import com.github.zly2006.zhihu.ArticleType
import com.github.zly2006.zhihu.MainActivity
import com.github.zly2006.zhihu.NavDestination
import com.github.zly2006.zhihu.Person
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.signFetchRequest
import com.github.zly2006.zhihu.ui.components.FeedCard
import com.github.zly2006.zhihu.ui.components.PaginatedList
import com.github.zly2006.zhihu.ui.components.ProgressIndicatorFooter
import com.github.zly2006.zhihu.viewmodel.PaginationViewModel
import com.github.zly2006.zhihu.viewmodel.feed.BaseFeedViewModel
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlin.reflect.typeOf

class PeopleAnswersViewModel(val person: Person, val sort: String = "voteups")
    : PaginationViewModel<DataHolder.Answer>(typeOf<DataHolder.Answer>()
) {
    override val initialUrl: String
        get() = "https://www.zhihu.com/api/v4/members/${person.id}/answers?sort_by=$sort"

    override val include: String
        get() = "data[*].is_normal,admin_closed_comment,reward_info,is_collapsed,annotation_action,annotation_detail,collapse_reason,collapsed_by,suggest_edit,comment_count,thanks_count,can_comment,content,editable_content,attachment,voteup_count,reshipment_settings,comment_permission,created_time,updated_time,review_info,excerpt,paid_info,reaction_instruction,is_labeled,label_info,relationship.is_authorized,voting,is_author,is_thanked,is_nothelp"
}

class PeopleArticlesViewModel(val person: Person, val sort: String = "created")
    : PaginationViewModel<DataHolder.Article>(typeOf<DataHolder.Article>()
) {
    override val initialUrl: String
        get() = "https://www.zhihu.com/api/v4/members/${person.id}/articles?sort_by=$sort"

    override val include: String
        get() = "data[*].comment_count,suggest_edit,is_normal,thumbnail_extra_info,thumbnail,can_comment,comment_permission,admin_closed_comment,content,voteup_count,created,updated,upvoted_followees,voting,review_info,is_labeled,label_info"
}

class PersonViewModel(
    val person: Person,
) : ViewModel() {
    var avatar by mutableStateOf("")
    var name by mutableStateOf(person.name)
    var headline by mutableStateOf("")
    var followerCount by mutableIntStateOf(0)
    var answerCount by mutableIntStateOf(0)
    var articleCount by mutableIntStateOf(0)

    // 只实现已有数据类型的 ViewModel
    val answersFeedModel = PeopleAnswersViewModel(person)
    val articlesFeedModel = PeopleArticlesViewModel(person)

    suspend fun load(context: Context) {
        context as MainActivity
        val jojo = context.httpClient.get("https://www.zhihu.com/api/v4/members/${person.id}") {
            url {
                parameters.append(
                    "include",
                    // todo question_count pins_count
                    "allow_message,is_followed,is_following,is_org,is_blocking,answer_count,follower_count,articles_count,question_count,pins_count"
                )
            }
            signFetchRequest(context)
        }.raiseForStatus().body<JsonObject>()
        val person = AccountData.decodeJson<DataHolder.People>(jojo)
        this.avatar = person.avatarUrl
        this.name = person.name
        this.headline = person.headline
        this.followerCount = person.followerCount
        this.answerCount = person.answerCount
        this.articleCount = person.articlesCount
        context.postHistory(
            Person(
                id = person.id,
                name = person.name,
                urlToken = person.urlToken ?: "",
            )
        )
    }
}

suspend fun HttpResponse.raiseForStatus() = apply {
    if (status.value >= 400) {
        throw RuntimeException("HTTP error: ${status.value} ${status.description} on ${request.url}: \n ${bodyAsText()}")
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PeopleScreen(
    person: Person,
    onNavigate: (NavDestination) -> Unit,
) {
    val context = LocalContext.current
    val viewModel = viewModel { PersonViewModel(person) }
    val coroutineScope = rememberCoroutineScope()

    val titles = listOf(
        "回答",
        "文章",
        "动态",
        "收藏",
        "提问",
        "想法",
        "专栏",
    )

    val pagerState = rememberPagerState(pageCount = { titles.size })

    LaunchedEffect(viewModel) {
        try {
            viewModel.load(context)
            // 根据当前页面加载对应数据
            when (pagerState.currentPage) {
                0 -> viewModel.answersFeedModel.loadMore(context)
                1 -> viewModel.articlesFeedModel.loadMore(context)
                // 其他页面暂时不加载数据
            }
        } catch (e: Exception) {
            Log.e("PeopleScreen", "Error loading person data", e)
            Toast.makeText(
                context,
                "加载用户信息失败: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    Column(
        modifier = Modifier.padding(8.dp)
    ) {
        ScrollableTabRow(
            selectedTabIndex = pagerState.currentPage,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            titles.forEachIndexed { index, title ->
                Tab(
                    selected = pagerState.currentPage == index,
                    onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }
                    }
                ) {
                    Text(
                        text = title,
                        modifier = Modifier.padding(16.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f)
        ) { page ->
            when (page) {
                0 -> {
                    // 回答
                    PaginatedList(
                        items = viewModel.answersFeedModel.allData,
                        onLoadMore = { viewModel.answersFeedModel.loadMore(context) },
                        isEnd = { viewModel.answersFeedModel.isEnd },
                        footer = ProgressIndicatorFooter,
                        topContent = {
                            item(0) {
                                UserInfoHeader(viewModel, person)
                            }
                        }
                    ) {
                        FeedCard(
                            BaseFeedViewModel.FeedDisplayItem(
                                title = it.question.title,
                                summary = it.excerpt,
                                details = "回答 · ${it.voteupCount} 赞同 · ${it.commentCount} 评论",
                                feed = null
                            )
                        ) {
                            onNavigate(
                                Article(
                                    type = ArticleType.Answer,
                                    id = it.id,
                                    title = it.question.title,
                                    excerpt = it.excerpt,
                                )
                            )
                        }
                    }
                }
                1 -> {
                    // 文章
                    PaginatedList(
                        items = viewModel.articlesFeedModel.allData,
                        onLoadMore = { viewModel.articlesFeedModel.loadMore(context) },
                        isEnd = { viewModel.articlesFeedModel.isEnd },
                        footer = ProgressIndicatorFooter,
                        topContent = {
                            item(0) {
                                UserInfoHeader(viewModel, person)
                            }
                        }
                    ) {
                        FeedCard(
                            BaseFeedViewModel.FeedDisplayItem(
                                title = it.title,
                                summary = it.excerpt,
                                details = "文章 · ${it.voteupCount} 赞同 · ${it.commentCount} 评论",
                                feed = null
                            )
                        ) {
                            onNavigate(
                                Article(
                                    type = ArticleType.Article,
                                    id = it.id,
                                    title = it.title,
                                    excerpt = it.excerpt,
                                )
                            )
                        }
                    }
                }
                else -> {
                    // 其他页面显示占位符内容，并包含用户信息头部
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        UserInfoHeader(viewModel, person)
                        Text(
                            text = "「${titles[page]}」功能正在开发中...",
                            modifier = Modifier.padding(top = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UserInfoHeader(viewModel: PersonViewModel, person: Person) {
    AsyncImage(
        model = viewModel.avatar,
        contentDescription = "用户头像",
        modifier = Modifier.padding(16.dp)
            .width(128.dp)
            .height(128.dp)
            .clip(CircleShape),
    )
    Text("用户: ${person.name}")
    Text(viewModel.headline)
    Text("关注者： ${viewModel.followerCount}")
    Text("回答数： ${viewModel.answerCount}")
    Text("文章数： ${viewModel.articleCount}")
}
