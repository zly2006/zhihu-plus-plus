package com.github.zly2006.zhihu.shared.people

import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.data.FeedDisplayItem
import com.github.zly2006.zhihu.shared.data.OfficialBadge
import com.github.zly2006.zhihu.ui.FollowedQuestion
import com.github.zly2006.zhihu.ui.FollowedTopic

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
