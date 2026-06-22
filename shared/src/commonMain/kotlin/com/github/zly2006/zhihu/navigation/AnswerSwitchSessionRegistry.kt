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

package com.github.zly2006.zhihu.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * 按 [NavBackStackEntry.id] 挂起 / 恢复回答切换会话，支持导航栈嵌套多队列。
 */
class AnswerSwitchSessionRegistry {
    var active: AnswerNavigator? by mutableStateOf(null)

    private val suspended = mutableMapOf<String, AnswerNavigator>()

    fun suspend(entryId: String, navigator: AnswerNavigator) {
        suspended[entryId] = navigator
        if (active === navigator) {
            active = null
        }
    }

    fun resume(entryId: String): AnswerNavigator? = suspended[entryId]

    fun remove(entryId: String) {
        suspended.remove(entryId)
        if (active?.let { suspended.containsValue(it) } == false && active != null) {
            // active may still point to removed if same ref — caller sets active explicitly
        }
    }

    fun clear() {
        suspended.clear()
        active = null
    }

    fun adoptPending(pending: AnswerNavigator?): AnswerNavigator? {
        if (pending == null) return null
        active = pending
        return pending
    }
}
