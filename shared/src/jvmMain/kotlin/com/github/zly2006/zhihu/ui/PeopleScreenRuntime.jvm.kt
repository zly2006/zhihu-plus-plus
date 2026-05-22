package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable
import com.github.zly2006.zhihu.shared.people.PeopleProfileUiState

@Composable
actual fun rememberPeopleScreenRuntime(): PeopleScreenRuntime = PeopleScreenRuntime(
    loadProfile = { person ->
        PeopleProfileLoadResult(
            profile = PeopleProfileUiState(
                name = person.name,
            ),
            urlToken = person.urlToken.takeIf { it.isNotBlank() },
        )
    },
    toggleFollow = { _, isFollowing, followerCount ->
        PeopleFollowResult(
            isFollowing = isFollowing,
            followerCount = followerCount,
        )
    },
    toggleBlock = { _, isBlocking -> isBlocking },
    toggleRecommendationBlock = { request -> request.isBlocked },
    showShortMessage = {},
    openWebUrl = {},
    openImage = {},
)
