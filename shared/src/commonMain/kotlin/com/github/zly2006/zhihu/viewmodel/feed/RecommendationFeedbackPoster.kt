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

package com.github.zly2006.zhihu.viewmodel.feed

import com.github.zly2006.zhihu.shared.data.ZhihuJson
import com.github.zly2006.zhihu.viewmodel.ZhihuApiEnvironment
import com.github.zly2006.zhihu.viewmodel.postSigned
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.setBody
import io.ktor.http.Parameters
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlin.time.Clock

private const val FEEDBACK_BATCH_SIZE = 5
private const val FEEDBACK_MAX_PENDING_MILLIS = 120_000L

/**
 * 按知乎推荐反馈协议批量发送触达和已读状态。
 *
 * `target` 不含事件类型；首页目标形如 `type, id`，关注推荐目标只有 `brief`。触达累计五条再发送，
 * 已读立即冲刷队列；超过两分钟仍有待发送触达时，在下一次列表停稳时冲刷。
 */
internal class RecommendationFeedbackPoster(
    private val endpoint: String,
    private val nowMillis: () -> Long = { Clock.System.now().toEpochMilliseconds() },
) {
    private val mutex = Mutex()
    private val pending = linkedSetOf<List<String>>()
    private val reported = hashSetOf<List<String>>()
    private var lastPostMillis = nowMillis()

    suspend fun touch(
        environment: ZhihuApiEnvironment,
        targets: Collection<List<String>>,
    ) = mutex.withLock {
        targets.forEach { target -> enqueue("t", target) }
        if (pending.size >= FEEDBACK_BATCH_SIZE || nowMillis() - lastPostMillis > FEEDBACK_MAX_PENDING_MILLIS) {
            postPending(environment)
        }
    }

    suspend fun read(
        environment: ZhihuApiEnvironment,
        target: List<String>,
    ) = mutex.withLock {
        enqueue("r", target)
        postPending(environment)
    }

    private fun enqueue(
        event: String,
        target: List<String>,
    ) {
        if (target.isEmpty()) return
        val payload = listOf(event) + target
        if (reported.add(payload)) {
            pending.add(payload)
        }
    }

    private suspend fun postPending(environment: ZhihuApiEnvironment) {
        if (pending.isEmpty() || environment.authenticatedCookies()["d_c0"] == null) return

        val batch = pending.toList()
        pending.clear()
        try {
            val response = environment.postSigned(endpoint) {
                setBody(
                    FormDataContent(
                        Parameters.build {
                            append("targets", ZhihuJson.json.encodeToString(batch))
                        },
                    ),
                )
            }
            if (!response.status.isSuccess()) {
                throw IllegalStateException("推荐反馈上报失败：${response.status}")
            }
            lastPostMillis = nowMillis()
            if (reported.size > 512) {
                reported.clear()
            }
        } catch (error: CancellationException) {
            pending.addAll(batch)
            throw error
        } catch (error: Exception) {
            pending.addAll(batch)
            environment.handleFetchFailure("RecommendationFeedbackPoster", error)
        }
    }
}
