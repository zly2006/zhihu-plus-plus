// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package top.yukonga.miuix.kmp.nav.runtime

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import top.yukonga.miuix.kmp.nav.core.NavEntry
import top.yukonga.miuix.kmp.nav.transition.NavGesture
import top.yukonga.miuix.kmp.nav.transition.NavRole

/** Epsilon around zero within which an entry is treated as the steady-state top. */
private const val TOP_EPSILON = 1e-3f

/**
 * Relative depth of an entry: `animatedTop - entryIndex` (design spec §4.1).
 *
 * The single shared driver `animatedTop` converges to `new.lastIndex`; every entry's visuals are
 * a pure function of this value. The name `relativeDepth` (alias `d`) is contract-fixed.
 */
internal fun relativeDepth(animatedTop: Float, entryIndex: Int): Float = animatedTop - entryIndex

/**
 * Maps a [relativeDepth] to a [NavRole] (design spec §4.1 / §6.1).
 *
 * - `|d| ≤ TOP_EPSILON` -> [NavRole.Top] (steady-state top, jitter-tolerant);
 * - `d > TOP_EPSILON` -> [NavRole.Covered] (below the top, possibly culled by [isVisibleAt]);
 * - `d < -TOP_EPSILON` -> [NavRole.Outgoing] when [isRemoving], else [NavRole.Incoming].
 *
 * Boundary ownership: the transition between layers `i` and `i-1` is governed by the upper
 * entry `i`; this classification feeds NavDisplay's choice of the governing transition (§4.3).
 */
internal fun roleFor(relativeDepth: Float, isRemoving: Boolean): NavRole = when {
    relativeDepth > TOP_EPSILON -> NavRole.Covered
    relativeDepth < -TOP_EPSILON -> if (isRemoving) NavRole.Outgoing else NavRole.Incoming
    else -> NavRole.Top
}

/**
 * Visibility window: an entry renders iff `-1 < relativeDepth <= opaqueDepth`.
 *
 * Entries with `d <= -1` are unloaded (fully past the leading edge); entries with
 * `d > opaqueDepth` are culled (fully occluded by an upper opaque layer). Modal-style
 * transitions raise [opaqueDepth] above 1 to keep the lower layer visible.
 *
 * Note: visibility is the authority for whether an entry renders. An entry at exactly `d == -1`
 * is culled here even though [roleFor] would still classify it as [NavRole.Incoming]/
 * [NavRole.Outgoing]; its role is simply never acted upon once it is invisible.
 */
internal fun isVisibleAt(relativeDepth: Float, opaqueDepth: Float): Boolean = relativeDepth > -1f && relativeDepth <= opaqueDepth

/**
 * Core driver of the whole navigation stack (spec §4.1 / §7.1).
 *
 * Holds the single driving float [animatedTop] and the live presentation set ([presented] = the
 * current back-stack entries UNION the entries still leaving). Merges what earlier drafts split into
 * a separate "presentation store": there is exactly one owner of both the float and the set.
 *
 * @param initialTopIndex the initial value of [animatedTop] (the top index of the initial back stack).
 */
@Stable
internal class NavPresentation(initialTopIndex: Float) {
    /**
     * The single driving float for the whole stack (spec §4.1 / §7.1 dual-mode driver).
     *
     * `snapToFinger` during a gesture (1:1, no interpolator); `settleTo` on settle/normal (the shared
     * spring). The renderer reads it lazily inside `graphicsLayer { }` blocks for zero-recomposition
     * per-frame visuals; both extensions live in `runtime/NavDriver.kt` (Phase 3, see §0.3).
     */
    val animatedTop: Animatable<Float, AnimationVector1D> = Animatable(initialTopIndex)

    // Presentation set = current back-stack entries UNION entries still leaving (relative depth > -1).
    private val _presented = mutableStateListOf<NavEntry<*>>()

    /** Entries currently presented, ordered bottom (root) to top; includes leaving entries. */
    val presented: List<NavEntry<*>> get() = _presented

    /** Last classified stack mutation; surfaced to the transition scope via `change`. */
    var change: NavChange by mutableStateOf(NavChange.None)
        private set

    /**
     * Live predictive-back gesture context, surfaced to transitions via
     * [top.yukonga.miuix.kmp.nav.transition.NavTransitionScope.gesture]. Snapshot state read
     * inside deferred `graphicsLayer { }` blocks, so per-event updates invalidate draw only.
     *
     * Ownership: the gesture layers (swipe dismiss / predictive back) write it per event. On
     * release the last value is **kept frozen through the settle** — clearing at lift would snap
     * a touch-following transform origin back to center mid-animation — and cleared when the
     * settle resolves (cancel settles back, or the leaving entry unloads on commit).
     */
    var gesture: NavGesture? by mutableStateOf(null)

    /**
     * Release velocity (depth-units per second, negative toward pop) estimated from the
     * predictive-back event stream, handed to the renderer's next settle so a flung system back
     * commits with momentum (the reference feeds the gesture velocity into its post-commit
     * spring). Plain (non-snapshot) bookkeeping: written by the gesture layer, consumed exactly
     * once by the settle launch, cleared on cancel.
     */
    var pendingSettleVelocity: Float = 0f

    /**
     * Merges the freshly-built [currentEntries] (one per current back-stack key) into the presentation
     * set, preserving leaving entries (flagged via [NavEntry.presentation]'s `isRemoving`) until they
     * are unloaded at relative depth <= -1. [change] is the classification computed by `navReconcile`
     * (Phase 2). Surviving instances are reused so `movableContentOf` identity is preserved, but they
     * adopt the fresh registration payload ([NavEntry.adoptFrom]) so a rebuilt entry provider
     * refreshes live entries instead of leaving them pinned to first-push captures.
     *
     * A [NavChange.None] classification (identical key lists — the reconcile re-ran because the
     * provider changed, not the stack) keeps the previous [change]: an in-flight transition keeps
     * reading the classification that started it.
     */
    fun reconcile(currentEntries: List<NavEntry<*>>, change: NavChange) {
        if (change != NavChange.None) this.change = change
        val currentKeys = currentEntries.mapTo(HashSet(currentEntries.size)) { it.contentKey }
        // Entries that fell off the back stack become "leaving" but stay rendered.
        _presented.forEach { e ->
            if (e.contentKey !in currentKeys) {
                e.presentation = e.presentation.copy(isRemoving = true)
            }
        }
        // Insert newly added current entries (keep existing instances for movableContent identity).
        val presentedKeys = _presented.mapTo(HashSet(_presented.size)) { it.contentKey }
        currentEntries.forEach { e ->
            if (e.contentKey !in presentedKeys) {
                e.presentation = e.presentation.copy(isRemoving = false)
                _presented.add(e)
            } else {
                // Reuse existing instance; clear any stale leaving flag (re-push of same key) and
                // adopt the freshly built registration payload (content / metadata).
                val existing = _presented.first { it.contentKey == e.contentKey }
                existing.presentation = existing.presentation.copy(isRemoving = false)
                existing.adoptFrom(e)
            }
        }
    }

    /** Removes a fully-left entry (relative depth <= -1). Caller releases its per-entry state scopes. */
    fun unload(entry: NavEntry<*>) {
        _presented.remove(entry)
    }
}

/**
 * Remembers a [NavPresentation] across recompositions, seeded with [initialTopIndex].
 *
 * The presentation owns the single driving float and outlives individual recompositions, so it is
 * created once via [remember] and never rebuilt.
 */
@Composable
internal fun rememberNavPresentation(initialTopIndex: Int): NavPresentation = remember { NavPresentation(initialTopIndex.toFloat()) }
