package com.github.zly2006.zhihu.viewmodel.local

import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.shared.data.CommonFeed
import com.github.zly2006.zhihu.shared.data.Feed
import com.github.zly2006.zhihu.shared.recommendation.LocalContentAffinity
import com.github.zly2006.zhihu.shared.recommendation.LocalReasonPreference
import kotlinx.coroutines.test.runTest
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class LocalRecommendationSupportTest {
    @Test
    fun buildFallbackRecommendationsRanksRecentResultsAndCreatesEntries() = runTest {
        val database = testLocalContentDatabase()
        val dao = database.contentDao()
        dao.insertResult(
            CrawlingResult(
                taskId = 1L,
                contentId = "42",
                title = "回答标题",
                summary = "摘要",
                url = "https://www.zhihu.com/answer/42",
                reason = CrawlingReason.Trending,
                score = 10.0,
            ),
        )

        val entries = buildFallbackRecommendations(
            dao = dao,
            userBehaviorAnalyzer = UserBehaviorAnalyzer(dao),
            feedGenerator = FeedGenerator(dao),
            limit = 5,
        )

        assertEquals(1, entries.size)
        assertEquals("回答标题", entries.single().feed.title)
        assertEquals("answer:42", entries.single().result.contentId)
        assertEquals(
            Article(type = ArticleType.Answer, id = 42L, title = "回答标题"),
            entries.single().navDestination,
        )
        database.close()
    }

    @Test
    fun createLocalFeedDisplayItemKeepsLocalRecommendationMetadata() {
        val item = createLocalFeedDisplayItem(
            LocalRecommendationEntry(
                feed = LocalFeed(
                    id = "local_feed_answer_42",
                    resultId = 1L,
                    title = "回答标题",
                    summary = "回答摘要",
                    reasonDisplay = "热门推荐",
                    navDestination = "answer:42",
                ),
                result = CrawlingResult(
                    id = 1L,
                    taskId = 1L,
                    contentId = "answer:42",
                    title = "回答标题",
                    summary = "回答摘要",
                    url = "https://www.zhihu.com/answer/42",
                    reason = CrawlingReason.Trending,
                ),
                navDestination = Article(type = ArticleType.Answer, id = 42L, title = "回答标题"),
            ),
        )

        assertEquals("回答标题", item.title)
        assertEquals("回答摘要", item.summary)
        assertEquals("热门推荐", item.details)
        assertEquals("answer:42", item.localContentId)
        assertEquals("local_feed_answer_42", item.localFeedId)
        assertEquals(CrawlingReason.Trending.name, item.localReason)
        assertNotNull(item.navDestinationJson)
    }

    @Test
    fun cleanupLocalRecommendationDataUsesWeekAndMonthRetention() = runTest {
        val database = testLocalContentDatabase()
        val dao = database.contentDao()
        val now = 100L * 24L * 60L * 60L * 1000L
        val olderThanWeek = now - 8L * 24L * 60L * 60L * 1000L
        val olderThanMonth = now - 31L * 24L * 60L * 60L * 1000L
        dao.insertTasks(
            listOf(
                CrawlingTask(
                    url = "completed",
                    reason = CrawlingReason.Trending,
                    status = CrawlingStatus.Completed,
                    createdAt = olderThanWeek,
                ),
                CrawlingTask(
                    url = "not-started",
                    reason = CrawlingReason.Trending,
                    status = CrawlingStatus.NotStarted,
                    createdAt = olderThanWeek,
                ),
            ),
        )
        dao.insertResult(
            CrawlingResult(
                taskId = 1L,
                contentId = "answer:1",
                title = "标题",
                summary = "摘要",
                url = "https://www.zhihu.com/answer/1",
                reason = CrawlingReason.Trending,
                createdAt = olderThanMonth,
            ),
        )
        dao.insertFeed(
            LocalFeed(
                id = "local_feed_answer_1",
                resultId = 1L,
                title = "标题",
                summary = "摘要",
                reasonDisplay = "热门推荐",
                navDestination = "answer:1",
                createdAt = olderThanMonth,
            ),
        )
        dao.insertBehavior(
            UserBehavior(
                contentId = "answer:1",
                action = "click",
                timestamp = olderThanMonth,
            ),
        )

        cleanupLocalRecommendationData(dao, nowMillis = now)

        assertEquals(emptyList(), dao.getTasksByStatus(CrawlingStatus.Completed))
        assertEquals(1, dao.getTasksByStatus(CrawlingStatus.NotStarted).size)
        assertEquals(emptyList(), dao.getRecentResults(10))
        assertEquals(emptyList(), dao.getRecentFeeds(10))
        assertEquals(emptyList(), dao.getBehaviorsSince(0L))
        database.close()
    }

    @Test
    fun collectCandidateResultsNormalizesAndDeduplicatesCandidates() = runTest {
        val database = testLocalContentDatabase()
        val dao = database.contentDao()
        dao.insertResults(
            listOf(
                CrawlingResult(
                    taskId = 1L,
                    contentId = "42",
                    title = "标题 1",
                    summary = "摘要",
                    url = "https://www.zhihu.com/answer/42",
                    reason = CrawlingReason.Trending,
                    score = 10.0,
                ),
                CrawlingResult(
                    taskId = 1L,
                    contentId = "answer:42",
                    title = "标题 2",
                    summary = "摘要",
                    url = "https://www.zhihu.com/answer/42",
                    reason = CrawlingReason.Following,
                    score = 9.0,
                ),
                CrawlingResult(
                    taskId = 1L,
                    contentId = "bad",
                    title = "标题 3",
                    summary = "摘要",
                    url = "https://www.zhihu.com/not-content",
                    reason = CrawlingReason.Trending,
                    score = 8.0,
                ),
            ),
        )

        val candidates = collectCandidateResults(dao, limit = 10)

        assertEquals(listOf("answer:42"), candidates.map { it.contentId })
        database.close()
    }

    @Test
    fun ensurePendingTasksAddsTasksForReasonsBelowThreshold() = runTest {
        val database = testLocalContentDatabase()
        val dao = database.contentDao()
        dao.insertTask(
            CrawlingTask(
                url = "existing",
                reason = CrawlingReason.Trending,
            ),
        )

        ensurePendingTasks(dao)

        assertEquals(3, dao.getTaskCountByReasonAndStatus(CrawlingReason.Trending, CrawlingStatus.NotStarted))
        assertEquals(3, dao.getTaskCountByReasonAndStatus(CrawlingReason.Following, CrawlingStatus.NotStarted))
        database.close()
    }

    @Test
    fun insertRefreshTasksAddsOneTaskForReasonsBelowPendingThreshold() = runTest {
        val database = testLocalContentDatabase()
        val dao = database.contentDao()
        dao.insertTasks(
            listOf(
                CrawlingTask(
                    url = "existing-1",
                    reason = CrawlingReason.Trending,
                ),
                CrawlingTask(
                    url = "existing-2",
                    reason = CrawlingReason.Trending,
                ),
            ),
        )

        insertRefreshTasks(dao)

        assertEquals(2, dao.getTaskCountByReasonAndStatus(CrawlingReason.Trending, CrawlingStatus.NotStarted))
        assertEquals(1, dao.getTaskCountByReasonAndStatus(CrawlingReason.Following, CrawlingStatus.NotStarted))
        database.close()
    }

    @Test
    fun waitForTaskCompletionReturnsWhenNoTaskInProgress() = runTest {
        val database = testLocalContentDatabase()

        waitForTaskCompletion(database.contentDao(), maxWaitTimeMs = 5_000L)

        database.close()
    }

    @Test
    fun createCrawlingResultKeepsFeedTargetMappingAndMultiplier() {
        val feed = CommonFeed(
            target = Feed.AnswerTarget(
                id = 42L,
                url = "https://www.zhihu.com/question/1/answer/42",
                question = Feed.QuestionTarget(
                    id = 1L,
                    _title = "问题标题",
                    url = "https://www.zhihu.com/question/1",
                    type = "question",
                ),
                excerpt = "回答摘要",
                voteupCount = 100,
                commentCount = 0,
            ),
        )

        val result = createCrawlingResult(feed, taskId = 7L, reason = CrawlingReason.Trending, scoreMultiplier = 1.2)

        assertNotNull(result)
        assertEquals(7L, result.taskId)
        assertEquals("answer:42", result.contentId)
        assertEquals("问题标题", result.title)
        assertEquals("回答摘要", result.summary)
        assertEquals("https://www.zhihu.com/question/1/answer/42", result.url)
        assertEquals(CrawlingReason.Trending, result.reason)
        assertEquals(2.4, result.score)
    }

    @Test
    fun isVoteupFeedMatchesBriefOrAttachedInfo() {
        assertTrue(isVoteupFeed(CommonFeed(brief = "某人赞同了回答")))
        assertTrue(isVoteupFeed(CommonFeed(attachedInfo = "ACTION_VOTEUP")))
        assertEquals(false, isVoteupFeed(CommonFeed(brief = "某人关注了问题")))
    }

    @Test
    fun extractQuestionIdFromUrlKeepsQuestionRegex() {
        assertEquals("123", extractQuestionIdFromUrl("https://www.zhihu.com/question/123/answer/456"))
        assertEquals(null, extractQuestionIdFromUrl("https://www.zhihu.com/answer/456"))
    }

    @Test
    fun createInitializerTasksKeepsUrlsAndPriorities() {
        assertEquals(
            "https://api.zhihu.com/moments_v3?feed_type=recommend&offset=20",
            createFollowingTasks(3).last().url,
        )
        assertEquals(8, createFollowingTasks(1).single().priority)
        assertEquals(
            "https://www.zhihu.com/api/v3/feed/topstory/recommend?desktop=true&limit=20&offset=40",
            createTrendingTasks(3).last().url,
        )
        assertEquals(
            "https://www.zhihu.com/api/v3/feed/topstory/recommend?desktop=true&limit=10&offset=10",
            createDefaultUpvotedQuestionTasks(2).last().url,
        )
        assertEquals(
            "https://www.zhihu.com/api/v4/questions/123/feeds?limit=20",
            createQuestionFeedTask("123").url,
        )
        assertEquals(
            "https://www.zhihu.com/api/v3/feed/topstory/recommend?action_feed=True&limit=20&offset=20",
            createFollowingUpvoteTasks(2).last().url,
        )
        assertEquals(
            "https://www.zhihu.com/api/v3/feed/topstory/recommend?desktop=true&limit=15&offset=15",
            createCollaborativeFilteringTasks(2).last().url,
        )
    }

    @Test
    fun extractQuestionIdFromContentIdKeepsLegacyFallback() {
        assertEquals("123", extractQuestionIdFromContentId("question:123"))
        assertEquals("123", extractQuestionIdFromContentId("q123"))
        assertEquals(null, extractQuestionIdFromContentId("answer:456"))
    }

    @Test
    fun localContentInitializerCreatesColdStartTasks() = runTest {
        val database = testLocalContentDatabase()
        val dao = database.contentDao()

        LocalContentInitializer(dao).initializeIfNeeded()

        assertEquals(3, dao.getTaskCountByReasonAndStatus(CrawlingReason.Following, CrawlingStatus.NotStarted))
        assertEquals(3, dao.getTaskCountByReasonAndStatus(CrawlingReason.Trending, CrawlingStatus.NotStarted))
        assertEquals(2, dao.getTaskCountByReasonAndStatus(CrawlingReason.UpvotedQuestion, CrawlingStatus.NotStarted))
        assertEquals(2, dao.getTaskCountByReasonAndStatus(CrawlingReason.FollowingUpvote, CrawlingStatus.NotStarted))
        assertEquals(2, dao.getTaskCountByReasonAndStatus(CrawlingReason.CollaborativeFiltering, CrawlingStatus.NotStarted))
        database.close()
    }

    @Test
    fun getFreshnessWeightUsesSameAgeBuckets() {
        val now = 10_000L * 60L * 60L * 1000L

        assertEquals(1.08, getFreshnessWeight(now - 11L * 60L * 60L * 1000L, now))
        assertEquals(1.0, getFreshnessWeight(now - 24L * 60L * 60L * 1000L, now))
        assertEquals(0.92, getFreshnessWeight(now - 72L * 60L * 60L * 1000L, now))
        assertEquals(0.82, getFreshnessWeight(now - 200L * 60L * 60L * 1000L, now))
    }

    @Test
    fun getDefaultWeightKeepsReasonWeights() {
        assertEquals(1.2, getDefaultWeight(CrawlingReason.Following))
        assertEquals(1.0, getDefaultWeight(CrawlingReason.Trending))
        assertEquals(0.95, getDefaultWeight(CrawlingReason.UpvotedQuestion))
        assertEquals(0.88, getDefaultWeight(CrawlingReason.FollowingUpvote))
        assertEquals(0.8, getDefaultWeight(CrawlingReason.CollaborativeFiltering))
    }

    @Test
    fun createTaskForReasonKeepsUrlAndPriority() {
        val task = createTaskForReason(CrawlingReason.Following)

        assertEquals("https://api.zhihu.com/moments_v3?feed_type=recommend", task.url)
        assertEquals(CrawlingReason.Following, task.reason)
        assertEquals(8, task.priority)
    }

    @Test
    fun rankCandidateNormalizesContentIdAndAppliesWeights() {
        val ranked = rankCandidate(
            candidate = CrawlingResult(
                id = 1L,
                taskId = 1L,
                contentId = "42",
                title = "标题",
                summary = "摘要",
                url = "https://www.zhihu.com/answer/42",
                reason = CrawlingReason.Trending,
                score = 10.0,
                createdAt = 0L,
            ),
            behaviorProfile = UserBehaviorAnalyzer.RecommendationBehaviorProfile(
                reasonPreferences = mapOf(
                    CrawlingReason.Trending to LocalReasonPreference(
                        multiplier = 1.2,
                        explanation = "你最近更偏好这类来源",
                    ),
                ),
                contentAffinities = mapOf(
                    "answer:42" to LocalContentAffinity(
                        multiplier = 1.5,
                        explanation = "你明确喜欢过类似内容",
                    ),
                ),
            ),
        )

        assertNotNull(ranked)
        assertEquals("answer:42", ranked.result.contentId)
        assertTrue(ranked.finalScore > 0.0)
        assertEquals("热门推荐 · 你明确喜欢过类似内容 · 你最近更偏好这类来源", ranked.reasonDisplay)
    }

    @Test
    fun localRecommendationEngineInitializesOnlyOnce() = runTest {
        val database = testLocalContentDatabase()
        var initializeCount = 0
        var startCount = 0
        val engine = localRecommendationEngineForTest(
            dao = database.contentDao(),
            initializeContentIfNeeded = { initializeCount++ },
            startScheduling = { startCount++ },
        )

        engine.initialize()
        engine.initialize()

        assertEquals(1, initializeCount)
        assertEquals(1, startCount)
        database.close()
    }

    @Test
    fun localRecommendationEngineRefreshContentInsertsAndExecutesTopPriorityTasks() = runTest {
        val database = testLocalContentDatabase()
        val executedReasons = mutableListOf<CrawlingReason>()
        val engine = localRecommendationEngineForTest(
            dao = database.contentDao(),
            executeTask = { task -> executedReasons.add(task.reason) },
        )

        engine.refreshContent()

        assertEquals(
            listOf(
                CrawlingReason.Following,
                CrawlingReason.Trending,
                CrawlingReason.UpvotedQuestion,
            ),
            executedReasons,
        )
        assertEquals(1, database.contentDao().getTaskCountByReasonAndStatus(CrawlingReason.Following, CrawlingStatus.NotStarted))
        database.close()
    }

    private fun localRecommendationEngineForTest(
        dao: LocalContentDao,
        initializeContentIfNeeded: suspend () -> Unit = {},
        startScheduling: () -> Unit = {},
        stopScheduling: () -> Unit = {},
        executeTask: suspend (CrawlingTask) -> Unit = {},
    ): LocalRecommendationEngine = LocalRecommendationEngine(
        dao = dao,
        feedGenerator = FeedGenerator(dao),
        userBehaviorAnalyzer = UserBehaviorAnalyzer(dao),
        initializeContentIfNeeded = initializeContentIfNeeded,
        startScheduling = startScheduling,
        stopScheduling = stopScheduling,
        executeTask = executeTask,
    )

    private fun testLocalContentDatabase(): LocalContentDatabase =
        getLocalContentDatabase(
            createTempDirectory("local-recommendation-support-room").resolve("local-content.db").toFile(),
        )
}
