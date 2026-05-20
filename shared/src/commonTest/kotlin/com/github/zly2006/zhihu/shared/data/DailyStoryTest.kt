package com.github.zly2006.zhihu.shared.data

import kotlin.test.Test
import kotlin.test.assertEquals

class DailyStoryTest {
    @Test
    fun decodesDailyStoriesResponse() {
        val response = ZhihuJson.json.decodeFromString<DailyStoriesResponse>(
            """
            {
              "date": "20260520",
              "stories": [
                {
                  "id": 42,
                  "title": "title",
                  "url": "https://example.com/story/42",
                  "hint": "hint",
                  "images": ["https://example.com/image.png"],
                  "type": 0
                }
              ]
            }
            """.trimIndent(),
        )

        assertEquals("20260520", response.date)
        assertEquals(42L, response.stories.single().id)
        assertEquals("title", response.stories.single().title)
        assertEquals(
            "https://example.com/image.png",
            response.stories
                .single()
                .images
                .single(),
        )
    }
}
