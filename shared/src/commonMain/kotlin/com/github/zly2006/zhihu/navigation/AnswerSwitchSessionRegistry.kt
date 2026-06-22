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

import androidx.compose.runtime.mutableStateMapOf

/**
 * 按回答详情实例 id 存取回答切换会话，避免不同来源的详情页共用同一个 navigator。
 */
class AnswerSwitchSessionRegistry {
    private val navigatorsBySessionId = mutableStateMapOf<String, AnswerNavigator>()

    fun put(
        sessionId: String,
        navigator: AnswerNavigator,
    ) {
        navigatorsBySessionId[sessionId] = navigator
    }

    fun get(sessionId: String): AnswerNavigator? = navigatorsBySessionId[sessionId]

    fun removeSession(sessionId: String) {
        navigatorsBySessionId.remove(sessionId)
    }

    fun clear() {
        navigatorsBySessionId.clear()
    }
}
