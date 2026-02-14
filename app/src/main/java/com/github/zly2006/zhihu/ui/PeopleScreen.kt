package com.github.zly2006.zhihu.ui

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import com.github.zly2006.zhihu.LocalNavigator
import com.github.zly2006.zhihu.MainActivity
import com.github.zly2006.zhihu.NavDestination
import com.github.zly2006.zhihu.Person
import com.github.zly2006.zhihu.Pin
import com.github.zly2006.zhihu.Question
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.ui.components.FeedCard
import com.github.zly2006.zhihu.ui.components.PaginatedList
import com.github.zly2006.zhihu.ui.components.ProgressIndicatorFooter
import com.github.zly2006.zhihu.util.signFetchRequest
import com.github.zly2006.zhihu.viewmodel.PaginationViewModel
import com.github.zly2006.zhihu.viewmodel.feed.BaseFeedViewModel
import com.github.zly2006.zhihu.viewmodel.filter.BlocklistManager
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.request
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import org.jsoup.Jsoup
import kotlin.reflect.typeOf

class PeopleAnswersViewModel(
    val person: Person,
) : PaginationViewModel<DataHolder.Answer>(
        typeOf<DataHolder.Answer>(),
    ) {
    var sortBy by mutableStateOf("voteups")
        private set

    override val initialUrl: String
        get() = "https://www.zhihu.com/api/v4/members/${person.userTokenOrId}/answers?sort_by=$sortBy"

    override val include: String
        get() = "data[*].is_normal,admin_closed_comment,reward_info,is_collapsed,annotation_action,annotation_detail,collapse_reason,collapsed_by,suggest_edit,comment_count,thanks_count,can_comment,content,editable_content,attachment,voteup_count,reshipment_settings,comment_permission,created_time,updated_time,review_info,excerpt,paid_info,reaction_instruction,is_labeled,label_info,relationship.is_authorized,voting,is_author,is_thanked,is_nothelp"

    fun changeSortBy(newSort: String, context: Context) {
        if (sortBy != newSort) {
            sortBy = newSort
            refresh(context)
        }
    }
}

class PeopleArticlesViewModel(
    val person: Person,
) : PaginationViewModel<DataHolder.Article>(
        typeOf<DataHolder.Article>(),
    ) {
    var sortBy by mutableStateOf("created")
        private set

    override val initialUrl: String
        get() = "https://www.zhihu.com/api/v4/members/${person.userTokenOrId}/articles?sort_by=$sortBy"

    override val include: String
        get() = "data[*].comment_count,suggest_edit,is_normal,thumbnail_extra_info,thumbnail,can_comment,comment_permission,admin_closed_comment,content,voteup_count,created,updated,upvoted_followees,voting,review_info,reaction_instruction,is_labeled,label_info;data[*].vessay_info;data[*].author.badge[?(type=best_answerer)].topics;"

    fun changeSortBy(newSort: String, context: Context) {
        if (sortBy != newSort) {
            sortBy = newSort
            refresh(context)
        }
    }
}

class PeopleActivitiesViewModel(
    val person: Person,
    val sort: String = "created",
) : BaseFeedViewModel() {
    override val initialUrl: String
        get() = "https://www.zhihu.com/api/v3/moments/${person.userTokenOrId}/activities"
}

class PeopleFollowersViewModel(
    val person: Person,
) : PaginationViewModel<DataHolder.People>(
        typeOf<DataHolder.People>(),
    ) {
    override val initialUrl: String
        get() = "https://www.zhihu.com/api/v4/members/${person.userTokenOrId}/followers"

    override val include: String
        get() = "data[*].answer_count,articles_count,gender,follower_count,is_followed,is_following,badge[?(type=best_answerer)].topics"
}

