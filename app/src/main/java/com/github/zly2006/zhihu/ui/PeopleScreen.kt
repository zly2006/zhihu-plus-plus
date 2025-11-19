package com.github.zly2006.zhihu.ui

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.github.zly2006.zhihu.Article
import com.github.zly2006.zhihu.ArticleType
import com.github.zly2006.zhihu.BuildConfig
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
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlin.reflect.typeOf

class PeopleAnswersViewModel(
    val person: Person,
    val sort: String = "voteups",
) : PaginationViewModel<DataHolder.Answer>(
        typeOf<DataHolder.Answer>(),
    ) {
    override val initialUrl: String
        get() = "https://www.zhihu.com/api/v4/members/${person.userTokenOrId}/answers?sort_by=$sort"

    override val include: String
        get() = "data[*].is_normal,admin_closed_comment,reward_info,is_collapsed,annotation_action,annotation_detail,collapse_reason,collapsed_by,suggest_edit,comment_count,thanks_count,can_comment,content,editable_content,attachment,voteup_count,reshipment_settings,comment_permission,created_time,updated_time,review_info,excerpt,paid_info,reaction_instruction,is_labeled,label_info,relationship.is_authorized,voting,is_author,is_thanked,is_nothelp"
}

class PeopleArticlesViewModel(
    val person: Person,
    val sort: String = "created",
) : PaginationViewModel<DataHolder.Article>(
        typeOf<DataHolder.Article>(),
    ) {
    override val initialUrl: String
        get() = "https://www.zhihu.com/api/v4/members/${person.userTokenOrId}/articles?sort_by=$sort"

    override val include: String
        get() = "data[*].comment_count,suggest_edit,is_normal,thumbnail_extra_info,thumbnail,can_comment,comment_permission,admin_closed_comment,content,voteup_count,created,updated,upvoted_followees,voting,review_info,reaction_instruction,is_labeled,label_info;data[*].vessay_info;data[*].author.badge[?(type=best_answerer)].topics;"
}

class PeopleActivitiesViewModel(
    val person: Person,
    val sort: String = "created",
) : BaseFeedViewModel() {
    override val initialUrl: String
        get() = "https://www.zhihu.com/api/v3/moments/${person.userTokenOrId}/activities"
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
    var isFollowing by mutableStateOf(false)
    var isBlocking by mutableStateOf(false)

    // 只实现已有数据类型的 ViewModel
    val answersFeedModel = PeopleAnswersViewModel(person)
    val articlesFeedModel = PeopleArticlesViewModel(person)
    val activitiesFeedModel = PeopleActivitiesViewModel(person)
    val subFeedModels = arrayOf(
        answersFeedModel,
        articlesFeedModel,
        activitiesFeedModel,
    )

    suspend fun toggleFollow(context: Context) {
        context as MainActivity
        val client = context.httpClient
        if (isFollowing) {
            client
                .delete("https://www.zhihu.com/api/v4/members/${person.id}/followers") {
                    signFetchRequest(context)
                }.raiseForStatus()
            isFollowing = false
        } else {
            client
                .post("https://www.zhihu.com/api/v4/members/${person.id}/followers") {
                    signFetchRequest(context)
                }.raiseForStatus()
            isFollowing = true
        }
    }

    suspend fun toggleBlock(context: Context) {
        context as MainActivity
        val client = context.httpClient
        if (isBlocking) {
            // unblock
            val response = client
                .delete("https://www.zhihu.com/api/v4/members/${person.id}/blacks") {
                    signFetchRequest(context)
                }.raiseForStatus()
            Log.d("PersonViewModel", "Unblock response: ${response.bodyAsText()}")
            isBlocking = false
        } else {
            // block
            val response = client
                .post("https://www.zhihu.com/api/v4/members/${person.id}/blacks") {
                    signFetchRequest(context)
                }.raiseForStatus()
            Log.d("PersonViewModel", "Block response: ${response.bodyAsText()}")
            isBlocking = true
        }
    }

    suspend fun load(context: Context) {
        context as MainActivity
        val jojo = context.httpClient
            .get("https://www.zhihu.com/api/v4/members/${person.id}") {
                url {
                    parameters.append(
                        "include",
                        // todo question_count pins_count
                        "allow_message,is_followed,is_following,is_org,is_blocking,answer_count,follower_count,articles_count,question_count,pins_count",
                    )
                }
                signFetchRequest(context)
            }.raiseForStatus()
            .body<JsonObject>()
        val person = AccountData.decodeJson<DataHolder.People>(jojo)
        this.avatar = person.avatarUrl
        this.name = person.name
        this.headline = person.headline
        this.followerCount = person.followerCount
        this.answerCount = person.answerCount
        this.articleCount = person.articlesCount
        this.isFollowing = person.isFollowing
        this.isBlocking = person.isBlocking
        if (person.urlToken != null) {
            this.person.urlToken = person.urlToken
        }
        context.postHistory(
            Person(
                id = person.id,
                name = person.name,
                urlToken = person.urlToken ?: "",
            ),
        )
    }
}

class HttpStatusException(
    val status: HttpStatusCode,
    val requestUrl: Url,
    val bodyText: String,
) : Exception() {
    override val message: String
        get() = "HTTP error: ${status.value} ${status.description} on $requestUrl: \n $bodyText"

    var dumpedCurlRequest: String? = null

    constructor(
        response: HttpResponse,
        dumpRequest: Boolean = false,
    ) : this(
        status = response.status,
        requestUrl = response.request.url,
        bodyText = runBlocking { response.bodyAsText() },
    ) {
        if (dumpRequest) {
            dumpedCurlRequest = dumpCurlRequest(response)
        }
    }
}

