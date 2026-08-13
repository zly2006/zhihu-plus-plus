package com.github.zly2006.zhihu.ui

import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.data.ZhihuJson
import com.github.zly2006.zhihu.data.installZhihuCommonClientConfig
import com.github.zly2006.zhihu.data.navDestination
import com.github.zly2006.zhihu.data.toDisplayItem
import com.github.zly2006.zhihu.navigation.Topic
import com.github.zly2006.zhihu.navigation.WritePin
import com.github.zly2006.zhihu.ui.components.getShareText
import com.github.zly2006.zhihu.ui.components.getShareTitle
import com.github.zly2006.zhihu.viewmodel.ZhihuApiEnvironment
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TopicContractTest {
    @Test
    fun buildsVerifiedTopicFeedUrls() {
        assertEquals(
            "https://www.zhihu.com/api/v5.1/topics/19550517/feeds/essence/v2?limit=20&offset=0",
            topicFeedUrl("19550517", TopicFeedTab.Discussion, TopicDiscussionSort.Essence),
        )
        assertEquals(null, normalizeTopicPagingUrl("https://evil.example/topics/1?offset=20"))
        assertEquals(
            "https://www.zhihu.com/api/v4/topics/19550517/feeds/timeline_activity?limit=20&offset=0",
            topicFeedUrl("19550517", TopicFeedTab.Discussion, TopicDiscussionSort.Timeline),
        )
        assertEquals(
            "https://www.zhihu.com/api/v4/topics/19550517/unanswered_questions?limit=20&offset=0",
            topicFeedUrl("19550517", TopicFeedTab.Unanswered),
        )
        assertEquals(
            "https://api.zhihu.com/v5.1/topics/19550517/feeds/pin-hot?offset=0&limit=10",
            topicFeedUrl("19550517", TopicFeedTab.Ideas, ideasSort = TopicIdeasSort.Hot),
        )
        assertEquals(
            "https://api.zhihu.com/v5.1/topics/19550517/feeds/pin-new?offset=0&limit=10",
            topicFeedUrl("19550517", TopicFeedTab.Ideas, ideasSort = TopicIdeasSort.Latest),
        )
    }

    @Test
    fun topicSearchUsesTopicVerticalAndEncodesQuery() {
        val url = topicSearchUrl("机器 学习")
        assertTrue("t=topic" in url)
        assertTrue("q=%E6%9C%BA%E5%99%A8+%E5%AD%A6%E4%B9%A0" in url)
    }

    @Test
    fun topicSearchRemovesOnlyObservedEmHighlightTags() {
        val json = ZhihuJson.json
            .parseToJsonElement(
                """{"data":[{"type":"search_result","object":{"id":"1","type":"topic","url":"zhihu://topic/1","name":"<em>编程</em> <b>语言</b>"}}]}""",
            ).jsonObject
        assertEquals("编程 <b>语言</b>", decodeTopicSearchResults(json).single().name)
    }

    @Test
    fun decodesIntroductionFromTopicDetail() {
        val detail = ZhihuJson.decodeJson<TopicDetail>(
            ZhihuJson.json.parseToJsonElement(
                """{"id":"1","name":"编程","introduction":"简介","followers_count":3,"questions_count":4,"is_following":true}""",
            ),
        )
        assertEquals("简介", detail.introduction)
        assertEquals(3, detail.followersCount)
        assertTrue(detail.isFollowing)
    }

    @Test
    fun decodesObservedTopicFeedTargetDescription() {
        val feed = ZhihuJson.decodeJson<Feed>(
            ZhihuJson.json.parseToJsonElement(
                """{"type":"topic_feed","id":"feed-1","target_description":"热门讨论","target":{"type":"question","id":1,"name":"问题","url":"https://www.zhihu.com/question/1","answer_count":2,"comment_count":0,"follower_count":3}}""",
            ),
        )
        assertTrue(feed.toDisplayItem(enableQualityFilter = false).details.endsWith(" · 热门讨论"))
    }

    @Test
    fun decodesObservedTopicPinShapeWithNavigation() {
        val json = ZhihuJson.json
            .parseToJsonElement(
                """{"data":[{"type":"pin","target":{"id":"123","type":"pin","url":"https://www.zhihu.com/pin/123","author":{"avatar_url":"https://pic.example/a.jpg","name":"作者"},"title":"想法标题","excerpt":"摘要","content":"正文","plain_content":"纯文本正文","counter":{"applaud":8,"comment":2,"favorite":1,"forward":0,"pv":20}}}],"paging":{"is_end":false,"next":"https://api.zhihu.com/v5.1/topics/1/feeds/pin-hot?offset=10&limit=10"}}""",
            ).jsonObject
        val item = decodeTopicPinFeeds(json).single()
        assertEquals("想法标题", item.title)
        assertEquals("纯文本正文", item.summary)
        assertEquals(
            com.github.zly2006.zhihu.navigation
                .Pin(123),
            item.navDestination,
        )
        assertEquals(
            "https://api.zhihu.com/v5.1/topics/1/feeds/pin-hot?offset=10&limit=10",
            normalizeTopicPagingUrl(json["paging"]!!.jsonObject["next"]!!.jsonPrimitive.content),
        )
    }

    @Test
    fun normalizesOnlyInternalPagingOriginAndKeepsVariantPath() {
        assertEquals(
            "https://www.zhihu.com/api/v4/topics/1/feeds/essence_v4?offset=20&limit=20",
            normalizeTopicPagingUrl("https://172.16.201.121:80/topics/1/feeds/essence_v4?offset=20&limit=20"),
        )
        assertEquals(
            "https://www.zhihu.com/api/v5.1/topics/1/feeds/essence/v2?offset=20",
            normalizeTopicPagingUrl("https://www.zhihu.com/api/v5.1/topics/1/feeds/essence/v2?offset=20"),
        )
    }

    @Test
    fun topicUsesSharedShareContract() {
        val topic = Topic("19554298", "编程")
        assertEquals("编程", getShareTitle(topic))
        assertEquals("https://www.zhihu.com/topic/19554298\n【编程】", getShareText(topic))
    }

    @Test
    fun topicWriteDestinationCarriesInitialTopic() {
        assertEquals(WritePin("19554298", "编程"), WritePin(topicId = "19554298", topicName = "编程"))
        assertEquals(WritePin(), WritePin())
    }

    @Test
    fun followAndUnfollowUseVerifiedFinalRequestsAndRollbackOnFailure() = runTest {
        val methods = mutableListOf<HttpMethod>()
        val urls = mutableListOf<String>()
        var failDelete = false
        val client = HttpClient(
            MockEngine { request ->
                methods += request.method
                urls += request.url.toString()
                if (request.method == HttpMethod.Delete && failDelete) {
                    respond("failure", HttpStatusCode.InternalServerError)
                } else if (request.method == HttpMethod.Post) {
                    respond("""{"is_following":true}""", HttpStatusCode.OK, headersOf(HttpHeaders.ContentType, "application/json"))
                } else {
                    respond("", HttpStatusCode.NoContent)
                }
            },
        ) {
            installZhihuCommonClientConfig(mutableMapOf(), "test-agent")
        }
        val environment = object : ZhihuApiEnvironment {
            override fun httpClient() = client

            override fun authenticatedCookies() = mapOf("d_c0" to "test")

            override suspend fun handleFetchFailure(tag: String?, error: Exception) = Unit
        }
        val viewModel = TopicViewModel(
            "19554298",
            TopicDetail(id = "19554298", name = "编程", followersCount = 10),
        )

        assertTrue(viewModel.setFollowing(environment, true).isSuccess)
        assertEquals(true, viewModel.detail?.isFollowing)
        assertEquals(11, viewModel.detail?.followersCount)
        failDelete = true
        assertTrue(viewModel.setFollowing(environment, false).isFailure)
        assertEquals(true, viewModel.detail?.isFollowing)
        assertEquals(11, viewModel.detail?.followersCount)
        assertEquals(listOf(HttpMethod.Post, HttpMethod.Delete), methods)
        assertEquals(List(2) { "https://www.zhihu.com/api/v4/topics/19554298/followers" }, urls)
    }
}
