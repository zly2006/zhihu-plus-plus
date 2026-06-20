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

package com.github.zly2006.zhihu

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.CollectionContent
import com.github.zly2006.zhihu.navigation.Person
import com.github.zly2006.zhihu.navigation.Pin
import com.github.zly2006.zhihu.navigation.Question
import com.github.zly2006.zhihu.navigation.Search
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.data.FeedDisplayItem
import com.github.zly2006.zhihu.shared.data.FollowedQuestion
import com.github.zly2006.zhihu.shared.data.FollowedTopic
import com.github.zly2006.zhihu.shared.data.OfficialBadge
import com.github.zly2006.zhihu.shared.data.ZhihuJson
import com.github.zly2006.zhihu.shared.data.toFeedDisplayItemNavDestinationJson
import com.github.zly2006.zhihu.test.InstrumentedTestEnvironment
import com.github.zly2006.zhihu.test.MainActivityComposeRule
import com.github.zly2006.zhihu.test.RecordingNavigator
import com.github.zly2006.zhihu.test.ZhihuMockApi
import com.github.zly2006.zhihu.test.performVerticalSwipeCycle
import com.github.zly2006.zhihu.test.resetAppPreferences
import com.github.zly2006.zhihu.test.seedViewModel
import com.github.zly2006.zhihu.test.setScreenContent
import com.github.zly2006.zhihu.ui.PEOPLE_SCREEN_ACTIVITIES_LIST_TAG
import com.github.zly2006.zhihu.ui.PEOPLE_SCREEN_ANSWERS_LIST_TAG
import com.github.zly2006.zhihu.ui.PEOPLE_SCREEN_ANSWER_COUNT_TAG
import com.github.zly2006.zhihu.ui.PEOPLE_SCREEN_ANSWER_SORT_HOT_TAG
import com.github.zly2006.zhihu.ui.PEOPLE_SCREEN_ANSWER_SORT_TIME_TAG
import com.github.zly2006.zhihu.ui.PEOPLE_SCREEN_ARTICLES_LIST_TAG
import com.github.zly2006.zhihu.ui.PEOPLE_SCREEN_ARTICLE_COUNT_TAG
import com.github.zly2006.zhihu.ui.PEOPLE_SCREEN_ARTICLE_SORT_HOT_TAG
import com.github.zly2006.zhihu.ui.PEOPLE_SCREEN_ARTICLE_SORT_TIME_TAG
import com.github.zly2006.zhihu.ui.PEOPLE_SCREEN_AVATAR_TAG
import com.github.zly2006.zhihu.ui.PEOPLE_SCREEN_BLOCK_BUTTON_TAG
import com.github.zly2006.zhihu.ui.PEOPLE_SCREEN_COLLECTIONS_LIST_TAG
import com.github.zly2006.zhihu.ui.PEOPLE_SCREEN_COLUMNS_LIST_TAG
import com.github.zly2006.zhihu.ui.PEOPLE_SCREEN_FOLLOWERS_LIST_TAG
import com.github.zly2006.zhihu.ui.PEOPLE_SCREEN_FOLLOWER_COUNT_TAG
import com.github.zly2006.zhihu.ui.PEOPLE_SCREEN_FOLLOWING_COUNT_TAG
import com.github.zly2006.zhihu.ui.PEOPLE_SCREEN_FOLLOWING_LIST_TAG
import com.github.zly2006.zhihu.ui.PEOPLE_SCREEN_FOLLOW_BUTTON_TAG
import com.github.zly2006.zhihu.ui.PEOPLE_SCREEN_HEADER_TAG
import com.github.zly2006.zhihu.ui.PEOPLE_SCREEN_OFFICIAL_BADGE_TAG
import com.github.zly2006.zhihu.ui.PEOPLE_SCREEN_PINS_LIST_TAG
import com.github.zly2006.zhihu.ui.PEOPLE_SCREEN_QUESTIONS_LIST_TAG
import com.github.zly2006.zhihu.ui.PEOPLE_SCREEN_RECOMMENDATION_BLOCK_BUTTON_TAG
import com.github.zly2006.zhihu.ui.PEOPLE_SCREEN_ROOT_TAG
import com.github.zly2006.zhihu.ui.PEOPLE_SCREEN_SEARCH_BUTTON_TAG
import com.github.zly2006.zhihu.ui.PEOPLE_SCREEN_SUBSCRIPTIONS_LIST_TAG
import com.github.zly2006.zhihu.ui.PeopleScreen
import com.github.zly2006.zhihu.ui.PersonViewModel
import io.ktor.http.HttpMethod
import kotlinx.serialization.encodeToString
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PeopleScreenInstrumentedTest {
    @get:Rule
    val composeRule: MainActivityComposeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        composeRule.resetAppPreferences()
        ZhihuMockApi.install(enabled = true)
        ZhihuMockApi.reset()
    }

    @After
    fun tearDown() {
        ZhihuMockApi.install(enabled = InstrumentedTestEnvironment.isMockMode())
    }

    @Test
    fun headerButtonsStatsAnswerAndArticleTabsRemainDeterministicOffline() {
        /*
         * Expected behavior:
         * 1. The profile header must render the seeded avatar area plus all four statistics from a
         *    precreated production ViewModel and a mocked profile fetch.
         * 2. Follow, block, and recommendation-block buttons must each use the real production
         *    mutation path while staying offline through mocked HTTP/local database state.
         * 3. On the answer tab, both sort buttons should issue deterministic production refreshes
         *    and a deep scroll should keep the seeded answer row interactive for navigation.
         * 4. On the article tab, the same sort and deep-row navigation behavior must remain stable.
         */
        val viewModel = seededViewModel()
        val navigator = setPeopleScreen()

        composeRule.onNodeWithTag(PEOPLE_SCREEN_ROOT_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(PEOPLE_SCREEN_HEADER_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(PEOPLE_SCREEN_OFFICIAL_BADGE_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("社区成就: 英语等 5 个话题下的优秀答主").assertIsDisplayed()
        composeRule.onNodeWithTag(PEOPLE_SCREEN_AVATAR_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(PEOPLE_SCREEN_ANSWER_COUNT_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(PEOPLE_SCREEN_ARTICLE_COUNT_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(PEOPLE_SCREEN_FOLLOWER_COUNT_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(PEOPLE_SCREEN_FOLLOWING_COUNT_TAG).assertIsDisplayed()

        composeRule.onNodeWithTag(PEOPLE_SCREEN_FOLLOW_BUTTON_TAG).performClick()
        composeRule.onNodeWithTag(PEOPLE_SCREEN_BLOCK_BUTTON_TAG).performClick()
        composeRule.onNodeWithTag(PEOPLE_SCREEN_RECOMMENDATION_BLOCK_BUTTON_TAG).performClick()
        composeRule.waitUntil("Expected profile actions to update state", timeoutMillis = 5_000) {
            viewModel.isFollowing && viewModel.isBlocking && viewModel.isBlockedInRecommendations
        }
        composeRule.waitUntilRequestCount(HttpMethod.Post, "members/${ROOT_PERSON.urlToken}/followers", 1)
        composeRule.waitUntilRequestCount(HttpMethod.Post, "members/${ROOT_PERSON.urlToken}/actions/block", 1)

        composeRule.onNodeWithTag(PEOPLE_SCREEN_ANSWER_SORT_TIME_TAG).performClick()
        composeRule.waitUntilRequestCount(HttpMethod.Get, "members/${ROOT_PERSON.urlToken}/answers?sort_by=created", 1)
        composeRule.onNodeWithTag(PEOPLE_SCREEN_ANSWER_SORT_HOT_TAG).performClick()
        composeRule.waitUntil("Expected answer sort to return to voteups", timeoutMillis = 5_000) {
            viewModel.answersFeedModel.sortBy == "voteups"
        }
        composeRule.onNodeWithTag(PEOPLE_SCREEN_ANSWERS_LIST_TAG).assertIsDisplayed()
        composeRule
            .onNodeWithTag(PEOPLE_SCREEN_ANSWERS_LIST_TAG)
            .performScrollToNode(hasTestTag("people_screen_answer_item_12"))
        composeRule.onNodeWithTag("people_screen_answer_item_12").assertIsDisplayed()
        composeRule
            .onNodeWithTag(PEOPLE_SCREEN_ANSWERS_LIST_TAG)
            .performScrollToNode(hasTestTag("people_screen_answer_item_2"))
        composeRule.onNodeWithTag("people_screen_answer_item_2").performClick()

        composeRule.onNodeWithTag("people_screen_tab_1").performClick()
        composeRule.onNodeWithTag(PEOPLE_SCREEN_ARTICLE_SORT_HOT_TAG).performClick()
        composeRule.waitUntilRequestCount(HttpMethod.Get, "members/${ROOT_PERSON.urlToken}/articles?sort_by=voteups", 1)
        composeRule.onNodeWithTag(PEOPLE_SCREEN_ARTICLE_SORT_TIME_TAG).performClick()
        composeRule.waitUntil("Expected article sort to return to created", timeoutMillis = 5_000) {
            viewModel.articlesFeedModel.sortBy == "created"
        }
        composeRule.onNodeWithTag(PEOPLE_SCREEN_ARTICLES_LIST_TAG).assertIsDisplayed()
        composeRule
            .onNodeWithTag(PEOPLE_SCREEN_ARTICLES_LIST_TAG)
            .performScrollToNode(hasTestTag("people_screen_article_item_12"))
        composeRule.onNodeWithTag("people_screen_article_item_12").assertIsDisplayed()
        composeRule
            .onNodeWithTag(PEOPLE_SCREEN_ARTICLES_LIST_TAG)
            .performScrollToNode(hasTestTag("people_screen_article_item_2"))
        composeRule.onNodeWithTag("people_screen_article_item_2").performClick()

        assertEquals(
            listOf(
                Article(type = ArticleType.Answer, id = 2L, title = "离线提问 2", excerpt = "离线回答摘要 2"),
                Article(type = ArticleType.Article, id = 2L, title = "离线文章 2", excerpt = "离线文章摘要 2"),
            ),
            navigator.destinations,
        )
    }

    @Test
    fun remainingTabsSupportScrollLoadMoreAndRepresentativeClicksOffline() {
        /*
         * Expected behavior:
         * 1. The non-primary tabs must all render from seeded local data, remain scrollable, and
         *    trigger their production load-more paths near the tail through mocked HTTP.
         * 2. The activities, questions, pins, followers, and following tabs each expose a stable
         *    representative click path that should navigate to the seeded destination exactly once.
         * 3. The collections and columns tabs do not currently navigate anywhere in production, but
         *    their seeded rows still need to stay visible and interactive after swipe cycles.
         * 4. Tab switching itself must remain deterministic through the tagged tab row.
         */
        seededViewModel(itemCount = 18)
        val navigator = setPeopleScreen()

        composeRule.onNodeWithTag("people_screen_tab_2").performClick()
        composeRule.onNodeWithTag(PEOPLE_SCREEN_ACTIVITIES_LIST_TAG).assertIsDisplayed()
        composeRule
            .onNodeWithTag(PEOPLE_SCREEN_ACTIVITIES_LIST_TAG)
            .performScrollToNode(hasTestTag("people_screen_activity_item_activity-18"))
        composeRule.waitUntilRequestCount(HttpMethod.Get, "moments/${ROOT_PERSON.urlToken}/activities", 1)
        composeRule.onNodeWithTag("people_screen_activity_item_activity-18").assertIsDisplayed()
        composeRule
            .onNodeWithTag(PEOPLE_SCREEN_ACTIVITIES_LIST_TAG)
            .performScrollToNode(hasTestTag("people_screen_activity_item_activity-2"))
        composeRule.onNodeWithTag("people_screen_activity_item_activity-2").performClick()

        composeRule.onNodeWithTag("people_screen_tab_3").performClick()
        composeRule.onNodeWithTag(PEOPLE_SCREEN_COLLECTIONS_LIST_TAG).assertIsDisplayed()
        composeRule
            .onNodeWithTag(PEOPLE_SCREEN_COLLECTIONS_LIST_TAG)
            .performScrollToNode(hasTestTag("people_screen_collection_item_collection-18"))
        composeRule.waitUntilRequestCount(HttpMethod.Get, "members/${ROOT_PERSON.urlToken}/favlists", 1)
        composeRule.onNodeWithTag(PEOPLE_SCREEN_COLLECTIONS_LIST_TAG).performVerticalSwipeCycle()
        composeRule
            .onNodeWithTag(PEOPLE_SCREEN_COLLECTIONS_LIST_TAG)
            .performScrollToNode(hasTestTag("people_screen_collection_item_collection-18"))
        composeRule.onNodeWithTag("people_screen_collection_item_collection-18").assertIsDisplayed()

        composeRule.onNodeWithTag("people_screen_tab_4").performClick()
        composeRule.onNodeWithTag(PEOPLE_SCREEN_QUESTIONS_LIST_TAG).assertIsDisplayed()
        composeRule
            .onNodeWithTag(PEOPLE_SCREEN_QUESTIONS_LIST_TAG)
            .performScrollToNode(hasTestTag("people_screen_question_item_18"))
        composeRule.waitUntilRequestCount(HttpMethod.Get, "members/${ROOT_PERSON.urlToken}/questions", 1)
        composeRule
            .onNodeWithTag(PEOPLE_SCREEN_QUESTIONS_LIST_TAG)
            .performScrollToNode(hasTestTag("people_screen_question_item_2"))
        composeRule.onNodeWithTag("people_screen_question_item_2").performClick()

        composeRule.onNodeWithTag("people_screen_tab_5").performClick()
        composeRule.onNodeWithTag(PEOPLE_SCREEN_PINS_LIST_TAG).assertIsDisplayed()
        composeRule
            .onNodeWithTag(PEOPLE_SCREEN_PINS_LIST_TAG)
            .performScrollToNode(hasTestTag("people_screen_pin_item_18"))
        composeRule.waitUntilRequestCount(HttpMethod.Get, "pins/${ROOT_PERSON.urlToken}/moments", 1)
        composeRule
            .onNodeWithTag(PEOPLE_SCREEN_PINS_LIST_TAG)
            .performScrollToNode(hasTestTag("people_screen_pin_item_2"))
        composeRule.onNodeWithTag("people_screen_pin_item_2").performClick()

        composeRule.onNodeWithTag("people_screen_tab_6").performClick()
        composeRule.onNodeWithTag(PEOPLE_SCREEN_COLUMNS_LIST_TAG).assertIsDisplayed()
        composeRule
            .onNodeWithTag(PEOPLE_SCREEN_COLUMNS_LIST_TAG)
            .performScrollToNode(hasTestTag("people_screen_column_item_column-18"))
        composeRule.waitUntilRequestCount(HttpMethod.Get, "members/${ROOT_PERSON.urlToken}/column-contributions", 1)
        composeRule.onNodeWithTag(PEOPLE_SCREEN_COLUMNS_LIST_TAG).performVerticalSwipeCycle()
        composeRule
            .onNodeWithTag(PEOPLE_SCREEN_COLUMNS_LIST_TAG)
            .performScrollToNode(hasTestTag("people_screen_column_item_column-18"))
        composeRule.onNodeWithTag("people_screen_column_item_column-18").assertIsDisplayed()

        composeRule.onNodeWithTag("people_screen_tab_7").performClick()
        composeRule.onNodeWithTag(PEOPLE_SCREEN_FOLLOWERS_LIST_TAG).assertIsDisplayed()
        composeRule
            .onNodeWithTag(PEOPLE_SCREEN_FOLLOWERS_LIST_TAG)
            .performScrollToNode(hasTestTag("people_screen_follower_item_follower-18"))
        composeRule.waitUntilRequestCount(HttpMethod.Get, "people/${ROOT_PERSON.id}/followers", 1)
        composeRule
            .onNodeWithTag(PEOPLE_SCREEN_FOLLOWERS_LIST_TAG)
            .performScrollToNode(hasTestTag("people_screen_follower_action_follower-2"))
        composeRule.onNodeWithTag("people_screen_follower_action_follower-2").performClick()

        composeRule.onNodeWithTag("people_screen_tab_8").performClick()
        composeRule.onNodeWithTag(PEOPLE_SCREEN_FOLLOWING_LIST_TAG).assertIsDisplayed()
        composeRule
            .onNodeWithTag(PEOPLE_SCREEN_FOLLOWING_LIST_TAG)
            .performScrollToNode(hasTestTag("people_screen_following_item_following-18"))
        composeRule.waitUntilRequestCount(HttpMethod.Get, "members/${ROOT_PERSON.urlToken}/followees", 1)
        composeRule
            .onNodeWithTag(PEOPLE_SCREEN_FOLLOWING_LIST_TAG)
            .performScrollToNode(hasTestTag("people_screen_following_action_following-2"))
        composeRule.onNodeWithTag("people_screen_following_action_following-2").performClick()

        assertEquals(
            listOf(
                Search(query = "离线动态 2"),
                Question(2L, "离线问题 2"),
                Pin(2L),
                Person(id = "follower-2", name = "粉丝 2", urlToken = "follower-token-2"),
                Person(id = "following-2", name = "关注的人 2", urlToken = "following-token-2"),
            ),
            navigator.destinations,
        )
    }

    @Test
    fun duplicatedAuthorAnswerKeysDoNotCrashAnswerListOffline() {
        val duplicatedAnswerId = 2_544_209_984L
        seededViewModel(
            itemCount = 0,
            answers = listOf(
                seededAnswer(
                    id = duplicatedAnswerId,
                    questionId = 1001L,
                    questionTitle = "重复 key 问题 A",
                    excerpt = "重复 key 回答 A",
                ),
                seededAnswer(
                    id = duplicatedAnswerId,
                    questionId = 1002L,
                    questionTitle = "重复 key 问题 B",
                    excerpt = "重复 key 回答 B",
                ),
            ),
            mockAnswers = emptyList(),
        )
        setPeopleScreen()

        composeRule.onNodeWithTag(PEOPLE_SCREEN_ANSWERS_LIST_TAG).assertIsDisplayed()
        composeRule.onNodeWithText("重复 key 问题 A").assertIsDisplayed()
        composeRule.onNodeWithText("重复 key 问题 B").assertIsDisplayed()
        composeRule.onAllNodesWithTag("people_screen_answer_item_$duplicatedAnswerId").assertCountEquals(2)
    }

    @Test
    fun headerSearchActionNavigatesToMemberScopedSearchOffline() {
        seededViewModel()
        val navigator = setPeopleScreen()

        val headerBounds = composeRule
            .onAllNodesWithTag(PEOPLE_SCREEN_HEADER_TAG)
            .fetchSemanticsNodes()
            .single()
            .boundsInRoot
        val avatarBounds = composeRule
            .onAllNodesWithTag(PEOPLE_SCREEN_AVATAR_TAG)
            .fetchSemanticsNodes()
            .single()
            .boundsInRoot
        val searchBounds = composeRule
            .onAllNodesWithTag(PEOPLE_SCREEN_SEARCH_BUTTON_TAG)
            .fetchSemanticsNodes()
            .single()
            .boundsInRoot
        assertTrue(
            "搜索按钮不应占用 TopAppBar actions 槽位并挤压 header，headerBounds=$headerBounds searchBounds=$searchBounds",
            searchBounds.left < headerBounds.right,
        )
        assertTrue(
            "搜索按钮应位于用户信息首屏右上区域，不能落到数据/操作区附近，avatarBounds=$avatarBounds searchBounds=$searchBounds",
            searchBounds.center.y < avatarBounds.bottom,
        )

        composeRule.onNodeWithTag(PEOPLE_SCREEN_SEARCH_BUTTON_TAG).assertIsDisplayed().performClick()
        composeRule.waitForIdle()

        assertEquals(
            listOf(
                Search(
                    restrictedMemberHashId = ROOT_PERSON.id,
                    restrictedMemberName = "离线用户",
                ),
            ),
            navigator.destinations,
        )
    }

    @Test
    fun followingSubscriptionsTabMatchesOfficialEntryPointsOffline() {
        /*
         * Expected behavior:
         * 1. The profile tab row exposes a Zhihu-Web-style "关注订阅" entry.
         * 2. Inside that page, non-duplicated official entry names are available as chips:
         *    我订阅的专栏、关注的话题、关注的问题、关注的收藏夹.
         * 3. Followed questions and followed collections use native navigation destinations,
         *    matching the app's existing question and collection-detail screens.
         */
        seededViewModel(itemCount = 8)
        val navigator = setPeopleScreen(person = ROOT_PERSON.copy(jumpTo = "关注订阅"))

        composeRule.onNodeWithTag(PEOPLE_SCREEN_SUBSCRIPTIONS_LIST_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag("people_screen_subscription_tab_0").assertExists()
        composeRule.onNodeWithTag("people_screen_subscription_tab_1").assertExists()
        composeRule.onNodeWithTag("people_screen_subscription_tab_2").assertExists()
        composeRule.onNodeWithTag("people_screen_subscription_tab_3").assertExists()

        composeRule.onNodeWithTag("people_screen_subscription_tab_1").performClick()
        composeRule.onNodeWithTag("people_screen_followed_topic_item_topic-1").assertIsDisplayed()

        composeRule.onNodeWithTag("people_screen_subscription_tab_2").performClick()
        composeRule.onNodeWithTag("people_screen_followed_question_item_1").performClick()

        composeRule.onNodeWithTag("people_screen_subscription_tab_3").performClick()
        composeRule.onNodeWithTag("people_screen_collection_item_follow-collection-1").performClick()

        assertEquals(
            listOf(
                Question(1L, "关注的问题 1"),
                CollectionContent("follow-collection-1"),
            ),
            navigator.destinations,
        )
    }

    private fun setPeopleScreen(
        person: Person = ROOT_PERSON,
    ): RecordingNavigator = composeRule.setScreenContent {
        PeopleScreen(
            person = person,
        )
    }

    private fun MainActivityComposeRule.waitUntilRequestCount(
        method: HttpMethod,
        urlSubstring: String,
        count: Int,
    ) {
        waitUntil("Expected $count $method requests containing $urlSubstring", timeoutMillis = 5_000) {
            ZhihuMockApi.requestCount(method, urlSubstring) >= count
        }
    }

    private fun seededViewModel(
        itemCount: Int = 12,
        answers: List<DataHolder.Answer>? = null,
        mockAnswers: List<DataHolder.Answer>? = null,
    ): PersonViewModel {
        val seededViewModel = composeRule.seedViewModel<PersonViewModel> {
            PersonViewModel(ROOT_PERSON.copy())
        }
        val answerData = answers ?: List(itemCount) { index -> seededAnswer(index + 1L) }
        val articleData = List(itemCount) { index -> seededArticle(index + 1L) }
        mockPeopleScreenApis(
            itemCount = itemCount,
            answers = mockAnswers ?: answerData,
            articles = articleData,
        )
        val seededActivities = List(itemCount) { index ->
            FeedDisplayItem(
                title = "离线动态 ${index + 1}",
                summary = "动态摘要 ${index + 1}",
                details = "动态详情 ${index + 1}",
                feed = null,
                navDestinationJson = Search(query = "离线动态 ${index + 1}").toFeedDisplayItemNavDestinationJson(),
                localFeedId = "activity-${index + 1}",
            )
        }

        composeRule.activity.runOnUiThread {
            seededViewModel.avatar = "https://example.invalid/avatar/root.png"
            seededViewModel.name = "离线用户"
            seededViewModel.headline = "离线个人简介"
            seededViewModel.officialBadge = OfficialBadge(
                title = "优秀答主",
                description = "英语等 5 个话题下的优秀答主",
                iconUrl = DataHolder.ZH_PLUS_AUTHOR_BADGE_ICON,
            )
            seededViewModel.officialBadgeDetails = listOf(
                OfficialBadge("社区成就", "英语等 5 个话题下的优秀答主"),
            )
            seededViewModel.followerCount = 120
            seededViewModel.followingCount = 45
            seededViewModel.answerCount = answers?.size ?: itemCount
            seededViewModel.articleCount = itemCount
            seededViewModel.isFollowing = false
            seededViewModel.isBlocking = false
            seededViewModel.isBlockedInRecommendations = false
            seededViewModel.memberHashId = ROOT_PERSON.id

            seededViewModel.answersFeedModel.allData.clear()
            seededViewModel.answersFeedModel.allData.addAll(answerData)
            seededViewModel.articlesFeedModel.allData.clear()
            seededViewModel.articlesFeedModel.allData.addAll(articleData)
            seededViewModel.activitiesFeedModel.displayItems.clear()
            seededViewModel.activitiesFeedModel.displayItems.addAll(seededActivities)
            seededViewModel.collectionsFeedModel.allData.clear()
            seededViewModel.collectionsFeedModel.allData.addAll(List(itemCount) { index -> seededCollection(index + 1) })
            seededViewModel.questionsFeedModel.allData.clear()
            seededViewModel.questionsFeedModel.allData.addAll(List(itemCount) { index -> seededQuestion(index + 1L) })
            seededViewModel.pinsFeedModel.allData.clear()
            seededViewModel.pinsFeedModel.allData.addAll(List(itemCount) { index -> seededPin(index + 1) })
            seededViewModel.columnsFeedModel.allData.clear()
            seededViewModel.columnsFeedModel.allData.addAll(List(itemCount) { index -> seededColumn(index + 1) })
            seededViewModel.followersFeedModel.allData.clear()
            seededViewModel.followersFeedModel.allData.addAll(List(itemCount) { index -> seededPeople("follower", index + 1, "粉丝") })
            seededViewModel.followingFeedModel.allData.clear()
            seededViewModel.followingFeedModel.allData.addAll(List(itemCount) { index -> seededPeople("following", index + 1, "关注的人") })
            seededViewModel.followingColumnsFeedModel.allData.clear()
            seededViewModel.followingColumnsFeedModel.allData.addAll(List(itemCount) { index -> seededFollowedColumn(index + 1) })
            seededViewModel.followingTopicsFeedModel.allData.clear()
            seededViewModel.followingTopicsFeedModel.allData.addAll(List(itemCount) { index -> seededFollowedTopic(index + 1) })
            seededViewModel.followingQuestionsFeedModel.allData.clear()
            seededViewModel.followingQuestionsFeedModel.allData.addAll(List(itemCount) { index -> seededFollowedQuestion(index + 1) })
            seededViewModel.followingCollectionsFeedModel.allData.clear()
            seededViewModel.followingCollectionsFeedModel.allData.addAll(List(itemCount) { index -> seededFollowedCollection(index + 1) })
        }
        composeRule.waitForIdle()
        return seededViewModel
    }

    private fun mockPeopleScreenApis(
        itemCount: Int,
        answers: List<DataHolder.Answer>,
        articles: List<DataHolder.Article>,
    ) {
        val token = ROOT_PERSON.urlToken
        val id = ROOT_PERSON.id
        ZhihuMockApi.mockJsonPrefix(
            method = HttpMethod.Get,
            urlPrefix = "https://api.zhihu.com/people/$token",
            body = ZhihuJson.json.encodeToString(seededProfile(itemCount)),
        )
        ZhihuMockApi.mockJson(
            method = HttpMethod.Post,
            url = "https://www.zhihu.com/api/v4/members/$token/followers",
            body = """{"follower_count":121}""",
        )
        ZhihuMockApi.mockJson(
            method = HttpMethod.Post,
            url = "https://www.zhihu.com/api/v4/members/$token/actions/block",
            body = "{}",
        )
        mockPagePrefix("https://www.zhihu.com/api/v4/members/$token/answers?sort_by=", answers)
        mockPagePrefix("https://www.zhihu.com/api/v4/members/$token/articles?sort_by=", articles)
        mockEmptyPagePrefix("https://www.zhihu.com/api/v3/moments/$token/activities")
        mockEmptyPagePrefix("https://www.zhihu.com/api/v4/members/$token/favlists")
        mockEmptyPagePrefix("https://www.zhihu.com/api/v4/members/$token/questions")
        mockEmptyPagePrefix("https://www.zhihu.com/api/v4/v2/pins/$token/moments")
        mockEmptyPagePrefix("https://www.zhihu.com/api/v4/members/$token/column-contributions")
        mockEmptyPagePrefix("https://api.zhihu.com/people/$id/followers")
        mockEmptyPagePrefix("https://www.zhihu.com/api/v4/members/$token/followees")
        mockEmptyPagePrefix("https://www.zhihu.com/api/v4/members/$token/following-columns")
        mockEmptyPagePrefix("https://www.zhihu.com/api/v4/members/$token/following-topic-contributions")
        mockEmptyPagePrefix("https://www.zhihu.com/api/v4/members/$token/following-questions")
        mockEmptyPagePrefix("https://www.zhihu.com/api/v4/members/$token/following-favlists")
    }

    private inline fun <reified T> mockPagePrefix(
        urlPrefix: String,
        data: List<T>,
    ) {
        ZhihuMockApi.mockJsonPrefix(
            method = HttpMethod.Get,
            urlPrefix = urlPrefix,
            body = """{"data":${ZhihuJson.json.encodeToString(data)}}""",
        )
    }

    private fun mockEmptyPagePrefix(urlPrefix: String) {
        ZhihuMockApi.mockJsonPrefix(
            method = HttpMethod.Get,
            urlPrefix = urlPrefix,
            body = """{"data":[]}""",
        )
    }

    private fun seededProfile(itemCount: Int) = DataHolder.People(
        id = ROOT_PERSON.id,
        urlToken = ROOT_PERSON.urlToken,
        name = "离线用户",
        avatarUrl = "https://example.invalid/avatar/root.png",
        url = "https://www.zhihu.com/people/${ROOT_PERSON.urlToken}",
        headline = "离线个人简介",
        gender = 0,
        followerCount = 120,
        followingCount = 45,
        answerCount = itemCount,
        articlesCount = itemCount,
        apiBadgeV2 = DataHolder.BadgeV2(
            title = "优秀答主",
            icon = DataHolder.ZH_PLUS_AUTHOR_BADGE_ICON,
            detailBadges = listOf(
                DataHolder.BadgeV2.Badge(
                    type = "best",
                    detailType = "best_answerer",
                    title = "社区成就",
                    description = "英语等 5 个话题下的优秀答主",
                    icon = DataHolder.ZH_PLUS_AUTHOR_BADGE_ICON,
                    badgeStatus = "passed",
                ),
            ),
        ),
    )

    private fun seededAnswer(
        id: Long,
        questionId: Long = id,
        questionTitle: String = "离线提问 $id",
        excerpt: String = "离线回答摘要 $id",
    ) = DataHolder.Answer(
        answerType = "answer",
        author = seededAuthor("answer-author-$id", "回答作者 $id"),
        canComment = DataHolder.CanComment(status = true, reason = ""),
        content = "<p>离线回答正文 $id</p>",
        createdTime = 1_713_500_000L,
        excerpt = excerpt,
        id = id,
        question = DataHolder.AnswerModelQuestion(
            created = 1_713_400_000L,
            id = questionId,
            questionType = "normal",
            title = questionTitle,
            type = "question",
            updatedTime = 1_713_500_100L,
            url = "https://www.zhihu.com/question/$questionId",
        ),
        thanksCount = 0,
        type = "answer",
        updatedTime = 1_713_500_100L,
        url = "https://www.zhihu.com/question/$questionId/answer/$id",
        voteupCount = id.toInt(),
    )

    private fun seededArticle(id: Long) = DataHolder.Article(
        id = id,
        author = seededAuthor("article-author-$id", "文章作者 $id"),
        canComment = DataHolder.CanComment(status = true, reason = ""),
        title = "离线文章 $id",
        content = "<p>离线文章正文 $id</p>",
        excerpt = "离线文章摘要 $id",
        type = "article",
        created = 1_713_500_000L,
        updated = 1_713_500_200L,
        url = "https://zhuanlan.zhihu.com/p/$id",
        voteupCount = id.toInt(),
    )

    private fun seededQuestion(id: Long) = DataHolder.Question(
        type = "question",
        id = id,
        title = "离线问题 $id",
        questionType = "normal",
        created = 1_713_500_000L,
        updatedTime = 1_713_500_200L,
        url = "https://www.zhihu.com/question/$id",
        answerCount = id.toInt(),
        visitCount = 100 + id.toInt(),
        commentCount = 2,
        followerCount = 10 + id.toInt(),
        detail = "离线问题详情 $id",
        relationship = DataHolder.QuestionRelationship(),
        topics = emptyList(),
        author = seededAuthor("question-author-$id", "提问者 $id"),
        voteupCount = 0,
    )

    private fun seededCollection(index: Int) = DataHolder.Collection(
        id = "collection-$index",
        title = "离线收藏夹 $index",
        url = "https://www.zhihu.com/collection/$index",
        answerCount = index,
        followerCount = 20 + index,
    )

    private fun seededPin(index: Int) = DataHolder.Pin(
        id = index.toString(),
        author = seededAuthor("pin-author-$index", "想法作者 $index"),
        excerptTitle = "离线想法 $index",
        likeCount = index,
        commentCount = index / 2,
    )

    private fun seededColumn(index: Int) = DataHolder.Column(
        id = "column-$index",
        title = "离线专栏 $index",
        description = "专栏描述 $index",
        articlesCount = index,
        followerCount = 30 + index,
    )

    private fun seededFollowedColumn(index: Int) = DataHolder.Column(
        id = "follow-column-$index",
        title = "我订阅的专栏 $index",
        description = "订阅专栏描述 $index",
        articlesCount = index,
        followerCount = 30 + index,
    )

    private fun seededFollowedTopic(index: Int) = FollowedTopic(
        id = "topic-$index",
        name = "关注的话题 $index",
        avatarUrl = "https://example.invalid/topic-$index.png",
    )

    private fun seededFollowedQuestion(index: Int) = FollowedQuestion(
        id = index.toString(),
        title = "关注的问题 $index",
    )

    private fun seededFollowedCollection(index: Int) = DataHolder.Collection(
        id = "follow-collection-$index",
        title = "关注的收藏夹 $index",
        url = "https://www.zhihu.com/collection/follow-collection-$index",
        answerCount = index,
        followerCount = 40 + index,
    )

    private fun seededPeople(prefix: String, index: Int, namePrefix: String) = DataHolder.People(
        id = "$prefix-$index",
        urlToken = "$prefix-token-$index",
        name = "$namePrefix $index",
        avatarUrl = "https://example.invalid/$prefix-$index.png",
        url = "https://www.zhihu.com/people/$prefix-token-$index",
        headline = "$namePrefix $index 的简介",
        gender = 0,
        followerCount = 100 + index,
        followingCount = 50 + index,
        answerCount = index,
        articlesCount = index / 2,
    )

    private fun seededAuthor(id: String, name: String) = DataHolder.Author(
        avatarUrl = "https://example.invalid/avatar/$id.png",
        gender = 0,
        headline = "$name 的签名",
        id = id,
        isAdvertiser = false,
        isOrg = false,
        name = name,
        type = "people",
        url = "https://www.zhihu.com/people/$id",
        urlToken = id,
        userType = "people",
    )

    private companion object {
        val ROOT_PERSON = Person(
            id = "offline-root-person",
            urlToken = "offline-root-person",
            name = "离线用户",
        )
    }
}
