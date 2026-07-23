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

package com.github.zly2006.zhihu.ui

import kotlin.test.Test
import kotlin.test.assertEquals

class ReadingPlayerOverlayOffsetStateTest {
    @Test
    fun staleOwnerCannotUpdateOrClearCurrentArticleOffset() {
        val state = ReadingPlayerOverlayOffsetState()
        val oldArticle = Any()
        val currentArticle = Any()

        state.activate(oldArticle)
        state.update(oldArticle, -80f)
        state.activate(currentArticle)
        state.update(currentArticle, -40f)

        state.deactivate(oldArticle)
        state.update(oldArticle, -120f)

        assertEquals(-40f, state.verticalOffsetPx)
        state.deactivate(currentArticle)
        assertEquals(0f, state.verticalOffsetPx)
    }

    @Test
    fun sessionResetKeepsOwnerWhileRouteRevokeRemovesIt() {
        val state = ReadingPlayerOverlayOffsetState()
        val article = Any()

        state.activate(article)
        state.update(article, -80f)
        state.resetOffset()
        assertEquals(0f, state.verticalOffsetPx)

        state.update(article, -30f)
        assertEquals(-30f, state.verticalOffsetPx)

        state.revokeOwner()
        state.update(article, -60f)
        assertEquals(0f, state.verticalOffsetPx)
    }
}
