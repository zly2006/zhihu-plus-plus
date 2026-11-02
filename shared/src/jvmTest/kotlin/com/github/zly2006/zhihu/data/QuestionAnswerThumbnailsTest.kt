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

import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class QuestionAnswerThumbnailsTest {
    @Test
    fun realQuestionAnswersPreserveThumbnailCountsAndImageUrls() {
        val samples = ZhihuJson.json
            .parseToJsonElement(
                checkNotNull(javaClass.getResource("/real-api/question-answer-thumbnails.json")).readText(),
            ).jsonArray
        assertEquals(
            listOf(0, 1, 2, 3),
            samples.map {
                it.jsonObject
                    .getValue("thumbnail_info")
                    .jsonObject
                    .getValue("count")
                    .jsonPrimitive.content
                    .toInt()
            },
        )
        for (sample in samples) {
            val answer = ZhihuJson.decodeJson<Feed.Target>(sample)
            val info = assertNotNull(ZhihuJson.json.encodeToJsonElement(answer).jsonObject["thumbnailInfo"])
            val raw = sample.jsonObject.getValue("thumbnail_info").jsonObject
            assertEquals(raw.getValue("count"), info.jsonObject.getValue("count"))
            assertEquals(
                raw.getValue("thumbnails").jsonArray.map { it.jsonObject.getValue("url") },
                info.jsonObject
                    .getValue("thumbnails")
                    .jsonArray
                    .map { it.jsonObject.getValue("url") },
            )
        }
    }
}
