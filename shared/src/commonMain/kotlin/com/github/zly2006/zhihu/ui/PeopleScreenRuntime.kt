@file:Suppress("ktlint:standard:property-naming")

package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.data.officialBadge
import com.github.zly2006.zhihu.shared.data.officialBadgeDetails
import com.github.zly2006.zhihu.shared.people.PeopleProfileUiState

data class PeopleProfileLoadResult(
    val profile: PeopleProfileUiState,
    val urlToken: String?,
)

data class PeopleScreenRuntime(
    val openWebUrl: (String) -> Unit,
    val openImage: (String) -> Unit,
)

internal const val peopleProfileIncludePath =
    "allow_message,is_followed,is_following,is_org,is_blocking,badge_v2,answer_count,follower_count,following_count,articles_count,question_count,pins_count"

internal fun toPeopleProfileLoadResult(
    loadedPerson: DataHolder.People,
    isBlockedInRecommendations: Boolean,
): PeopleProfileLoadResult = PeopleProfileLoadResult(
    profile = PeopleProfileUiState(
        avatar = loadedPerson.avatarUrl,
        name = loadedPerson.name,
        headline = loadedPerson.headline,
        officialBadge = loadedPerson.badgeV2.officialBadge(),
        officialBadgeDetails = loadedPerson.badgeV2.officialBadgeDetails(),
        followerCount = loadedPerson.followerCount,
        followingCount = loadedPerson.followingCount,
        answerCount = loadedPerson.answerCount,
        articleCount = loadedPerson.articlesCount,
        isFollowing = loadedPerson.isFollowing,
        isBlocking = loadedPerson.isBlocking,
        isBlockedInRecommendations = isBlockedInRecommendations,
    ),
    urlToken = loadedPerson.urlToken,
)

@Composable
expect fun rememberPeopleScreenRuntime(): PeopleScreenRuntime
