// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package top.yukonga.miuix.kmp.nav.gesture

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.nav.runtime.NavDriverSpec
import top.yukonga.miuix.kmp.nav.runtime.anchoredProgress
import top.yukonga.miuix.kmp.nav.runtime.navBackCommitDecision
import top.yukonga.miuix.kmp.nav.runtime.settleTo
import top.yukonga.miuix.kmp.nav.runtime.snapToFinger
import top.yukonga.miuix.kmp.nav.transition.NavGesture
import top.yukonga.miuix.kmp.nav.transition.NavSwipeDirection
import top.yukonga.miuix.kmp.nav.transition.NavSwipeEdge
import kotlin.math.abs

/**
 * Interactive swipe-to-dismiss that drives `animatedTop` directly, finger-following, along the axis
 * and direction the current transition declares ([NavSwipeDirection]).
 *
 * Attached by NavDisplay to the display container (so an in-flight transition can be grabbed from
 * anywhere on screen, not just the moving top entry's translated bounds — the container sees every
 * pointer on the Initial pass parent-first). A single unified pointer handler replaces the orientation-locked
 * `detectHorizontalDragGestures` / `detectVerticalDragGestures` detectors, which only owned one axis
 * and yielded the pointer to any other consumer — the source of two defects: an in-progress dismiss
 * cancelled the instant the finger drifted onto the cross axis (a nested scroll claimed the pointer),
 * and in-page taps/scrolls fired while the gesture was driving the transition. The unified handler
 * instead works in two phases:
 *
 * 1. **Engagement** — movement is watched on [PointerEventPass.Initial] (parent-first), so the moment
 *    travel **in the dismiss direction** crosses the [androidx.compose.ui.platform.ViewConfiguration]
 *    touch slop **and** dominates the cross axis, the pointer is claimed (consumed) before any nested
 *    scrollable can take it. A drag that is clearly cross-axis dominant is left untouched, so the
 *    page's own scrolling and taps still work when there is no dismiss intent. Travel opposite the
 *    dismiss direction never engages (there is nothing to drag the entry "forward" toward).
 * 2. **Follow** — once claimed, **every** pointer change is consumed on **both** axes, so neither a
 *    nested scroll nor any in-page handler can act on the finger that is driving the dismiss, and a
 *    cross-axis wiggle can no longer steal or cancel the gesture. The dismiss ends **only** on finger
 *    lift; there is no cross-axis cancel path.
 *
 * [NavSwipeDirection.None] disables the gesture entirely (pop via back button / system back).
 *
 * While following, `animatedTop` is driven via [snapToFinger] to
 * `topIndex - anchoredProgress(anchor, fingerProgress)`, where `anchor` is the progress already
 * travelled toward pop at the claim instant (sampled atomically with halting any in-flight settle —
 * grab-anchor invariant 6, so interrupting a running push/pop transition is jump-free), and
 * `fingerProgress` is the finger travel **in the dismiss direction since the claim point** over the
 * layout extent on that axis — linear, 1:1 with the finger (§7.1, no interpolation on this axis).
 *
 * On release the decision is delegated to Phase 3's [navBackCommitDecision] (**velocity-first,
 * position-fallback**, §7.2), using the shared thresholds [NavDriverSpec.COMMIT_VELOCITY_THRESHOLD] /
 * [NavDriverSpec.COMMIT_POSITION_THRESHOLD]. Release velocity is sampled with a [VelocityTracker]
 * (instantaneous, not a per-event delta proxy) and expressed in **progress-units per second** (axis
 * pixels/sec ÷ layout extent, signed toward the dismiss direction). That value feeds
 * [navBackCommitDecision] directly. For the convergence spring it is negated to the depth axis
 * (`animatedTop = topIndex - progress`, so depth velocity = -progress velocity) and handed to the
 * single shared spring via [settleTo]'s `initialVelocity`, making the snap -> spring handoff
 * velocity-continuous (§0.3 single spring).
 *
 * On commit [onCommit] is invoked **synchronously**, decoupled from the settle (see the release body
 * for why gating the pop behind the settle is unsafe in a per-entry coroutine scope).
 *
 * @param enabled Whether the swipe is active. Disable on the root entry (nothing to pop).
 * @param direction The dismiss axis + direction; [NavSwipeDirection.None] disables the gesture.
 * @param animatedTop The single shared depth driver.
 * @param topIndex Current top entry index being peeled away.
 * @param onCommit Invoked synchronously the moment a release is classified as a commit (pop the back
 *   stack here); the shared spring then settles `animatedTop` to the new top.
 * @param onCancel Invoked once after the cancel spring settles back to [topIndex].
 * @param onGesture Receives the live [NavGesture] context per follow event (progress, edge, touch
 *   position) for gesture-aware transitions, and `null` when the gesture has fully resolved. The
 *   last value is intentionally kept through the release settle (a transition pivoting around the
 *   touch point must not snap back to center at lift); the cancel branch clears it after its
 *   settle, the commit branch leaves clearing to the host (the leaving entry's unload).
 */