class PeopleFollowingViewModel(
    val person: Person,
) : PaginationViewModel<DataHolder.People>(
        typeOf<DataHolder.People>(),
    ) {
    override val initialUrl: String
        get() = "https://www.zhihu.com/api/v4/members/${person.userTokenOrId}/followees"

    override val include: String
        get() = "data[*].answer_count,articles_count,gender,follower_count,is_followed,is_following,badge[?(type=best_answerer)].topics"
}

class PeopleCollectionsViewModel(
    val person: Person,
) : PaginationViewModel<DataHolder.Collection>(
        typeOf<DataHolder.Collection>(),
    ) {
    override val initialUrl: String
        get() = "https://www.zhihu.com/api/v4/members/${person.userTokenOrId}/favlists"

    override val include: String
        get() = "data[*].updated_time,answer_count,follower_count,creator"
}

class PeopleQuestionsViewModel(
    val person: Person,
) : PaginationViewModel<DataHolder.Question>(
        typeOf<DataHolder.Question>(),
    ) {
    override val initialUrl: String
        get() = "https://www.zhihu.com/api/v4/members/${person.userTokenOrId}/questions"

    override val include: String
        get() = "data[*].created,answer_count,follower_count,author,visit_count,comment_count,detail,relationship,topics,voteup_count"
}

class PeoplePinsViewModel(
    val person: Person,
) : PaginationViewModel<DataHolder.Pin>(
        typeOf<DataHolder.Pin>(),
    ) {
    override val initialUrl: String
        get() = "https://www.zhihu.com/api/v4/v2/pins/${person.userTokenOrId}/moments"

    override val include: String
        get() = "data[*].like_count,comment_count,created,updated,content"
}

class PeopleColumnContributionsViewModel(
    val person: Person,
) : PaginationViewModel<DataHolder.Column>(
        typeOf<DataHolder.Column>(),
    ) {
    override val initialUrl: String
        get() = "https://www.zhihu.com/api/v4/members/${person.userTokenOrId}/column-contributions"

    override val include: String
        get() = "data[*].articles_count,followers,author"
}

