/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.github.zly2006.zhihu.ui

import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.github.zly2006.zhihu.BuildConfig
import com.github.zly2006.zhihu.MainActivity
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.Person
import com.github.zly2006.zhihu.navigation.Pin
import com.github.zly2006.zhihu.navigation.Question
import com.github.zly2006.zhihu.ui.components.FeedCard
import com.github.zly2006.zhihu.ui.components.OpenImageDialog
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
        // 签名有bug，暂时无法使用新的API，先回退到旧的API
        // get() = "https://www.zhihu.com/api/v4/members/${person.userTokenOrId}/followers"
        get() = "https://api.zhihu.com/people/${person.id}/followers"

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
                    signFetchRequest()
                }.raiseForStatus()
                .body<JsonObject>()
            this.followerCount = jojo["follower_count"]?.jsonPrimitive?.int ?: (this.followerCount - 1)
            isFollowing = false
        } else {
            val jojo = client
                .post("https://www.zhihu.com/api/v4/members/${person.urlToken}/followers") {
                    signFetchRequest()
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
                    signFetchRequest()
                }.raiseForStatus()
            Log.d("PersonViewModel", "Unblock response: ${response.bodyAsText()}")
            isBlocking = false
        } else {
            // block
            val response = client
                .post("https://www.zhihu.com/api/v4/members/${person.urlToken}/actions/block") {
                    signFetchRequest()
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
                // todo question_count pins_count
                parameters["include"] = "allow_message,is_followed,is_following,is_org,is_blocking,answer_count,follower_count,following_count,articles_count,question_count,pins_count"
            }
            signFetchRequest()
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

private val PEOPLE_SCREEN_TITLES = listOf(
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

const val PEOPLE_SCREEN_ROOT_TAG = "people_screen_root"
const val PEOPLE_SCREEN_HEADER_TAG = "people_screen_header"
const val PEOPLE_SCREEN_AVATAR_TAG = "people_screen_avatar"
const val PEOPLE_SCREEN_TAB_ROW_TAG = "people_screen_tab_row"
const val PEOPLE_SCREEN_PAGER_TAG = "people_screen_pager"
const val PEOPLE_SCREEN_ANSWERS_LIST_TAG = "people_screen_answers_list"
const val PEOPLE_SCREEN_ARTICLES_LIST_TAG = "people_screen_articles_list"
const val PEOPLE_SCREEN_ACTIVITIES_LIST_TAG = "people_screen_activities_list"
const val PEOPLE_SCREEN_COLLECTIONS_LIST_TAG = "people_screen_collections_list"
const val PEOPLE_SCREEN_QUESTIONS_LIST_TAG = "people_screen_questions_list"
const val PEOPLE_SCREEN_PINS_LIST_TAG = "people_screen_pins_list"
const val PEOPLE_SCREEN_COLUMNS_LIST_TAG = "people_screen_columns_list"
const val PEOPLE_SCREEN_FOLLOWERS_LIST_TAG = "people_screen_followers_list"
const val PEOPLE_SCREEN_FOLLOWING_LIST_TAG = "people_screen_following_list"
const val PEOPLE_SCREEN_ANSWER_COUNT_TAG = "people_screen_stat_answers"
const val PEOPLE_SCREEN_ARTICLE_COUNT_TAG = "people_screen_stat_articles"
const val PEOPLE_SCREEN_FOLLOWER_COUNT_TAG = "people_screen_stat_followers"
const val PEOPLE_SCREEN_FOLLOWING_COUNT_TAG = "people_screen_stat_following"
const val PEOPLE_SCREEN_FOLLOW_BUTTON_TAG = "people_screen_follow_button"
const val PEOPLE_SCREEN_BLOCK_BUTTON_TAG = "people_screen_block_button"
const val PEOPLE_SCREEN_RECOMMENDATION_BLOCK_BUTTON_TAG = "people_screen_recommendation_block_button"
const val PEOPLE_SCREEN_ANSWER_SORT_HOT_TAG = "people_screen_answer_sort_voteups"
const val PEOPLE_SCREEN_ANSWER_SORT_TIME_TAG = "people_screen_answer_sort_created"
const val PEOPLE_SCREEN_ARTICLE_SORT_HOT_TAG = "people_screen_article_sort_voteups"
const val PEOPLE_SCREEN_ARTICLE_SORT_TIME_TAG = "people_screen_article_sort_created"

fun peopleScreenTabTag(index: Int): String = "people_screen_tab_$index"

fun peopleScreenPageTag(index: Int): String = "people_screen_page_$index"

fun peopleScreenAnswerItemTag(id: Long): String = "people_screen_answer_item_$id"

fun peopleScreenArticleItemTag(id: Long): String = "people_screen_article_item_$id"

fun peopleScreenCollectionItemTag(id: String): String = "people_screen_collection_item_$id"

fun peopleScreenQuestionItemTag(id: Long): String = "people_screen_question_item_$id"

fun peopleScreenPinItemTag(id: String): String = "people_screen_pin_item_$id"

fun peopleScreenColumnItemTag(id: String): String = "people_screen_column_item_$id"

fun peopleScreenFollowerItemTag(id: String): String = "people_screen_follower_item_$id"

fun peopleScreenFollowerActionTag(id: String): String = "people_screen_follower_action_$id"

fun peopleScreenFollowingItemTag(id: String): String = "people_screen_following_item_$id"

fun peopleScreenFollowingActionTag(id: String): String = "people_screen_following_action_$id"

private fun peopleScreenInitialPage(person: Person): Int {
    val jumpToIndex = PEOPLE_SCREEN_TITLES.indexOf(person.jumpTo)
    return if (jumpToIndex >= 0) jumpToIndex else 0
}

data class PeopleProfileUiState(
    val avatar: String = "",
    val name: String = "",
    val headline: String = "",
    val followerCount: Int = 0,
    val followingCount: Int = 0,
    val answerCount: Int = 0,
    val articleCount: Int = 0,
    val isFollowing: Boolean = false,
    val isBlocking: Boolean = false,
    val isBlockedInRecommendations: Boolean = false,
)

data class PeopleListUiState<T>(
    val items: List<T> = emptyList(),
    val isEnd: Boolean = true,
)

data class PeopleSortedListUiState<T>(
    val sortBy: String,
    val items: List<T> = emptyList(),
    val isEnd: Boolean = true,
)

data class PeopleScreenUiState(
    val profile: PeopleProfileUiState = PeopleProfileUiState(),
    val answers: PeopleSortedListUiState<DataHolder.Answer> = PeopleSortedListUiState(sortBy = "voteups"),
    val articles: PeopleSortedListUiState<DataHolder.Article> = PeopleSortedListUiState(sortBy = "created"),
    val activities: PeopleListUiState<BaseFeedViewModel.FeedDisplayItem> = PeopleListUiState(),
    val collections: PeopleListUiState<DataHolder.Collection> = PeopleListUiState(),
    val questions: PeopleListUiState<DataHolder.Question> = PeopleListUiState(),
    val pins: PeopleListUiState<DataHolder.Pin> = PeopleListUiState(),
    val columns: PeopleListUiState<DataHolder.Column> = PeopleListUiState(),
    val followers: PeopleListUiState<DataHolder.People> = PeopleListUiState(),
    val following: PeopleListUiState<DataHolder.People> = PeopleListUiState(),
)

/**
 * Instrumented tests inject a fixed profile snapshot, seeded tab contents, and offline callbacks
 * here so PeopleScreen can be exercised without touching remote profile fetches or mutations.
 */
data class PeopleScreenTestOverrides(
    val initialUiState: PeopleScreenUiState,
    val initialPage: Int? = null,
    val onAnswerSortChange: ((String) -> Unit)? = null,
    val onArticleSortChange: ((String) -> Unit)? = null,
    val onToggleFollow: ((Boolean) -> Unit)? = null,
    val onToggleBlock: ((Boolean) -> Unit)? = null,
    val onToggleRecommendationBlock: ((Boolean) -> Unit)? = null,
    val onAnswersLoadMore: (() -> Unit)? = null,
    val onArticlesLoadMore: (() -> Unit)? = null,
    val onActivitiesLoadMore: (() -> Unit)? = null,
    val onCollectionsLoadMore: (() -> Unit)? = null,
    val onQuestionsLoadMore: (() -> Unit)? = null,
    val onPinsLoadMore: (() -> Unit)? = null,
    val onColumnsLoadMore: (() -> Unit)? = null,
    val onFollowersLoadMore: (() -> Unit)? = null,
    val onFollowingLoadMore: (() -> Unit)? = null,
)

private fun PersonViewModel.toUiState(): PeopleScreenUiState = PeopleScreenUiState(
    profile = PeopleProfileUiState(
        avatar = avatar,
        name = name,
        headline = headline,
        followerCount = followerCount,
        followingCount = followingCount,
        answerCount = answerCount,
        articleCount = articleCount,
        isFollowing = isFollowing,
        isBlocking = isBlocking,
        isBlockedInRecommendations = isBlockedInRecommendations,
    ),
    answers = PeopleSortedListUiState(
        sortBy = answersFeedModel.sortBy,
        items = answersFeedModel.allData,
        isEnd = answersFeedModel.isEnd,
    ),
    articles = PeopleSortedListUiState(
        sortBy = articlesFeedModel.sortBy,
        items = articlesFeedModel.allData,
        isEnd = articlesFeedModel.isEnd,
    ),
    activities = PeopleListUiState(
        items = activitiesFeedModel.displayItems,
        isEnd = activitiesFeedModel.isEnd,
    ),
    collections = PeopleListUiState(
        items = collectionsFeedModel.allData,
        isEnd = collectionsFeedModel.isEnd,
    ),
    questions = PeopleListUiState(
        items = questionsFeedModel.allData,
        isEnd = questionsFeedModel.isEnd,
    ),
    pins = PeopleListUiState(
        items = pinsFeedModel.allData,
        isEnd = pinsFeedModel.isEnd,
    ),
    columns = PeopleListUiState(
        items = columnsFeedModel.allData,
        isEnd = columnsFeedModel.isEnd,
    ),
    followers = PeopleListUiState(
        items = followersFeedModel.allData,
        isEnd = followersFeedModel.isEnd,
    ),
    following = PeopleListUiState(
        items = followingFeedModel.allData,
        isEnd = followingFeedModel.isEnd,
    ),
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PeopleScreen(
    innerPadding: PaddingValues,
    person: Person,
    testOverrides: PeopleScreenTestOverrides? = null,
) {
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val viewModel = viewModel { PersonViewModel(person) }
    val coroutineScope = rememberCoroutineScope()
    var testUiState by remember(person.id, person.urlToken, testOverrides?.initialUiState) {
        mutableStateOf(testOverrides?.initialUiState ?: PeopleScreenUiState())
    }
    val uiState = testOverrides?.let { testUiState } ?: viewModel.toUiState()

    val pagerState = rememberPagerState(
        initialPage = testOverrides?.initialPage ?: peopleScreenInitialPage(person),
        pageCount = { PEOPLE_SCREEN_TITLES.size },
    )

    LaunchedEffect(viewModel, testOverrides) {
        if (testOverrides != null) {
            return@LaunchedEffect
        }
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
    LaunchedEffect(pagerState.currentPage, testOverrides) {
        if (testOverrides != null) {
            return@LaunchedEffect
        }
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

    fun updateAnswersSort(newSort: String) {
        if (testOverrides != null) {
            if (uiState.answers.sortBy != newSort) {
                testUiState = uiState.copy(answers = uiState.answers.copy(sortBy = newSort))
                testOverrides.onAnswerSortChange?.invoke(newSort)
            }
            return
        }
        viewModel.answersFeedModel.changeSortBy(newSort, context)
    }

    fun updateArticlesSort(newSort: String) {
        if (testOverrides != null) {
            if (uiState.articles.sortBy != newSort) {
                testUiState = uiState.copy(articles = uiState.articles.copy(sortBy = newSort))
                testOverrides.onArticleSortChange?.invoke(newSort)
            }
            return
        }
        viewModel.articlesFeedModel.changeSortBy(newSort, context)
    }

    Scaffold(
        modifier = Modifier
            .testTag(PEOPLE_SCREEN_ROOT_TAG)
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .padding(innerPadding),
        topBar = {
            TopAppBar(
                title = {
                    UserInfoHeader(
                        profile = uiState.profile,
                        pagerState = pagerState,
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .testTag(PEOPLE_SCREEN_HEADER_TAG),
                        onFollowToggle = {
                            if (testOverrides != null) {
                                val newFollowing = !uiState.profile.isFollowing
                                testUiState = uiState.copy(
                                    profile = uiState.profile.copy(
                                        isFollowing = newFollowing,
                                        followerCount = (uiState.profile.followerCount + if (newFollowing) 1 else -1).coerceAtLeast(0),
                                    ),
                                )
                                testOverrides.onToggleFollow?.invoke(newFollowing)
                            } else {
                                coroutineScope.launch {
                                    try {
                                        viewModel.toggleFollow(context)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "操作失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        onBlockToggle = {
                            if (testOverrides != null) {
                                val newBlocking = !uiState.profile.isBlocking
                                testUiState = uiState.copy(profile = uiState.profile.copy(isBlocking = newBlocking))
                                testOverrides.onToggleBlock?.invoke(newBlocking)
                            } else {
                                coroutineScope.launch {
                                    try {
                                        viewModel.toggleBlock(context)
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "操作失败: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        onRecommendationBlockToggle = {
                            if (testOverrides != null) {
                                val newRecommendationBlock = !uiState.profile.isBlockedInRecommendations
                                testUiState = uiState.copy(
                                    profile = uiState.profile.copy(isBlockedInRecommendations = newRecommendationBlock),
                                )
                                testOverrides.onToggleRecommendationBlock?.invoke(newRecommendationBlock)
                            } else {
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
                            }
                        },
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
                modifier = Modifier.testTag(PEOPLE_SCREEN_TAB_ROW_TAG),
            ) {
                PEOPLE_SCREEN_TITLES.forEachIndexed { index, title ->
                    Tab(
                        selected = pagerState.currentPage == index,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(index)
                            }
                        },
                        modifier = Modifier.testTag(peopleScreenTabTag(index)),
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
                modifier = Modifier
                    .weight(1f)
                    .testTag(PEOPLE_SCREEN_PAGER_TAG),
            ) { page ->
                when (page) {
                    0 -> {
                        // 回答
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag(peopleScreenPageTag(page)),
                        ) {
                            SortBar(
                                currentSort = uiState.answers.sortBy,
                                onSortChange = ::updateAnswersSort,
                                hotTag = PEOPLE_SCREEN_ANSWER_SORT_HOT_TAG,
                                timeTag = PEOPLE_SCREEN_ANSWER_SORT_TIME_TAG,
                            )
                            PaginatedList(
                                items = uiState.answers.items,
                                onLoadMore = {
                                    testOverrides?.onAnswersLoadMore?.invoke()
                                        ?: viewModel.answersFeedModel.loadMore(context)
                                },
                                isEnd = { uiState.answers.isEnd },
                                footer = ProgressIndicatorFooter,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag(PEOPLE_SCREEN_ANSWERS_LIST_TAG),
                                key = { it.id },
                            ) {
                                FeedCard(
                                    BaseFeedViewModel.FeedDisplayItem(
                                        title = it.question.title,
                                        summary = it.excerpt,
                                        details = "回答 · ${it.voteupCount} 赞同 · ${it.commentCount} 评论",
                                        feed = null,
                                    ),
                                    modifier = Modifier.testTag(peopleScreenAnswerItemTag(it.id)),
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
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag(peopleScreenPageTag(page)),
                        ) {
                            SortBar(
                                currentSort = uiState.articles.sortBy,
                                onSortChange = ::updateArticlesSort,
                                hotTag = PEOPLE_SCREEN_ARTICLE_SORT_HOT_TAG,
                                timeTag = PEOPLE_SCREEN_ARTICLE_SORT_TIME_TAG,
                            )
                            PaginatedList(
                                items = uiState.articles.items,
                                onLoadMore = {
                                    testOverrides?.onArticlesLoadMore?.invoke()
                                        ?: viewModel.articlesFeedModel.loadMore(context)
                                },
                                isEnd = { uiState.articles.isEnd },
                                footer = ProgressIndicatorFooter,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag(PEOPLE_SCREEN_ARTICLES_LIST_TAG),
                                key = { it.id },
                            ) {
                                FeedCard(
                                    BaseFeedViewModel.FeedDisplayItem(
                                        title = it.title,
                                        summary = it.excerpt,
                                        details = "文章 · ${it.voteupCount} 赞同 · ${it.commentCount} 评论",
                                        feed = null,
                                    ),
                                    modifier = Modifier.testTag(peopleScreenArticleItemTag(it.id)),
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
                            items = uiState.activities.items,
                            onLoadMore = {
                                testOverrides?.onActivitiesLoadMore?.invoke()
                                    ?: viewModel.activitiesFeedModel.loadMore(context)
                            },
                            isEnd = { uiState.activities.isEnd },
                            footer = ProgressIndicatorFooter,
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag(PEOPLE_SCREEN_ACTIVITIES_LIST_TAG),
                        ) {
                            FeedCard(
                                it,
                                modifier = Modifier.testTag("people_screen_activity_item_${it.localFeedId ?: it.title}"),
                                horizontalPadding = 4.dp,
                            )
                        }
                    }

                    3 -> {
                        // 收藏
                        PaginatedList(
                            items = uiState.collections.items,
                            onLoadMore = {
                                testOverrides?.onCollectionsLoadMore?.invoke()
                                    ?: viewModel.collectionsFeedModel.loadMore(context)
                            },
                            isEnd = { uiState.collections.isEnd },
                            footer = ProgressIndicatorFooter,
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag(PEOPLE_SCREEN_COLLECTIONS_LIST_TAG),
                            key = { it.id },
                        ) { collection ->
                            CollectionListItem(
                                collection = collection,
                                itemTag = peopleScreenCollectionItemTag(collection.id),
                            )
                        }
                    }

                    4 -> {
                        // 提问
                        PaginatedList(
                            items = uiState.questions.items,
                            onLoadMore = {
                                testOverrides?.onQuestionsLoadMore?.invoke()
                                    ?: viewModel.questionsFeedModel.loadMore(context)
                            },
                            isEnd = { uiState.questions.isEnd },
                            footer = ProgressIndicatorFooter,
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag(PEOPLE_SCREEN_QUESTIONS_LIST_TAG),
                            key = { it.id },
                        ) { question ->
                            QuestionListItem(
                                question = question,
                                itemTag = peopleScreenQuestionItemTag(question.id),
                            )
                        }
                    }

                    5 -> {
                        // 想法
                        PaginatedList(
                            items = uiState.pins.items,
                            onLoadMore = {
                                testOverrides?.onPinsLoadMore?.invoke()
                                    ?: viewModel.pinsFeedModel.loadMore(context)
                            },
                            isEnd = { uiState.pins.isEnd },
                            footer = ProgressIndicatorFooter,
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag(PEOPLE_SCREEN_PINS_LIST_TAG),
                            key = { it.id },
                        ) { pin ->
                            PinListItem(
                                pin = pin,
                                itemTag = peopleScreenPinItemTag(pin.id),
                            )
                        }
                    }

                    6 -> {
                        // 专栏
                        PaginatedList(
                            items = uiState.columns.items,
                            onLoadMore = {
                                testOverrides?.onColumnsLoadMore?.invoke()
                                    ?: viewModel.columnsFeedModel.loadMore(context)
                            },
                            isEnd = { uiState.columns.isEnd },
                            footer = ProgressIndicatorFooter,
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag(PEOPLE_SCREEN_COLUMNS_LIST_TAG),
                            key = { it.id },
                        ) { column ->
                            ColumnListItem(
                                column = column,
                                itemTag = peopleScreenColumnItemTag(column.id),
                            )
                        }
                    }

                    7 -> {
                        // 粉丝
                        PaginatedList(
                            items = uiState.followers.items,
                            onLoadMore = {
                                testOverrides?.onFollowersLoadMore?.invoke()
                                    ?: viewModel.followersFeedModel.loadMore(context)
                            },
                            isEnd = { uiState.followers.isEnd },
                            footer = ProgressIndicatorFooter,
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag(PEOPLE_SCREEN_FOLLOWERS_LIST_TAG),
                            key = { it.id },
                        ) { people ->
                            PeopleListItem(
                                people = people,
                                itemTag = peopleScreenFollowerItemTag(people.id),
                                actionTag = peopleScreenFollowerActionTag(people.id),
                            )
                        }
                    }

                    8 -> {
                        // 关注
                        PaginatedList(
                            items = uiState.following.items,
                            onLoadMore = {
                                testOverrides?.onFollowingLoadMore?.invoke()
                                    ?: viewModel.followingFeedModel.loadMore(context)
                            },
                            isEnd = { uiState.following.isEnd },
                            footer = ProgressIndicatorFooter,
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag(PEOPLE_SCREEN_FOLLOWING_LIST_TAG),
                            key = { it.id },
                        ) { people ->
                            PeopleListItem(
                                people = people,
                                itemTag = peopleScreenFollowingItemTag(people.id),
                                actionTag = peopleScreenFollowingActionTag(people.id),
                            )
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
    itemTag: String? = null,
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (itemTag != null) Modifier.testTag(itemTag) else Modifier)
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
    itemTag: String? = null,
) {
    val navigator = LocalNavigator.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (itemTag != null) Modifier.testTag(itemTag) else Modifier)
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
    itemTag: String? = null,
) {
    val navigator = LocalNavigator.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (itemTag != null) Modifier.testTag(itemTag) else Modifier)
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
    itemTag: String? = null,
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (itemTag != null) Modifier.testTag(itemTag) else Modifier)
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
    itemTag: String? = null,
    actionTag: String? = null,
) {
    val navigator = LocalNavigator.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (itemTag != null) Modifier.testTag(itemTag) else Modifier)
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
            modifier = if (actionTag != null) Modifier.testTag(actionTag) else Modifier,
        ) {
            Text("查看")
        }
    }
}

@Composable
private fun StatItem(label: String, value: Int, onClick: () -> Unit = {}, tag: String? = null) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .then(if (tag != null) Modifier.testTag(tag) else Modifier)
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
    hotTag: String? = null,
    timeTag: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(
            onClick = { onSortChange("voteups") },
            modifier = Modifier
                .weight(1f)
                .then(if (hotTag != null) Modifier.testTag(hotTag) else Modifier),
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
            modifier = Modifier
                .weight(1f)
                .then(if (timeTag != null) Modifier.testTag(timeTag) else Modifier),
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
    profile: PeopleProfileUiState,
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    onFollowToggle: () -> Unit,
    onBlockToggle: () -> Unit,
    onRecommendationBlockToggle: () -> Unit,
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
                model = profile.avatar,
                contentDescription = "用户头像",
                modifier = Modifier
                    .testTag(PEOPLE_SCREEN_AVATAR_TAG)
                    .padding(end = 16.dp)
                    .size(80.dp)
                    .clip(CircleShape)
                    .clickable {
                        OpenImageDialog(
                            context,
                            AccountData.httpClient(context),
                            profile.avatar.substringBefore("_") + ".jpg",
                        ).show()
                    },
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(profile.name, style = MaterialTheme.typography.titleLarge)
                Text(
                    profile.headline,
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
            StatItem("回答", profile.answerCount, onClick = {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(0)
                }
            }, tag = PEOPLE_SCREEN_ANSWER_COUNT_TAG)
            StatItem("文章", profile.articleCount, onClick = {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(1)
                }
            }, tag = PEOPLE_SCREEN_ARTICLE_COUNT_TAG)
            StatItem("粉丝", profile.followerCount, onClick = {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(7)
                }
            }, tag = PEOPLE_SCREEN_FOLLOWER_COUNT_TAG)
            StatItem("关注", profile.followingCount, onClick = {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(8)
                }
            }, tag = PEOPLE_SCREEN_FOLLOWING_COUNT_TAG)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        ) {
            OutlinedButton(
                onClick = onFollowToggle,
                modifier = Modifier.testTag(PEOPLE_SCREEN_FOLLOW_BUTTON_TAG),
            ) {
                Text(if (profile.isFollowing) "取消关注" else "关注")
            }
            OutlinedButton(
                onClick = onBlockToggle,
                modifier = Modifier.testTag(PEOPLE_SCREEN_BLOCK_BUTTON_TAG),
            ) {
                Text(if (profile.isBlocking) "取消拉黑" else "拉黑")
            }
            OutlinedButton(
                onClick = onRecommendationBlockToggle,
                modifier = Modifier.testTag(PEOPLE_SCREEN_RECOMMENDATION_BLOCK_BUTTON_TAG),
            ) {
                Text(if (profile.isBlockedInRecommendations) "取消屏蔽推荐" else "屏蔽推荐")
            }
        }
    }
}
