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

        val behaviorsByContentId = dao.getBehaviorsSince(0L).groupBy { it.contentId }
        val answerBehaviors = behaviorsByContentId.getValue("answer:1").map { it.action }.toSet()
        val questionBehaviors = behaviorsByContentId.getValue("question:2").map { it.action }.toSet()

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
