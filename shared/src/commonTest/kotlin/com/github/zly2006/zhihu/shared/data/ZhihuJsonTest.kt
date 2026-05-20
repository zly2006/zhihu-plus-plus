package com.github.zly2006.zhihu.shared.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals

class ZhihuJsonTest {
    @Test
    fun snakeCaseKeysAreConvertedRecursively() {
        val source = ZhihuJson.json.parseToJsonElement(
            """
            {
              "author_name": "Alice",
              "nested_items": [
                {
                  "answer_count": 2
                }
              ]
            }
            """.trimIndent(),
        )

        val converted = ZhihuJson.snakeCaseToCamelCase(source).jsonObject

        assertEquals("Alice", converted["authorName"]?.jsonPrimitive?.content)
        assertEquals(
            "2",
            converted["nestedItems"]
                ?.jsonArray
                ?.first()
                ?.jsonObject
                ?.get("answerCount")
                ?.jsonPrimitive
                ?.content,
        )
    }

    @Test
    fun decodeJsonUsesCamelCaseDataClasses() {
        val source = ZhihuJson.json.parseToJsonElement(
            """
            {
              "user_name": "Alice",
              "vote_count": 10
            }
            """.trimIndent(),
        )

        val decoded = ZhihuJson.decodeJson<SampleData>(source)

        assertEquals(SampleData(userName = "Alice", voteCount = 10), decoded)
    }

    @Serializable
    private data class SampleData(
        val userName: String,
        val voteCount: Int,
    )
}
