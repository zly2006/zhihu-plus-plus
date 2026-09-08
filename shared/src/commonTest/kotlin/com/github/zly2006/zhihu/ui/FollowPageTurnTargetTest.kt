/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 */

package com.github.zly2006.zhihu.ui

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FollowPageTurnTargetTest {
    /**
     * Contract: https://github.com/zly2006/zhihu-plus-plus/issues/630
     * Introduced by: https://github.com/zly2006/zhihu-plus-plus/pull/732
     */
    @Test
    fun preloadedInnerPageIsEligibleOnlyWhileFollowScreenIsVisible() {
        assertTrue(isFollowPageTurnTargetActive(followScreenActive = true, selectedPage = 0, page = 0))
        assertFalse(isFollowPageTurnTargetActive(followScreenActive = true, selectedPage = 0, page = 1))
        assertFalse(isFollowPageTurnTargetActive(followScreenActive = false, selectedPage = 0, page = 0))
        assertFalse(isFollowPageTurnTargetActive(followScreenActive = false, selectedPage = 1, page = 1))
    }
}
