// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package top.yukonga.miuix.kmp.nav.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.runtime.saveable.rememberSaveableStateHolder

/**
 * Per-entry [androidx.compose.runtime.saveable.rememberSaveable] scoping for miuix-nav.
 *
 * Wraps a single [SaveableStateHolder] and namespaces each navigation entry's saved state by its
 * value-stable `contentKey`. While an entry is presented its `rememberSaveable` state is preserved
 * across recomposition and configuration/process restore; once the entry fully leaves the
 * presentation set (relativeDepth `d <= -1`) the host must call [removeState] to release its slot.
 *
 * This is the runtime counterpart of design doc §5.3:
 * `rememberSaveableStateHolder()` + per-`contentKey` `SaveableStateProvider`, with
 * `removeState(contentKey)` on exit.
 */
@Stable
internal class NavSaveableStateHolder(
    private val holder: SaveableStateHolder,
) {
    /**
     * Renders [content] inside a `SaveableStateProvider` keyed by [contentKey], so that any
     * `rememberSaveable` reads/writes performed by [content] are isolated to this entry.
     *
     * @param contentKey the value-stable identity of the entry (`NavEntry.contentKey`).
     * @param content the entry body to render within the saveable scope.
     */
    @Composable
    fun EntryStateContent(
        contentKey: Any,
        content: @Composable () -> Unit,
    ) {
        holder.SaveableStateProvider(saveableKey(contentKey), content)
    }

    /**
     * Releases the saved-state slot associated with [contentKey]. Must be called once an entry has
     * fully exited (relativeDepth `d <= -1`) so its retained state is not restored on a later push
     * of a different entry that happens to reuse the key value.
     *
     * @param contentKey the value-stable identity of the entry whose state should be dropped.
     */
    fun removeState(contentKey: Any) {
        holder.removeState(saveableKey(contentKey))
    }
}

/**
 * Maps a `contentKey` to a Bundle-storable [String] key for [SaveableStateHolder].
 *
 * `SaveableStateProvider` requires its key to be a type the platform can persist (on Android, only
 * Bundle-storable types — a raw `@Serializable` route object such as `Route.Home` is NOT one and
 * throws `IllegalArgumentException`). The `contentKey` is value-stable, so its [toString] is a stable,
 * unique String for `@Serializable` data classes / data objects and survives configuration changes
 * and process death. Used for both saving and removing so the slot identity is consistent.
 */
private fun saveableKey(contentKey: Any): String = contentKey.toString()

/**
 * Remembers a [NavSaveableStateHolder] backed by a single [rememberSaveableStateHolder].
 *
 * One holder is shared by all entries of a `NavDisplay`; each entry is namespaced by its
 * `contentKey` via [NavSaveableStateHolder.EntryStateContent].
 */
@Composable
internal fun rememberNavSaveableStateHolder(): NavSaveableStateHolder {
    val holder = rememberSaveableStateHolder()
    return remember(holder) { NavSaveableStateHolder(holder) }
}
