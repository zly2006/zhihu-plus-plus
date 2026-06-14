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

package com.github.zly2006.zhihu.shared.data

import com.github.zly2006.zhihu.shared.notification.NotificationType
import com.github.zly2006.zhihu.viewmodel.mergeNotificationsByCreateTime
import com.github.zly2006.zhihu.viewmodel.shouldReportNotificationFetchFailure
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ZhihuNotificationClientTest {
    @Test
    fun inviteAnswerNotificationsKeepOptInDisplayDefault() {
        assertEquals(false, NotificationType.INVITE_ANSWER.defaultValue)
    }

    @Test
    fun decodesInviteAnswerNotificationWithQuestionTarget() {
        val notification = ZhihuJson.decodeJson<NotificationItem>(
            ZhihuJson.json.parseToJsonElement(
                """
                {
                  "id": "invite-answer-notification",
                  "type": "notification",
                  "is_read": false,
                  "create_time": 1760000000,
                  "content": {
                    "verb": " 邀请你回答问题",
                    "actors": [
                      {
                        "name": "邀请者",
                        "type": "member",
                        "link": "https://www.zhihu.com/people/inviter",
                        "url_token": "inviter"
                      }
                    ],
                    "target": {
                      "text": "如何选择开发工具？",
                      "link": "https://www.zhihu.com/question/123"
                    },
                    "extend": {
                      "text": "",
                      "icon": "https://pic.example/icon.png"
                    }
                  },
                  "target": {
                    "type": "question",
                    "url": "https://www.zhihu.com/question/123",
                    "title": "如何选择开发工具？",
                    "id": "123",
                    "created": 1760000000,
                    "updated_time": 1760000001,
                    "question_type": "normal"
                  }
                }
                """.trimIndent(),
            ),
        )

        assertEquals(" 邀请你回答问题", notification.content.verb)
        val target = assertIs<NotificationTarget.Question>(notification.target)
        assertEquals("123", target.id)
        assertEquals("如何选择开发工具？", target.title)
    }

    @Test
    fun decodesArticleNotificationWithoutQuestionField() {
        val notification = ZhihuJson.decodeJson<NotificationItem>(
            ZhihuJson.json.parseToJsonElement(
                """
                {
                  "id": "article-notification",
                  "type": "notification",
                  "is_read": true,
                  "create_time": 1760000000,
                  "content": {
                    "verb": "赞同了你的文章",
                    "actors": {
                      "name": "读者",
                      "type": "member",
                      "link": "https://www.zhihu.com/people/reader",
                      "url_token": "reader"
                    },
                    "target": {
                      "text": "文章标题",
                      "link": "https://zhuanlan.zhihu.com/p/456"
                    },
                    "extend": {
                      "text": "",
                      "icon": "https://pic.example/icon.png"
                    }
                  },
                  "target": {
                    "type": "article",
                    "url": "https://zhuanlan.zhihu.com/p/456",
                    "title": "文章标题",
                    "id": "456",
                    "excerpt": "文章摘要"
                  }
                }
                """.trimIndent(),
            ),
        )

        val target = assertIs<NotificationTarget.Article>(notification.target)
        assertEquals("456", target.id)
        assertEquals("文章标题", target.title)
        assertEquals("文章摘要", target.excerpt)
    }

    @Test
    fun notificationViewModelSortsMergedCategoryNotificationsByCreateTimeDescending() = runTest {
        val merged = mergeNotificationsByCreateTime(
            existing = listOf(
                notificationItem(id = "default-old", createTime = 100),
                notificationItem(id = "follow-middle", createTime = 200),
            ),
            incoming = listOf(
                notificationItem(id = "default-new", createTime = 300),
                notificationItem(id = "vote-latest", createTime = 400),
            ),
        )

        assertEquals(
            listOf("vote-latest", "default-new", "follow-middle", "default-old"),
            merged.map { it.id },
        )
    }

    @Test
    fun notificationViewModelReportsFailureOnlyWhenEverySourceFails() {
        assertEquals(false, shouldReportNotificationFetchFailure(successfulSourceCount = 1, failureCount = 1))
        assertEquals(false, shouldReportNotificationFetchFailure(successfulSourceCount = 1, failureCount = 0))
        assertEquals(true, shouldReportNotificationFetchFailure(successfulSourceCount = 0, failureCount = 1))
    }

    @Test
    fun decodesUnreadNotificationCountsFromSnakeCasePayload() {
        val notifications: ZhihuMeNotifications = ZhihuJson.decodeJson(
            buildJsonObject {
                put("default_notifications_count", 1)
                put("follow_notifications_count", 2)
                put("vote_thank_notifications_count", 3)
            },
        )

        assertEquals(1, notifications.defaultNotificationsCount)
        assertEquals(2, notifications.followNotificationsCount)
        assertEquals(3, notifications.voteThankNotificationsCount)
        assertEquals(6, notifications.totalCount)
    }

    private fun notificationItem(
        id: String,
        createTime: Long,
    ): NotificationItem = ZhihuJson.decodeJson(
        ZhihuJson.json.parseToJsonElement(
            """
            {
              "id": "$id",
              "type": "notification",
              "is_read": true,
              "create_time": $createTime,
              "content": {
                "verb": "邀请你回答问题",
                "actors": [],
                "target": {
                  "text": "排序问题 $id",
                  "link": "https://www.zhihu.com/question/$createTime"
                },
                "extend": {
                  "text": "",
                  "icon": "https://pic.example/icon.png"
                }
              },
              "target": {
                "type": "question",
                "url": "https://www.zhihu.com/question/$createTime",
                "title": "排序问题 $id",
                "id": "$createTime"
              }
            }
            """.trimIndent(),
        ),
    )
}
