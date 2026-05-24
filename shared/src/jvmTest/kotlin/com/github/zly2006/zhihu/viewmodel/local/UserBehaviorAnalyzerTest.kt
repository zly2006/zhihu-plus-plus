package com.github.zly2006.zhihu.viewmodel.local

import kotlinx.coroutines.test.runTest
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class UserBehaviorAnalyzerTest {
    @Test
    fun recordsOpenedAndFeedbackBehaviors() = runTest {
        val database = testLocalContentDatabase()
        val dao = database.contentDao()
        val analyzer = UserBehaviorAnalyzer(dao)

        analyzer.recordContentOpened("answer:1", CrawlingReason.Trending)
        analyzer.recordRecommendationFeedback("answer:1", CrawlingReason.Trending, 1.0)
        analyzer.recordRecommendationFeedback("question:2", CrawlingReason.Following, -1.0)

        val answerBehaviors = dao.getBehaviorsByContentId("answer:1").map { it.action }.toSet()
        val questionBehaviors = dao.getBehaviorsByContentId("question:2").map { it.action }.toSet()

        assertEquals(setOf("click", "click:Trending", "like", "like:Trending"), answerBehaviors)
        assertEquals(setOf("dislike", "dislike:Following"), questionBehaviors)
        database.close()
    }

    @Test
    fun buildsBehaviorProfileFromRecentBehaviors() = runTest {
        val database = testLocalContentDatabase()
        val dao = database.contentDao()
        val analyzer = UserBehaviorAnalyzer(dao)

        analyzer.recordContentOpened("answer:1", CrawlingReason.Trending)
        analyzer.recordRecommendationFeedback("answer:1", CrawlingReason.Trending, 1.0)
        analyzer.recordRecommendationFeedback("question:2", CrawlingReason.Following, -1.0)

        val profile = analyzer.buildBehaviorProfile()

        val trendingPreference = profile.reasonPreferences.getValue(CrawlingReason.Trending)
        val followingPreference = profile.reasonPreferences.getValue(CrawlingReason.Following)
        val answerAffinity = profile.contentAffinities["answer:1"]
        val questionAffinity = profile.contentAffinities["question:2"]

        assertTrue(trendingPreference.multiplier > 1.0)
        assertTrue(followingPreference.multiplier < 1.0)
        assertNotNull(answerAffinity)
        assertTrue(answerAffinity.multiplier > 1.0)
        assertNotNull(questionAffinity)
        assertTrue(questionAffinity.multiplier < 1.0)
        database.close()
    }

    private fun testLocalContentDatabase(): LocalContentDatabase =
        getLocalContentDatabase(
            createTempDirectory("local-behavior-room").resolve("local-content.db").toFile(),
        )
}
