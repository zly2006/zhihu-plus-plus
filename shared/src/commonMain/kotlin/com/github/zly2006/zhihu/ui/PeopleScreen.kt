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

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.fleeksoft.ksoup.Ksoup
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.CollectionContent
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.Person
import com.github.zly2006.zhihu.navigation.Pin
import com.github.zly2006.zhihu.navigation.Question
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.data.FeedDisplayItem
import com.github.zly2006.zhihu.shared.data.OfficialBadge
import com.github.zly2006.zhihu.shared.data.ZhihuJson
import com.github.zly2006.zhihu.shared.data.officialBadge
import com.github.zly2006.zhihu.shared.data.officialBadgeDetails
import com.github.zly2006.zhihu.shared.platform.rememberImagePreviewOpener
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.shared.platform.rememberZhihuWebUrlOpener
import com.github.zly2006.zhihu.shared.util.raiseForStatus
import com.github.zly2006.zhihu.ui.FollowedQuestion
import com.github.zly2006.zhihu.ui.FollowedTopic
import com.github.zly2006.zhihu.ui.PeopleListUiState
import com.github.zly2006.zhihu.ui.PeopleProfileUiState
import com.github.zly2006.zhihu.ui.PeopleSortedListUiState
import com.github.zly2006.zhihu.ui.components.AuthorBadge
import com.github.zly2006.zhihu.ui.components.FeedCard
import com.github.zly2006.zhihu.ui.components.PaginatedList
import com.github.zly2006.zhihu.ui.components.ProgressIndicatorFooter
import com.github.zly2006.zhihu.viewmodel.ContentBlocklistEnvironment
import com.github.zly2006.zhihu.viewmodel.PaginationEnvironment
import com.github.zly2006.zhihu.viewmodel.PaginationViewModel
import com.github.zly2006.zhihu.viewmodel.ProfileLoadEnvironment
import com.github.zly2006.zhihu.viewmodel.ZhihuApiEnvironment
import com.github.zly2006.zhihu.viewmodel.addReadHistory
import com.github.zly2006.zhihu.viewmodel.deleteSigned
import com.github.zly2006.zhihu.viewmodel.feed.BaseFeedViewModel
import com.github.zly2006.zhihu.viewmodel.postSigned
import com.github.zly2006.zhihu.viewmodel.rememberPaginationEnvironment
import io.ktor.client.call.body
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.compose.resources.painterResource
import zhihu.shared.generated.resources.Res
import zhihu.shared.generated.resources.ic_zh_plus_author_badge
import kotlin.reflect.typeOf
import com.github.zly2006.zhihu.navigation.Search as SearchDestination

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
        get() = "data[*].is_normal,admin_closed_comment,reward_info,is_collapsed,annotation_action,annotation_detail,collapse_reason,collapsed_by,suggest_edit,comment_count,thanks_count,can_comment,content,editable_content,attachment,voteup_count,reshipment_settings,comment_permission,created_time,updated_time,review_info,excerpt,paid_info,reaction_instruction,is_labeled,label_info,relationship.is_authorized,voting,is_author,is_thanked,is_nothelp,author.badge_v2"

    fun changeSortBy(newSort: String, environment: PaginationEnvironment) {
        if (sortBy != newSort) {
            sortBy = newSort
            refresh(environment)
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
        get() = "data[*].comment_count,suggest_edit,is_normal,thumbnail_extra_info,thumbnail,can_comment,comment_permission,admin_closed_comment,content,voteup_count,created,updated,upvoted_followees,voting,review_info,reaction_instruction,is_labeled,label_info,author.badge_v2;data[*].vessay_info;data[*].author.badge[?(type=best_answerer)].topics;"

    fun changeSortBy(newSort: String, environment: PaginationEnvironment) {
        if (sortBy != newSort) {
            sortBy = newSort
            refresh(environment)
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
        get() = "data[*].answer_count,articles_count,gender,follower_count,is_followed,is_following,badge_v2,badge[?(type=best_answerer)].topics"
}

class PeopleFollowingViewModel(
    val person: Person,
) : PaginationViewModel<DataHolder.People>(
        typeOf<DataHolder.People>(),
    ) {
    override val initialUrl: String
        get() = "https://www.zhihu.com/api/v4/members/${person.userTokenOrId}/followees"

    override val include: String
        get() = "data[*].answer_count,articles_count,gender,follower_count,is_followed,is_following,badge_v2,badge[?(type=best_answerer)].topics"
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

class PeopleFollowingCollectionsViewModel(
    val person: Person,
) : PaginationViewModel<DataHolder.Collection>(
        typeOf<DataHolder.Collection>(),
    ) {
    override val initialUrl: String
        get() = "https://www.zhihu.com/api/v4/members/${person.userTokenOrId}/following-favlists"

    override val include: String
        get() = "data[*].updated_time,answer_count,follower_count,creator"
}

@Serializable
data class FollowedQuestion(
    val id: String,
    val type: String = "question",
    val url: String = "",
    val title: String = "",
    val questionType: String = "",
    val created: Long = 0L,
    val updatedTime: Long = 0L,
)

@Serializable
data class FollowedTopic(
    val id: String = "",
    val type: String = "topic",
    val url: String = "",
    val name: String = "",
    val avatarUrl: String? = null,
    val topicType: String? = null,
    val topic: DataHolder.Topic? = null,
) {
    val displayId: String get() = topic?.id ?: id
    val displayName: String get() = topic?.name ?: name
    val displayAvatarUrl: String? get() = topic?.avatarUrl ?: avatarUrl
}

class PeopleFollowingQuestionsViewModel(
    val person: Person,
) : PaginationViewModel<FollowedQuestion>(
        typeOf<FollowedQuestion>(),
    ) {
    override val initialUrl: String
        get() = "https://www.zhihu.com/api/v4/members/${person.userTokenOrId}/following-questions"

    override val include: String
        get() = ""
}

class PeopleFollowingTopicsViewModel(
    val person: Person,
) : PaginationViewModel<FollowedTopic>(
        typeOf<FollowedTopic>(),
    ) {
    override val initialUrl: String
        get() = "https://www.zhihu.com/api/v4/members/${person.userTokenOrId}/following-topic-contributions"

    override val include: String
        get() = ""
}

class PeopleFollowingColumnsViewModel(
    val person: Person,
) : PaginationViewModel<DataHolder.Column>(
        typeOf<DataHolder.Column>(),
    ) {
    override val initialUrl: String
        get() = "https://www.zhihu.com/api/v4/members/${person.userTokenOrId}/following-columns"

    override val include: String
        get() = "data[*].articles_count,followers,author"
}

class PersonViewModel(
    val person: Person,
) : ViewModel() {
    var avatar by mutableStateOf("")
    var name by mutableStateOf(person.name)
    var headline by mutableStateOf("")
    var officialBadge by mutableStateOf<OfficialBadge?>(null)
    var officialBadgeDetails by mutableStateOf<List<OfficialBadge>>(emptyList())
    var followerCount by mutableIntStateOf(0)
    var followingCount by mutableIntStateOf(0)
    var answerCount by mutableIntStateOf(0)
    var articleCount by mutableIntStateOf(0)
    var isFollowing by mutableStateOf(false)
    var isBlocking by mutableStateOf(false)
    var isBlockedInRecommendations by mutableStateOf(false)
    var memberHashId by mutableStateOf(person.id)

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
    val followingCollectionsFeedModel = PeopleFollowingCollectionsViewModel(person)
    val followingQuestionsFeedModel = PeopleFollowingQuestionsViewModel(person)
    val followingTopicsFeedModel = PeopleFollowingTopicsViewModel(person)
    val followingColumnsFeedModel = PeopleFollowingColumnsViewModel(person)
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

    suspend fun toggleFollow(environment: ZhihuApiEnvironment) {
        val followersUrl = "https://www.zhihu.com/api/v4/members/${person.urlToken}/followers"
        val newFollowingState = !isFollowing
        val response = if (newFollowingState) {
            environment.postSigned(followersUrl)
        } else {
            environment.deleteSigned(followersUrl)
        }
        val jojo = response.raiseForStatus().body<JsonObject>()
        followerCount = jojo["follower_count"]?.jsonPrimitive?.int ?: (followerCount + if (newFollowingState) 1 else -1)
        isFollowing = newFollowingState
    }

    suspend fun toggleBlock(environment: ZhihuApiEnvironment) {
        val blockUrl = "https://www.zhihu.com/api/v4/members/${person.urlToken}/actions/block"
        val newBlockingState = !isBlocking
        if (newBlockingState) {
            environment.postSigned(blockUrl)
        } else {
            environment.deleteSigned(blockUrl)
        }.raiseForStatus()
        isBlocking = newBlockingState
    }

    suspend fun toggleRecommendationBlock(environment: ContentBlocklistEnvironment) {
        if (isBlockedInRecommendations) {
            environment.removeBlockedUser(person.id)
            isBlockedInRecommendations = false
        } else {
            environment.addBlockedUser(
                userId = person.id,
                userName = name,
                urlToken = person.urlToken,
                avatarUrl = avatar,
            )
            isBlockedInRecommendations = true
        }
    }

    suspend fun load(environment: ProfileLoadEnvironment) {
        environment.addReadHistory(person.id, "profile")

        val jojo = environment.fetchJson(peopleProfileUrl(person), PEOPLE_PROFILE_INCLUDE_PATH)
            ?: error("用户资料为空")

        val loadedPerson = ZhihuJson.decodeJson<DataHolder.People>(jojo)
        val urlToken = loadedPerson.urlToken

        environment.postHistoryDestination(
            Person(
                id = loadedPerson.id,
                name = loadedPerson.name,
                urlToken = urlToken ?: "",
            ),
        )

        val isBlocked = environment.isUserBlocked(loadedPerson.id)
        val profile = toPeopleProfileLoadResult(loadedPerson, isBlocked).profile

        this.avatar = profile.avatar
        this.name = profile.name
        this.headline = profile.headline
        this.officialBadge = profile.officialBadge
        this.officialBadgeDetails = profile.officialBadgeDetails
        this.followerCount = profile.followerCount
        this.followingCount = profile.followingCount
        this.answerCount = profile.answerCount
        this.articleCount = profile.articleCount
        this.isFollowing = profile.isFollowing
        this.isBlocking = profile.isBlocking
        this.isBlockedInRecommendations = profile.isBlockedInRecommendations
        this.memberHashId = loadedPerson.id
        this.person.id = loadedPerson.id
        if (urlToken != null) {
            this.person.urlToken = urlToken
        }
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
    "关注订阅",
)

private val PEOPLE_SCREEN_SUBSCRIPTION_TITLES = listOf(
    "我订阅的专栏",
    "关注的话题",
    "关注的问题",
    "关注的收藏夹",
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
const val PEOPLE_SCREEN_SUBSCRIPTION_TABS_TAG = "people_screen_subscription_tabs"
const val PEOPLE_SCREEN_SUBSCRIPTIONS_LIST_TAG = "people_screen_subscriptions_list"
const val PEOPLE_SCREEN_ANSWER_COUNT_TAG = "people_screen_stat_answers"
const val PEOPLE_SCREEN_ARTICLE_COUNT_TAG = "people_screen_stat_articles"
const val PEOPLE_SCREEN_FOLLOWER_COUNT_TAG = "people_screen_stat_followers"
const val PEOPLE_SCREEN_FOLLOWING_COUNT_TAG = "people_screen_stat_following"
const val PEOPLE_SCREEN_FOLLOW_BUTTON_TAG = "people_screen_follow_button"
const val PEOPLE_SCREEN_BLOCK_BUTTON_TAG = "people_screen_block_button"
const val PEOPLE_SCREEN_RECOMMENDATION_BLOCK_BUTTON_TAG = "people_screen_recommendation_block_button"
const val PEOPLE_SCREEN_SEARCH_BUTTON_TAG = "people_screen_search_button"
const val PEOPLE_SCREEN_ANSWER_SORT_HOT_TAG = "people_screen_answer_sort_voteups"
const val PEOPLE_SCREEN_ANSWER_SORT_TIME_TAG = "people_screen_answer_sort_created"
const val PEOPLE_SCREEN_ARTICLE_SORT_HOT_TAG = "people_screen_article_sort_voteups"
const val PEOPLE_SCREEN_ARTICLE_SORT_TIME_TAG = "people_screen_article_sort_created"
const val PEOPLE_SCREEN_OFFICIAL_BADGE_TAG = "people_screen_official_badge"

private fun peopleScreenInitialPage(person: Person): Int {
    val jumpToIndex = PEOPLE_SCREEN_TITLES.indexOf(person.jumpTo)
    return if (jumpToIndex >= 0) jumpToIndex else 0
}

internal fun peopleProfileUrl(person: Person): String {
    val identifier = person.urlToken.takeIf { it.isNotBlank() } ?: person.id
    return "https://api.zhihu.com/people/$identifier"
}

/**
 * 用户主页的测试替身配置。
 *
 * instrumentation 测试通过这里注入固定资料快照、预置 tab 内容和离线回调，避免触碰远程资料拉取或关注状态变更。
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
    val onFollowingColumnsLoadMore: (() -> Unit)? = null,
    val onFollowingTopicsLoadMore: (() -> Unit)? = null,
    val onFollowingQuestionsLoadMore: (() -> Unit)? = null,
    val onFollowingCollectionsLoadMore: (() -> Unit)? = null,
)

private fun PersonViewModel.toUiState(): PeopleScreenUiState = PeopleScreenUiState(
    profile = PeopleProfileUiState(
        avatar = avatar,
        name = name,
        headline = headline,
        officialBadge = officialBadge,
        officialBadgeDetails = officialBadgeDetails,
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
    followingColumns = PeopleListUiState(
        items = followingColumnsFeedModel.allData,
        isEnd = followingColumnsFeedModel.isEnd,
    ),
    followingTopics = PeopleListUiState(
        items = followingTopicsFeedModel.allData,
        isEnd = followingTopicsFeedModel.isEnd,
    ),
    followingQuestions = PeopleListUiState(
        items = followingQuestionsFeedModel.allData,
        isEnd = followingQuestionsFeedModel.isEnd,
    ),
    followingCollections = PeopleListUiState(
        items = followingCollectionsFeedModel.allData,
        isEnd = followingCollectionsFeedModel.isEnd,
    ),
)

/**
 * 用户主页的生产入口。
 *
 * 用户页展示资料头部、关注/屏蔽状态、回答、文章、想法、收藏等内容 tab，并支持从 `Person.jumpTo` 跳到指定子区域。
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun PeopleScreen(
    person: Person,
): Unit = PeopleScreenContent(person, testOverrides = null)

/**
 * 用户主页的测试入口。
 *
 * 与生产入口复用同一套内容布局，但允许测试注入资料状态和各 tab 的分页模型，避免 UI 测试依赖真实用户数据。
 */
@Composable
fun PeopleScreen(
    person: Person,
    testOverrides: PeopleScreenTestOverrides,
): Unit = PeopleScreenContent(person, testOverrides)

/**
 * 用户主页的实际布局实现。
 *
 * 这里统一处理资料头部、关注/屏蔽操作、顶部 tab、各类用户内容列表、分享和跳转逻辑。新增 tab 或快捷跳转时，要同步处理
 * [Person.jumpTo]、测试 override 和可访问的 tab 文案。
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun PeopleScreenContent(
    person: Person,
    testOverrides: PeopleScreenTestOverrides? = null,
) {
    val navigator = LocalNavigator.current
    val userMessages = rememberUserMessageSink()
    val paginationEnvironment = rememberPaginationEnvironment(allowGuestAccess = false)
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
            viewModel.load(paginationEnvironment)
        } catch (e: Exception) {
            userMessages.showShortMessage("加载用户信息失败: ${e.message}")
        }
    }
    LaunchedEffect(pagerState.currentPage, testOverrides) {
        if (testOverrides != null) {
            return@LaunchedEffect
        }
        try {
            viewModel.subFeedModels.getOrNull(pagerState.currentPage)?.loadMore(paginationEnvironment)
        } catch (e: Exception) {
            userMessages.showShortMessage("加载页面内容失败: ${e.message}")
        }
    }

    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val searchMemberHashId = viewModel.memberHashId
        .takeUnless { it.isBlank() || it == Person.EMPTY_ID }

    fun updateAnswersSort(newSort: String) {
        if (testOverrides != null) {
            if (uiState.answers.sortBy != newSort) {
                testUiState = uiState.copy(answers = uiState.answers.copy(sortBy = newSort))
                testOverrides.onAnswerSortChange?.invoke(newSort)
            }
            return
        }
        viewModel.answersFeedModel.changeSortBy(newSort, paginationEnvironment)
    }

    fun updateArticlesSort(newSort: String) {
        if (testOverrides != null) {
            if (uiState.articles.sortBy != newSort) {
                testUiState = uiState.copy(articles = uiState.articles.copy(sortBy = newSort))
                testOverrides.onArticleSortChange?.invoke(newSort)
            }
            return
        }
        viewModel.articlesFeedModel.changeSortBy(newSort, paginationEnvironment)
    }

    Scaffold(
        modifier = Modifier
            .testTag(PEOPLE_SCREEN_ROOT_TAG)
            .nestedScroll(scrollBehavior.nestedScrollConnection)
            .fillMaxSize(),
        topBar = {
            Box {
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
                                            viewModel.toggleFollow(paginationEnvironment)
                                        } catch (e: Exception) {
                                            userMessages.showShortMessage("操作失败: ${e.message}")
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
                                            viewModel.toggleBlock(paginationEnvironment)
                                        } catch (e: Exception) {
                                            userMessages.showShortMessage("操作失败: ${e.message}")
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
                                            viewModel.toggleRecommendationBlock(paginationEnvironment)
                                            userMessages.showShortMessage(if (viewModel.isBlockedInRecommendations) "已屏蔽推荐" else "已取消屏蔽推荐")
                                        } catch (e: Exception) {
                                            userMessages.showShortMessage("操作失败: ${e.message}")
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
                )
                if (searchMemberHashId != null) {
                    IconButton(
                        onClick = {
                            val memberName = uiState.profile.name.takeIf { it.isNotBlank() } ?: person.name
                            navigator.onNavigate(
                                SearchDestination(
                                    restrictedMemberHashId = searchMemberHashId.orEmpty(),
                                    restrictedMemberName = memberName,
                                ),
                            )
                        },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 32.dp, end = 8.dp)
                            .testTag(PEOPLE_SCREEN_SEARCH_BUTTON_TAG),
                    ) {
                        Icon(Icons.Default.Search, contentDescription = "搜索 TA 的创作")
                    }
                }
            }
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
                        modifier = Modifier.testTag("people_screen_tab_$index"),
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
                                .testTag("people_screen_page_$page"),
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
                                        ?: viewModel.answersFeedModel.loadMore(paginationEnvironment)
                                },
                                isEnd = { uiState.answers.isEnd },
                                footer = ProgressIndicatorFooter,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag(PEOPLE_SCREEN_ANSWERS_LIST_TAG),
                                key = { it.id },
                            ) {
                                FeedCard(
                                    FeedDisplayItem(
                                        title = it.question.title,
                                        summary = it.excerpt,
                                        details = "回答 · ${it.voteupCount} 赞同 · ${it.commentCount} 评论",
                                        feed = null,
                                    ),
                                    modifier = Modifier.testTag("people_screen_answer_item_${it.id}"),
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
                                .testTag("people_screen_page_$page"),
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
                                        ?: viewModel.articlesFeedModel.loadMore(paginationEnvironment)
                                },
                                isEnd = { uiState.articles.isEnd },
                                footer = ProgressIndicatorFooter,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag(PEOPLE_SCREEN_ARTICLES_LIST_TAG),
                                key = { it.id },
                            ) {
                                FeedCard(
                                    FeedDisplayItem(
                                        title = it.title,
                                        summary = it.excerpt,
                                        details = "文章 · ${it.voteupCount} 赞同 · ${it.commentCount} 评论",
                                        feed = null,
                                    ),
                                    modifier = Modifier.testTag("people_screen_article_item_${it.id}"),
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
                                    ?: viewModel.activitiesFeedModel.loadMore(paginationEnvironment)
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
                                    ?: viewModel.collectionsFeedModel.loadMore(paginationEnvironment)
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
                                itemTag = "people_screen_collection_item_${collection.id}",
                            )
                        }
                    }

                    4 -> {
                        // 提问
                        PaginatedList(
                            items = uiState.questions.items,
                            onLoadMore = {
                                testOverrides?.onQuestionsLoadMore?.invoke()
                                    ?: viewModel.questionsFeedModel.loadMore(paginationEnvironment)
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
                                itemTag = "people_screen_question_item_${question.id}",
                            )
                        }
                    }

                    5 -> {
                        // 想法
                        PaginatedList(
                            items = uiState.pins.items,
                            onLoadMore = {
                                testOverrides?.onPinsLoadMore?.invoke()
                                    ?: viewModel.pinsFeedModel.loadMore(paginationEnvironment)
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
                                itemTag = "people_screen_pin_item_${pin.id}",
                            )
                        }
                    }

                    6 -> {
                        // 专栏
                        PaginatedList(
                            items = uiState.columns.items,
                            onLoadMore = {
                                testOverrides?.onColumnsLoadMore?.invoke()
                                    ?: viewModel.columnsFeedModel.loadMore(paginationEnvironment)
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
                                itemTag = "people_screen_column_item_${column.id}",
                            )
                        }
                    }

                    7 -> {
                        // 粉丝
                        PaginatedList(
                            items = uiState.followers.items,
                            onLoadMore = {
                                testOverrides?.onFollowersLoadMore?.invoke()
                                    ?: viewModel.followersFeedModel.loadMore(paginationEnvironment)
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
                                itemTag = "people_screen_follower_item_${people.id}",
                                actionTag = "people_screen_follower_action_${people.id}",
                            )
                        }
                    }

                    8 -> {
                        // 关注
                        PaginatedList(
                            items = uiState.following.items,
                            onLoadMore = {
                                testOverrides?.onFollowingLoadMore?.invoke()
                                    ?: viewModel.followingFeedModel.loadMore(paginationEnvironment)
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
                                itemTag = "people_screen_following_item_${people.id}",
                                actionTag = "people_screen_following_action_${people.id}",
                            )
                        }
                    }

                    9 -> {
                        FollowingSubscriptionsPage(
                            uiState = uiState,
                            onLoadMore = { subscriptionPage ->
                                if (testOverrides != null) {
                                    when (subscriptionPage) {
                                        0 -> testOverrides.onFollowingColumnsLoadMore?.invoke()
                                        1 -> testOverrides.onFollowingTopicsLoadMore?.invoke()
                                        2 -> testOverrides.onFollowingQuestionsLoadMore?.invoke()
                                        3 -> testOverrides.onFollowingCollectionsLoadMore?.invoke()
                                    }
                                } else {
                                    when (subscriptionPage) {
                                        0 -> viewModel.followingColumnsFeedModel.loadMore(paginationEnvironment)
                                        1 -> viewModel.followingTopicsFeedModel.loadMore(paginationEnvironment)
                                        2 -> viewModel.followingQuestionsFeedModel.loadMore(paginationEnvironment)
                                        3 -> viewModel.followingCollectionsFeedModel.loadMore(paginationEnvironment)
                                    }
                                }
                            },
                            modifier = Modifier.testTag("people_screen_page_$page"),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FollowingSubscriptionsPage(
    uiState: PeopleScreenUiState,
    onLoadMore: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedPage by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(selectedPage) {
        onLoadMore(selectedPage)
    }

    Column(
        modifier = modifier.fillMaxSize(),
    ) {
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(PEOPLE_SCREEN_SUBSCRIPTION_TABS_TAG)
                .padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PEOPLE_SCREEN_SUBSCRIPTION_TITLES.forEachIndexed { index, title ->
                OutlinedButton(
                    onClick = { selectedPage = index },
                    modifier = Modifier.testTag("people_screen_subscription_tab_$index"),
                    shape = RoundedCornerShape(8.dp),
                    colors = if (selectedPage == index) {
                        ButtonDefaults.outlinedButtonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    } else {
                        ButtonDefaults.outlinedButtonColors()
                    },
                ) {
                    Text(title)
                }
            }
        }

        when (selectedPage) {
            0 -> PaginatedList(
                items = uiState.followingColumns.items,
                onLoadMore = { onLoadMore(0) },
                isEnd = { uiState.followingColumns.isEnd },
                footer = ProgressIndicatorFooter,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(PEOPLE_SCREEN_SUBSCRIPTIONS_LIST_TAG),
                key = { it.id },
            ) { column ->
                ColumnListItem(
                    column = column,
                    itemTag = "people_screen_column_item_${column.id}",
                )
            }

            1 -> PaginatedList(
                items = uiState.followingTopics.items,
                onLoadMore = { onLoadMore(1) },
                isEnd = { uiState.followingTopics.isEnd },
                footer = ProgressIndicatorFooter,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(PEOPLE_SCREEN_SUBSCRIPTIONS_LIST_TAG),
                key = { it.displayId },
            ) { topic ->
                FollowedTopicListItem(topic)
            }

            2 -> PaginatedList(
                items = uiState.followingQuestions.items,
                onLoadMore = { onLoadMore(2) },
                isEnd = { uiState.followingQuestions.isEnd },
                footer = ProgressIndicatorFooter,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(PEOPLE_SCREEN_SUBSCRIPTIONS_LIST_TAG),
                key = { it.id },
            ) { question ->
                FollowedQuestionListItem(question)
            }

            3 -> PaginatedList(
                items = uiState.followingCollections.items,
                onLoadMore = { onLoadMore(3) },
                isEnd = { uiState.followingCollections.isEnd },
                footer = ProgressIndicatorFooter,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag(PEOPLE_SCREEN_SUBSCRIPTIONS_LIST_TAG),
                key = { it.id },
            ) { collection ->
                CollectionListItem(
                    collection = collection,
                    itemTag = "people_screen_collection_item_${collection.id}",
                )
            }
        }
    }
}

@Composable
private fun CollectionListItem(
    collection: DataHolder.Collection,
    itemTag: String? = null,
) {
    val navigator = LocalNavigator.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (itemTag != null) Modifier.testTag(itemTag) else Modifier)
            .clickable {
                navigator.onNavigate(CollectionContent(collection.id))
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
        val text = remember { Ksoup.parse(pin.excerptTitle).text() }
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
    val openZhihuWebUrl = rememberZhihuWebUrlOpener()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (itemTag != null) Modifier.testTag(itemTag) else Modifier)
            .clickable {
                openZhihuWebUrl(column.webUrl())
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
                text = "${column.articlesCount} 文章 · ${column.followerCount.coerceAtLeast(column.followers)} 关注",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun FollowedQuestionListItem(question: FollowedQuestion) {
    val navigator = LocalNavigator.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("people_screen_followed_question_item_${question.id}")
            .clickable {
                question.id.toLongOrNull()?.let {
                    navigator.onNavigate(Question(it, question.title))
                }
            }.padding(vertical = 8.dp, horizontal = 4.dp),
    ) {
        Text(
            text = question.title,
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun FollowedTopicListItem(topic: FollowedTopic) {
    val openZhihuWebUrl = rememberZhihuWebUrlOpener()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("people_screen_followed_topic_item_${topic.displayId}")
            .clickable {
                openZhihuWebUrl("https://www.zhihu.com/topic/${topic.displayId}")
            }.padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = topic.displayAvatarUrl,
            contentDescription = "话题头像",
            modifier = Modifier
                .padding(end = 12.dp)
                .size(40.dp)
                .clip(CircleShape),
        )
        Text(
            text = topic.displayName,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun DataHolder.Column.webUrl(): String = when {
    url.contains("/api/v4/columns/") ->
        url
            .replace("http://", "https://")
            .replace("/api/v4/columns/", "/column/")

    url.startsWith("http") && !url.contains("/api/") -> url.replace("http://", "https://")
    else -> "https://www.zhihu.com/column/$id"
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = people.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                val officialBadge = people.badgeV2.officialBadge()
                if (officialBadge?.isUsefulInList == true) {
                    AuthorBadge(
                        badge = officialBadge,
                        compact = true,
                        modifier = Modifier.padding(start = 6.dp),
                    )
                }
            }
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
private fun OfficialBadgeDetails(
    badges: List<OfficialBadge>,
    modifier: Modifier = Modifier,
) {
    if (badges.isEmpty()) return
    Column(modifier = modifier) {
        badges.forEach { badge ->
            Row(
                modifier = Modifier.padding(top = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (badge.iconUrl.isNotBlank()) {
                    if (badge.iconUrl == DataHolder.ZH_PLUS_AUTHOR_BADGE_ICON) {
                        Image(
                            painter = painterResource(Res.drawable.ic_zh_plus_author_badge),
                            contentDescription = badge.description,
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .size(18.dp),
                        )
                    } else {
                        AsyncImage(
                            model = badge.iconUrl,
                            contentDescription = badge.description,
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .size(18.dp),
                        )
                    }
                }
                Text(
                    text = "${badge.peopleDetailTitle}: ${badge.description}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private val OfficialBadge.peopleDetailTitle: String
    get() = when {
        title == "认证" || title == "已认证的个人" -> "认证信息"
        else -> title
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
    val openImagePreview = rememberImagePreviewOpener()
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
                        openImagePreview(profile.avatar.substringBefore("_") + ".jpg")
                    },
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        profile.name,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (profile.officialBadge != null) {
                        AuthorBadge(
                            badge = profile.officialBadge,
                            modifier = Modifier
                                .padding(start = 6.dp)
                                .testTag(PEOPLE_SCREEN_OFFICIAL_BADGE_TAG),
                        )
                    }
                }
                Text(
                    profile.headline,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                OfficialBadgeDetails(
                    badges = profile.officialBadgeDetails,
                    modifier = Modifier.padding(top = 6.dp),
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

data class PeopleProfileUiState(
    val avatar: String = "",
    val name: String = "",
    val headline: String = "",
    val officialBadge: OfficialBadge? = null,
    val officialBadgeDetails: List<OfficialBadge> = emptyList(),
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
    val activities: PeopleListUiState<FeedDisplayItem> = PeopleListUiState(),
    val collections: PeopleListUiState<DataHolder.Collection> = PeopleListUiState(),
    val questions: PeopleListUiState<DataHolder.Question> = PeopleListUiState(),
    val pins: PeopleListUiState<DataHolder.Pin> = PeopleListUiState(),
    val columns: PeopleListUiState<DataHolder.Column> = PeopleListUiState(),
    val followers: PeopleListUiState<DataHolder.People> = PeopleListUiState(),
    val following: PeopleListUiState<DataHolder.People> = PeopleListUiState(),
    val followingColumns: PeopleListUiState<DataHolder.Column> = PeopleListUiState(),
    val followingTopics: PeopleListUiState<FollowedTopic> = PeopleListUiState(),
    val followingQuestions: PeopleListUiState<FollowedQuestion> = PeopleListUiState(),
    val followingCollections: PeopleListUiState<DataHolder.Collection> = PeopleListUiState(),
)
