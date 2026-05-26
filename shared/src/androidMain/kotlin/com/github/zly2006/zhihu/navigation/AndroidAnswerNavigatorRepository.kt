package com.github.zly2006.zhihu.navigation

import android.content.Context
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.ContentDetailCache
import com.github.zly2006.zhihu.data.getOrFetch
import com.github.zly2006.zhihu.shared.data.CollectionItem
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.data.Feed
import com.github.zly2006.zhihu.shared.filter.ContentOpenEventSupport
import com.github.zly2006.zhihu.util.signFetchRequest
import com.github.zly2006.zhihu.viewmodel.filter.ContentType
import com.github.zly2006.zhihu.viewmodel.filter.getContentFilterDatabase
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class AndroidAnswerNavigatorRepository(
    context: Context,
) : AnswerNavigatorRepository {
    private val appContext = context.applicationContext

    override suspend fun fetchAnswerContent(article: Article): DataHolder.Answer? =
        ContentDetailCache.getOrFetch(appContext, article) as? DataHolder.Answer

    override suspend fun fetchQuestionFeeds(
        questionId: Long,
        pageUrl: String?,
    ): AnswerNavigatorPage<Feed> {
        val url = pageUrl ?: zhihuQuestionFeedsUrl(questionId, limit = 6)
        val jojo = AccountData.fetchGet(appContext, url) { signFetchRequest() } ?: return AnswerNavigatorPage(
            items = emptyList(),
            nextUrl = "",
        )
        return AnswerNavigatorPage(
            items = AccountData.decodeJson<List<Feed>>(jojo["data"] ?: return AnswerNavigatorPage(emptyList(), "")),
            nextUrl = jojo["paging"]
                ?.jsonObject
                ?.get("next")
                ?.jsonPrimitive
                ?.content ?: "",
        )
    }

    override suspend fun fetchCollectionItems(pageUrl: String): AnswerNavigatorPage<CollectionItem> {
        val jojo = AccountData.fetchGet(appContext, pageUrl) { signFetchRequest() } ?: return AnswerNavigatorPage(
            items = emptyList(),
            nextUrl = "",
        )
        return AnswerNavigatorPage(
            items = AccountData.decodeJson<List<CollectionItem>>(jojo["data"] ?: return AnswerNavigatorPage(emptyList(), "")),
            nextUrl = jojo["paging"]
                ?.jsonObject
                ?.get("next")
                ?.jsonPrimitive
                ?.content ?: "",
        )
    }

    override suspend fun getAlreadyOpenedAnswerIds(answerIds: List<Long>): Set<Long> =
        ContentOpenEventSupport
            .getAlreadyOpenedContentIds(
                database = getContentFilterDatabase(appContext),
                content = answerIds.map { ContentType.ANSWER to it.toString() },
            ).mapNotNullTo(mutableSetOf()) { key ->
                key.substringAfter(':', "").toLongOrNull()
            }
}
