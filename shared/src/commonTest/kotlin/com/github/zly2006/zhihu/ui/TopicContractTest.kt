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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TopicContractTest {
    @Test
    fun buildsVerifiedTopicFeedUrls() {
        assertEquals(
            "https://www.zhihu.com/api/v5.1/topics/19550517/feeds/top_activity/v2?limit=20&offset=0",
            topicFeedUrl("19550517", TopicFeedTab.Discussion, TopicDiscussionSort.Essence),
        )
        assertEquals(null, normalizeTopicPagingUrl("https://evil.example/topics/1?offset=20"))
        assertEquals(
            "https://www.zhihu.com/api/v5.1/topics/19550517/feeds/timeline_activity/v2?limit=20&offset=0",
            topicFeedUrl("19550517", TopicFeedTab.Discussion, TopicDiscussionSort.Timeline),
        )
        assertEquals(
            "https://www.zhihu.com/api/v5.1/topics/19550517/feeds/top_question/v2?limit=20&offset=0",
            topicFeedUrl("19550517", TopicFeedTab.Unanswered),
        )
        assertEquals(
            "https://www.zhihu.com/api/v5.1/topics/19550517/feeds/pin-hot?offset=0&limit=10",
            topicFeedUrl("19550517", TopicFeedTab.Ideas, ideasSort = TopicIdeasSort.Hot),
        )
        assertEquals(
            "https://www.zhihu.com/api/v5.1/topics/19550517/feeds/pin-new?offset=0&limit=10",
            topicFeedUrl("19550517", TopicFeedTab.Ideas, ideasSort = TopicIdeasSort.Latest),
        )
    }

    @Test
    fun decodesPlainExcerptAndInternalPublishTopicIdFromTopicDetail() {
        val detail = ZhihuJson.decodeJson<TopicDetail>(
            ZhihuJson.json.parseToJsonElement(
                """{"id":"1","name":"编程","excerpt":"纯文本简介","introduction":"<p>纯文本简介</p>","followers_count":3,"questions_count":4,"is_following":true,"topic_id":1354,"total_pv":"1628616121","discuss_count":"703641"}""",
            ),
        )
        assertEquals("纯文本简介", detail.excerpt)
        assertEquals(1354, detail.topicId)
        assertEquals("16.2 亿", formatTopicCount(detail.totalPv))
        assertEquals("70.3 万", formatTopicCount(detail.discussCount))
        assertEquals("189 万", formatTopicCount("1890565"))
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
        assertFalse(feed.toDisplayItem(enableQualityFilter = false).isFiltered)
    }

    @Test
    fun decodesObservedTopicPinShapeWithNavigation() {
        val json = ZhihuJson.json
            .parseToJsonElement(
                """{"data":[{"type":"pin","target":{"id":"123","type":"pin","url":"https://www.zhihu.com/pin/123","author":{"avatar_url":"https://pic.example/a.jpg","name":"作者"},"title":"想法标题","excerpt":"摘要","content":"正文","plain_content":"纯文本正文","counter":{"applaud":8,"comment":2,"favorite":1,"forward":0,"pv":20}}}],"paging":{"is_end":false,"next":"https://www.zhihu.com/api/v5.1/topics/1/feeds/pin-hot?offset=10&limit=10"}}""",
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
            "https://www.zhihu.com/api/v5.1/topics/1/feeds/pin-hot?offset=10&limit=10",
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
        assertEquals(null, normalizeTopicPagingUrl("not a url"))
    }

    @Test
    fun initializesOfficialTopicSectionsWithoutFallingBackToHot() {
        TopicViewModel("1").apply {
            initializeSection("top-answers")
            assertEquals(TopicFeedTab.Discussion, selectedTab)
            assertEquals(TopicDiscussionSort.Essence, discussionSort)
        }
        TopicViewModel("1").apply {
            initializeSection("newest")
            assertEquals(TopicFeedTab.Discussion, selectedTab)
            assertEquals(TopicDiscussionSort.Timeline, discussionSort)
        }
        TopicViewModel("1").apply {
            initializeSection("unanswered")
            assertEquals(TopicFeedTab.Unanswered, selectedTab)
        }
    }

    @Test
    fun topicUsesSharedShareContract() {
        val topic = Topic("19554298", "编程")
        assertEquals("编程", getShareTitle(topic))
        assertEquals("https://www.zhihu.com/topic/19554298\n【编程】", getShareText(topic))
    }

    @Test
    fun topicWriteDestinationCarriesInitialTopic() {
        assertEquals(
            WritePin("编程", "1354"),
            WritePin(topicName = "编程", publishTopicId = "1354"),
        )
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
