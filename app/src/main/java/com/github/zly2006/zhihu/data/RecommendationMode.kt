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

package com.github.zly2006.zhihu.data

enum class RecommendationMode(
    val displayName: String,
    val key: String,
    val description: String,
) {
    WEB("Web 端推荐", "server", "使用知乎网页端的推荐算法"),
    ANDROID("安卓端推荐", "android", "使用知乎安卓端的推荐算法"),
    LOCAL("本地推荐", "local", "基于本地数据的推荐算法"),
    MIXED("混合推荐", "mixed", "融合安卓和网页端推荐算法，并过滤严选内容"),
}
