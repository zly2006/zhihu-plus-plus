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

package com.github.zly2006.zhihu.ui

import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.shared.platform.SettingsStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReadLaterStoreTest {
    @Test
    fun addPersistsAndMovesDuplicateToTop() {
        val settings = mapBackedSettingsStore()
        val store = ReadLaterStore(settings)
        val first = Article(title = "第一篇", type = ArticleType.Article, id = 1)
        val second = Article(title = "第二篇", type = ArticleType.Answer, id = 2)

        store.add(first)
        store.add(second)
        store.add(first.copy(title = "第一篇更新"))

        assertEquals(listOf(1L, 2L), store.items.map { it.article.id })
        assertEquals(
            "第一篇更新",
            store.items
                .first()
                .article.title,
        )
        assertTrue(ReadLaterStore(settings).contains(first))
    }

    @Test
    fun removeOnlyDeletesMatchingArticle() {
        val settings = mapBackedSettingsStore()
        val store = ReadLaterStore(settings)
        val answer = Article(type = ArticleType.Answer, id = 1)
        val article = Article(type = ArticleType.Article, id = 1)

        store.add(answer)
        store.add(article)
        store.remove(answer)

        assertFalse(store.contains(answer))
        assertTrue(store.contains(article))
    }

    @Test
    fun clearRemovesPersistedItems() {
        val settings = mapBackedSettingsStore()
        val store = ReadLaterStore(settings)

        store.add(Article(type = ArticleType.Answer, id = 1))
        store.clear()

        assertTrue(store.items.isEmpty())
        assertTrue(ReadLaterStore(settings).items.isEmpty())
    }

    private fun mapBackedSettingsStore(): SettingsStore {
        val map = mutableMapOf<String, Any>()
        return SettingsStore(
            getBoolean = { key, defaultValue -> map[key] as? Boolean ?: defaultValue },
            putBoolean = { key, value -> map[key] = value },
            getString = { key, defaultValue -> map[key] as? String ?: defaultValue },
            putString = { key, value -> map[key] = value },
            getStringOrNull = { key -> map[key] as? String },
            putStringSet = { key, value -> map[key] = value },
            getStringSet = { key, defaultValue ->
                (map[key] as? Set<*>)
                    ?.filterIsInstance<String>()
                    ?.toSet()
                    ?: defaultValue
            },
            getInt = { key, defaultValue -> map[key] as? Int ?: defaultValue },
            putInt = { key, value -> map[key] = value },
            getLong = { key, defaultValue -> map[key] as? Long ?: defaultValue },
            putLong = { key, value -> map[key] = value },
            getFloat = { key, defaultValue -> map[key] as? Float ?: defaultValue },
            putFloat = { key, value -> map[key] = value },
            remove = { key -> map.remove(key) },
        )
    }
}
