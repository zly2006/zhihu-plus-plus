/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 */

package com.github.zly2006.zhihu.ui.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PageTurnDispatcherTest {
    @Test
    fun dispatchesOnlyToMostRecentlyRegisteredTarget() {
        val dispatcher = PageTurnDispatcher()
        val article = dispatcher.registerTarget()
        val comments = dispatcher.registerTarget()

        assertTrue(dispatcher.dispatch(PageTurnCommand.PageDown))
        assertNull(article.commands.tryReceive().getOrNull())
        assertEquals(PageTurnCommand.PageDown, comments.commands.tryReceive().getOrNull())

        comments.close()
        assertTrue(dispatcher.dispatch(PageTurnCommand.PageUp))
        assertEquals(PageTurnCommand.PageUp, article.commands.tryReceive().getOrNull())
    }

    @Test
    fun fallsBackWhenNoWhitelistedTargetIsActive() {
        val dispatcher = PageTurnDispatcher()

        assertFalse(dispatcher.hasActiveTarget)
        assertFalse(dispatcher.dispatch(PageTurnCommand.PageDown))

        val target = dispatcher.registerTarget()
        assertTrue(dispatcher.hasActiveTarget)
        target.close()
        assertFalse(dispatcher.hasActiveTarget)
        assertFalse(dispatcher.dispatch(PageTurnCommand.PageDown))
    }
}
