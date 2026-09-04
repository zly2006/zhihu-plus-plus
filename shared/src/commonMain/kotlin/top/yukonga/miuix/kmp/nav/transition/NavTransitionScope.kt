// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package top.yukonga.miuix.kmp.nav.transition

import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import top.yukonga.miuix.kmp.nav.runtime.NavChange

/**
 * The screen edge a back gesture originates from.
 *
 * Mirrors the edge semantics of the Android predictive-back event. [None] is reported for sources
 * that have no meaningful edge (Desktop ESC, Web custom triggers).
 */
enum class NavSwipeEdge {
    /** Gesture started from the left edge. */
    Left,

    /** Gesture started from the right edge. */
    Right,

    /** No edge association (non-edge sources such as ESC or programmatic back). */
    None,
}

/**
 * Predictive-back gesture context surfaced to a [NavTransition] while a back gesture is in progress.
 *
 * [progress] runs from 0 (gesture just started) to 1 (gesture fully committed). A transition can use
 * it together with [touchY] to bias its motion toward the finger (spec section 6.1);
 * [initialTouchY] is the vertical touch position at the gesture start, so a transition can follow
 * the finger's travel (`touchY - initialTouchY`) rather than its absolute position.
 */
@Stable
class NavGesture(
    val progress: Float,
    val swipeEdge: NavSwipeEdge,
    val touchY: Float,
    val initialTouchY: Float = touchY,
)

/**
 * The read-only context a [NavTransition] receives in [NavTransition.transformEntry] and
 * [NavTransition.scrimFraction].
 *
 * Every property is a deferred-read source: a [NavTransition] should read them inside a
 * `graphicsLayer { }` (or layout) block so the animation runs without recomposition. The property
 * set is append-only (spec section 6.1): existing properties never change meaning or disappear,
 * but new deferred-read sources may be added in minor versions.
 */
@Stable
interface NavTransitionScope {
    /**
     * The entry's continuous relative depth: `animatedTop - entryIndex`.
     *
     * The single float progress value that drives the whole transition. Approximately 0 means the
     * settled top; positive means covered/deeper; negative means above the top (entering or leaving).
     */
    val relativeDepth: Float

    /** The entry's current [NavRole], derived from [relativeDepth] and removal state. */
    val role: NavRole

    /** Classification of the stack change that produced this transition. */
    val change: NavChange

    /** Predictive-back gesture context, or `null` when the transition is not driven by a gesture. */
    val gesture: NavGesture?

    /** The size of the navigation host layout, for translating depth into pixel offsets. */
    val layoutSize: IntSize

    /** The layout direction, so transitions can mirror horizontal motion for RTL. */
    val layoutDirection: LayoutDirection

    /** The current density, for converting dp-based transition parameters to pixels. */
    val density: Density
}
