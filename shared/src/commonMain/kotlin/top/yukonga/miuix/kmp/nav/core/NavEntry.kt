// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package top.yukonga.miuix.kmp.nav.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import top.yukonga.miuix.kmp.nav.transition.NavRole

/**
 * Internal rendering unit of the navigation runtime.
 *
 * One [NavEntry] is produced per back-stack key by the [NavEntryBuilder] DSL. It is the unit of
 * diffing (via [contentKey]), of [androidx.compose.runtime.movableContentOf] identity, and of
 * per-entry state scoping (saveable state / lifecycle / view-model).
 *
 * @param K the concrete [NavKey] subtype this entry renders.
 * @property key the navigation key this entry was created for.
 * @property contentKey value-stable identity used for diffing and for keeping composition identity
 *   stable across recompositions and reorderings. Defaults to [key] itself when the DSL caller does
 *   not supply one.
 * @property metadata arbitrary per-entry data (e.g. a per-route transition override is stored here
 *   by the DSL). Empty by default.
 * @property content the composable that renders this entry, receiving the typed [key].
 */
@PublishedApi
internal class NavEntry<K : NavKey>(
    val key: K,
    contentKey: Any,
    metadata: Map<String, Any>,
    content: @Composable (K) -> Unit,
) {
    /**
     * Value-stable identity used for diffing, `movableContentOf` identity, and per-entry state
     * scoping. Starts as the DSL-supplied key (or the key instance); [disambiguateOccurrence] may
     * replace it when the same base key appears more than once in the current back stack, so two
     * value-equal keys (e.g. `Article(42)` reached twice) never collapse onto one `key()` slot or
     * one saveable-state slot. The replacement is deterministic from the back stack content, so the
     * saveable slot survives process death.
     */
    var contentKey: Any = contentKey
        private set

    /**
     * Disambiguates this entry's [contentKey] by its [occurrence] index among value-equal base keys
     * in the current back stack (0 = first occurrence, left untouched by the caller). Called by the
     * renderer before reconcile, so equal keys diff as distinct entries.
     */
    fun disambiguateOccurrence(occurrence: Int) {
        contentKey = NavEntryContentKey(base = contentKey, occurrence = occurrence)
    }

    /**
     * Registration payload as built by the DSL. A holder class (rather than two snapshot fields)
     * because a composable lambda cannot round-trip through a function-typed snapshot delegate —
     * its runtime class implements the composable calling convention, not `FunctionN`, so the
     * delegate's checkcast throws; a plain class field sidesteps the cast entirely.
     */
    private class Registration<K : NavKey>(
        val metadata: Map<String, Any>,
        val content: @Composable (K) -> Unit,
    )

    /**
     * Snapshot-backed so [adoptFrom] can refresh a surviving instance in place when the entry
     * provider is rebuilt (a DSL capture changed): instance identity must stay stable for
     * `movableContentOf`, so the reconciler reuses the old instance and swaps the registration.
     */
    private var registration: Registration<K> by mutableStateOf(Registration(metadata, content))

    /** Arbitrary per-entry data (per-route transition / swipe overrides live here). */
    val metadata: Map<String, Any> get() = registration.metadata

    /**
     * Adopts the registration payload of a freshly built [other] entry with the same [contentKey],
     * keeping this instance (and its composition identity) alive. Guarded by payload equality so
     * the routine reconcile on every stack mutation — which rebuilds entries from an unchanged
     * provider — never invalidates readers: the content lambda reference and the metadata values
     * only change when the provider itself was rebuilt.
     */
    fun adoptFrom(other: NavEntry<*>) {
        @Suppress("UNCHECKED_CAST")
        val incoming = (other as NavEntry<K>).registration
        val current = registration
        if (current.content !== incoming.content || current.metadata != incoming.metadata) {
            registration = incoming
        }
    }

    /**
     * Per-entry presentation snapshot maintained by the reconciler (role + removal flag). Read by
     * the renderer to pick the governing [top.yukonga.miuix.kmp.nav.transition.NavTransition].
     */
    var presentation: NavPresentationState by mutableStateOf(
        NavPresentationState(role = NavRole.Top, isRemoving = false),
    )

    /** Renders this entry's content with its typed key. */
    @Composable
    fun Content() {
        registration.content(key)
    }
}

/**
 * Render-state snapshot of a single [NavEntry].
 *
 * @property role the entry's current [NavRole] (Top / Incoming / Outgoing / Covered).
 * @property isRemoving true while the entry has been removed from the back stack but is still
 *   animating out (relative depth has not yet reached the unload threshold).
 */
@Immutable
internal data class NavPresentationState(
    val role: NavRole,
    val isRemoving: Boolean,
)

/**
 * Disambiguated [NavEntry.contentKey] for the 2nd+ occurrence of a value-equal base key in the back
 * stack. [equals]/[hashCode] keep distinct occurrences apart for diffing and `key()`, while
 * [toString] yields a stable, Bundle-storable saveable-state slot id that is deterministic from the
 * (persisted) back stack — so per-entry saved state survives process death.
 */
@Immutable
internal data class NavEntryContentKey(
    val base: Any,
    val occurrence: Int,
)
