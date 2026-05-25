package com.github.zly2006.zhihu.ui

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.navigation.Person
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.data.officialBadge
import com.github.zly2006.zhihu.shared.data.officialBadgeDetails
import com.github.zly2006.zhihu.shared.people.PeopleProfileUiState
import com.github.zly2006.zhihu.shared.platform.androidUserMessageSink
import com.github.zly2006.zhihu.shared.util.raiseForStatus
import com.github.zly2006.zhihu.ui.components.OpenImageDialog
import com.github.zly2006.zhihu.util.signFetchRequest
import com.github.zly2006.zhihu.viewmodel.filter.getBlocklistManager
import io.ktor.client.call.body
import io.ktor.client.request.delete
import io.ktor.client.request.post
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonPrimitive

private const val WEBVIEW_ACTIVITY_CLASS = "com.github.zly2006.zhihu.WebviewActivity"

@Composable
actual fun rememberPeopleScreenRuntime(): PeopleScreenRuntime {
    val context = LocalContext.current
    return remember(context) {
        val userMessages = androidUserMessageSink(context)
        PeopleScreenRuntime(
            loadProfile = { person ->
                AccountData.addReadHistory(context, person.id, "profile")
                val jojo = AccountData.fetchGet(context, peopleProfileUrl(person)) {
                    url {
                        parameters["include"] = "allow_message,is_followed,is_following,is_org,is_blocking,badge_v2,answer_count,follower_count,following_count,articles_count,question_count,pins_count"
                    }
                    signFetchRequest()
                }!!
                val loadedPerson = AccountData.decodeJson<DataHolder.People>(jojo)
                val blocklistManager = getBlocklistManager(context)
                (context as? ArticleHost)?.postHistoryDestination(
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
                val client = AccountData.httpClient(context)
                if (isFollowing) {
                    val jojo = client
                        .delete("https://www.zhihu.com/api/v4/members/${person.urlToken}/followers") {
                            signFetchRequest()
                        }.raiseForStatus()
                        .body<JsonObject>()
                    PeopleFollowResult(
                        isFollowing = false,
                        followerCount = jojo["follower_count"]?.jsonPrimitive?.int ?: (followerCount - 1),
                    )
                } else {
                    val jojo = client
                        .post("https://www.zhihu.com/api/v4/members/${person.urlToken}/followers") {
                            signFetchRequest()
                        }.raiseForStatus()
                        .body<JsonObject>()
                    PeopleFollowResult(
                        isFollowing = true,
                        followerCount = jojo["follower_count"]?.jsonPrimitive?.int ?: (followerCount + 1),
                    )
                }
            },
            toggleBlock = { person, isBlocking ->
                val client = AccountData.httpClient(context)
                if (isBlocking) {
                    client
                        .delete("https://www.zhihu.com/api/v4/members/${person.urlToken}/actions/block") {
                            signFetchRequest()
                        }.raiseForStatus()
                    false
                } else {
                    client
                        .post("https://www.zhihu.com/api/v4/members/${person.urlToken}/actions/block") {
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
