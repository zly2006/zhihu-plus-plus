@file:Suppress("unused")

package com.github.zly2006.zhihu.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Search result item from Zhihu search API v3
 * The API returns items with structure: { type, id, object: { answer/article/question } }
 */
@Serializable
data class SearchResult(
    val type: String,
    val id: String,
    @SerialName("object")
    val obj: JsonElement, // Use JsonElement for flexible parsing
    val highlight: Highlight? = null,
) {
    /**
     * Convert search result to Feed for display
     * The search API wraps the actual content in an "object" field with type-specific data
     */
    fun toFeed(): Feed? = try {
        // Try to decode as the actual Feed target types
        when (type) {
            "search_result" -> {
                // The object field contains the actual answer/article/question data
                val target = AccountData.decodeJson<Feed.Target>(obj)
                CommonFeed(
                    id = id,
                    type = "search_result",
                    verb = "SEARCH_RESULT",
                    target = target,
                )
            }
            else -> null
        }
    } catch (e: Exception) {
        null
    }
}

@Serializable
data class Highlight(
    val title: List<String>? = null,
    val description: List<String>? = null,
)
