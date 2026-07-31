/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
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

package com.chloemlla.zhplus.ui

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
import coil3.compose.AsyncImage
import com.fleeksoft.ksoup.Ksoup
import com.chloemlla.zhplus.navigation.Article
import com.chloemlla.zhplus.navigation.ArticleType
import com.chloemlla.zhplus.navigation.CollectionContent
import com.chloemlla.zhplus.navigation.LocalNavigator
import com.chloemlla.zhplus.navigation.Person
import com.chloemlla.zhplus.navigation.Pin
import com.chloemlla.zhplus.navigation.Question
import com.chloemlla.zhplus.shared.data.DataHolder
import com.chloemlla.zhplus.shared.data.FeedDisplayItem
import com.chloemlla.zhplus.shared.data.OfficialBadge
import com.chloemlla.zhplus.shared.data.ZhihuJson
import com.chloemlla.zhplus.shared.data.officialBadge
import com.chloemlla.zhplus.shared.data.officialBadgeDetails
import com.chloemlla.zhplus.shared.platform.rememberExternalUrlOpener
import com.chloemlla.zhplus.shared.platform.rememberImagePreviewOpener
import com.chloemlla.zhplus.shared.platform.rememberUserMessageSink
import com.chloemlla.zhplus.shared.platform.rememberZhihuWebUrlOpener
import com.chloemlla.zhplus.shared.util.Log
import com.chloemlla.zhplus.shared.util.raiseForStatus
import com.chloemlla.zhplus.ui.components.AuthorBadge
import com.chloemlla.zhplus.ui.components.FeedCard
import com.chloemlla.zhplus.ui.components.PaginatedList
import com.chloemlla.zhplus.ui.components.ProgressIndicatorFooter
import com.chloemlla.zhplus.viewmodel.ContentBlocklistEnvironment
import com.chloemlla.zhplus.viewmodel.PaginationEnvironment
import com.chloemlla.zhplus.viewmodel.PaginationViewModel
import com.chloemlla.zhplus.viewmodel.ProfileLoadEnvironment
import com.chloemlla.zhplus.viewmodel.ZhihuApiEnvironment
import com.chloemlla.zhplus.viewmodel.addReadHistory
import com.chloemlla.zhplus.viewmodel.deleteSigned
import com.chloemlla.zhplus.viewmodel.feed.BaseFeedViewModel
import com.chloemlla.zhplus.viewmodel.postSigned
import com.chloemlla.zhplus.viewmodel.rememberPaginationEnvironment
import io.ktor.client.call.body
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import org.jetbrains.compose.resources.painterResource
import zhihu.shared.generated.resources.Res
import zhihu.shared.generated.resources.ic_zh_plus_author_badge
import kotlin.reflect.typeOf
import androidx.lifecycle.viewmodel.compose.viewModel as composeViewModel
import com.chloemlla.zhplus.navigation.Search as SearchDestination

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

    fun updateSortBy(newSort: String): Boolean {
        if (sortBy == newSort) {
            return false
        }
        sortBy = newSort
        return true
    }

    fun changeSortBy(newSort: String, environment: PaginationEnvironment) {
        if (updateSortBy(newSort)) {
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

    fun updateSortBy(newSort: String): Boolean {
        if (sortBy == newSort) {
            return false
        }
        sortBy = newSort
        return true
    }

    fun changeSortBy(newSort: String, environment: PaginationEnvironment) {
        if (updateSortBy(newSort)) {
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
    var githubSocial by mutableStateOf<GithubSocialUiState?>(null)
    var followerCount by mutableIntStateOf(0)
    var followingCount by mutableIntStateOf(0)
    var answerCount by mutableIntStateOf(0)
    var articleCount by mutableIntStateOf(0)
    var isFollowing by mutableStateOf(false)
    var isBlocking by mutableStateOf(false)
    var isBlockedInRecommendations by mutableStateOf(false)
    var isBlockedAsQuestionAuthor by mutableStateOf(false)
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

    suspend fun toggleQuestionAuthorBlock(environment: ContentBlocklistEnvironment) {
        if (isBlockedAsQuestionAuthor) {
            environment.removeBlockedQuestionAuthor(person.id)
            isBlockedAsQuestionAuthor = false
        } else {
            environment.addBlockedQuestionAuthor(
                userId = person.id,
                userName = name,
                urlToken = person.urlToken,
                avatarUrl = avatar,
            )
            isBlockedAsQuestionAuthor = true
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

        this.avatar = loadedPerson.avatarUrl
        this.name = loadedPerson.name
        this.headline = loadedPerson.headline
        this.officialBadge = loadedPerson.badgeV2.officialBadge()
        this.officialBadgeDetails = loadedPerson.badgeV2.officialBadgeDetails()
        this.followerCount = loadedPerson.followerCount
        this.followingCount = loadedPerson.followingCount
        this.answerCount = loadedPerson.answerCount
        this.articleCount = loadedPerson.articlesCount
        this.isFollowing = loadedPerson.isFollowing
        this.isBlocking = loadedPerson.isBlocking
        this.isBlockedInRecommendations = environment.isUserBlocked(loadedPerson.id)
        this.isBlockedAsQuestionAuthor = environment.isQuestionAuthorBlocked(loadedPerson.id)
        this.memberHashId = loadedPerson.id
        this.person.id = loadedPerson.id
        if (urlToken != null) {
            this.person.urlToken = urlToken
        }

        this.githubSocial = try {
            environment
                .fetchJson("${peopleProfileUrl(person)}/profile/detail", "")
                ?.let { ZhihuJson.decodeJson<DataHolder.People>(it).githubSocialUiState() }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.e("PersonViewModel", "Failed to load optional social media profile detail", error)
            null
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
const val PEOPLE_SCREEN_ANSWERS_LIST_TAG =…7076 tokens truncated…  .fillMaxWidth()
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

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
private fun UserInfoHeader(
    viewModel: PersonViewModel,
    pagerState: PagerState,
    modifier: Modifier = Modifier,
    onFollowToggle: () -> Unit,
    onBlockToggle: () -> Unit,
    onRecommendationBlockToggle: () -> Unit,
    onQuestionAuthorBlockToggle: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val openImagePreview = rememberImagePreviewOpener()
    val openExternalUrl = rememberExternalUrlOpener()
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
                    .testTag(PEOPLE_SCREEN_AVATAR_TAG)
                    .padding(end = 16.dp)
                    .size(80.dp)
                    .clip(CircleShape)
                    .clickable {
                        openImagePreview(viewModel.avatar.substringBefore("_") + ".jpg")
                    },
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        viewModel.name,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    viewModel.officialBadge?.let { badge ->
                        AuthorBadge(
                            badge = badge,
                            modifier = Modifier
                                .padding(start = 6.dp)
                                .testTag(PEOPLE_SCREEN_OFFICIAL_BADGE_TAG),
                        )
                    }
                }
                Text(
                    viewModel.headline,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                OfficialBadgeDetails(
                    badges = viewModel.officialBadgeDetails,
                    modifier = Modifier.padding(top = 6.dp),
                )
                viewModel.githubSocial?.let { githubSocial ->
                    Row(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .testTag(PEOPLE_SCREEN_GITHUB_STARS_TAG)
                            .clickable { openExternalUrl(githubSocial.profileUrl) },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        githubSocial.iconUrl?.let { iconUrl ->
                            AsyncImage(
                                model = iconUrl,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        Text(
                            text = githubSocial.title,
                            modifier = Modifier.weight(1f, fill = false),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "· ${githubSocial.starCount} stars",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }
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
            }, tag = PEOPLE_SCREEN_ANSWER_COUNT_TAG)
            StatItem("文章", viewModel.articleCount, onClick = {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(1)
                }
            }, tag = PEOPLE_SCREEN_ARTICLE_COUNT_TAG)
            StatItem("粉丝", viewModel.followerCount, onClick = {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(7)
                }
            }, tag = PEOPLE_SCREEN_FOLLOWER_COUNT_TAG)
            StatItem("关注", viewModel.followingCount, onClick = {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(8)
                }
            }, tag = PEOPLE_SCREEN_FOLLOWING_COUNT_TAG)
        }
        FlowRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            OutlinedButton(
                onClick = onFollowToggle,
                modifier = Modifier.testTag(PEOPLE_SCREEN_FOLLOW_BUTTON_TAG),
            ) {
                Text(if (viewModel.isFollowing) "取消关注" else "关注")
            }
            OutlinedButton(
                onClick = onBlockToggle,
                modifier = Modifier.testTag(PEOPLE_SCREEN_BLOCK_BUTTON_TAG),
            ) {
                Text(if (viewModel.isBlocking) "取消拉黑" else "拉黑")
            }
            OutlinedButton(
                onClick = onRecommendationBlockToggle,
                modifier = Modifier.testTag(PEOPLE_SCREEN_RECOMMENDATION_BLOCK_BUTTON_TAG),
            ) {
                Text(if (viewModel.isBlockedInRecommendations) "取消屏蔽推荐" else "屏蔽推荐")
            }
            OutlinedButton(
                onClick = onQuestionAuthorBlockToggle,
                modifier = Modifier.testTag(PEOPLE_SCREEN_QUESTION_AUTHOR_BLOCK_BUTTON_TAG),
            ) {
                Text(if (viewModel.isBlockedAsQuestionAuthor) "取消屏蔽其提问" else "屏蔽其提问")
            }
        }
    }
}
