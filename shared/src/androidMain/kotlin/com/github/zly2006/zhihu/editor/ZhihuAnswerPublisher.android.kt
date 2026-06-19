/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.github.zly2006.zhihu.editor

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.decodeArticleContentDetail
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.zhihuQuestionRelationshipUrl
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.util.raiseForStatus
import com.github.zly2006.zhihu.util.signFetchRequest
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import java.util.UUID

@Composable
actual fun rememberZhihuAnswerPublisher(): ZhihuAnswerPublisher {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        AndroidZhihuAnswerPublisher(context)
    }
}

private class AndroidZhihuAnswerPublisher(
    private val context: Context,
) : ZhihuAnswerPublisher {
    override val isSupported: Boolean = true
    private val imageUploader: ZhihuImageUploader by lazy {
        ZhihuImageUploader(
            client = AccountData.httpClient(context),
            cookies = AccountData.data.cookies,
            userAgent = AccountData.data.userAgent,
        )
    }

    override suspend fun findMyAnswerId(questionId: Long): Long? {
        AccountData.data.self?.id ?: return null

        val url = zhihuQuestionRelationshipUrl(questionId)
        val element = runCatching {
            AccountData
                .httpClient(context)
                .get(url) {
                    signFetchRequest()
                }.raiseForStatus(dumpRequest = true)
                .body<JsonElement>()
        }.getOrNull() ?: return null

        val response = AccountData.decodeJson(DataHolder.QuestionRelationshipApiResponse.serializer(), element)
        val myAnswer = response.relationship?.myAnswer ?: return null

        if (myAnswer.isDeleted == true) {
            return null
        }

        return myAnswer.answerId?.toLongOrNull()
    }

    override suspend fun fetchAnswerForEditing(answerId: Long): ExistingAnswerForEditing? {
        val url =
            "https://www.zhihu.com/api/v4/answers/$answerId?include=" +
                "is_visible,paid_info,paid_info_content,has_column,admin_closed_comment," +
                "reward_info,annotation_action,annotation_detail,collapse_reason,is_normal," +
                "is_sticky,collapsed_by,suggest_edit,comment_count,thanks_count," +
                "favlists_count,can_comment,content,editable_content,voteup_count," +
                "reshipment_settings,comment_permission,created_time,updated_time," +
                "review_info,relevant_info,question,excerpt,attachment,content_source," +
                "is_labeled,endorsements,reaction_instruction,ip_info,relationship.is_authorized," +
                "voting,is_thanked,is_author,is_nothelp,is_favorited,pagination_info," +
                "question.topics,reaction.relation.voting,author.badge_v2," +
                "settings.table_of_contents.enabled"
        return runCatching {
            val json = AccountData
                .httpClient(context)
                .get(url) {
                    signFetchRequest()
                }.raiseForStatus(dumpRequest = true)
                .body<JsonObject>()
            val answer = decodeArticleContentDetail(
                article = Article(type = ArticleType.Answer, id = answerId),
                json = json,
            ) as? DataHolder.Answer ?: return null
            val html = answer.editableContent ?: answer.content
            ExistingAnswerForEditing(
                answerId = answerId,
                html = html,
                tocEnabled = answer.settings?.tableOfContents?.enabled ?: false,
            )
        }.getOrThrow()
    }

    override suspend fun uploadImage(bytes: ByteArray, mimeType: String?, fileName: String?): UploadedZhihuImage =
        imageUploader.upload(bytes, mimeType, fileName)

    override suspend fun uploadImageFromUrl(url: String): UploadedZhihuImage =
        imageUploader.uploadFromUrl(url)

    override suspend fun patchDraft(
        questionId: Long,
        answerId: Long?,
        html: String,
        tocEnabled: Boolean,
    ) {
        val xsrf = AccountData.data.cookies["_xsrf"]
            ?: throw IllegalStateException("缺少 _xsrf Cookie，无法写入草稿；请先确保已登录。")

        val body = PatchDraftRequest(
            content = html,
            settings = PatchDraftSettings(
                tableOfContentsEnabled = tocEnabled,
            ),
        )

        AccountData
            .httpClient(context)
            .post("https://www.zhihu.com/api/v4/questions/$questionId/draft") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Referrer, "https://www.zhihu.com/question/$questionId/answer/${answerId ?: ""}")
                header("x-requested-with", "fetch")
                header("x-xsrftoken", xsrf)
                signFetchRequest()
                setBody(body)
            }.raiseForStatus(dumpRequest = true)
    }

    override suspend fun publishAnswer(
        questionId: Long,
        answerId: Long?,
        html: String,
        tocEnabled: Boolean,
    ): Long {
        val xsrf = AccountData.data.cookies["_xsrf"]
            ?: throw IllegalStateException("缺少 _xsrf Cookie，无法发布回答；请先确保已登录。")

        val isPublished = answerId != null
        val traceId = "${System.currentTimeMillis()},${UUID.randomUUID()}"

        val requestBody = PublishAnswerRequest(
            data = PublishAnswerData(
                publish = PublishTrace(traceId = traceId),
                draft = PublishDraft(
                    isPublished = isPublished,
                    contentId = answerId?.toString(),
                ),
                extraInfo = PublishExtraInfo(
                    questionId = questionId.toString(),
                    pcBusinessParams = buildPcBusinessParams(tocEnabled),
                ),
                hybrid = PublishHybrid(
                    html = html,
                ),
                contentsTables = PublishContentsTables(
                    tableOfContentsEnabled = tocEnabled,
                ),
            ),
        )

        val responseElement = AccountData
            .httpClient(context)
            .post("https://www.zhihu.com/api/v4/content/publish") {
                contentType(ContentType.Application.Json)
                header("x-requested-with", "fetch")
                header("x-xsrftoken", xsrf)
                signFetchRequest()
                setBody(requestBody)
            }.raiseForStatus(dumpRequest = true)
            .body<JsonElement>()

        val response = AccountData.decodeJson(DataHolder.ContentPublishResponse.serializer(), responseElement)
        val message = response.message
        if (message == "success") {
            val resultText = response.data?.result
                ?: throw IllegalStateException("发布成功但返回缺少 data.result: $responseElement")

            return parsePublishAnswerId(resultText)
                ?: throw IllegalStateException("发布成功但无法解析 publish.id")
        }

        val code = response.code
        if (code == 103003) {
            throw IllegalStateException(response.message ?: "已回答过该问题，创建回答失败")
        }

        throw IllegalStateException(
            "发布失败: ${response.message ?: "unknown"}\n$responseElement",
        )
    }
}
