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

package com.github.zly2006.zhihu.viewmodel

import com.github.zly2006.zhihu.shared.data.Feed
import com.github.zly2006.zhihu.shared.data.ZhihuJson
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.reflect.typeOf

class CollectionItemSerializationTest {
    @Test
    fun collectionItemCanBeDecodedWithPaginationSerializer() {
        val source = ZhihuJson.json.parseToJsonElement(
            """
            {
              "created": "2026-05-31T12:00:00+08:00",
              "content": {
                "id": 123,
                "type": "question",
                "title": "收藏夹里的问题",
                "url": "https://www.zhihu.com/question/123",
                "answer_count": 2,
                "follower_count": 3,
                "comment_count": 1,
                "excerpt": "摘要"
              }
            }
            """.trimIndent(),
        )

        @Suppress("UNCHECKED_CAST")
        val itemSerializer = serializer(typeOf<CollectionItem>()) as KSerializer<CollectionItem>
        val item = ZhihuJson.decodeJson(itemSerializer, source)
        val question = item.content

        assertTrue(question is Feed.QuestionTarget)
        assertEquals("2026-05-31T12:00:00+08:00", item.created)
        assertEquals(123L, (question as Feed.QuestionTarget).id)
        assertEquals("收藏夹里的问题", question.title)
    }
}
