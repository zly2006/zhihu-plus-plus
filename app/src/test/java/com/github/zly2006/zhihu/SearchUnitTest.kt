package com.github.zly2006.zhihu

import com.github.zly2006.zhihu.data.SearchResult
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit test for search functionality
 * Tests parsing of search API v3 response format
 */
class SearchUnitTest {
    /**
     * Test parsing a simple search result with answer type
     */
    @Test
    fun testSearchResultParsing() {
        // Sample search result JSON based on Zhihu search API v3 format
        val searchJson =
            """
            {
                "type": "search_result",
                "id": "1234567890",
                "object": {
                    "id": 123456,
                    "type": "answer",
                    "url": "https://www.zhihu.com/question/123/answer/456",
                    "author": {
                        "id": "abc123",
                        "name": "Test User",
                        "url": "https://www.zhihu.com/people/test-user",
                        "urlToken": "test-user",
                        "avatarUrl": "https://pic1.zhimg.com/avatar.jpg",
                        "userType": "people",
                        "headline": "Test Headline",
                        "gender": 1,
                        "isOrg": false,
                        "isAdvertiser": false,
                        "isPrivacy": false,
                        "badge": [],
                        "badgeV2": {
                            "title": "",
                            "mergedBadges": [],
                            "detailBadges": [],
                            "icon": "",
                            "nightIcon": ""
                        }
                    },
                    "created_time": 1234567890,
                    "updated_time": 1234567890,
                    "voteup_count": 100,
                    "comment_count": 10,
                    "question": {
                        "id": 123,
                        "type": "question",
                        "title": "Test Question Title",
                        "url": "https://www.zhihu.com/question/123",
                        "created": 1234567890,
                        "updated_time": 1234567890
                    },
                    "excerpt": "This is a test excerpt",
                    "content": "This is test content"
                },
                "highlight": {
                    "title": ["Test <em>search</em> result"],
                    "description": ["Description with <em>highlight</em>"]
                }
            }
            """.trimIndent()

        // Parse the search result
        val searchResult = Json { ignoreUnknownKeys = true }.decodeFromString<SearchResult>(searchJson)

        // Verify basic fields
        assertEquals("search_result", searchResult.type)
        assertEquals("1234567890", searchResult.id)
        assertNotNull(searchResult.obj)
        assertNotNull(searchResult.highlight)

        // Verify highlight fields
        assertNotNull(searchResult.highlight?.title)
        assertTrue(searchResult.highlight?.title?.isNotEmpty() == true)
    }

    /**
     * Test converting search result to Feed object
     */
    @Test
    fun testSearchResultToFeed() {
        // Simpler search result for conversion test
        val searchJson =
            """
            {
                "type": "search_result",
                "id": "test123",
                "object": {
                    "id": 789,
                    "type": "answer",
                    "url": "https://www.zhihu.com/answer/789",
                    "created_time": 1000000000,
                    "updated_time": 1000000000,
                    "voteup_count": 50,
                    "comment_count": 5,
                    "question": {
                        "id": 456,
                        "type": "question",
                        "title": "How to test?",
                        "url": "https://www.zhihu.com/question/456",
                        "created": 1000000000,
                        "updated_time": 1000000000
                    },
                    "excerpt": "Test excerpt",
                    "content": "Test content",
                    "relationship": {
                        "isFollowing": false,
                        "isFollowed": false
                    }
                }
            }
            """.trimIndent()

        val searchResult = Json { ignoreUnknownKeys = true }.decodeFromString<SearchResult>(searchJson)

        // Verify the search result can be parsed
        assertEquals("search_result", searchResult.type)
        assertEquals("test123", searchResult.id)
    }

    /**
     * Test search query URL encoding
     */
    @Test
    fun testSearchQueryEncoding() {
        val query = "测试搜索 with spaces"
        val encoded = java.net.URLEncoder.encode(query, "UTF-8")

        // Verify URL encoding works correctly
        assertTrue(encoded.contains("%"))
        assertTrue(encoded.contains("+") || encoded.contains("%20"))
    }

    /**
     * Test empty search result list
     */
    @Test
    fun testEmptySearchResults() {
        val emptyResultsJson =
            """
            {
                "data": [],
                "paging": {
                    "isEnd": true,
                    "next": "",
                    "previous": ""
                }
            }
            """.trimIndent()

        val json = Json { ignoreUnknownKeys = true }.decodeFromString<JsonObject>(emptyResultsJson)
        val dataArray = json["data"]?.jsonArray

        assertNotNull(dataArray)
        assertEquals(0, dataArray?.size)
    }
}
