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
