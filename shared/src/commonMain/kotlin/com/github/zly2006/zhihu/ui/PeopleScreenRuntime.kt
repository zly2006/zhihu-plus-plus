package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable
import com.github.zly2006.zhihu.navigation.Person
import com.github.zly2006.zhihu.shared.people.PeopleProfileUiState

data class PeopleProfileLoadResult(
    val profile: PeopleProfileUiState,
    val urlToken: String?,
)

data class PeopleFollowResult(
    val isFollowing: Boolean,
    val followerCount: Int,
)

data class PeopleRecommendationBlockRequest(
    val userId: String,
    val userName: String,
    val urlToken: String,
    val avatarUrl: String,
    val isBlocked: Boolean,
)

data class PeopleScreenRuntime(
    val loadProfile: suspend (Person) -> PeopleProfileLoadResult,
    val toggleFollow: suspend (Person, Boolean, Int) -> PeopleFollowResult,
    val toggleBlock: suspend (Person, Boolean) -> Boolean,
    val toggleRecommendationBlock: suspend (PeopleRecommendationBlockRequest) -> Boolean,
    val showShortMessage: (String) -> Unit,
    val openWebUrl: (String) -> Unit,
    val openImage: (String) -> Unit,
)

@Composable
expect fun rememberPeopleScreenRuntime(): PeopleScreenRuntime
