@file:Suppress("unused")

package com.github.zly2006.zhihu.data

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.encoding.decodeStructure
import kotlinx.serialization.encoding.encodeStructure
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

/**
 * Search result item from Zhihu search API v3
 * The API returns items with different object structures based on type
 */
@Serializable(with = SearchResultSerializer::class)
data class SearchResult(
    val type: String,
    val id: String,
    val obj: SearchObject? = null,
    val highlight: Highlight? = null,
    val index: Int? = null,
    val hitLabels: String? = null,
) {
    /**
     * Convert search result to Feed for display
     * Returns null if the result type doesn't have displayable content
     */
    fun toFeed(): Feed? = try {
        when (val searchObj = obj) {
            is SearchObjectResult -> {
                // The object field IS the Feed.Target (answer/article/question)
                CommonFeed(
                    id = id,
                    type = "search_result",
                    verb = "SEARCH_RESULT",
                    target = searchObj.target,
                )
            }
            else -> null
        }
    } catch (e: Exception) {
        null
    }
}

/**
 * Custom serializer for SearchResult that handles polymorphic object field
 */
object SearchResultSerializer : KSerializer<SearchResult> {
    override val descriptor: SerialDescriptor = buildClassSerialDescriptor("SearchResult") {
        element<String>("type")
        element<String>("id")
        element("object", SearchObject.serializer().descriptor, isOptional = true)
        element("highlight", Highlight.serializer().descriptor, isOptional = true)
        element<Int>("index", isOptional = true)
        element<String>("hit_labels", isOptional = true)
    }

    override fun serialize(encoder: Encoder, value: SearchResult) {
        encoder.encodeStructure(descriptor) {
            encodeStringElement(descriptor, 0, value.type)
            encodeStringElement(descriptor, 1, value.id)
            value.obj?.let { encodeSerializableElement(descriptor, 2, SearchObject.serializer(), it) }
            value.highlight?.let { encodeSerializableElement(descriptor, 3, Highlight.serializer(), it) }
            value.index?.let { encodeIntElement(descriptor, 4, it) }
            value.hitLabels?.let { encodeStringElement(descriptor, 5, it) }
        }
    }

    @OptIn(ExperimentalSerializationApi::class)
    override fun deserialize(decoder: Decoder): SearchResult {
        require(decoder is JsonDecoder)

        return decoder.decodeStructure(descriptor) {
            var type = ""
            var id = ""
            var obj: SearchObject? = null
            var highlight: Highlight? = null
            var index: Int? = null
            var hitLabels: String? = null

            while (true) {
                when (val i = decodeElementIndex(descriptor)) {
                    0 -> type = decodeStringElement(descriptor, 0)
                    1 -> id = decodeStringElement(descriptor, 1)
                    2 -> {
                        // Parse object field based on type
                        obj = when (type) {
                            "search_result" -> {
                                val target = decodeSerializableElement(descriptor, 2, Feed.Target.serializer())
                                SearchObjectResult(target)
                            }
                            "koc_box" -> decodeSerializableElement(descriptor, 2, SearchObjectKocBox.serializer())
                            "knowledge_ad" -> decodeSerializableElement(descriptor, 2, SearchObjectKnowledgeAd.serializer())
                            else -> null
                        }
                    }
                    3 -> highlight = decodeNullableSerializableElement(descriptor, 3, Highlight.serializer())
                    4 -> index = decodeIntElement(descriptor, 4)
                    5 -> hitLabels = decodeStringElement(descriptor, 5)
                    CompositeDecoder.DECODE_DONE -> break
                    else -> throw IllegalArgumentException("Unknown index $i")
                }
            }

            SearchResult(type, id, obj, highlight, index, hitLabels)
        }
    }
}

/**
 * Base interface for different search result object types
 */
@Serializable
sealed interface SearchObject

/**
 * Wrapper for Feed.Target that represents the object field in search_result type
 * The JSON object field directly contains the Feed.Target fields
 */
@Serializable
@SerialName("search_result")
data class SearchObjectResult(
    val target: Feed.Target,
) : SearchObject

/**
 * Search result object for type="koc_box"
 * Contains KOC (Key Opinion Consumer) content recommendation
 */
@Serializable
@SerialName("koc_box")
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
@SerialName("knowledge_ad")
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
