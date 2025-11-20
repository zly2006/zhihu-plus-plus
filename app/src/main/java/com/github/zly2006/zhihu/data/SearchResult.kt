@file:Suppress("unused")

package com.github.zly2006.zhihu.data

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

/**
 * Custom serializer for SearchObject that dispatches based on parent SearchResult.type
 */
object SearchObjectSerializer : KSerializer<SearchObject?> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("SearchObject")

    override fun serialize(encoder: Encoder, value: SearchObject?) {
        // Not implemented as we only need deserialization
        throw NotImplementedError("Serialization not implemented")
    }

    override fun deserialize(decoder: Decoder): SearchObject? {
        require(decoder is JsonDecoder)
        val element = decoder.decodeJsonElement()
        return null // Will be handled manually based on type
    }
}

/**
 * Search result item from Zhihu search API v3
 * The API returns items with different object structures based on type
 */
@Serializable
data class SearchResult(
    val type: String,
    val id: String,
    @Serializable(with = SearchObjectRawSerializer::class)
    @SerialName("object")
    val objRaw: JsonElement? = null,
    val highlight: Highlight? = null,
    val index: Int? = null,
    @SerialName("hit_labels")
    val hitLabels: String? = null,
) {
    /**
     * Parse the object field based on type
     */
    val obj: SearchObject?
        get() {
            return try {
                if (objRaw == null) return null
                when (type) {
                    "search_result" -> {
                        // Object field IS the Feed.Target directly
                        val target = AccountData.decodeJson<Feed.Target>(objRaw)
                        SearchObjectResult(target)
                    }
                    "koc_box" -> AccountData.decodeJson<SearchObjectKocBox>(objRaw)
                    "knowledge_ad" -> AccountData.decodeJson<SearchObjectKnowledgeAd>(objRaw)
                    "relevant_query" -> null // No object field
                    else -> null
                }
            } catch (e: Exception) {
                null
            }
        }

    /**
     * Convert search result to Feed for display
     * Returns null if the result type doesn't have displayable content
     */
    fun toFeed(): Feed? {
        return try {
            when (type) {
                "search_result" -> {
                    // The object field IS the Feed.Target (answer/article/question)
                    val searchObj = obj as? SearchObjectResult
                    val target = searchObj?.target ?: return null
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
}

/**
 * Serializer for JsonElement passthrough
 */
object SearchObjectRawSerializer : KSerializer<JsonElement?> {
    override val descriptor: SerialDescriptor = JsonElement.serializer().descriptor

    override fun serialize(encoder: Encoder, value: JsonElement?) {
        if (value != null) {
            JsonElement.serializer().serialize(encoder, value)
        }
    }

    override fun deserialize(decoder: Decoder): JsonElement? = try {
        JsonElement.serializer().deserialize(decoder)
    } catch (e: Exception) {
        null
    }
}

/**
 * Base interface for different search result object types
 */
sealed interface SearchObject

/**
 * Wrapper for Feed.Target that represents the object field in search_result type
 * The JSON object field directly contains the Feed.Target fields
 */
@Serializable
data class SearchObjectResult(
    val target: Feed.Target,
) : SearchObject

/**
 * Search result object for type="koc_box"
 * Contains KOC (Key Opinion Consumer) content recommendation
 */
@Serializable
data class SearchObjectKocBox(
    val title: String,
    val excerpt: String,
    val type: String,
    val id: String,
    val description: String,
    @SerialName("icon_url")
    val iconUrl: String? = null,
    @SerialName("attached_info_bytes")
    val attachedInfoBytes: String? = null,
    val source: String? = null,
    @SerialName("sub_type")
    val subType: String? = null,
    @SerialName("is_multi_koc")
    val isMultiKoc: Boolean? = null,
    @SerialName("more_url")
    val moreUrl: String? = null,
    @SerialName("paid_column")
    val paidColumn: PaidColumn? = null,
) : SearchObject

/**
 * Paid column information
 */
@Serializable
data class PaidColumn(
    val id: String,
    @SerialName("paid_column_id")
    val paidColumnId: String,
    val title: String,
    @SerialName("attached_info_bytes")
    val attachedInfoBytes: String? = null,
    @SerialName("is_mid_long")
    val isMidLong: Boolean? = null,
)

/**
 * Search result object for type="knowledge_ad"
 * Contains knowledge base advertisement
 */
@Serializable
data class SearchObjectKnowledgeAd(
    val header: KnowledgeAdHeader,
    val body: KnowledgeAdBody,
    val footer: String? = null,
    @SerialName("commodity_id")
    val commodityId: String? = null,
    @SerialName("commodity_type")
    val commodityType: String? = null,
    val url: String? = null,
    val source: String? = null,
    @SerialName("card_version")
    val cardVersion: String? = null,
    @SerialName("slave_url")
    val slaveUrl: String? = null,
    @SerialName("tab_type")
    val tabType: String? = null,
    @SerialName("ab_id_list")
    val abIdList: List<String>? = null,
) : SearchObject

/**
 * Knowledge ad header
 */
@Serializable
data class KnowledgeAdHeader(
    @SerialName("card_title")
    val cardTitle: String,
    @SerialName("no_more")
    val noMore: Boolean,
)

/**
 * Knowledge ad body
 */
@Serializable
data class KnowledgeAdBody(
    val title: String,
    val description: String? = null,
    val authors: List<Author>? = null,
    val images: List<String>? = null,
    @SerialName("play_icon")
    val playIcon: String? = null,
    @SerialName("show_image")
    val showImage: String? = null,
    @SerialName("show_author")
    val showAuthor: String? = null,
)

/**
 * Author information for knowledge ad
 */
@Serializable
data class Author(
    val name: String,
    @SerialName("url_token")
    val urlToken: String,
)

/**
 * Highlight information
 * Fields can be either String or List<String> in the API
 */
@Serializable
data class Highlight(
    @Serializable(with = StringOrListSerializer::class)
    val title: List<String>? = null,
    @Serializable(with = StringOrListSerializer::class)
    val description: List<String>? = null,
)

/**
 * Custom serializer that handles both String and List<String>
 */
object StringOrListSerializer : KSerializer<List<String>?> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("StringOrList")

    override fun serialize(encoder: Encoder, value: List<String>?) {
        // Not implemented as we only need deserialization
        throw NotImplementedError("Serialization not implemented")
    }

    override fun deserialize(decoder: Decoder): List<String>? {
        require(decoder is JsonDecoder)
        val element = decoder.decodeJsonElement()

        return when {
            element is JsonPrimitive && element.isString -> {
                listOf(element.content)
            }
            element is JsonArray -> {
                element.map { it.jsonPrimitive.content }
            }
            else -> null
        }
    }
}
