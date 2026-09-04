// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package top.yukonga.miuix.kmp.nav.runtime

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Immutable
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Anchored gesture progress (grab-anchor model, spec 2026-06-10 §3.1 / invariant 6).
 *
 * A gesture may claim the stack while the shared spring is still mid-flight. [anchor] is the
 * progress already travelled toward pop at the claim instant (`topIndex - animatedTop.value`,
 * signed): positive when grabbed mid-push, negative when grabbed while a pop-settle is still
 * reeling a leaving entry out above the new top. The total progress is strictly additive —
 * `anchor + fingerProgress` — so the slope stays exactly 1 (1:1 with the finger, no easing on
 * this axis) and the first frame (`fingerProgress == 0`) maps back to the sampled anchor: zero
 * jump by construction.
 *
 * Clamp range:
 * - upper bound `1f`: the fully-popped end; an `anchor > 0` grab saturates early (the page pins
 *   at the end while the finger keeps travelling), matching the reference interactive-pop feel.
 * - lower bound `min(anchor, 0f)`: with `anchor >= 0` a reverse drag can at most push the page
 *   back to rest (never into the covered regime — the static analogue of the cancel velocity
 *   clamp); with `anchor < 0` it can at most freeze the leaving entry at the grab point, never
 *   re-revealing an already-popped page (the back stack no longer holds it).
 *
 * @param anchor progress toward pop at the claim instant, signed; `0f` for a rest-state grab.
 * @param fingerProgress finger travel since the claim in progress units, unclamped.
 * @return total progress on the pop axis, in `min(anchor, 0f)..1f`.
 */
internal fun anchoredProgress(anchor: Float, fingerProgress: Float): Float = (anchor + fingerProgress).coerceIn(anchor.coerceAtMost(0f), 1f)

/**
 * Pure mapping from a rest-state gesture progress to the `animatedTop` target value.
 *
 * The `anchor == 0` special case of the grab-anchor model: a gesture starting from a settled
 * top (`animatedTop == topIndex`). A fully completed gesture (`progress == 1`) drives
 * `animatedTop` exactly one step toward the previous entry, i.e. `topIndex - 1`. The mapping is
 * strictly linear (1:1 with the finger): no easing lives on the `finger -> animatedTop` axis,
 * so no inverse-transform is ever needed (contrast nav3's SeekableTransitionState, which bakes
 * easing into its fraction).
 *
 * @param topIndex index of the current top entry in the back stack (`lastIndex`).
 * @param progress raw gesture progress; clamped to `0f..1f` by [anchoredProgress].
 * @return the `animatedTop` value the gesture should snap to.
 */
internal fun fingerTarget(topIndex: Int, progress: Float): Float = topIndex - anchoredProgress(anchor = 0f, fingerProgress = progress)

/**
 * The single source of truth for the shared `animatedTop` spring (spec §9
 * "single shared spring"). Bundles the spring parameters, the release commit/cancel
 * thresholds, the shared [Default] instance, and the [spring] factory consumed by
 * [settleTo] — so no other phase ever redefines spring or threshold constants.
 *
 * Holds no lambdas and is never mutated, so it is safely [@Immutable]. The spring
 * fields feed [spring]/[settleTo]; the threshold fields feed [navBackCommitDecision].
 */
