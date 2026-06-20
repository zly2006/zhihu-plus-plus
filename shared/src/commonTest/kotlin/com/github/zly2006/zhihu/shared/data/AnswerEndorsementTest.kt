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
    fun creationStatementUsesDisclaimerEndorsementText() {
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
                      "elements": [
                        {
                          "type": "TEXT",
                          "content": "知乎圆桌: AI 观察"
                        }
                      ],
                      "za": {
                        "block_text": "RoundTableLabel"
                      }
                    },
                    {
                      "elements": [
                        {
                          "type": "TEXT",
                          "content": ""
                        },
                        {
                          "type": "TEXT",
                          "content": "创作声明: 内容包含剧透"
                        }
                      ],
                      "za": {
                        "block_text": "DisclaimerLabel"
                      }
                    },
                    {
                      "elements": [
                        {
                          "type": "TEXT",
                          "content": "收录于话题: 科技"
                        }
                      ],
                      "za": {
                        "block_text": "TopicLabel"
                      }
                    }
                  ]
                }
                """.trimIndent(),
            ),
        )

        assertEquals("创作声明: 内容包含剧透", answer.creationStatementText)
    }
}
