/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License (version 3 only).
 */

package com.github.zly2006.zhihu.ui

import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.Pin
import com.github.zly2006.zhihu.navigation.Question
import com.github.zly2006.zhihu.navigation.Search
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AdaptiveListDetailTest {
    @Test
    fun defaultRatioMatchesReferenceAndRespectsMinimumWidths() {
        assertEquals((1264f / 3f).dp, normalizedListPaneWidth(1280.dp, DEFAULT_LIST_PANE_RATIO))
        assertEquals(MIN_LIST_PANE_WIDTH, normalizedListPaneWidth(840.dp, 0f))
        assertEquals(344.dp, normalizedListPaneWidth(840.dp, 1f))
    }

    @Test
    fun readingDestinationClassificationOnlyIncludesReadingSurfaces() {
        assertTrue(Article(type = ArticleType.Article, id = 1).isReadingDestination())
        assertTrue(Question(questionId = 2).isReadingDestination())
        assertTrue(Pin(id = 3).isReadingDestination())
        assertFalse(Search().isReadingDestination())
    }
}
