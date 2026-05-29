package com.github.zly2006.zhihu.ui

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.navigation.Person
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.data.ZhihuJson
import com.github.zly2006.zhihu.shared.platform.androidUserMessageSink
import com.github.zly2006.zhihu.shared.util.raiseForStatus
import com.github.zly2006.zhihu.ui.components.OpenImageDialog
import com.github.zly2006.zhihu.util.signFetchRequest
import com.github.zly2006.zhihu.viewmodel.filter.getBlocklistManager
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.post
import kotlinx.serialization.json.JsonObject

private const val WEBVIEW_ACTIVITY_CLASS = "com.github.zly2006.zhihu.WebviewActivity"

@Composable
actual fun rememberPeopleScreenRuntime(): PeopleScreenRuntime {
    val context = LocalContext.current
    return remember(context) {
        val userMessages = androidUserMessageSink(context)
        PeopleScreenRuntime(
            loadProfile = { person ->
                AccountData.addReadHistory(context, person.id, "profile")
                val jojo = AccountData.signedFetchGet(context, peopleProfileUrl(person)) {
                    url {
                        parameters["include"] = peopleProfileIncludePath
                    }
                }!!
                val loadedPerson = ZhihuJson.decodeJson<DataHolder.People>(jojo)
                val blocklistManager = getBlocklistManager(context)
                (context as? ArticleHost)?.postHistoryDestination(
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
                if (isFollowing) {
                    val jojo = AccountData.httpClient(context)
                        .delete(peopleFollowersUrl(person)) {
                            signFetchRequest()
                        }.raiseForStatus().body<JsonObject>()
                    peopleFollowResult(
                        isFollowingBefore = isFollowing,
                        followerCountBefore = followerCount,
                        responseJson = jojo,
                    )
                } else {
                    val jojo = AccountData.httpClient(context)
                        .post(peopleFollowersUrl(person)) {
                            signFetchRequest()
                        }.raiseForStatus().body<JsonObject>()
                    peopleFollowResult(
                        isFollowingBefore = isFollowing,
                        followerCountBefore = followerCount,
                        responseJson = jojo,
                    )
                }
            },
            toggleBlock = { person, isBlocking ->
                if (isBlocking) {
                    AccountData.httpClient(context)
                        .delete(peopleBlockUrl(person)) {
                            signFetchRequest()
                        }.raiseForStatus()
                    false
                } else {
                    AccountData.httpClient(context)
                        .post(peopleBlockUrl(person)) {
                            signFetchRequest()
                        }.raiseForStatus()
                    true
                }
            },
            toggleRecommendationBlock = { request ->
                val blocklistManager = getBlocklistManager(context)
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
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, url.toUri()).setClassName(context, WEBVIEW_ACTIVITY_CLASS),
                )
            },
            openImage = { url ->
                OpenImageDialog(
                    context,
                    AccountData.httpClient(context),
                    url,
                ).show()
            },
        )
    }
}
