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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.toRoute
import com.github.zly2006.zhihu.navigation.Account
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.CollectionContent
import com.github.zly2006.zhihu.navigation.Collections
import com.github.zly2006.zhihu.navigation.History
import com.github.zly2006.zhihu.navigation.Notification
import com.github.zly2006.zhihu.navigation.Person
import com.github.zly2006.zhihu.navigation.Pin
import com.github.zly2006.zhihu.navigation.Question
import com.github.zly2006.zhihu.navigation.Search
import com.github.zly2006.zhihu.navigation.Topic

internal const val CONTENT_SLOT_RELEASE_DELAY_MILLIS = 500L

internal fun NavBackStackEntry.contentIdentityOrNull(): String? = when {
    destination.hasRoute<Article>() -> routeOrNull<Article>()?.let { "article/${it.type.name}/${it.id}" }
    destination.hasRoute<Pin>() -> routeOrNull<Pin>()?.let { "pin/${it.id}" }
    destination.hasRoute<Question>() -> routeOrNull<Question>()?.let { "question/${it.questionId}" }
    destination.hasRoute<Person>() -> routeOrNull<Person>()?.let {
        "person/" + if (it.id != Person.EMPTY_ID) it.id else it.urlToken
    }
    destination.hasRoute<Topic>() -> routeOrNull<Topic>()?.let { "topic/${it.id}/${it.section}" }
    destination.hasRoute<Search>() -> routeOrNull<Search>()?.let {
        "search/${it.query}/${it.restrictedMemberHashId}"
    }
    destination.hasRoute<Collections>() -> routeOrNull<Collections>()?.let { "collections/${it.userToken}" }
    destination.hasRoute<CollectionContent>() -> routeOrNull<CollectionContent>()?.let { "collection/${it.collectionId}" }
    destination.hasRoute<History>() -> "history"
    destination.hasRoute<Notification.Entry>() -> routeOrNull<Notification.Entry>()?.let { "notification/${it.entryName}" }
    destination.hasRoute<Notification.Invitations>() -> "notification/invitations"
    destination.hasRoute<Notification>() -> "notification"
    destination.hasRoute<Account.RecommendSettings.BlockedFeedHistory>() -> "blocked_feed_history"
    else -> null
}

private inline fun <reified T : Any> NavBackStackEntry.routeOrNull(): T? = runCatching { toRoute<T>() }.getOrNull()

internal class ContentViewModelStores : ViewModel() {
    private val stores = mutableMapOf<String, ViewModelStore>()

    fun storeFor(key: String): ViewModelStore = stores.getOrPut(key) { ViewModelStore() }

    fun clear(key: String) {
        stores.remove(key)?.clear()
    }

    override fun onCleared() {
        stores.values.forEach(ViewModelStore::clear)
        stores.clear()
    }
}

@Stable
internal class ContentSlot(
    val viewModelStore: ViewModelStore,
) {
    var ownerEntryId: String? by mutableStateOf(null)

    val movable: @Composable (target: Any?, render: @Composable (Any?) -> Unit) -> Unit =
        movableContentOf { target, render -> render(target) }
}

@Stable
internal class ContentSlotRegistry(
    private val viewModelStores: ContentViewModelStores,
    val isEntryLive: (entryId: String) -> Boolean = { true },
) {
    private val slots = mutableMapOf<String, ContentSlot>()

    fun slotFor(key: String): ContentSlot = slots.getOrPut(key) { ContentSlot(viewModelStores.storeFor(key)) }

    fun claim(entry: NavBackStackEntry?) {
        val key = entry?.contentIdentityOrNull() ?: return
        slotFor(key).ownerEntryId = entry.id
    }

    fun dropMissing(liveKeys: Set<String>) {
        slots.keys.toList().forEach { key ->
            if (key !in liveKeys) {
                slots.remove(key)
                viewModelStores.clear(key)
            }
        }
    }
}

private class ContentStoreOwner(
    private val store: ViewModelStore,
) : ViewModelStoreOwner {
    override val viewModelStore: ViewModelStore
        get() = store
}

@Composable
internal fun ContentSlotHost(
    slots: ContentSlotRegistry,
    entry: NavBackStackEntry,
    target: Any?,
    render: @Composable (Any?) -> Unit,
) {
    val key = entry.contentIdentityOrNull() ?: return render(target)
    ContentSlotHost(slots, key, entry.id, target, render)
}

@Composable
internal fun ContentSlotHost(
    slots: ContentSlotRegistry,
    key: String,
    entryId: String,
    target: Any?,
    render: @Composable (Any?) -> Unit,
) {
    val slot = slots.slotFor(key)
    val owner = slot.ownerEntryId
    if (owner != null && owner != entryId && slots.isEntryLive(owner)) return
    if (owner != entryId) slot.ownerEntryId = entryId
    val storeOwner = remember(slot) { ContentStoreOwner(slot.viewModelStore) }
    CompositionLocalProvider(LocalViewModelStoreOwner provides storeOwner) {
        slot.movable(target, render)
    }
}