class PersonViewModel(
    val person: Person,
) : ViewModel() {
    var avatar by mutableStateOf("")
    var name by mutableStateOf(person.name)
    var headline by mutableStateOf("")
    var followerCount by mutableIntStateOf(0)
    var followingCount by mutableIntStateOf(0)
    var answerCount by mutableIntStateOf(0)
    var articleCount by mutableIntStateOf(0)
    var isFollowing by mutableStateOf(false)
    var isBlocking by mutableStateOf(false)
    var isBlockedInRecommendations by mutableStateOf(false)

    // 只实现已有数据类型的 ViewModel
    val answersFeedModel = PeopleAnswersViewModel(person)
    val articlesFeedModel = PeopleArticlesViewModel(person)
    val activitiesFeedModel = PeopleActivitiesViewModel(person)
    val collectionsFeedModel = PeopleCollectionsViewModel(person)
    val questionsFeedModel = PeopleQuestionsViewModel(person)
    val pinsFeedModel = PeoplePinsViewModel(person)
    val columnsFeedModel = PeopleColumnContributionsViewModel(person)
    val followersFeedModel = PeopleFollowersViewModel(person)
    val followingFeedModel = PeopleFollowingViewModel(person)
    val subFeedModels = arrayOf(
        answersFeedModel,
        articlesFeedModel,
        activitiesFeedModel,
        collectionsFeedModel,
        questionsFeedModel,
        pinsFeedModel,
        columnsFeedModel,
        followersFeedModel,
        followingFeedModel,
    )

    suspend fun toggleFollow(context: Context) {
        context as MainActivity
        val client = context.httpClient
        if (isFollowing) {
            val jojo = client
                .delete("https://www.zhihu.com/api/v4/members/${person.urlToken}/followers") {
                    signFetchRequest(context)
                }.raiseForStatus()
                .body<JsonObject>()
            this.followerCount = jojo["follower_count"]?.jsonPrimitive?.int ?: (this.followerCount - 1)
            isFollowing = false
        } else {
            val jojo = client
                .post("https://www.zhihu.com/api/v4/members/${person.urlToken}/followers") {
                    signFetchRequest(context)
                }.raiseForStatus()
                .body<JsonObject>()
            this.followerCount = jojo["follower_count"]?.jsonPrimitive?.int ?: (this.followerCount + 1)
            isFollowing = true
        }
    }

    suspend fun toggleBlock(context: Context) {
        context as MainActivity
        val client = context.httpClient
        if (isBlocking) {
            // unblock
            val response = client
                .delete("https://www.zhihu.com/api/v4/members/${person.urlToken}/actions/block") {
                    signFetchRequest(context)
                }.raiseForStatus()
            Log.d("PersonViewModel", "Unblock response: ${response.bodyAsText()}")
            isBlocking = false
        } else {
            // block
            val response = client
                .post("https://www.zhihu.com/api/v4/members/${person.urlToken}/actions/block") {
                    signFetchRequest(context)
                }.raiseForStatus()
            Log.d("PersonViewModel", "Block response: ${response.bodyAsText()}")
            isBlocking = true
        }
    }

    suspend fun toggleRecommendationBlock(context: Context) {
        val blocklistManager = BlocklistManager.getInstance(context)
        if (isBlockedInRecommendations) {
            // Remove from blocklist
            blocklistManager.removeBlockedUser(person.id)
            isBlockedInRecommendations = false
        } else {
            // Add to blocklist
            blocklistManager.addBlockedUser(
                userId = person.id,
                userName = name,
                urlToken = person.urlToken,
                avatarUrl = avatar,
            )
            isBlockedInRecommendations = true
        }
    }

    suspend fun load(context: Context) {
        context as MainActivity
        val jojo = AccountData.fetchGet(context, "https://www.zhihu.com/api/v4/members/${person.id}") {
            url {
                parameters.append(
                    "include",
                    // todo question_count pins_count
                    "allow_message,is_followed,is_following,is_org,is_blocking,answer_count,follower_count,following_count,articles_count,question_count,pins_count",
                )
            }
            signFetchRequest(context)
        }!!
        val person = AccountData.decodeJson<DataHolder.People>(jojo)
        this.avatar = person.avatarUrl
        this.name = person.name
        this.headline = person.headline
        this.followerCount = person.followerCount
        this.followingCount = person.followingCount
        this.answerCount = person.answerCount
        this.articleCount = person.articlesCount
        this.isFollowing = person.isFollowing
        this.isBlocking = person.isBlocking
        if (person.urlToken != null) {
            this.person.urlToken = person.urlToken
        }

        // Check if user is blocked in recommendations
        val blocklistManager = BlocklistManager.getInstance(context)
        this.isBlockedInRecommendations = blocklistManager.isUserBlocked(person.id)

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
) {
    val navigator = LocalNavigator.current
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
        "粉丝",
        "关注",
    )

    val pagerState = rememberPagerState(pageCount = { titles.size })

    LaunchedEffect(viewModel) {
        try {
            viewModel.load(context)
            AccountData.addReadHistory(context, person.id, "profile")
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

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = {
                    UserInfoHeader(
                        viewModel = viewModel,
                        pagerState = pagerState,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors().copy(
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                ),
                scrollBehavior = scrollBehavior,
                expandedHeight = 200.dp,
                windowInsets = WindowInsets(0.dp),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 8.dp),
        ) {
            PrimaryScrollableTabRow(
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
                when (page) {
                    0 -> {
                        // 回答
                        Column(
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            SortBar(
                                currentSort = viewModel.answersFeedModel.sortBy,
                                onSortChange = { newSort ->
                                    viewModel.answersFeedModel.changeSortBy(newSort, context)
                                },
                            )
                            PaginatedList(
                                items = viewModel.answersFeedModel.allData,
                                onLoadMore = { viewModel.answersFeedModel.loadMore(context) },
                                isEnd = { viewModel.answersFeedModel.isEnd },
                                footer = ProgressIndicatorFooter,
                                modifier = Modifier.fillMaxSize(),
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
                                    navigator.onNavigate(
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
                    }

                    1 -> {
                        // 文章
                        Column(
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            SortBar(
                                currentSort = viewModel.articlesFeedModel.sortBy,
                                onSortChange = { newSort ->
                                    viewModel.articlesFeedModel.changeSortBy(newSort, context)
                                },
                            )
                            PaginatedList(
                                items = viewModel.articlesFeedModel.allData,
                                onLoadMore = { viewModel.articlesFeedModel.loadMore(context) },
                                isEnd = { viewModel.articlesFeedModel.isEnd },
                                footer = ProgressIndicatorFooter,
                                modifier = Modifier.fillMaxSize(),
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
                                    navigator.onNavigate(
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
                                it.navDestination?.let(navigator.onNavigate)
                            }
                        }
                    }

                    3 -> {
                        // 收藏
                        PaginatedList(
                            items = viewModel.collectionsFeedModel.allData,
                            onLoadMore = { viewModel.collectionsFeedModel.loadMore(context) },
                            isEnd = { viewModel.collectionsFeedModel.isEnd },
                            footer = ProgressIndicatorFooter,
                        ) { collection ->
                            CollectionListItem(
                                collection = collection,
                                onNavigate = navigator.onNavigate,
                            )
                        }
                    }

                    4 -> {
                        // 提问
                        PaginatedList(
                            items = viewModel.questionsFeedModel.allData,
                            onLoadMore = { viewModel.questionsFeedModel.loadMore(context) },
                            isEnd = { viewModel.questionsFeedModel.isEnd },
                            footer = ProgressIndicatorFooter,
                        ) { question ->
                            QuestionListItem(question)
                        }
                    }

                    5 -> {
                        // 想法
                        PaginatedList(
                            items = viewModel.pinsFeedModel.allData,
                            onLoadMore = { viewModel.pinsFeedModel.loadMore(context) },
                            isEnd = { viewModel.pinsFeedModel.isEnd },
                            footer = ProgressIndicatorFooter,
                        ) { pin ->
                            PinListItem(pin)
                        }
                    }

                    6 -> {
                        // 专栏
                        PaginatedList(
                            items = viewModel.columnsFeedModel.allData,
                            onLoadMore = { viewModel.columnsFeedModel.loadMore(context) },
                            isEnd = { viewModel.columnsFeedModel.isEnd },
                            footer = ProgressIndicatorFooter,
                        ) { column ->
                            ColumnListItem(column)
                        }
                    }

                    7 -> {
                        // 粉丝
                        PaginatedList(
                            items = viewModel.followersFeedModel.allData,
                            onLoadMore = { viewModel.followersFeedModel.loadMore(context) },
                            isEnd = { viewModel.followersFeedModel.isEnd },
                            footer = ProgressIndicatorFooter,
                        ) { people ->
                            PeopleListItem(people)
                        }
                    }

                    8 -> {
                        // 关注
                        PaginatedList(
                            items = viewModel.followingFeedModel.allData,
                            onLoadMore = { viewModel.followingFeedModel.loadMore(context) },
                            isEnd = { viewModel.followingFeedModel.isEnd },
                            footer = ProgressIndicatorFooter,
                        ) { people ->
                            PeopleListItem(people)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CollectionListItem(
    collection: DataHolder.Collection,
    onNavigate: (NavDestination) -> Unit,
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                // TODO: Navigate to collection detail
                Toast
                    .makeText(
                        context,
                        "收藏夹详情功能开发中",
                        Toast.LENGTH_SHORT,
                    ).show()
            }.padding(vertical = 8.dp, horizontal = 4.dp),
    ) {
        Text(
            text = collection.title,
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = "${collection.answerCount} 内容 · ${collection.followerCount} 关注",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun QuestionListItem(
    question: DataHolder.Question,
) {
    val navigator = LocalNavigator.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                navigator.onNavigate(Question(question.id, question.title))
            }.padding(vertical = 8.dp, horizontal = 4.dp),
    ) {
        Text(
            text = question.title,
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = "${question.answerCount} 回答 · ${question.followerCount} 关注",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun PinListItem(
    pin: DataHolder.Pin,
) {
    val navigator = LocalNavigator.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                navigator.onNavigate(Pin(pin.id.toLong()))
            }.padding(vertical = 8.dp, horizontal = 4.dp),
    ) {
        val text = remember { Jsoup.parse(pin.excerptTitle).text() }
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "${pin.likeCount} 赞 · ${pin.commentCount} 评论",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
}

@Composable
private fun ColumnListItem(
    column: DataHolder.Column,
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                // TODO: Navigate to column detail
                Toast
                    .makeText(
                        context,
                        "专栏详情功能开发中",
                        Toast.LENGTH_SHORT,
                    ).show()
            }.padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = column.title,
                style = MaterialTheme.typography.titleMedium,
            )
            if (column.description.isNotEmpty()) {
                Text(
                    text = column.description,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Text(
                text = "${column.articlesCount} 文章 · ${column.followerCount} 关注",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun PeopleListItem(
    people: DataHolder.People,
) {
    val navigator = LocalNavigator.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = people.avatarUrl,
            contentDescription = "用户头像",
            modifier = Modifier
                .padding(end = 12.dp)
                .size(48.dp)
                .clip(CircleShape),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = people.name,
                style = MaterialTheme.typography.titleMedium,
            )
            if (people.headline.isNotEmpty()) {
                Text(
                    text = people.headline,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.padding(top = 4.dp),
            ) {
                Text(
                    text = "${people.answerCount} 回答",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "${people.articlesCount} 文章",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "${people.followerCount} 粉丝",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        OutlinedButton(
            onClick = {
                navigator.onNavigate(
                    Person(
                        id = people.id,
                        name = people.name,
                        urlToken = people.urlToken ?: "",
                    ),
                )
            },
        ) {
            Text("查看")
        }
    }
}

@Composable
private fun StatItem(label: String, value: Int, onClick: () -> Unit = {}) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
    ) {
        Text(text = value.toString(), style = MaterialTheme.typography.titleMedium)
        Text(text = label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun SortBar(
    currentSort: String,
    onSortChange: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(
            onClick = { onSortChange("voteups") },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(8.dp),
            colors = if (currentSort == "voteups") {
                ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            } else {
                ButtonDefaults.outlinedButtonColors()
            },
        ) {
            Text("按热度")
        }
        OutlinedButton(
            onClick = { onSortChange("created") },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(8.dp),
            colors = if (currentSort == "created") {
                ButtonDefaults.outlinedButtonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            } else {
                ButtonDefaults.outlinedButtonColors()
            },
        ) {
            Text("按时间")
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun UserInfoHeader(
    viewModel: PersonViewModel,
    pagerState: PagerState,
    modifier: Modifier = Modifier,
) {
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
            StatItem("回答", viewModel.answerCount, onClick = {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(0)
                }
            })
            StatItem("文章", viewModel.articleCount, onClick = {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(1)
                }
            })
            StatItem("粉丝", viewModel.followerCount, onClick = {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(7)
                }
            })
            StatItem("关注", viewModel.followingCount, onClick = {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(8)
                }
            })
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
            OutlinedButton(onClick = {
                coroutineScope.launch {
                    try {
                        viewModel.toggleRecommendationBlock(context)
                        Toast
                            .makeText(
                                context,
                                if (viewModel.isBlockedInRecommendations) "已屏蔽推荐" else "已取消屏蔽推荐",
                                Toast.LENGTH_SHORT,
                            ).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "操作失败: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }) {
                Text(if (viewModel.isBlockedInRecommendations) "取消屏蔽推荐" else "屏蔽推荐")
            }
        }
    }
}
