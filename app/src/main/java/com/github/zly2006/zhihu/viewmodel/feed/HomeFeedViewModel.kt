package com.github.zly2006.zhihu.viewmodel.feed

import android.content.Context
import android.util.Log
import com.github.zly2006.zhihu.MainActivity
import com.github.zly2006.zhihu.checkForAd
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.data.target
import com.github.zly2006.zhihu.ui.IHomeFeedViewModel
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import com.github.zly2006.zhihu.util.signFetchRequest
import com.github.zly2006.zhihu.viewmodel.filter.ContentFilterExtensions
import com.github.zly2006.zhihu.viewmodel.filter.ContentType
import io.ktor.client.HttpClient
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray

class HomeFeedViewModel :
    BaseFeedViewModel(),
    IHomeFeedViewModel {
    override val initialUrl: String
//        get() = "https://www.zhihu.com/api/v3/feed/topstory/recommend?desktop=true&limit=10"
        get() = "https://api.zhihu.com/topstory/recommend"

    init {
        allowGuestAccess = true
    }

    public override suspend fun fetchFeeds(context: Context) {
        val httpClient = AccountData.httpClient(context)
        markItemsAsTouched(context, httpClient)
        super.fetchFeeds(context)
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun processResponse(context: Context, data: List<Feed>, rawData: JsonArray) {
        isPullToRefresh = false
        allData.addAll(data)
        debugData.addAll(rawData)

        GlobalScope.launch(Dispatchers.Main) {
            val filteredData = applyContentFilter(context, data)
            recordContentDisplays(context, filteredData)

            filteredData
                .flatten()
                .map { feed ->
                    coroutineScope {
                        launch(Dispatchers.Main) {
                            if (feed.target?.navDestination != null &&
                                !checkForAd(feed.target!!.navDestination!!, context as MainActivity) &&
                                displayItems.none { it.navDestination == feed.target?.navDestination }
                            ) {
                                displayItems.add(createDisplayItem(feed))
                            }
                        }
                    }
                }.joinAll()
        }
    }

    private suspend fun recordContentDisplays(context: Context, feeds: List<Feed>) {
        withContext(Dispatchers.IO) {
            try {
                feeds.flatten().forEach { feed ->
                    when (val target = feed.target) {
                        is Feed.AnswerTarget -> {
                            ContentFilterExtensions.recordContentDisplay(
                                context,
                                ContentType.ANSWER,
                                target.id.toString(),
                            )
                        }
                        is Feed.ArticleTarget -> {
                            ContentFilterExtensions.recordContentDisplay(
                                context,
                                ContentType.ARTICLE,
                                target.id.toString(),
                            )
                        }
                        is Feed.QuestionTarget -> {
                            ContentFilterExtensions.recordContentDisplay(
                                context,
                                ContentType.QUESTION,
                                target.id.toString(),
                            )
                        }
                        else -> {
                            // 其他类型暂不处理
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("HomeFeedViewModel", "Failed to record content displays", e)
            }
        }
    }

    /**
     * 记录用户与内容的交互行为
     * 应该在用户点击、点赞等操作时调用
     */
    override suspend fun recordContentInteraction(context: Context, feed: Feed) {
        withContext(Dispatchers.IO) {
            try {
                when (val target = feed.target) {
                    is Feed.AnswerTarget -> {
                        ContentFilterExtensions.recordContentInteraction(
                            context,
                            ContentType.ANSWER,
                            target.id.toString(),
                        )
                    }
                    is Feed.ArticleTarget -> {
                        ContentFilterExtensions.recordContentInteraction(
                            context,
                            ContentType.ARTICLE,
                            target.id.toString(),
                        )
                    }
                    is Feed.QuestionTarget -> {
                        ContentFilterExtensions.recordContentInteraction(
                            context,
                            ContentType.QUESTION,
                            target.id.toString(),
                        )
                    }
                    else -> {
                        // 其他类型暂不处理
                    }
                }
            } catch (e: Exception) {
                Log.e("HomeFeedViewModel", "Failed to record content interaction", e)
            }
        }
    }

    /**
     * 应用内容过滤，移除应该被过滤的内容
     * 根据用户设置决定是否过滤已关注用户的内容
     */
    suspend fun applyContentFilter(context: Context, feeds: List<Feed>): List<Feed> = withContext(Dispatchers.IO) {
        try {
            // 检查用户是否启用了对已关注用户内容的过滤
            val shouldFilterFollowed = shouldFilterFollowedUserContent(context)

            if (shouldFilterFollowed) {
                // 如果启用了对已关注用户的过滤，则对所有内容应用过滤规则
                ContentFilterExtensions.filterContentList(context, feeds)
            } else {
                // 如果关闭了对已关注用户的过滤，则分离处理
                val (followedUserContent, otherContent) = feeds.partition { feed ->
                    isFromFollowedUser(feed)
                }

                // 只对非关注用户的内容应用过滤
                val filteredOtherContent = ContentFilterExtensions.filterContentList(context, otherContent)

                // 合并已关注用户的内容和过滤后的其他内容
                followedUserContent + filteredOtherContent
            }
        } catch (e: Exception) {
            Log.e("HomeFeedViewModel", "Failed to apply content filter", e)
            feeds // 出错时返回原始数据
        }
    }

    /**
     * 判断内容是否来自已关注用户
     */
    private fun isFromFollowedUser(feed: Feed): Boolean = feed.target?.author?.isFollowing == true

    /**
     * 检查是否应该过滤已关注用户的内容
     */
    private fun shouldFilterFollowedUserContent(context: Context): Boolean {
        val preferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        return preferences.getBoolean("filterFollowedUserContent", false)
    }

    private suspend fun markItemsAsTouched(context: Context, httpClient: HttpClient = AccountData.httpClient(context)) {
        try {
            val untouchedAnswers = displayItems
                .filter { !it.isFiltered && it.feed?.target is Feed.AnswerTarget }

            if (untouchedAnswers.isNotEmpty()) {
                httpClient
                    .post("https://www.zhihu.com/lastread/touch") {
                        header("x-requested-with", "fetch")
                        signFetchRequest(context)
                        setBody(
                            MultiPartFormDataContent(
                                formData {
                                    append(
                                        "items",
                                        buildJsonArray {
                                            untouchedAnswers.forEach { item ->
                                                item.feed?.let { feed ->
                                                    when (val target = feed.target) {
                                                        is Feed.AnswerTarget -> {
                                                            add(
                                                                buildJsonArray {
                                                                    add("answer")
                                                                    add(target.id.toString())
                                                                    add("touch")
                                                                },
                                                            )
                                                        }

                                                        is Feed.ArticleTarget -> {
                                                            add(
                                                                buildJsonArray {
                                                                    add("article")
                                                                    add(target.id.toString())
                                                                    add("touch")
                                                                },
                                                            )
                                                        }

                                                        else -> {}
                                                    }
                                                }
                                            }
                                        }.toString(),
                                    )
                                },
                            ),
                        )
                    }.let { response ->
                        if (!response.status.isSuccess()) {
                            Log.e("Browse-Touch", response.bodyAsText())
                        }
                    }
            }
        } catch (e: Exception) {
            Log.e("FeedViewModel", "Failed to mark items as touched", e)
        }
    }
}
