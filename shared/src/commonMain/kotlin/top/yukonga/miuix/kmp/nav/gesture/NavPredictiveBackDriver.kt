// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package top.yukonga.miuix.kmp.nav.gesture

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import kotlinx.coroutines.flow.Flow
import top.yukonga.miuix.kmp.nav.runtime.snapToFinger

/**
 * Drives `animatedTop` from a predictive-back [NavBackEvent] stream, implementing the finger-
 * following half of the §7.1 double-mode contract with the grab-anchor model (invariant 6):
 *
 * - **On entry** the in-flight settle (if any) is halted FIRST via `stop()` (cancelling it
 *   through the Animatable mutex) and the anchor is sampled from the halted value — atomically,
 *   so the spring cannot advance another frame between sampling and the first snap. The anchor
 *   is the progress already travelled toward pop (`topIndex - value`, signed; negative while a
 *   pop-settle is still reeling a leaving entry out above the new top).
 * - **During the gesture** each event maps to
 *   `animatedTop.snapToFinger(topIndex, event.progress, anchor)` — additive, strictly linear,
 *   1:1 with the finger (Phase 3 driver). The first event (`progress ≈ 0`) therefore lands
 *   exactly on the sampled value: zero jump when interrupting a running push/pop.
 * - The function returns when the stream completes. Convergence (commit -> `topIndex - 1`,
 *   cancel -> `topIndex`) is **not** done here: the caller decides commit/cancel via
 *   [top.yukonga.miuix.kmp.nav.runtime.navBackCommitDecision] and settles with the single shared
 *   spring via [top.yukonga.miuix.kmp.nav.runtime.settleTo], handing off the release velocity so
 *   the snap -> spring boundary is velocity-continuous.
 *
 * Keeping settle/decision out of this function is deliberate: `PredictiveBackHandler`'s
 * commit/cancel signal comes from the outer try/catch around the stream, not from inside it.
 *
 * @param events Per-gesture cold flow of [NavBackEvent].
 * @param animatedTop The single shared depth driver.
 * @param topIndex The current top entry index the gesture is peeling away from.
 */
internal suspend fun drivePredictiveBack(
    events: Flow<NavBackEvent>,
    animatedTop: Animatable<Float, AnimationVector1D>,
    topIndex: Int,
) {
    // Atomic grab: halt the in-flight settle, THEN read the value (spec §3.2). Order matters —
    // sampling before stop() would leave a one-frame window for the spring to advance.
    animatedTop.stop()
    val anchor = topIndex - animatedTop.value
    events.collect { event ->
        animatedTop.snapToFinger(topIndex = topIndex, progress = event.progress, anchor = anchor)
    }
}
