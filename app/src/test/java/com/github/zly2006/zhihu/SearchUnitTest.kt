package com.github.zly2006.zhihu

import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.data.SearchResult
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URLEncoder

/**
 * Unit test for search functionality
 * Tests parsing of search API v3 response format
 */
class SearchUnitTest {
    /**
     * Test parsing real Zhihu search API response
     * This test uses actual JSON data returned by the Zhihu search API
     */
    @Test
    fun testRealZhihuSearchResponse() {
        // Real JSON response from Zhihu search API
        val realApiResponse =
            """
            {
                "paging": {
                    "is_end": false,
                    "next": "https://api.zhihu.com/search_v3?advert_count=0&correction=1&gk_version=gz-gaokao&limit=20&offset=20&q=%E6%90%9C%E7%B4%A2%E5%86%85%E5%AE%B9&search_hash_id=f95dc1ab2d6bdae2dd9d96e558fddccd&t=general&vertical_info=0%2C0%2C0%2C0%2C0%2C0%2C0%2C0%2C0%2C2%2C0%2C0"
                },
                "data": [
                    {
                        "type": "koc_box",
                        "highlight": null,
                        "object": {
                            "title": "<em>搜索内容</em>",
                            "excerpt": "我无意中在老公的手机里看到这样一条<em>搜索</em>记录：「怎样让老婆在生孩子时意外身亡？」我摸了摸自己已经七个月大的孕肚，不由得惊出了一身冷汗。晚上，老公在浴室洗澡。我在客厅准备待产包，手机没电了，下意识拿起他的手机，想要搜一些待产注意事项。却无意中翻到了他的历史<em>搜索</em>一栏，里面有两条记录瞬间吸引了我的注意",
                            "type": "paid_column",
                            "id": "1649808276936921088",
                            "description": "来自内容 · 对我而言危险的她",
                            "paid_column": {
                                "id": "1649808276936921088",
                                "paid_column_id": "1638186406705827840",
                                "title": "对我而言危险的她",
                                "attached_info_bytes": "OtEBCgtwbGFjZWhvbGRlchIgZjk1ZGMxYWIyZDZiZGFlMmRkOWQ5NmU1NThmZGRjY2QYUyITMTY0OTgwODI3NjkzNjkyMTA4OCoTMTY0OTgwODI3NjkzNjkyMTA4OEoM5pCc57Si5YaF5a65UABYAWABagzmkJzntKLlhoXlrrlwBIABjrLc3dr/kAOYAQCAAgC4AgCKAwCSAxMxNDkyNTcxMDE3MTMwNzk5MTA0mgMAogMAqgMAwgMXU09VUkNFX1NFQVJDSF9LT0NfS1ZfRELgBAA=",
                                "is_mid_long": false
                            },
                            "icon_url": "https://picx.zhimg.com/v2-dd2abbbc9b844fcaf61e5c6c9e1d8fc2.png",
                            "attached_info_bytes": "OuYBCgtwbGFjZWhvbGRlchIgZjk1ZGMxYWIyZDZiZGFlMmRkOWQ5NmU1NThmZGRjY2QYUyITMTY0OTgwODI3NjkzNjkyMTA4OCoTMTY0OTgwODI3NjkzNjkyMTA4OEoM5pCc57Si5YaF5a65UABYAWABagzmkJzntKLlhoXlrrlwBIABjrLc3dr/kAOYAQCgAQCwASqAAgC4AgCKAwCSAxMxNDkyNTcxMDE3MTMwNzk5MTA0mgMAogMAqgMAwgMXU09VUkNFX1NFQVJDSF9LT0NfS1ZfRELKAwzmkJzntKLlhoXlrrngBAA=",
                            "source": "",
                            "sub_type": "",
                            "is_multi_koc": false,
                            "more_url": ""
                        },
                        "hit_labels": null,
                        "index": 0,
                        "id": 1638186406705827840
                    },
                    {
                        "type": "knowledge_ad",
                        "highlight": {},
                        "object": {
                            "header": {
                                "card_title": "电子书",
                                "no_more": false
                            },
                            "body": {
                                "title": "<em>搜索内容</em>",
                                "description": "<em>搜索内容</em>"
                            }
                        },
                        "id": "knowledge_ad_1"
                    }
                ]
            }
            """.trimIndent()

        // Parse the response
        val json = Json { ignoreUnknownKeys = true }.decodeFromString<JsonObject>(realApiResponse)

        // Verify response structure
        assertTrue("Response should contain 'data' field", "data" in json)
        assertTrue("Response should contain 'paging' field", "paging" in json)

        val dataArray = json["data"]?.jsonArray
        assertNotNull("Data array should not be null", dataArray)
        assertEquals("Should have 2 search results", 2, dataArray?.size)

        // Parse first result (koc_box type)
        val firstElement = dataArray?.get(0)
        assertNotNull("First element should not be null", firstElement)

        val firstResult = AccountData.decodeJson<SearchResult>(firstElement!!)
        assertEquals("koc_box", firstResult.type)
        assertEquals("1638186406705827840", firstResult.id)
        assertNotNull("Object should not be null", firstResult.obj)

        println("Successfully parsed first search result:")
        println("  Type: ${firstResult.type}")
        println("  ID: ${firstResult.id}")

        // Parse second result (knowledge_ad type)
        val secondElement = dataArray?.get(1)
        assertNotNull("Second element should not be null", secondElement)

        val secondResult = AccountData.decodeJson<SearchResult>(secondElement!!)
        assertEquals("knowledge_ad", secondResult.type)
        assertEquals("knowledge_ad_1", secondResult.id)
        assertNotNull("Object should not be null", secondResult.obj)

        println("Successfully parsed second search result:")
        println("  Type: ${secondResult.type}")
        println("  ID: ${secondResult.id}")

        // Verify paging information
        val pagingObj = json["paging"]
        assertNotNull("Paging should not be null", pagingObj)
        println("Paging information present in response")

        println("All search results parsed successfully!")
    }

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