fun dumpCurlRequest(response: HttpResponse): String {
    val sb = StringBuilder()
    sb.append("curl -X ${response.request.method.value} '${response.request.url}' ")
    response.request.headers.forEach { key, values ->
        values.forEach { value ->
            sb.append("\\\n  -H '$key: $value' ")
        }
    }
    return sb.toString()
}

suspend fun HttpResponse.raiseForStatus() = apply {
    if (status.value >= 400) {
        throw HttpStatusException(this, dumpRequest = BuildConfig.DEBUG)
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
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
        } catch (e: Exception) {
            Log.e("PeopleScreen", "Error loading person data", e)
            Toast
                .makeText(
                    context,
                    "加载用户信息失败: ${e.message}",
                    Toast.LENGTH_LONG,
                ).show()
        }
    }
    LaunchedEffect(pagerState.currentPage) {
        try {
            viewModel.subFeedModels.getOrNull(pagerState.currentPage)?.loadMore(context)
        } catch (e: Exception) {
            Log.e("PeopleScreen", "Error loading page data", e)
            Toast
                .makeText(
                    context,
                    "加载页面内容失败: ${e.message}",
                    Toast.LENGTH_LONG,
                ).show()
        }
    }

    val headerListStates = List(titles.size) {
    }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    UserInfoHeader(
                        viewModel,
                        modifier = Modifier.fillMaxWidth(),
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors().copy(
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                ),
                scrollBehavior = scrollBehavior,
                expandedHeight = 220.dp,
                windowInsets = WindowInsets(0.dp),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 8.dp),
        ) {
            ScrollableTabRow(
                selectedTabIndex = pagerState.currentPage,
            ) {
                titles.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                    ) {
                        Text(
                            text = title,
                            modifier = Modifier.padding(16.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f),
            ) { page ->
                val pageHeaderState = headerListStates[page]
                when (page) {
                    0 -> {
                        // 回答
                        PaginatedList(
                            items = viewModel.answersFeedModel.allData,
                            onLoadMore = { viewModel.answersFeedModel.loadMore(context) },
                            isEnd = { viewModel.answersFeedModel.isEnd },
                            footer = ProgressIndicatorFooter,
                        ) {
                            FeedCard(
                                BaseFeedViewModel.FeedDisplayItem(
                                    title = it.question.title,
                                    summary = it.excerpt,
                                    details = "回答 · ${it.voteupCount} 赞同 · ${it.commentCount} 评论",
                                    feed = null,
                                ),
                                horizontalPadding = 4.dp,
                            ) {
                                onNavigate(
                                    Article(
                                        type = ArticleType.Answer,
                                        id = it.id,
                                        title = it.question.title,
                                        excerpt = it.excerpt,
                                    ),
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
                        ) {
                            FeedCard(
                                BaseFeedViewModel.FeedDisplayItem(
                                    title = it.title,
                                    summary = it.excerpt,
                                    details = "文章 · ${it.voteupCount} 赞同 · ${it.commentCount} 评论",
                                    feed = null,
                                ),
                                horizontalPadding = 4.dp,
                            ) {
                                onNavigate(
                                    Article(
                                        type = ArticleType.Article,
                                        id = it.id,
                                        title = it.title,
                                        excerpt = it.excerpt,
                                    ),
                                )
                            }
                        }
                    }

                    2 -> {
                        // 动态
                        PaginatedList(
                            items = viewModel.activitiesFeedModel.displayItems,
                            onLoadMore = { viewModel.activitiesFeedModel.loadMore(context) },
                            isEnd = { viewModel.activitiesFeedModel.isEnd },
                            footer = ProgressIndicatorFooter,
                        ) {
                            FeedCard(
                                it,
                                horizontalPadding = 4.dp,
                            ) {
                                it.navDestination?.let(onNavigate)
                            }
                        }
                    }

                    else -> {
                        // 其他页面显示占位符内容
                        Column(
                            modifier = Modifier.padding(16.dp),
                        ) {
                            Text(
                                text = "「${titles[page]}」功能正在开发中...",
                                modifier = Modifier.padding(top = 16.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value.toString(), style = MaterialTheme.typography.titleMedium)
        Text(text = label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun UserInfoHeader(viewModel: PersonViewModel, modifier: Modifier = Modifier) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    Column(
        modifier = modifier.padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AsyncImage(
                model = viewModel.avatar,
                contentDescription = "用户头像",
                modifier = Modifier
                    .padding(end = 16.dp)
                    .size(80.dp)
                    .clip(CircleShape),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(viewModel.name, style = MaterialTheme.typography.titleLarge)
                Text(
                    viewModel.headline,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            StatItem("回答", viewModel.answerCount)
            StatItem("文章", viewModel.articleCount)
            StatItem("关注者", viewModel.followerCount)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            OutlinedButton(onClick = {
                coroutineScope.launch {
                    try {
                        viewModel.toggleFollow(context)
                    } catch (e: Exception) {
                        Toast.makeText(context, "操作失败: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }) {
                Text(if (viewModel.isFollowing) "取消关注" else "关注")
            }
            OutlinedButton(onClick = {
                coroutineScope.launch {
                    try {
                        viewModel.toggleBlock(context)
                    } catch (e: Exception) {
                        Toast.makeText(context, "操作失败: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }) {
                Text(if (viewModel.isBlocking) "取消拉黑" else "拉黑")
            }
        }
    }
}
