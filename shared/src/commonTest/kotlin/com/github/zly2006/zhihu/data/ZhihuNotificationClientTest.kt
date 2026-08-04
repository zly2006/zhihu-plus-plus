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

package com.github.zly2006.zhihu.data

import com.github.zly2006.zhihu.notification.NotificationType
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

    @Test
    fun decodesMobileUnreadNotificationCountsFromHeadEntries() {
        val overview: MobileNotificationMessageOverview = ZhihuJson.decodeJson(
            ZhihuJson.json.parseToJsonElement(
                """
                {
                  "head": [
                    {
                      "type": "entry",
                      "detail_title": "评论转发@",
                      "unread_count": 1
                    },
                    {
                      "type": "entry",
                      "detail_title": "赞同喜欢",
                      "unread_count": 2
                    }
                  ]
                }
                """.trimIndent(),
            ),
        )

        assertEquals("评论转发@", overview.head[0].detailTitle)
        assertEquals(1, overview.head[0].unreadCount)
        assertEquals("赞同喜欢", overview.head[1].detailTitle)
        assertEquals(2, overview.head[1].unreadCount)
    }

    @Test
    fun decodesMobileTimelineNotificationWithTargetSource() {
        val notification: MobileNotificationTimelineItem = ZhihuJson.decodeJson(
            ZhihuJson.json.parseToJsonElement(
                """
                {
                  "id": "mobile-notification",
                  "unique_id": "mobile-notification-unique",
                  "type": "aggregate_notification",
                  "card_type": "noti_simple_card",
                  "is_read": false,
                  "created": 1781990000,
                  "head": {
                    "avatar_url": "https://pic.example/avatar.jpg",
                    "target_link": "https://www.zhihu.com/people/tester"
                  },
                  "content": {
                    "title": "测试用户 评论了你的回答",
                    "sub_title": "评论和回复",
                    "text": "<p>测试评论</p>",
                    "target_link": "zhihu://comment/list/answer/2?anchor_comment_id=3"
                  },
                  "target_source": {
                    "text": "测试回答标题",
                    "target_link": "https://www.zhihu.com/question/1/answer/2"
                  }
                }
                """.trimIndent(),
            ),
        )

        assertEquals("mobile-notification-unique", notification.stableId)
        assertEquals(false, notification.isRead)
        assertEquals(1781990000, notification.created)
        assertEquals("https://pic.example/avatar.jpg", notification.head?.avatarUrl)
        assertEquals("测试用户 评论了你的回答", notification.content?.title)
        assertEquals("测试回答标题", notification.targetSource?.text)
    }

    @Test
    fun decodesOfficialNotificationHomePage() {
        val page: MobileNotificationMessageOverview = ZhihuJson.decodeJson(
            ZhihuJson.json.parseToJsonElement(
                """
                {
                  "head": [{"detail_title": "评论转发@", "unread_count": 3}],
                  "column_head": [{
                    "id": "invite",
                    "title": "邀请回答",
                    "text_prefix": "测试用户",
                    "text": "等人邀请你回答问题",
                    "target_link": "https://www.zhihu.com/compose_answer_tab",
                    "avatar_urls": [{"url": "https://pic.example/1.jpg", "night_url": "https://pic.example/1-night.jpg"}],
                    "images": [],
                    "unread_count": 2
                  }],
                  "data": [{
                    "id": "conversation",
                    "type": "aggregate_notification",
                    "created_str": "07-31",
                    "unread_count": 1,
                    "content": {
                      "title": "系统消息",
                      "text": "消息预览",
                      "target_link": "https://www.zhihu.com/notifications/v3/timeline/entry/system"
                    }
                  }],
                  "paging": {
                    "is_end": false,
                    "next": "https://api.zhihu.com/notifications/v3/message/v3?limit=20&offset=1"
                  }
                }
                """.trimIndent(),
            ),
        )

        assertEquals(3, page.head.single().unreadCount)
        assertEquals("邀请回答", page.columnHead.single().title)
        assertEquals(2, page.columnHead.single().unreadCount)
        assertEquals(
            "系统消息",
            page.data
                .single()
                .content
                ?.title,
        )
        assertEquals(false, page.paging?.isEnd)
    }

    @Test
    fun decodesInvitationCardAndDateHeader() {
        val dateHeader: MobileNotificationTimelineItem = ZhihuJson.decodeJson(
            ZhihuJson.json.parseToJsonElement(
                """{"type":"empty","created":1,"empty_info":{"text":"今天","number":2}}""",
            ),
        )
        val invitation: MobileNotificationTimelineItem = ZhihuJson.decodeJson(
            ZhihuJson.json.parseToJsonElement(
                """
                {
                  "id": "invitation",
                  "type": "aggregate_notification",
                  "head": {
                    "author": {"id":null,"name":"","avatar_url":""},
                    "avatar_url":"https://pic.example/avatar.jpg",
                    "labels": [{"text":"关注你"}]
                  },
                  "content": {"title":"邀请者","sub_title":"邀请你回答","text":"测试问题"},
                  "target_source": {"text":"测试问题","sub_text":"46 回答 · 101 关注"},
                  "target": {"id":"42","type":"question","title":"测试问题","follow_num":101},
                  "additional_info": [
                    {"id":301,"text":"回答该<b>测试</b>问题，创作分额外 +20 分","icon":""}
                  ]
                }
                """.trimIndent(),
            ),
        )

        assertEquals("今天", dateHeader.emptyInfo?.text)
        assertEquals(
            "关注你",
            invitation.head
                ?.labels
                ?.single()
                ?.text,
        )
        assertEquals("邀请者", invitation.content?.title)
        assertEquals("测试问题", invitation.target?.title)
        assertEquals("46 回答 · 101 关注", invitation.targetSource?.subText)
        assertEquals(301, invitation.additionalInfo.single().id)
    }

    @Test
    fun decodesPrivateMessagePageWithPluginMessage() {
        val page: ZhihuPrivateMessagePage = ZhihuJson.decodeJson(
            ZhihuJson.json.parseToJsonElement(
                """
                {
                  "data": [{
                    "id": "message-1",
                    "type": "message",
                    "content_type": 7,
                    "content": "",
                    "created_time": 1785742000,
                    "sender": {"id":"peer","name":"知乎小管家","url_token":"helper"},
                    "receiver": {"id":"me","name":"我","url_token":"me"},
                    "plugin": {
                      "plugin_type": "GUESS_YOU_WANT_ASK",
                      "plugin_content": "[]",
                      "excerpt": "猜你想问"
                    }
                  }],
                  "paging": {
                    "is_end": false,
                    "next": "https://api.zhihu.com/messages?sender_id=peer&after_id=1&limit=20"
                  }
                }
                """.trimIndent(),
            ),
        )

        assertEquals(7, page.data.single().contentType)
        assertEquals(
            "GUESS_YOU_WANT_ASK",
            page.data
                .single()
                .plugin
                ?.pluginType,
        )
        assertEquals(
            "知乎小管家",
            page.data
                .single()
                .sender
                ?.name,
        )
        assertEquals(false, page.paging.isEnd)
    }
}
