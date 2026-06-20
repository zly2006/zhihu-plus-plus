/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
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
import com.github.zly2006.zhihu.data.asApiEnvironment
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.data.ZhihuJson
import com.github.zly2006.zhihu.shared.util.raiseForStatus
import com.github.zly2006.zhihu.viewmodel.fetchContentDetail
import com.github.zly2006.zhihu.viewmodel.postSigned
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.serialization.json.JsonElement
import java.util.UUID

@Composable
actual fun rememberZhihuAnswerPublisher(): ZhihuAnswerPublisher {
    val context = LocalContext.current.applicationContext
    return remember(context) {
        AndroidZhihuAnswerPublisher(context)
    }
}

private const val QUESTION_RELATIONSHIP_INCLUDE = "relationship,relationship.my_answer"

private class AndroidZhihuAnswerPublisher(
    private val context: Context,
) : ZhihuAnswerPublisher {
    override val isSupported: Boolean = true

    override suspend fun findMyAnswerId(questionId: Long): Long? {
        AccountData.data.self?.id ?: return null

        val element = runCatching {
            context.asApiEnvironment().fetchJson("https://api.zhihu.com/questions/$questionId", QUESTION_RELATIONSHIP_INCLUDE)
        }.getOrNull() ?: return null

        val response = ZhihuJson.decodeJson(DataHolder.QuestionRelationshipApiResponse.serializer(), element)
        val myAnswer = response.relationship?.myAnswer ?: return null

        if (myAnswer.isDeleted == true) {
            return null
        }

        return myAnswer.answerId?.toLongOrNull()
    }

    override suspend fun fetchAnswerForEditing(answerId: Long): ExistingAnswerForEditing? {
        val environment = context.asApiEnvironment()
        val destination = Article(type = ArticleType.Answer, id = answerId)
        val answer = environment.fetchContentDetail(destination) as? DataHolder.Answer ?: return null
        val html = answer.editableContent ?: answer.content
        val tocEnabled = answer.settings?.tableOfContents?.enabled ?: false

        return ExistingAnswerForEditing(
            answerId = answerId,
            html = html,
            tocEnabled = tocEnabled,
        )
    }

    override suspend fun uploadImage(
        bytes: ByteArray,
        mimeType: String?,
        fileName: String?,
    ): UploadedZhihuImage =
        uploadZhihuImage(context.asApiEnvironment(), bytes, mimeType, fileName, ZhihuImageUploadSource.Article)

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

        context
            .asApiEnvironment()
            .postSigned("https://www.zhihu.com/api/v4/questions/$questionId/draft") {
                contentType(ContentType.Application.Json)
                header(HttpHeaders.Referrer, "https://www.zhihu.com/question/$questionId/answer/${answerId ?: ""}")
                header("x-xsrftoken", xsrf)
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

        val responseElement = context
            .asApiEnvironment()
            .postSigned("https://www.zhihu.com/api/v4/content/publish") {
                contentType(ContentType.Application.Json)
                header("x-xsrftoken", xsrf)
                setBody(requestBody)
            }.raiseForStatus(dumpRequest = true)
            .body<JsonElement>()

        val response = ZhihuJson.decodeJson(DataHolder.ContentPublishResponse.serializer(), responseElement)
        if (response.message == "success") {
            val resultText = response.data?.result
                ?: throw IllegalStateException("发布成功但返回缺少 data.result: $responseElement")

            return parsePublishContentId(resultText)
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