@Immutable
internal data class NavDriverSpec(
    val dampingRatio: Float = DAMPING_RATIO,
    val stiffness: Float = STIFFNESS,
    val visibilityThreshold: Float = VISIBILITY_THRESHOLD,
) {
    /**
     * Builds the [SpringSpec] for this driver. Single factory reused by [settleTo]
     * (and any future settle path), so the curve is identical everywhere. Calls the
     * top-level `spring` factory via its fully-qualified name to avoid colliding with
     * this same-named member function.
     */
    fun spring(): SpringSpec<Float> = androidx.compose.animation.core.spring(
        dampingRatio = dampingRatio,
        stiffness = stiffness,
        visibilityThreshold = visibilityThreshold,
    )

    /**
     * Builds the fixed-duration tween used for programmatic full-step settles
     * ([settleProgrammatic]): the established navigation curve ([NavProgrammaticEasing]) over
     * [PROGRAMMATIC_DURATION_MILLIS], matching the nav3-based miuix navigation point for point.
     */
    fun programmaticTween(): TweenSpec<Float> = tween(durationMillis = PROGRAMMATIC_DURATION_MILLIS, easing = NavProgrammaticEasing)

    /**
     * The most negative (toward-target) velocity a commit settle may be seeded with without
     * overshooting the target, given the [remainingDistance] still to travel (no-bounce
     * requirement).
     *
     * For a critically damped spring `x(t) = e^(-ωt)·(x0 + (v0 + ω·x0)·t)`, the trajectory
     * crosses zero iff `v0 < -ω·x0` — so flooring the seed at `-ω·distance` is the exact
     * no-overshoot condition, not an approximation. Slower releases keep their full velocity
     * (snap -> spring continuity intact); only the excess speed that could ONLY have become a
     * visible bounce-back past the fully-popped position is dropped.
     *
     * @param remainingDistance distance from the current value down to the settle target, `>= 0`.
     * @return the velocity floor (a value `<= 0`) to pass through `coerceAtLeast`.
     */
    fun noOvershootVelocityFloor(remainingDistance: Float): Float = if (remainingDistance <= 0f) 0f else -sqrt(stiffness) * remainingDistance

    companion object {
        /**
         * Critically damped: navigation must never bounce. An underdamped settle (the earlier
         * 0.9) oscillates past the target — invisible at ~0.15% when starting from rest, but a
         * velocity-seeded commit (a flung back-swipe) overshoots visibly and springs back.
         */
        const val DAMPING_RATIO: Float = 1f

        /**
         * Low stiffness on the depth scale (units = entries). With [DAMPING_RATIO] and the tight
         * [VISIBILITY_THRESHOLD], a full one-step push/pop settles in roughly half a second
         * (`t ≈ -ln(threshold) / (damping·√stiffness)`), matching the established miuix navigation
         * feel (a ~500ms transition) rather than a snappier default. The value keeps the decay
         * envelope of the original tuning (`1·√146 ≈ 0.9·√180`), so moving to critical damping
         * did not change the perceived duration.
         */
        const val STIFFNESS: Float = 146f

        /**
         * `animatedTop` is measured in entry-index units, so it converges visually once
         * within a few hundredths of an index. Tighter than the default 0.01 for px.
         */
        const val VISIBILITY_THRESHOLD: Float = 0.0025f

        /**
         * Duration of the programmatic full-step tween ([programmaticTween]). The same 500ms the
         * established navigation uses; the curve completes in exactly this time regardless of
         * distance (a multi-step pop sweeps all layers within the same window, like the
         * reference's single content transition).
         */
        const val PROGRAMMATIC_DURATION_MILLIS: Int = 500

        /**
         * Minimum settle distance (in entry-index units) for a from-rest settle to qualify as a
         * programmatic full step ([usesProgrammaticCurve]). Programmatic pushes/pops always move
         * integer distances; anything shorter is a partial-position continuation (a released
         * gesture, an interrupted transition) and belongs to the live spring. Slightly under 1
         * to tolerate visibility-threshold residue from a previous settle.
         */
        const val FULL_STEP_THRESHOLD: Float = 0.999f

        /**
         * Velocity (in progress-units per second) above which a release is treated as a
         * deliberate fling and commits/cancels by sign alone, ignoring position.
         */
        const val COMMIT_VELOCITY_THRESHOLD: Float = 1.0f

        /** Position fallback: progress at/after which a low-velocity release commits. */
        const val COMMIT_POSITION_THRESHOLD: Float = 0.5f

        /** Shared default instance to avoid per-frame allocation. */
        val Default: NavDriverSpec = NavDriverSpec()
    }
}

/**
 * Decides whether a released predictive-back / edge-swipe gesture should commit
 * (pop the top entry) or cancel (spring back), per spec §7.2 "velocity-first,
 * position-fallback".
 *
 * Velocity takes priority: a release flung hard enough in either direction wins
 * regardless of how far the finger travelled. Only when the release velocity sits
 * inside the dead zone (`|velocity| < velocityThreshold`) does position decide.
 *
 * @param progress gesture completion at release, `0f..1f` (0 = untouched, 1 = fully popped).
 * @param velocity release velocity in progress-units per second; positive points toward pop.
 * @param velocityThreshold magnitude above which velocity alone decides.
 * @param positionThreshold progress at/after which a low-velocity release commits.
 * @return `true` to commit (pop), `false` to cancel (spring back).
 */
internal fun navBackCommitDecision(
    progress: Float,
    velocity: Float,
    velocityThreshold: Float = NavDriverSpec.COMMIT_VELOCITY_THRESHOLD,
    positionThreshold: Float = NavDriverSpec.COMMIT_POSITION_THRESHOLD,
): Boolean = when {
    velocity >= velocityThreshold -> true
    velocity <= -velocityThreshold -> false
    else -> progress >= positionThreshold
}

