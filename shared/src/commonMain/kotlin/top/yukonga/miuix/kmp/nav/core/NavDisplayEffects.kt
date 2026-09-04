// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package top.yukonga.miuix.kmp.nav.core

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Which corners of the transitioning top entry the corner-clip effect rounds.
 *
 * The clip is the effects layer's job (not the transition's) because it needs composable
 * machinery the per-frame transition transform cannot host: the smooth (squircle) corner shape
 * and a platform-provided radius ([rememberNavSystemCornerRadius]).
 */
enum class NavCornerClipMode {
    /**
     * Only the leading-edge corners (left under LTR, right under RTL) — the corners that meet
     * the screen edge as a slide-style entering page moves in over the layer below.
     */
    Leading,

    /**
     * All four corners — for card-style transitions that scale the whole page, so every corner
     * meets visible background.
     */
    All,
}

/**
 * Orthogonal visual effects applied by [NavDisplay] on top of the active
 * [top.yukonga.miuix.kmp.nav.transition.NavTransition].
 *
 * All switches are computed against each entry's relative depth, independently of the chosen
 * transition. This type holds no lambda fields and is therefore [Immutable]; the per-depth helpers
 * are pure functions of the switches and the depth.
 *
 * @property enableCornerClip whether to clip the transitioning top entry with smooth rounded
 *   corners (per [cornerClipMode]) while it animates over the layer below.
 * @property cornerClipRadius the corner radius used when [enableCornerClip] is on. Defaults to
 *   0.dp (no rounding); pass [rememberNavSystemCornerRadius] to follow the device screen corner
 *   (matching the platform), which still yields no rounding where the platform reports 0.
 * @property cornerClipMode which corners to round; [NavCornerClipMode.Leading] for slide-style
 *   transitions, [NavCornerClipMode.All] for card-style ones.
 * @property dimAmount maximum alpha of the fullscreen dim scrim rendered just beneath the
 *   top-most layer during a transition. It covers the revealed page and the [backdropColor]
 *   area alike (the reference scrim spans the whole entering surface). Set to 0f to disable.
 *   The curve along the motion is owned by the governing transition
 *   ([top.yukonga.miuix.kmp.nav.transition.NavTransition.scrimFraction]); this knob only caps
 *   how dark it gets.
 * @property blockInputDuringTransition whether to swallow touch input on non-settled entries while a
 *   transition is in progress, so taps cannot reach a half-animated screen.
 * @property backdropColor solid color drawn behind every entry layer. Card-style transitions scale
 *   layers below full size, revealing the area behind the navigation host; the reference shell
 *   animation fills it with a color layer in the entering page's background color, so the page
 *   background reads as extending outward. Pass the theme background to reproduce that;
 *   [Color.Unspecified] (default) draws no backdrop.
 */
@Immutable
data class NavDisplayEffects(
    val enableCornerClip: Boolean = true,
    val cornerClipRadius: Dp = 0.dp,
    val cornerClipMode: NavCornerClipMode = NavCornerClipMode.Leading,
    val dimAmount: Float = 0.5f,
    val blockInputDuringTransition: Boolean = true,
    val backdropColor: Color = Color.Unspecified,
) {
    /**
     * Whether an entry should be corner-clipped at relative depth [relativeDepth], when
     * [enableCornerClip]:
     *
     * - [NavCornerClipMode.Leading]: only the entry strictly crossing the front edge
     *   (-1 < d < 0); a settled top (d == 0) or fully-exited entry (d == -1) is not clipped.
     * - [NavCornerClipMode.All]: any layer at a fractional depth (-1 < d < 1, d != 0) — the
     *   reference card animation rounds BOTH surfaces (the card and the revealed layer below)
     *   for the whole animation, and the revealed layer is visibly scaled while covered.
     */
    @Stable
    fun shouldClipCornersAt(relativeDepth: Float): Boolean {
        if (!enableCornerClip) return false
        return when (cornerClipMode) {
            NavCornerClipMode.Leading -> relativeDepth > -1f && relativeDepth < 0f
            NavCornerClipMode.All -> relativeDepth > -1f && relativeDepth < 1f && relativeDepth != 0f
        }
    }

    /**
     * Whether pointer input should be blocked for an entry at relative depth [relativeDepth]. Input is
     * blocked only on a layer that is mid-transition — neither the settled top (d == 0) nor any other
     * settled integer depth (e.g. a fully covered layer at d == 1) — while [blockInputDuringTransition].
     */
    @Stable
    fun shouldBlockInputAt(relativeDepth: Float): Boolean {
        if (!blockInputDuringTransition) return false
        val isSettledTop = relativeDepth == 0f
        val isSettled = relativeDepth == relativeDepth.toInt().toFloat()
        return !isSettledTop && !isSettled
    }

    companion object {
        /** The default effects (corner clip on, 0.5 dim, input blocking on). */
        val Default: NavDisplayEffects = NavDisplayEffects()

        /** All effects disabled: no corner clip, no dim, no input blocking. */
        val None: NavDisplayEffects = NavDisplayEffects(
            enableCornerClip = false,
            dimAmount = 0f,
            blockInputDuringTransition = false,
        )
    }
}
