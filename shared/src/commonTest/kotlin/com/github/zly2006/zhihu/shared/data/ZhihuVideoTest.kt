package com.github.zly2006.zhihu.shared.data

import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ZhihuVideoTest {
    @Test
    fun selectHighestQualityVideoUrlUsesLargestBitrate() {
        val json = ZhihuJson.json
            .parseToJsonElement(
                """
                {
                  "video_play": {
                    "playlist": {
                      "mp4": [
                        {"bitrate": 360, "url": ["https://example.com/360.mp4"]},
                        {"bitrate": 1080, "url": ["https://example.com/1080.mp4"]},
                        {"bitrate": 720, "url": ["https://example.com/720.mp4"]}
                      ]
                    }
                  }
                }
                """.trimIndent(),
            ).jsonObject

        assertEquals("https://example.com/1080.mp4", selectHighestQualityZhihuVideoUrl(json))
    }

    @Test
    fun selectHighestQualityVideoUrlReturnsNullWhenPlaylistIsMissing() {
        val json = ZhihuJson.json.parseToJsonElement("""{"video_play":{}}""").jsonObject

        assertNull(selectHighestQualityZhihuVideoUrl(json))
    }
}