// composed { } is required here: the swipe needs composition-scoped state (coroutine scope,
// up-to-date callbacks). A Modifier.Node rewrite is a deferred optimization, so suppress the
// no-composed lint locally rather than disabling the performance rule project-wide.
@Suppress("ktlint:compose:modifier-composed-check")
fun Modifier.navSwipeDismiss(
    enabled: Boolean,
    direction: NavSwipeDirection,
    animatedTop: Animatable<Float, AnimationVector1D>,
    topIndex: Int,
    onCommit: () -> Unit,
    onCancel: () -> Unit,
    onGesture: (NavGesture?) -> Unit = {},
): Modifier = composed {
    if (!enabled || direction == NavSwipeDirection.None) return@composed this

    val scope = rememberCoroutineScope()
    val currentOnCommit = rememberUpdatedState(onCommit)
    val currentOnCancel = rememberUpdatedState(onCancel)
    val currentOnGesture = rememberUpdatedState(onGesture)

    // Edge semantics for gesture-aware transitions: a horizontal back swipe reads as starting from
    // the screen edge the finger travels away from; vertical dismissal has no meaningful edge.
    val swipeEdge = when (direction) {
        NavSwipeDirection.LeftToRight -> NavSwipeEdge.Left
        NavSwipeDirection.RightToLeft -> NavSwipeEdge.Right
        else -> NavSwipeEdge.None
    }

    val isHorizontal = direction == NavSwipeDirection.LeftToRight || direction == NavSwipeDirection.RightToLeft
    // Sign mapping a raw axis delta onto "progress toward dismiss": +1 when the dismiss direction is the
    // axis-positive one (right / down), -1 when it is axis-negative (left / up).
    val dismissSign = when (direction) {
        NavSwipeDirection.LeftToRight, NavSwipeDirection.TopToBottom -> 1f
        NavSwipeDirection.RightToLeft, NavSwipeDirection.BottomToTop -> -1f
        NavSwipeDirection.None -> 0f // unreachable (guarded above)
    }

    this.pointerInput(enabled, direction, topIndex) {
        awaitEachGesture {
            // Layout extent along the gesture axis, used to normalise finger travel into 0f..1f progress.
            // Read per-gesture from the live pointer scope so a resize between gestures is picked up.
            val extent = (if (isHorizontal) size.width else size.height).toFloat().coerceAtLeast(1f)
            val slop = viewConfiguration.touchSlop

            val down = awaitFirstDown(requireUnconsumed = false)
            // Tracks pointer position over time for an accurate instantaneous release velocity. Sampled
            // from the down so the velocity window is populated even before the gesture engages.
            val velocityTracker = VelocityTracker()
            velocityTracker.addPosition(down.uptimeMillis, down.position)

            // --- Engagement phase: decide whether this is our dismiss swipe, watching Initial pass. ---
            var preClaimDrag = 0f // accumulated travel on the gesture axis (raw signed pixels)
            var crossTravel = 0f // accumulated travel on the cross axis (raw signed pixels)
            var claimed = false
            while (!claimed) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id } ?: return@awaitEachGesture
                velocityTracker.addPosition(change.uptimeMillis, change.position)
                if (!change.pressed) return@awaitEachGesture // lifted before engaging: a tap / short press
                val delta = change.positionChange()
                preClaimDrag += if (isHorizontal) delta.x else delta.y
                crossTravel += if (isHorizontal) delta.y else delta.x
                val toward = dismissSign * preClaimDrag // > 0 means moving toward the dismiss direction
                if (toward > slop && toward >= abs(crossTravel)) {
                    // Dismiss-direction travel crossed slop and dominates: claim before nested scroll can.
                    claimed = true
                    change.consume()
                } else if (abs(crossTravel) > slop && abs(crossTravel) > abs(toward)) {
                    // Clearly a cross-axis gesture (e.g. the page's vertical scroll): never our swipe.
                    return@awaitEachGesture
                }
            }

            // --- Grab anchor (invariant 6): sampled atomically with halting the in-flight settle.
            // stop() must run off this handler: awaitEachGesture is a @RestrictsSuspension scope, so
            // a non-member suspend like Animatable.stop() can only be called from a launched
            // coroutine. Rather than rely on FIFO dispatch ordering to keep follow snaps from reading
            // an unset anchor (they can run while this coroutine is suspended inside stop()), publish
            // the sampled value through a CompletableDeferred that every follow snap awaits — no NaN
            // window, and the first snap target is still exactly where the spring halted (zero jump). ---
            val anchorReady = CompletableDeferred<Float>()
            scope.launch {
                animatedTop.stop()
                anchorReady.complete(topIndex - animatedTop.value)
            }

            // --- Follow phase: we own the pointer. Consume every change on both axes (so neither a nested
            // scroll nor any in-page handler sees the finger, and no cross-axis movement can cancel). The
            // dismiss follows the finger 1:1 from the claim point and ends only on lift. ---
            var drag = 0f
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                velocityTracker.addPosition(change.uptimeMillis, change.position)
                if (!change.pressed) {
                    change.consume()
                    break
                }
                val delta = change.positionChange()
                drag += if (isHorizontal) delta.x else delta.y
                change.consume()
                // Unclamped here; anchoredProgress clamps the total to [min(anchor, 0), 1].
                val fingerProgress = dismissSign * drag / extent
                val touchY = change.position.y
                scope.launch {
                    // Await the sampled grab anchor: returns immediately once stop() has published it.
                    val anchor = anchorReady.await()
                    animatedTop.snapToFinger(topIndex = topIndex, progress = fingerProgress, anchor = anchor)
                    // Published inside the driver coroutine so the anchor is always the sampled one.
                    currentOnGesture.value(
                        NavGesture(
                            progress = anchoredProgress(anchor = anchor, fingerProgress = fingerProgress).coerceAtLeast(0f),
                            swipeEdge = swipeEdge,
                            touchY = touchY,
                            initialTouchY = down.position.y,
                        ),
                    )
                }
            }

            // --- Release: velocity-first / position-fallback, velocity-continuous spring handoff. ---
            // If the finger lifted before stop() published the anchor (claim -> immediate lift), fall
            // back to sampling here: the decision then sees the freshest value, and the settle below
            // re-owns the float through the mutex either way.
            val releaseAnchor = if (anchorReady.isCompleted) anchorReady.getCompleted() else topIndex - animatedTop.value
            // Total progress on the pop axis; negative totals (leaving entry frozen above the grab
            // point) count as 0 for the commit decision — cancel-biased by construction.
            val progress = anchoredProgress(anchor = releaseAnchor, fingerProgress = dismissSign * drag / extent).coerceAtLeast(0f)
            // Instantaneous release velocity along the axis, in pixels/sec, projected onto the dismiss
            // direction and normalised to progress-units/sec (positive points toward pop).
            val velocity = velocityTracker.calculateVelocity()
            val axisVelocity = if (isHorizontal) velocity.x else velocity.y
            val progressVelocity = dismissSign * axisVelocity / extent
            // Depth axis is the inverse of the progress axis, so negate for the spring handoff.
            val depthVelocity = -progressVelocity
            if (navBackCommitDecision(progress = progress, velocity = progressVelocity)) {
                // Commit the back-stack change SYNCHRONOUSLY, never gated behind the settle. `scope` is
                // this entry's composition scope; the commit settle drives `animatedTop` to `topIndex - 1`,
                // at which point this (top) entry reaches relativeDepth -1, leaves the visible window, and
                // its host leaves composition -- cancelling `scope`. If the pop were sequenced AFTER
                // `settleTo` inside that scope, the cancellation could drop the pop entirely: the back stack
                // would stay unchanged while the renderer's shared spring pulls `animatedTop` back to the
                // un-popped top, so the page "springs back" and feels stuck. Popping first lets the renderer
                // own the final convergence; the settle below only seeds the spring with the release velocity.
                currentOnCommit.value()
                scope.launch {
                    // No-bounce commit: even a critically damped spring crosses its target once
                    // when the seeded fling speed exceeds ω·distance, which reads as the entry
                    // overshooting the fully-popped position and bouncing back. Floor the seed at
                    // the exact no-overshoot bound (computed from the value the settle actually
                    // starts at — all pending finger snaps are FIFO-ordered before this launch);
                    // slower releases keep full snap -> spring velocity continuity.
                    val floor = NavDriverSpec.Default.noOvershootVelocityFloor(animatedTop.value - (topIndex - 1f))
                    animatedTop.settleTo(
                        target = (topIndex - 1).toFloat(),
                        spec = NavDriverSpec.Default,
                        initialVelocity = depthVelocity.coerceAtLeast(floor),
                    )
                }
            } else {
                // Cancel keeps the entry in place (never marked removing), so its scope survives the settle
                // and onCancel can safely run after it.
                //
                // Clamp the seeded velocity to <= 0. The depth axis runs from `topIndex - 1` (fully
                // dismissed) up to `topIndex` (rest), and at release `animatedTop` is always <= topIndex.
                // A release that flicks AWAY from the dismiss direction (e.g. a back-swipe yanked back the
                // other way) produces a POSITIVE depth velocity, which would carry the shared spring PAST
                // the rest position to `animatedTop > topIndex`. That momentarily drops THIS top entry into
                // the covered regime (relativeDepth > 0), so its transition applies the covered parallax and
                // the page flashes a few frames in the wrong direction. A cancel has nothing above the top
                // to reveal, so that upward momentum is purely spurious: drop it and let the spring ease back
                // to rest from the current position. Legitimate "return" motion (negative depth velocity) is
                // kept, preserving the snap -> spring velocity continuity; it points away from the cancel
                // target, so it can never cross it (no bounce, no overshoot floor needed on this branch).
                scope.launch {
                    animatedTop.settleTo(
                        target = topIndex.toFloat(),
                        spec = NavDriverSpec.Default,
                        initialVelocity = depthVelocity.coerceAtMost(0f),
                    )
                    // The gesture context stays frozen through the settle (no pivot snap at lift)
                    // and is released only now, with the entry back at rest.
                    currentOnGesture.value(null)
                    currentOnCancel.value()
                }
            }
        }
    }
}
