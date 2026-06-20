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

class OnlineHistoryTest {
    @Test
    fun decodesOnlineHistoryItem() {
        val item = ZhihuJson.json.decodeFromString<OnlineHistoryItem>(
            """
            {
              "cardType": "read_history",
              "data": {
                "header": {
                  "icon": "https://example.com/icon.png",
                  "title": "标题"
                },
                "content": {
                  "authorName": "作者",
                  "summary": "摘要",
                  "coverImage": "https://example.com/cover.png"
                },
                "action": {
                  "type": "open_url",
                  "url": "zhihu://answers/123"
                },
                "extra": {
                  "contentToken": "123",
                  "contentType": "answer",
                  "readTime": 1716120000,
                  "questionToken": "456"
                },
                "matrix": [
                  {
                    "type": "text",
                    "data": {
                      "text": "已阅读"
                    }
                  }
                ]
              }
            }
            """.trimIndent(),
        )

        assertEquals("read_history", item.cardType)
        assertEquals("标题", item.data.header.title)
        assertEquals("zhihu://answers/123", item.data.action.url)
        assertEquals(
            "已阅读",
            item.data.matrix
                ?.single()
                ?.data
                ?.text,
        )
        assertEquals("456", item.data.extra.questionToken)
    }
}
