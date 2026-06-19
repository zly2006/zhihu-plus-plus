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
