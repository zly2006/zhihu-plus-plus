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

package com.github.zly2006.zhihu

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.Person
import com.github.zly2006.zhihu.navigation.Pin
import com.github.zly2006.zhihu.navigation.Question
import com.github.zly2006.zhihu.navigation.Search
import com.github.zly2006.zhihu.test.MainActivityComposeRule
import com.github.zly2006.zhihu.test.RecordingNavigator
import com.github.zly2006.zhihu.test.performVerticalSwipeCycle
import com.github.zly2006.zhihu.test.resetAppPreferences
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
import com.github.zly2006.zhihu.ui.PEOPLE_SCREEN_PINS_LIST_TAG
import com.github.zly2006.zhihu.ui.PEOPLE_SCREEN_QUESTIONS_LIST_TAG
import com.github.zly2006.zhihu.ui.PEOPLE_SCREEN_RECOMMENDATION_BLOCK_BUTTON_TAG
import com.github.zly2006.zhihu.ui.PEOPLE_SCREEN_ROOT_TAG
import com.github.zly2006.zhihu.ui.PeopleListUiState
import com.github.zly2006.zhihu.ui.PeopleProfileUiState
import com.github.zly2006.zhihu.ui.PeopleScreen
import com.github.zly2006.zhihu.ui.PeopleScreenTestOverrides
import com.github.zly2006.zhihu.ui.PeopleScreenUiState
import com.github.zly2006.zhihu.ui.PeopleSortedListUiState
import com.github.zly2006.zhihu.ui.peopleScreenAnswerItemTag
import com.github.zly2006.zhihu.ui.peopleScreenArticleItemTag
import com.github.zly2006.zhihu.ui.peopleScreenCollectionItemTag
import com.github.zly2006.zhihu.ui.peopleScreenColumnItemTag
import com.github.zly2006.zhihu.ui.peopleScreenFollowerActionTag
import com.github.zly2006.zhihu.ui.peopleScreenFollowerItemTag
import com.github.zly2006.zhihu.ui.peopleScreenFollowingActionTag
import com.github.zly2006.zhihu.ui.peopleScreenFollowingItemTag
import com.github.zly2006.zhihu.ui.peopleScreenPinItemTag
import com.github.zly2006.zhihu.ui.peopleScreenQuestionItemTag
import com.github.zly2006.zhihu.ui.peopleScreenTabTag
import com.github.zly2006.zhihu.viewmodel.feed.BaseFeedViewModel
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
    }

    @Test
    fun headerButtonsStatsAnswerAndArticleTabsRemainDeterministicOffline() {
        /*
         * Expected behavior:
         * 1. The profile header must render the seeded avatar area plus all four statistics from the
         *    injected offline snapshot, so the test never depends on a real profile fetch.
         * 2. Follow, block, and recommendation-block buttons must each flip the local state exactly
         *    once and report the new boolean through the injected callbacks.
         * 3. On the answer tab, both sort buttons should report deterministic sort keys and a deep
         *    scroll should keep the seeded answer row interactive for navigation.
         * 4. On the article tab, the same sort and deep-row navigation behavior must remain stable.
         */
        val followStates = mutableListOf<Boolean>()
        val blockStates = mutableListOf<Boolean>()
        val recommendationStates = mutableListOf<Boolean>()
        val answerSorts = mutableListOf<String>()
        val articleSorts = mutableListOf<String>()
        val navigator = setPeopleScreen(
            overrides = PeopleScreenTestOverrides(
                initialUiState = seededUiState(),
                onToggleFollow = { followStates += it },
                onToggleBlock = { blockStates += it },
                onToggleRecommendationBlock = { recommendationStates += it },
                onAnswerSortChange = { answerSorts += it },
                onArticleSortChange = { articleSorts += it },
            ),
        )

        composeRule.onNodeWithTag(PEOPLE_SCREEN_ROOT_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(PEOPLE_SCREEN_HEADER_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(PEOPLE_SCREEN_AVATAR_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(PEOPLE_SCREEN_ANSWER_COUNT_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(PEOPLE_SCREEN_ARTICLE_COUNT_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(PEOPLE_SCREEN_FOLLOWER_COUNT_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(PEOPLE_SCREEN_FOLLOWING_COUNT_TAG).assertIsDisplayed()

        composeRule.onNodeWithTag(PEOPLE_SCREEN_FOLLOW_BUTTON_TAG).performClick()
        composeRule.onNodeWithTag(PEOPLE_SCREEN_BLOCK_BUTTON_TAG).performClick()
        composeRule.onNodeWithTag(PEOPLE_SCREEN_RECOMMENDATION_BLOCK_BUTTON_TAG).performClick()
        assertEquals(listOf(true), followStates)
        assertEquals(listOf(true), blockStates)
        assertEquals(listOf(true), recommendationStates)

        composeRule.onNodeWithTag(PEOPLE_SCREEN_ANSWER_SORT_TIME_TAG).performClick()
        composeRule.onNodeWithTag(PEOPLE_SCREEN_ANSWER_SORT_HOT_TAG).performClick()
        composeRule.onNodeWithTag(PEOPLE_SCREEN_ANSWERS_LIST_TAG).assertIsDisplayed()
        composeRule
            .onNodeWithTag(PEOPLE_SCREEN_ANSWERS_LIST_TAG)
            .performScrollToNode(hasTestTag(peopleScreenAnswerItemTag(12)))
        composeRule.onNodeWithTag(peopleScreenAnswerItemTag(12)).assertIsDisplayed()
        composeRule
            .onNodeWithTag(PEOPLE_SCREEN_ANSWERS_LIST_TAG)
            .performScrollToNode(hasTestTag(peopleScreenAnswerItemTag(2)))
        composeRule.onNodeWithTag(peopleScreenAnswerItemTag(2)).performClick()
        assertEquals(listOf("created", "voteups"), answerSorts)

        composeRule.onNodeWithTag(peopleScreenTabTag(1)).performClick()
        composeRule.onNodeWithTag(PEOPLE_SCREEN_ARTICLE_SORT_HOT_TAG).performClick()
        composeRule.onNodeWithTag(PEOPLE_SCREEN_ARTICLE_SORT_TIME_TAG).performClick()
        composeRule.onNodeWithTag(PEOPLE_SCREEN_ARTICLES_LIST_TAG).assertIsDisplayed()
        composeRule
            .onNodeWithTag(PEOPLE_SCREEN_ARTICLES_LIST_TAG)
            .performScrollToNode(hasTestTag(peopleScreenArticleItemTag(12)))
        composeRule.onNodeWithTag(peopleScreenArticleItemTag(12)).assertIsDisplayed()
        composeRule
            .onNodeWithTag(PEOPLE_SCREEN_ARTICLES_LIST_TAG)
            .performScrollToNode(hasTestTag(peopleScreenArticleItemTag(2)))
        composeRule.onNodeWithTag(peopleScreenArticleItemTag(2)).performClick()
        assertEquals(listOf("voteups", "created"), articleSorts)

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
         *    trigger their dedicated offline load-more callbacks near the tail instead of fetching.
         * 2. The activities, questions, pins, followers, and following tabs each expose a stable
         *    representative click path that should navigate to the seeded destination exactly once.
         * 3. The collections and columns tabs do not currently navigate anywhere in production, but
         *    their seeded rows still need to stay visible and interactive after swipe cycles.
         * 4. Tab switching itself must remain deterministic through the tagged tab row.
         */
        var activitiesLoadMore = 0
        var collectionsLoadMore = 0
        var questionsLoadMore = 0
        var pinsLoadMore = 0
        var columnsLoadMore = 0
        var followersLoadMore = 0
        var followingLoadMore = 0
        val navigator = setPeopleScreen(
            overrides = PeopleScreenTestOverrides(
                initialUiState = seededUiState(itemCount = 18),
                onActivitiesLoadMore = { activitiesLoadMore++ },
                onCollectionsLoadMore = { collectionsLoadMore++ },
                onQuestionsLoadMore = { questionsLoadMore++ },
                onPinsLoadMore = { pinsLoadMore++ },
                onColumnsLoadMore = { columnsLoadMore++ },
                onFollowersLoadMore = { followersLoadMore++ },
                onFollowingLoadMore = { followingLoadMore++ },
            ),
        )

        composeRule.onNodeWithTag(peopleScreenTabTag(2)).performClick()
        composeRule.onNodeWithTag(PEOPLE_SCREEN_ACTIVITIES_LIST_TAG).assertIsDisplayed()
        composeRule
            .onNodeWithTag(PEOPLE_SCREEN_ACTIVITIES_LIST_TAG)
            .performScrollToNode(hasTestTag("people_screen_activity_item_activity-15"))
        composeRule.onNodeWithTag("people_screen_activity_item_activity-15").assertIsDisplayed()
        composeRule
            .onNodeWithTag(PEOPLE_SCREEN_ACTIVITIES_LIST_TAG)
            .performScrollToNode(hasTestTag("people_screen_activity_item_activity-2"))
        composeRule.onNodeWithTag("people_screen_activity_item_activity-2").performClick()

        composeRule.onNodeWithTag(peopleScreenTabTag(3)).performClick()
        composeRule.onNodeWithTag(PEOPLE_SCREEN_COLLECTIONS_LIST_TAG).assertIsDisplayed()
        composeRule
            .onNodeWithTag(PEOPLE_SCREEN_COLLECTIONS_LIST_TAG)
            .performScrollToNode(hasTestTag(peopleScreenCollectionItemTag("collection-15")))
        composeRule.onNodeWithTag(PEOPLE_SCREEN_COLLECTIONS_LIST_TAG).performVerticalSwipeCycle()
        composeRule
            .onNodeWithTag(PEOPLE_SCREEN_COLLECTIONS_LIST_TAG)
            .performScrollToNode(hasTestTag(peopleScreenCollectionItemTag("collection-15")))
        composeRule.onNodeWithTag(peopleScreenCollectionItemTag("collection-15")).assertIsDisplayed()

        composeRule.onNodeWithTag(peopleScreenTabTag(4)).performClick()
        composeRule.onNodeWithTag(PEOPLE_SCREEN_QUESTIONS_LIST_TAG).assertIsDisplayed()
        composeRule
            .onNodeWithTag(PEOPLE_SCREEN_QUESTIONS_LIST_TAG)
            .performScrollToNode(hasTestTag(peopleScreenQuestionItemTag(15)))
        composeRule
            .onNodeWithTag(PEOPLE_SCREEN_QUESTIONS_LIST_TAG)
            .performScrollToNode(hasTestTag(peopleScreenQuestionItemTag(2)))
        composeRule.onNodeWithTag(peopleScreenQuestionItemTag(2)).performClick()

        composeRule.onNodeWithTag(peopleScreenTabTag(5)).performClick()
        composeRule.onNodeWithTag(PEOPLE_SCREEN_PINS_LIST_TAG).assertIsDisplayed()
        composeRule
            .onNodeWithTag(PEOPLE_SCREEN_PINS_LIST_TAG)
            .performScrollToNode(hasTestTag(peopleScreenPinItemTag("15")))
        composeRule
            .onNodeWithTag(PEOPLE_SCREEN_PINS_LIST_TAG)
            .performScrollToNode(hasTestTag(peopleScreenPinItemTag("2")))
        composeRule.onNodeWithTag(peopleScreenPinItemTag("2")).performClick()

        composeRule.onNodeWithTag(peopleScreenTabTag(6)).performClick()
        composeRule.onNodeWithTag(PEOPLE_SCREEN_COLUMNS_LIST_TAG).assertIsDisplayed()
        composeRule
            .onNodeWithTag(PEOPLE_SCREEN_COLUMNS_LIST_TAG)
            .performScrollToNode(hasTestTag(peopleScreenColumnItemTag("column-15")))
        composeRule.onNodeWithTag(PEOPLE_SCREEN_COLUMNS_LIST_TAG).performVerticalSwipeCycle()
        composeRule
            .onNodeWithTag(PEOPLE_SCREEN_COLUMNS_LIST_TAG)
            .performScrollToNode(hasTestTag(peopleScreenColumnItemTag("column-15")))
        composeRule.onNodeWithTag(peopleScreenColumnItemTag("column-15")).assertIsDisplayed()

        composeRule.onNodeWithTag(peopleScreenTabTag(7)).performClick()
        composeRule.onNodeWithTag(PEOPLE_SCREEN_FOLLOWERS_LIST_TAG).assertIsDisplayed()
        composeRule
            .onNodeWithTag(PEOPLE_SCREEN_FOLLOWERS_LIST_TAG)
            .performScrollToNode(hasTestTag(peopleScreenFollowerItemTag("follower-15")))
        composeRule
            .onNodeWithTag(PEOPLE_SCREEN_FOLLOWERS_LIST_TAG)
            .performScrollToNode(hasTestTag(peopleScreenFollowerActionTag("follower-2")))
        composeRule.onNodeWithTag(peopleScreenFollowerActionTag("follower-2")).performClick()

        composeRule.onNodeWithTag(peopleScreenTabTag(8)).performClick()
        composeRule.onNodeWithTag(PEOPLE_SCREEN_FOLLOWING_LIST_TAG).assertIsDisplayed()
        composeRule
            .onNodeWithTag(PEOPLE_SCREEN_FOLLOWING_LIST_TAG)
            .performScrollToNode(hasTestTag(peopleScreenFollowingItemTag("following-15")))
        composeRule
            .onNodeWithTag(PEOPLE_SCREEN_FOLLOWING_LIST_TAG)
            .performScrollToNode(hasTestTag(peopleScreenFollowingActionTag("following-2")))
        composeRule.onNodeWithTag(peopleScreenFollowingActionTag("following-2")).performClick()

        assertTrue(activitiesLoadMore > 0)
        assertTrue(collectionsLoadMore > 0)
        assertTrue(questionsLoadMore > 0)
        assertTrue(pinsLoadMore > 0)
        assertTrue(columnsLoadMore > 0)
        assertTrue(followersLoadMore > 0)
        assertTrue(followingLoadMore > 0)
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

    private fun setPeopleScreen(overrides: PeopleScreenTestOverrides): RecordingNavigator = composeRule.setScreenContent {
        PeopleScreen(
            innerPadding = PaddingValues(),
            person = ROOT_PERSON,
            testOverrides = overrides,
        )
    }

    private fun seededUiState(itemCount: Int = 12): PeopleScreenUiState = PeopleScreenUiState(
        profile = PeopleProfileUiState(
            avatar = "https://example.invalid/avatar/root.png",
            name = "离线用户",
            headline = "离线个人简介",
            followerCount = 120,
            followingCount = 45,
            answerCount = itemCount,
            articleCount = itemCount,
            isFollowing = false,
            isBlocking = false,
            isBlockedInRecommendations = false,
        ),
        answers = PeopleSortedListUiState(
            sortBy = "voteups",
            items = List(itemCount) { index -> seededAnswer(index + 1L) },
            isEnd = false,
        ),
        articles = PeopleSortedListUiState(
            sortBy = "created",
            items = List(itemCount) { index -> seededArticle(index + 1L) },
            isEnd = false,
        ),
        activities = PeopleListUiState(
            items = List(itemCount) { index ->
                BaseFeedViewModel.FeedDisplayItem(
                    title = "离线动态 ${index + 1}",
                    summary = "动态摘要 ${index + 1}",
                    details = "动态详情 ${index + 1}",
                    feed = null,
                    navDestination = Search(query = "离线动态 ${index + 1}"),
                    localFeedId = "activity-${index + 1}",
                )
            },
            isEnd = false,
        ),
        collections = PeopleListUiState(
            items = List(itemCount) { index -> seededCollection(index + 1) },
            isEnd = false,
        ),
        questions = PeopleListUiState(
            items = List(itemCount) { index -> seededQuestion(index + 1L) },
            isEnd = false,
        ),
        pins = PeopleListUiState(
            items = List(itemCount) { index -> seededPin(index + 1) },
            isEnd = false,
        ),
        columns = PeopleListUiState(
            items = List(itemCount) { index -> seededColumn(index + 1) },
            isEnd = false,
        ),
        followers = PeopleListUiState(
            items = List(itemCount) { index -> seededPeople("follower", index + 1, "粉丝") },
            isEnd = false,
        ),
        following = PeopleListUiState(
            items = List(itemCount) { index -> seededPeople("following", index + 1, "关注的人") },
            isEnd = false,
        ),
    )

    private fun seededAnswer(id: Long) = DataHolder.Answer(
        answerType = "answer",
        author = seededAuthor("answer-author-$id", "回答作者 $id"),
        canComment = DataHolder.CanComment(status = true, reason = ""),
        content = "<p>离线回答正文 $id</p>",
        createdTime = 1_713_500_000L,
        excerpt = "离线回答摘要 $id",
        id = id,
        question = DataHolder.AnswerModelQuestion(
            created = 1_713_400_000L,
            id = id,
            questionType = "normal",
            title = "离线提问 $id",
            type = "question",
            updatedTime = 1_713_500_100L,
            url = "https://www.zhihu.com/question/$id",
        ),
        thanksCount = 0,
        type = "answer",
        updatedTime = 1_713_500_100L,
        url = "https://www.zhihu.com/question/$id/answer/$id",
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
