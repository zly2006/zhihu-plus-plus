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

import androidx.lifecycle.ViewModel
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ContentSlotRegistryTest {
    private class TrackedViewModel : ViewModel() {
        var cleared = false

        override fun onCleared() {
            cleared = true
        }
    }

    private fun ContentSlotRegistry.install(key: String, name: String, viewModel: TrackedViewModel) {
        slotFor(key).viewModelStore.put(name, viewModel)
    }

    private fun ContentSlotRegistry.viewModelOf(key: String, name: String): TrackedViewModel? =
        slotFor(key).viewModelStore.get(name) as TrackedViewModel?

    @Test
    fun same_content_key_keeps_one_slot_and_one_view_model_store() {
        val stores = ContentViewModelStores()
        val registry = ContentSlotRegistry(stores)
        val article = TrackedViewModel()
        val pin = TrackedViewModel()

        registry.install(ARTICLE, "article", article)
        registry.install(PIN, "pin", pin)

        assertSame(registry.slotFor(ARTICLE), registry.slotFor(ARTICLE))
        assertSame(article, registry.viewModelOf(ARTICLE, "article"))
        assertFalse(article.cleared)
        assertTrue(registry.slotFor(ARTICLE) !== registry.slotFor(PIN))
        assertTrue(registry.slotFor(ARTICLE).viewModelStore !== registry.slotFor(PIN).viewModelStore)
        assertNull(registry.viewModelOf(ARTICLE, "pin"))
    }

    @Test
    fun still_live_content_keeps_its_slot_and_view_model() {
        val stores = ContentViewModelStores()
        val registry = ContentSlotRegistry(stores)
        val article = TrackedViewModel()
        registry.install(ARTICLE, "article", article)

        registry.dropMissing(setOf(ARTICLE, PIN))

        assertSame(article, registry.viewModelOf(ARTICLE, "article"))
        assertFalse(article.cleared)
    }

    @Test
    fun content_that_left_the_back_stacks_releases_slot_and_view_model() {
        val stores = ContentViewModelStores()
        val registry = ContentSlotRegistry(stores)
        val article = TrackedViewModel()
        registry.install(ARTICLE, "article", article)
        val storeBeforeDrop = registry.slotFor(ARTICLE).viewModelStore

        registry.dropMissing(setOf(PIN))

        assertTrue(article.cleared)
        assertNull(registry.viewModelOf(ARTICLE, "article"))
        assertTrue(registry.slotFor(ARTICLE).viewModelStore !== storeBeforeDrop)
    }

    private companion object {
        const val ARTICLE = "article/Answer/318"
        const val PIN = "pin/1"
    }
}
