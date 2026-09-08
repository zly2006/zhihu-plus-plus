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

import androidx.navigation.NavType
import androidx.savedstate.SavedState
import androidx.savedstate.read
import androidx.savedstate.write

/**
 * Custom NavType for [ArticleType] enum.
 *
 * Required because the KMP navigation-compose library does not automatically
 * resolve NavTypes for @Serializable enums on non-Android targets (JVM/desktop).
 * See `NavTypeConverter.nonAndroid.kt` where `parseEnum()` returns `UNKNOWN`.
 */
object ArticleTypeNavType : NavType<ArticleType>(false) {
    override fun put(bundle: SavedState, key: String, value: ArticleType) {
        bundle.write { putString(key, value.name) }
    }

    override fun get(bundle: SavedState, key: String): ArticleType? = bundle.read {
        if (!contains(key) || isNull(key)) {
            null
        } else {
            getString(key).let { name -> ArticleType.entries.find { it.name == name } }
        }
    }

    override fun parseValue(value: String): ArticleType = ArticleType.entries.find { it.name == value } ?: ArticleType.Article

    override fun serializeAsValue(value: ArticleType): String = value.name
}
