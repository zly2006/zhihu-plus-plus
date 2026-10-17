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

import androidx.compose.runtime.AbstractApplier
import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composition
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ContentSlotHostTest {
    private class Probe {
        val rendered = mutableListOf<String>()
        val stores = mutableListOf<ViewModelStore?>()

        fun last(): String = rendered.lastOrNull() ?: "<未渲染>"
    }

    @Composable
    private fun PageBody(host: String, seed: () -> Int, probe: Probe) {
        var value by remember { mutableIntStateOf(seed()) }
        probe.rendered += "$host:$value"
        probe.stores += LocalViewModelStoreOwner.current?.viewModelStore
    }

    @Test
    fun ownership_switch_moves_the_composition_and_keeps_its_state() = slotComposition { scope ->
        val slots = ContentSlotRegistry(ContentViewModelStores())
        val probe = Probe()
        val seed = mutableStateOf(7)
        val render: @Composable (Any?) -> Unit = { host -> PageBody(host as String, { seed.value }, probe) }
        slots.slotFor(KEY).ownerEntryId = HOST_A

        scope.setContent {
            ContentSlotHost(slots, KEY, HOST_A, HOST_A, render)
            ContentSlotHost(slots, KEY, HOST_B, HOST_B, render)
        }

        assertEquals("$HOST_A:7", probe.last())

        probe.rendered.clear()
        seed.value = 99
        scope.idle()

        assertEquals("$HOST_A:7", probe.last())

        probe.rendered.clear()
        slots.slotFor(KEY).ownerEntryId = HOST_B
        scope.idle()

        assertEquals("$HOST_B:7", probe.last())
        assertTrue(probe.rendered.none { it.startsWith(HOST_A) })
    }

    @Test
    fun owner_that_left_the_back_stack_is_reclaimed_by_the_composing_host() = slotComposition { scope ->
        val slots = ContentSlotRegistry(ContentViewModelStores(), isEntryLive = { it != HOST_A })
        val probe = Probe()
        val seed = mutableStateOf(7)
        val render: @Composable (Any?) -> Unit = { host -> PageBody(host as String, { seed.value }, probe) }
        slots.slotFor(KEY).ownerEntryId = HOST_A

        scope.setContent {
            ContentSlotHost(slots, KEY, HOST_B, HOST_B, render)
        }

        assertEquals("$HOST_B:7", probe.last())
        assertEquals(HOST_B, slots.slotFor(KEY).ownerEntryId)
    }

    @Test
    fun first_composer_claims_a_slot_without_owner() = slotComposition { scope ->
        val slots = ContentSlotRegistry(ContentViewModelStores())
        val probe = Probe()
        val render: @Composable (Any?) -> Unit = { host -> PageBody(host as String, { 7 }, probe) }

        scope.setContent {
            ContentSlotHost(slots, KEY, HOST_A, HOST_A, render)
            ContentSlotHost(slots, KEY, HOST_B, HOST_B, render)
        }

        assertEquals(HOST_A, slots.slotFor(KEY).ownerEntryId)
        scope.idle()
        assertEquals("$HOST_A:7", probe.last())
    }

    @Test
    fun page_resolves_view_model_store_from_the_slot() = slotComposition { scope ->
        val stores = ContentViewModelStores()
        val slots = ContentSlotRegistry(stores)
        val probe = Probe()
        val render: @Composable (Any?) -> Unit = { host -> PageBody(host as String, { 7 }, probe) }
        slots.slotFor(KEY).ownerEntryId = HOST_A

        scope.setContent {
            ContentSlotHost(slots, KEY, HOST_A, HOST_A, render)
        }

        assertSame(stores.storeFor(KEY), probe.stores.last())
        assertTrue(probe.stores.last() !== scope.hostStore)
    }

    private class UnitApplier : AbstractApplier<Unit>(Unit) {
        override fun insertTopDown(index: Int, instance: Unit) = Unit

        override fun insertBottomUp(index: Int, instance: Unit) = Unit

        override fun remove(index: Int, count: Int) = Unit

        override fun move(from: Int, to: Int, count: Int) = Unit

        override fun onClear() = Unit
    }

    private interface SlotCompositionScope {
        val hostStore: ViewModelStore

        suspend fun setContent(content: @Composable () -> Unit)

        suspend fun idle()
    }

    private fun slotComposition(body: suspend (SlotCompositionScope) -> Unit) {
        runBlocking {
            val clock = BroadcastFrameClock()
            val frameContext = Dispatchers.Unconfined + clock
            val recomposer = Recomposer(frameContext)
            val composition = Composition(UnitApplier(), recomposer)
            val recomposerScope = CoroutineScope(frameContext)
            recomposerScope.launch { recomposer.runRecomposeAndApplyChanges() }
            val hostStore = ViewModelStore()
            val hostOwner = object : ViewModelStoreOwner {
                override val viewModelStore: ViewModelStore = hostStore
            }
            val scope = object : SlotCompositionScope {
                override val hostStore: ViewModelStore = hostStore

                override suspend fun setContent(content: @Composable () -> Unit) {
                    composition.setContent {
                        CompositionLocalProvider(LocalViewModelStoreOwner provides hostOwner) {
                            content()
                        }
                    }
                    idle()
                }

                override suspend fun idle() {
                    repeat(IDLE_STEPS) {
                        Snapshot.sendApplyNotifications()
                        if (clock.hasAwaiters) clock.sendFrame(0L)
                        yield()
                    }
                }
            }
            try {
                withTimeout(COMPOSITION_TIMEOUT_MILLIS) { body(scope) }
            } finally {
                recomposer.cancel()
                composition.dispose()
                recomposerScope.cancel()
            }
        }
    }

    private companion object {
        const val KEY = "article/Answer/318"
        const val HOST_A = "entry-a"
        const val HOST_B = "entry-b"
        const val IDLE_STEPS = 6
        const val COMPOSITION_TIMEOUT_MILLIS = 60_000L
    }
}