/**
 * Drives [this] `animatedTop` to follow a gesture finger 1:1, with no spring or easing on the
 * path (spec §7.1, "snap mode"). Each gesture event calls this with the latest [progress]; the
 * value lands exactly on `topIndex - anchoredProgress(anchor, progress)`, so a later [settleTo]
 * can hand off from precisely where the finger left it.
 *
 * [anchor] implements the grab-anchor model ([anchoredProgress]): callers that claim the stack
 * while the shared spring is mid-flight pass the progress sampled at the claim instant, making
 * the first snap a no-op (zero jump). Rest-state gestures pass the default `0f`.
 *
 * @param topIndex index of the current top entry (`backStack.lastIndex`).
 * @param progress finger travel since the claim in progress units, unclamped.
 * @param anchor progress toward pop at the claim instant; see [anchoredProgress].
 */
internal suspend fun Animatable<Float, AnimationVector1D>.snapToFinger(
    topIndex: Int,
    progress: Float,
    anchor: Float = 0f,
) {
    snapTo(topIndex - anchoredProgress(anchor = anchor, fingerProgress = progress))
}

/**
 * Converges [this] `animatedTop` to [target] through the single shared spring
 * ([NavDriverSpec.spring], spec §7.1 "settle mode" / §9 "single shared spring").
 * Used for normal push/pop and for gesture release (commit -> `topIndex - 1`,
 * cancel -> `topIndex`).
 *
 * By default [initialVelocity] is [this] Animatable's own current [velocity], so the
 * value AND its first derivative stay continuous across the snap->spring boundary,
 * eliminating the visual jolt a fresh-from-zero spring would cause. Callers that
 * track a separate finger velocity (e.g. an edge swipe whose drag delta differs
 * from the Animatable's internal velocity) may pass it explicitly.
 *
 * @param target destination value on the depth axis (an entry index, possibly fractional during interruption).
 * @param spec spring + threshold parameters; defaults to [NavDriverSpec.Default].
 * @param initialVelocity velocity (depth-units per second) to seed the spring with; defaults to the current [velocity].
 */
internal suspend fun Animatable<Float, AnimationVector1D>.settleTo(
    target: Float,
    spec: NavDriverSpec = NavDriverSpec.Default,
    initialVelocity: Float = velocity,
) {
    animateTo(
        targetValue = target,
        animationSpec = spec.spring(),
        initialVelocity = initialVelocity,
    )
}

/**
 * Whether a settle starting with [velocity] over [distance] should play the programmatic
 * fixed-duration curve ([NavDriverSpec.programmaticTween]) instead of the live spring.
 *
 * Only a from-rest, full-step settle qualifies — exactly the programmatic push/pop case, which
 * must match the established navigation curve point for point. Anything carrying velocity (an
 * interrupted tween, a gesture handoff) or covering a partial distance (a settle resumed from a
 * mid-gesture position) stays on the spring: a fixed-duration tween cannot seed velocity and
 * would mis-pace short distances.
 *
 * @param velocity the driver's velocity at settle start (depth-units per second).
 * @param distance signed distance from the current value to the target (entry-index units).
 */
internal fun usesProgrammaticCurve(velocity: Float, distance: Float): Boolean = velocity == 0f && abs(distance) >= NavDriverSpec.FULL_STEP_THRESHOLD

/**
 * Renderer-side settle: converges [this] `animatedTop` to [target], dispatching between the two
 * settle curves (spec 2026-06-10 §"programmatic curve match"):
 *
 * - **From rest over a full step** (a programmatic push/pop/multi-pop): the fixed 500ms tween
 *   with the established easing — identical pacing to the nav3-based miuix navigation.
 * - **Anything else** (carrying velocity from an interrupted tween, or resuming from a partial
 *   position after a gesture): the live shared spring via [settleTo], seeded with the current
 *   velocity so the handoff is velocity-continuous.
 *
 * @param target destination value on the depth axis.
 * @param spec spring + tween parameters; defaults to [NavDriverSpec.Default].
 */
internal suspend fun Animatable<Float, AnimationVector1D>.settleProgrammatic(
    target: Float,
    spec: NavDriverSpec = NavDriverSpec.Default,
) {
    if (usesProgrammaticCurve(velocity = velocity, distance = target - value)) {
        animateTo(targetValue = target, animationSpec = spec.programmaticTween())
    } else {
        settleTo(target, spec)
    }
}
