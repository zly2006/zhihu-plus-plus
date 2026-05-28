package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.github.zly2006.zhihu.navigation.Person
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.data.ZhihuJson
import com.github.zly2006.zhihu.shared.desktop.DesktopAccountStore
import com.github.zly2006.zhihu.shared.desktop.DesktopHistoryStorage
import com.github.zly2006.zhihu.shared.desktop.openDesktopExternalUrl
import com.github.zly2006.zhihu.shared.desktop.signDesktopRequest
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.shared.util.raiseForStatus
import com.github.zly2006.zhihu.viewmodel.filter.createBlocklistManager
import com.github.zly2006.zhihu.viewmodel.filter.desktopContentFilterDatabaseFile
import com.github.zly2006.zhihu.viewmodel.filter.getContentFilterDatabase
import io.ktor.client.call.body
import io.ktor.client.request.parameter
import io.ktor.http.HttpMethod
import kotlinx.serialization.json.JsonObject

@Composable
actual fun rememberPeopleScreenRuntime(): PeopleScreenRuntime {
    val userMessages = rememberUserMessageSink()
    return remember(userMessages) {
        val store = DesktopAccountStore()
        val historyStorage = DesktopHistoryStorage()
        val databaseFile = desktopContentFilterDatabaseFile()
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
                        peopleProfileIncludePath,
                    )
                    signDesktopRequest(account.cookies)
                } ?: error("Empty people profile response")
                val loadedPerson = ZhihuJson.decodeJson<DataHolder.People>(jojo)
                historyStorage.add(
                    Person(
                        id = loadedPerson.id,
                        name = loadedPerson.name,
                        urlToken = loadedPerson.urlToken ?: "",
                    ),
                )
                toPeopleProfileLoadResult(
                    loadedPerson = loadedPerson,
                    isBlockedInRecommendations = blocklistManager.isUserBlocked(loadedPerson.id),
                )
            },
            toggleFollow = { person, isFollowing, followerCount ->
                val account = store.load()
                if (isFollowing) {
                    val jojo = store.withAuthenticatedResponse(
                        url = peopleFollowersUrl(person),
                        block = {
                            method = HttpMethod.Delete
                            signDesktopRequest(account.cookies)
                        },
                    ) { response ->
                        response.raiseForStatus().body<JsonObject>()
                    }
                    peopleFollowResult(
                        isFollowingBefore = isFollowing,
                        followerCountBefore = followerCount,
                        responseJson = jojo,
                    )
                } else {
                    val jojo = store.withAuthenticatedResponse(
                        url = peopleFollowersUrl(person),
                        block = {
                            method = HttpMethod.Post
                            signDesktopRequest(account.cookies)
                        },
                    ) { response ->
                        response.raiseForStatus().body<JsonObject>()
                    }
                    peopleFollowResult(
                        isFollowingBefore = isFollowing,
                        followerCountBefore = followerCount,
                        responseJson = jojo,
                    )
                }
            },
            toggleBlock = { person, isBlocking ->
                val account = store.load()
                if (isBlocking) {
                    store.withAuthenticatedResponse(
                        url = peopleBlockUrl(person),
                        block = {
                            method = HttpMethod.Delete
                            signDesktopRequest(account.cookies)
                        },
                    ) { response ->
                        response.raiseForStatus()
                    }
                    false
                } else {
                    store.withAuthenticatedResponse(
                        url = peopleBlockUrl(person),
                        block = {
                            method = HttpMethod.Post
                            signDesktopRequest(account.cookies)
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
        openDesktopExternalUrl(url)
    }
}
