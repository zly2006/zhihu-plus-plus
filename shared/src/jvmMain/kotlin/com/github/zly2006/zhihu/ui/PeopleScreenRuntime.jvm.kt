package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.github.zly2006.zhihu.navigation.Person
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.data.ZhihuJson
import com.github.zly2006.zhihu.shared.data.officialBadge
import com.github.zly2006.zhihu.shared.data.officialBadgeDetails
import com.github.zly2006.zhihu.shared.desktop.DesktopAccountStore
import com.github.zly2006.zhihu.shared.desktop.DesktopHistoryStorage
import com.github.zly2006.zhihu.shared.people.PeopleProfileUiState
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.shared.util.raiseForStatus
import com.github.zly2006.zhihu.shared.util.signZhihuFetchRequest
import com.github.zly2006.zhihu.viewmodel.filter.createBlocklistManager
import com.github.zly2006.zhihu.viewmodel.filter.getContentFilterDatabase
import io.ktor.client.call.body
import io.ktor.client.request.parameter
import io.ktor.http.HttpMethod
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive
import java.awt.Desktop
import java.io.File
import java.net.URI

@Composable
actual fun rememberPeopleScreenRuntime(): PeopleScreenRuntime {
    val userMessages = rememberUserMessageSink()
    return remember(userMessages) {
        val store = DesktopAccountStore()
        val historyStorage = DesktopHistoryStorage()
        val databaseFile = File(System.getProperty("user.home"), ".zhihu-plus/content-filter.db")
        databaseFile.parentFile?.mkdirs()
        val database = getContentFilterDatabase(databaseFile)
        val blocklistManager = database.createBlocklistManager()
        PeopleScreenRuntime(
            loadProfile = { person ->
                addDesktopReadHistory(store, person.id, "profile")
                val account = store.load()
                val jojo = store.fetchAuthenticatedJson(peopleProfileUrl(person)) {
                    parameter(
                        "include",
                        "allow_message,is_followed,is_following,is_org,is_blocking,badge_v2,answer_count,follower_count,following_count,articles_count,question_count,pins_count",
                    )
                    account.cookies["d_c0"]?.let { dc0 -> signZhihuFetchRequest(dc0 = dc0) }
                } ?: error("Empty people profile response")
                val loadedPerson = ZhihuJson.decodeJson<DataHolder.People>(jojo)
                historyStorage.add(
                    Person(
                        id = loadedPerson.id,
                        name = loadedPerson.name,
                        urlToken = loadedPerson.urlToken ?: "",
                    ),
                )
                PeopleProfileLoadResult(
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
                        isBlockedInRecommendations = blocklistManager.isUserBlocked(loadedPerson.id),
                    ),
                    urlToken = loadedPerson.urlToken,
                )
            },
            toggleFollow = { person, isFollowing, followerCount ->
                val account = store.load()
                if (isFollowing) {
                    val jojo = store.withAuthenticatedResponse(
                        url = "https://www.zhihu.com/api/v4/members/${person.urlToken}/followers",
                        block = {
                            method = HttpMethod.Delete
                            account.cookies["d_c0"]?.let { dc0 -> signZhihuFetchRequest(dc0 = dc0) }
                        },
                    ) { response ->
                        response.raiseForStatus().body<JsonObject>()
                    }
                    PeopleFollowResult(
                        isFollowing = false,
                        followerCount = jojo["follower_count"]?.jsonPrimitive?.int ?: (followerCount - 1),
                    )
                } else {
                    val jojo = store.withAuthenticatedResponse(
                        url = "https://www.zhihu.com/api/v4/members/${person.urlToken}/followers",
                        block = {
                            method = HttpMethod.Post
                            account.cookies["d_c0"]?.let { dc0 -> signZhihuFetchRequest(dc0 = dc0) }
                        },
                    ) { response ->
                        response.raiseForStatus().body<JsonObject>()
                    }
                    PeopleFollowResult(
                        isFollowing = true,
                        followerCount = jojo["follower_count"]?.jsonPrimitive?.int ?: (followerCount + 1),
                    )
                }
            },
            toggleBlock = { person, isBlocking ->
                val account = store.load()
                if (isBlocking) {
                    store.withAuthenticatedResponse(
                        url = "https://www.zhihu.com/api/v4/members/${person.urlToken}/actions/block",
                        block = {
                            method = HttpMethod.Delete
                            account.cookies["d_c0"]?.let { dc0 -> signZhihuFetchRequest(dc0 = dc0) }
                        },
                    ) { response ->
                        response.raiseForStatus()
                    }
                    false
                } else {
                    store.withAuthenticatedResponse(
                        url = "https://www.zhihu.com/api/v4/members/${person.urlToken}/actions/block",
                        block = {
                            method = HttpMethod.Post
                            account.cookies["d_c0"]?.let { dc0 -> signZhihuFetchRequest(dc0 = dc0) }
                        },
                    ) { response ->
                        response.raiseForStatus()
                    }
                    true
                }
            },
            toggleRecommendationBlock = { request ->
                if (request.isBlocked) {
                    blocklistManager.removeBlockedUser(request.userId)
                    false
                } else {
                    blocklistManager.addBlockedUser(
                        userId = request.userId,
                        userName = request.userName,
                        urlToken = request.urlToken,
                        avatarUrl = request.avatarUrl,
                    )
                    true
                }
            },
            showShortMessage = { message ->
                userMessages.showShortMessage(message)
            },
            openWebUrl = { url ->
                openDesktopUri(url)
            },
            openImage = { url ->
                openDesktopUri(url)
            },
        )
    }
}

private suspend fun addDesktopReadHistory(
    store: DesktopAccountStore,
    contentId: String,
    contentType: String,
) {
    runCatching {
        store.addReadHistory(
            contentToken = contentId,
            contentTypeName = contentType,
        )
    }
}

private fun openDesktopUri(url: String) {
    runCatching {
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(URI(url))
        }
    }
}
