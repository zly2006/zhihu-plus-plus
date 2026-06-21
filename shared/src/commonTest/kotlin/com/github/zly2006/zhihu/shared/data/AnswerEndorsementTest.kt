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

package com.github.zly2006.zhihu.shared.data

import kotlin.test.Test
import kotlin.test.assertEquals

class AnswerEndorsementTest {
    @Test
    fun endorsementTextsJoinTextElementsFromEachEndorsement() {
        val answer = ZhihuJson.decodeJson<DataHolder.Answer>(
            ZhihuJson.json.parseToJsonElement(
                """
                {
                  "answer_type": "normal",
                  "author": {
                    "avatar_url": "",
                    "gender": 0,
                    "headline": "",
                    "id": "author",
                    "is_advertiser": false,
                    "is_org": false,
                    "name": "答主",
                    "type": "people",
                    "url": "https://www.zhihu.com/people/author",
                    "url_token": "author",
                    "user_type": "people"
                  },
                  "can_comment": {
                    "status": true,
                    "reason": ""
                  },
                  "content": "<p>正文</p>",
                  "created_time": 1710000000,
                  "excerpt": "摘要",
                  "id": 1,
                  "question": {
                    "created": 1710000000,
                    "id": 2,
                    "question_type": "normal",
                    "title": "问题",
                    "type": "question",
                    "updated_time": 1710000001,
                    "url": "https://www.zhihu.com/question/2"
                  },
                  "thanks_count": 0,
                  "type": "answer",
                  "updated_time": 1710000001,
                  "url": "https://www.zhihu.com/question/2/answer/1",
                  "voteup_count": 3,
                  "endorsements": [
                    {
                      "background_color": {
                        "alpha": 0.1,
                        "group": "GYL02A"
                      },
                      "elements": [
                        {
                          "type": "IMAGE",
                          "image_key": "zhicon_icon_24_chat_bubble_hash_fill",
                          "image_color": {
                            "alpha": 1,
                            "group": "GYL02A"
                          }
                        },
                        {
                          "type": "TEXT",
                          "content": "话题收录",
                          "font_color": {
                            "alpha": 1,
                            "group": "GYL02A"
                          }
                        },
                        {
                          "type": "TEXT",
                          "content": "我的开源名片",
                          "font_color": {
                            "alpha": 1,
                            "group": "GYL02A"
                          }
                        },
                        {
                          "type": "IMAGE",
                          "image_key": "zhicon_icon_16_arrow_right",
                          "image_color": {
                            "alpha": 1,
                            "group": "GYL02A"
                          }
                        }
                      ]
                    },
                    {
                      "background_color": {
                        "alpha": 0.1,
                        "group": "GBL01A"
                      },
                      "elements": [
                        {
                          "type": "TEXT",
                          "content": ""
                        },
                        {
                          "type": "TEXT",
                          "content": "创作声明: 内容包含剧透",
                          "font_color": {
                            "alpha": 1,
                            "group": "GBL07A"
                          }
                        },
                        {
                          "type": "IMAGE",
                          "image_key": "zhicon_icon_16_arrow_down",
                          "selected_image_key": "zhicon_icon_16_arrow_up",
                          "image_color": {
                            "alpha": 1,
                            "group": "GBL07A"
                          }
                        }
                      ]
                    },
                    {
                      "elements": [
                        {
                          "type": "TEXT",
                          "content": "收录于话题: 科技"
                        }
                      ]
                    }
                  ]
                }
                """.trimIndent(),
            ),
        )

        assertEquals(
            listOf(
                "话题收录 我的开源名片",
                "创作声明: 内容包含剧透",
                "收录于话题: 科技",
            ),
            answer.endorsementTexts,
        )
        assertEquals(
            listOf(
                DataHolder.AnswerEndorsementDisplay(
                    text = "话题收录 我的开源名片",
                    backgroundColor = DataHolder.AnswerEndorsementColor(alpha = 0.1f, group = "GYL02A"),
                    textColor = DataHolder.AnswerEndorsementColor(alpha = 1f, group = "GYL02A"),
                    leadingIconKey = "zhicon_icon_24_chat_bubble_hash_fill",
                    leadingIconColor = DataHolder.AnswerEndorsementColor(alpha = 1f, group = "GYL02A"),
                    trailingIconKey = "zhicon_icon_16_arrow_right",
                ),
                DataHolder.AnswerEndorsementDisplay(
                    text = "创作声明: 内容包含剧透",
                    backgroundColor = DataHolder.AnswerEndorsementColor(alpha = 0.1f, group = "GBL01A"),
                    textColor = DataHolder.AnswerEndorsementColor(alpha = 1f, group = "GBL07A"),
                    trailingIconKey = "zhicon_icon_16_arrow_down",
                ),
                DataHolder.AnswerEndorsementDisplay(
                    text = "收录于话题: 科技",
                ),
            ),
            answer.endorsementItems,
        )
    }
}
