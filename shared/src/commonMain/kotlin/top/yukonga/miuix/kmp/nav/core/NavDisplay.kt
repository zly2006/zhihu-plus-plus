// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package top.yukonga.miuix.kmp.nav.core

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.nav.gesture.PredictiveBackHandler
import top.yukonga.miuix.kmp.nav.gesture.drivePredictiveBack
import top.yukonga.miuix.kmp.nav.gesture.navSwipeDismiss
import top.yukonga.miuix.kmp.nav.runtime.NavChange
import top.yukonga.miuix.kmp.nav.runtime.NavDriverSpec
import top.yukonga.miuix.kmp.nav.runtime.NavPresentation
import top.yukonga.miuix.kmp.nav.runtime.isVisibleAt
import top.yukonga.miuix.kmp.nav.runtime.navReconcile
import top.yukonga.miuix.kmp.nav.runtime.relativeDepth
import top.yukonga.miuix.kmp.nav.runtime.rememberNavPresentation
import top.yukonga.miuix.kmp.nav.runtime.settleProgrammatic
import top.yukonga.miuix.kmp.nav.runtime.settleTo
import top.yukonga.miuix.kmp.nav.state.NavSaveableStateHolder
import top.yukonga.miuix.kmp.nav.state.ProvideNavEntryLifecycle
import top.yukonga.miuix.kmp.nav.state.ProvideNavEntryViewModelStore
import top.yukonga.miuix.kmp.nav.state.navMaxLifecycleFor
import top.yukonga.miuix.kmp.nav.state.rememberNavEntryLifecycleOwner
import top.yukonga.miuix.kmp.nav.state.rememberNavEntryViewModelStoreOwner
import top.yukonga.miuix.kmp.nav.state.rememberNavSaveableStateHolder
import top.yukonga.miuix.kmp.nav.transition.NavGesture
import top.yukonga.miuix.kmp.nav.transition.NavSwipeDirection
import top.yukonga.miuix.kmp.nav.transition.NavTransition
import top.yukonga.miuix.kmp.nav.transition.NavTransitions
import top.yukonga.miuix.kmp.squircle.absoluteSquircleClip
import kotlin.reflect.KClass

/**
 * Metadata key under which a per-route [NavTransition] override is stored on a built [NavEntry].
 *
 * The renderer reads this key from [NavEntry.metadata]; a `null`/absent value means the entry
 * inherits the global transition passed to [NavDisplay].
 */
@PublishedApi
internal const val NAV_TRANSITION_METADATA_KEY: String = "top.yukonga.miuix.kmp.nav.transition"

/**
 * Metadata key under which a per-route [NavSwipeDirection] override is stored on a built [NavEntry].
 *
 * A `null`/absent value means the entry inherits the dismiss direction of its governing
 * [NavTransition.dismissDirection]; an explicit value (including [NavSwipeDirection.None] to disable
 * the gesture) wins.
 */
@PublishedApi
internal const val NAV_SWIPE_DISMISS_METADATA_KEY: String = "top.yukonga.miuix.kmp.nav.swipeDismiss"

/**
 * DSL receiver of the [NavDisplay] content lambda.
 *
 * Each `entry<T> { ... }` call registers, keyed by the route's runtime [KClass], how to build a
 * [NavEntry] for keys of that type. [build] materializes the registrations into a lookup function.
 */
class NavEntryBuilder<T : NavKey> {
    private val factories: MutableMap<KClass<*>, (T) -> NavEntry<*>> = mutableMapOf()

    /**
     * Registers how to render keys of the route subtype [S].
     *
     * @param contentKey value-stable identity for the produced entry; when `null` the key instance
     *   itself is used (data classes / data objects give stable equality out of the box).
     * @param transition per-route transition override; `null` inherits [NavDisplay]'s global
     *   transition (see design §6.4). Stored in the entry's metadata.
     * @param swipeDismiss per-route interactive swipe-to-dismiss direction; `null` inherits the
     *   governing transition's [NavTransition.dismissDirection]. Pass [NavSwipeDirection.None] to
     *   disable the gesture on this route, or another direction to override it.
     * @param metadata extra per-entry metadata, merged with the transition / swipe-dismiss overrides.
     * @param content the composable rendering a key of type [S].
     */
    inline fun <reified S : T> entry(
        contentKey: Any? = null,
        transition: NavTransition? = null,
        swipeDismiss: NavSwipeDirection? = null,
        metadata: Map<String, Any> = emptyMap(),
        noinline content: @Composable (S) -> Unit,
    ) {
        register(S::class) { key ->
            @Suppress("UNCHECKED_CAST")
            val typed = key as S
            var mergedMetadata = metadata
            if (transition != null) mergedMetadata = mergedMetadata + (NAV_TRANSITION_METADATA_KEY to transition)
            if (swipeDismiss != null) mergedMetadata = mergedMetadata + (NAV_SWIPE_DISMISS_METADATA_KEY to swipeDismiss)
            NavEntry(
                key = typed,
                contentKey = contentKey ?: typed,
                metadata = mergedMetadata,
                content = content,
            )
        }
    }

    /**
     * Low-level registration hook used by the [entry] inline function. Public only so the inline
     * reified [entry] can call it across the inline boundary; not intended for direct use.
     */
    @PublishedApi
    internal fun register(clazz: KClass<*>, factory: (T) -> NavEntry<*>) {
        factories[clazz] = factory
    }

    /**
     * Materializes the accumulated registrations into an entry lookup. Throws if a key has no
     * registered [entry] of its (or a superclass') type.
     */
    @PublishedApi
    internal fun build(): (T) -> NavEntry<*> = { key ->
        val factory = factories[key::class]
            ?: error("No entry { } registered for ${key::class.simpleName ?: key::class}")
        factory(key)
    }
}

/**
 * Materializes a [NavEntryBuilder] DSL block into an entry lookup function.
 *
 * The [NavDisplay] DSL overloads call this internally; the low-level [NavDisplay] overload accepts a
 * prebuilt lookup of this exact shape.
 */
internal fun <T : NavKey> entryProvider(builder: NavEntryBuilder<T>.() -> Unit): (T) -> NavEntry<*> = NavEntryBuilder<T>().apply(builder).build()

/**
 * Drives [NavPresentation.animatedTop] towards [target], unless a gesture currently owns the
 * float (in which case the gesture layer snaps it via `snapToFinger`). Implemented as a
 * [LaunchedEffect] keyed on [target] that defers to the driver's settle dispatch
 * (`settleProgrammatic`: the fixed-duration programmatic curve from rest over a full step, the
 * velocity-continuous shared spring otherwise) — the renderer never builds its own
 * [androidx.compose.animation.core.AnimationSpec].
 */
@Composable
private fun NavPresentation.animateTopTo(target: Float) {
    LaunchedEffect(target) {
        // Consume the release velocity exactly once per retarget, whether or not it gets used.
        val released = pendingSettleVelocity
        pendingSettleVelocity = 0f
        if (animatedTop.value == target) return@LaunchedEffect
        // A gesture-release settle toward this same target may already own the float (a swipe
        // commit pops synchronously and queues its velocity-seeded settle ahead of this effect);
        // defer to it instead of stomping the seeded spring with a fresh-from-rest curve.
        if (animatedTop.isRunning && animatedTop.targetValue == target) return@LaunchedEffect
        if (released < 0f && target < animatedTop.value) {
            // A flung predictive-back commit: seed the spring with the gesture's release velocity
            // so the settle carries the momentum (the reference feeds it into its post-commit
            // spring, clamped toward the target). The no-overshoot floor keeps it bounce-free.
            val floor = NavDriverSpec.Default.noOvershootVelocityFloor(animatedTop.value - target)
            animatedTop.settleTo(target, initialVelocity = released.coerceAtLeast(floor))
        } else {
            animatedTop.settleProgrammatic(target)
        }
    }
}

/** Reads the per-route [NavTransition] override from an entry's metadata, or null to inherit. */
private fun NavEntry<*>.transitionOrNull(): NavTransition? = metadata[NAV_TRANSITION_METADATA_KEY] as? NavTransition

/** Reads the per-route [NavSwipeDirection] override from an entry's metadata, or null to inherit. */
private fun NavEntry<*>.swipeDismissOrNull(): NavSwipeDirection? = metadata[NAV_SWIPE_DISMISS_METADATA_KEY] as? NavSwipeDirection

/**
 * Private rendering core shared by all [NavDisplay] overloads.
 *
 * Responsibilities:
 * - build one [NavEntry] per current back-stack key via [entryProvider];
 * - reconcile against the previous render set ([NavPresentation.reconcile]) to obtain the presentation
 *   set (current ∪ exiting) and the [NavChange];
 * - keep the single driving float [NavPresentation.animatedTop] converging to `lastIndex` via the
 *   shared spring ([animateTopTo] → `settleTo`);
 * - precompute a `contentKey -> index` map at reconcile time so depth math is an O(1) lookup at
 *   render time (no O(n^2) rebuild of NavEntry instances per frame);
 * - render only the visible window ([derivedStateOf] over `presented` + the governing opaqueDepth),
 *   applying the governing transition (boundary ownership §4.3), the orthogonal [effects], and
 *   per-entry state scopes; fully-exited entries are unloaded.
 *
 * Visual reads of [NavPresentation.animatedTop] happen inside deferred `graphicsLayer { }` blocks so
 * the per-frame float change does not recompose this function. The saveable-state holder is created
 * **once here** and passed down to each [NavEntryHost] (stable identity, see §0.4).
 */
@Composable
private fun <T : NavKey> NavDisplayLayout(
    backStack: NavBackStack<T>,
    entryProvider: (T) -> NavEntry<*>,
    onBack: () -> Unit,
    transition: NavTransition,
    effects: NavDisplayEffects,
    modifier: Modifier = Modifier,
    // [Zhihu++ fork] 关闭后系统返回仍可用，但不跟手驱动 animatedTop（提交时由收敛弹簧正常播放 pop），
    // 对应设置项“启用预测性返回”。
    enablePredictiveBack: Boolean = true,
) {
    val presentation = rememberNavPresentation(backStack.lastIndex)
    val stateHolder = rememberNavSaveableStateHolder()

    val topIndex = backStack.lastIndex
    val backScope = rememberCoroutineScope()
    val currentOnBack = rememberUpdatedState(onBack)

    // Wire the platform predictive back gesture to the single driver. Enabled only when there is
    // something to pop (size > 1). During the gesture each NavBackEvent snaps animatedTop 1:1;
    // on commit onBack() pops and the renderer's animateTopTo converges to the new top; on cancel
    // the shared spring settles back to topIndex. Modifier.navSwipeDismiss on the display
    // container drives the same animatedTop for the interactive edge swipe.
    PredictiveBackHandler(
        enabled = backStack.size > 1,
        onProgress = { events ->
            if (!enablePredictiveBack) {
                // [Zhihu++ fork] 关闭预测性返回：只消费手势事件、不驱动 animatedTop；onCommit 仍会 pop，
                // 由收敛弹簧把 pop 当作普通编程式返回播放（无跟手动画）。
                events.collect { }
                return@PredictiveBackHandler
            }
            // Per-gesture trackers: the initial touch anchors vertical-follow transitions, the
            // last two timestamped events estimate the release velocity (depth-units/sec).
            var initialTouchY = Float.NaN
            var lastProgress = 0f
            var lastTimeMillis = 0L
            drivePredictiveBack(
                // Mirror each event into the live gesture context for gesture-aware transitions
                // (touch-following shifts etc.); NavBackEvent and NavGesture share field semantics.
                events = events.onEach { event ->
                    if (initialTouchY.isNaN()) initialTouchY = event.touchY
                    if (event.frameTimeMillis > lastTimeMillis && lastTimeMillis != 0L) {
                        val progressVelocity = (event.progress - lastProgress) * 1000f / (event.frameTimeMillis - lastTimeMillis)
                        presentation.pendingSettleVelocity = -progressVelocity
                    }
                    if (event.frameTimeMillis != 0L) {
                        lastProgress = event.progress
                        lastTimeMillis = event.frameTimeMillis
                    }
                    presentation.gesture = NavGesture(
                        progress = event.progress,
                        swipeEdge = event.swipeEdge,
                        touchY = event.touchY,
                        initialTouchY = initialTouchY,
                    )
                },
                animatedTop = presentation.animatedTop,
                topIndex = topIndex,
            )
        },
        onCommit = {
            // Pop synchronously; convergence is owned by the renderer. The pop retargets
            // animateTopTo, which picks the programmatic curve for a from-rest discrete back
            // (identical to a programmatic pop) and the velocity-continuous spring when the
            // gesture left the float mid-flight. The gesture context stays frozen through the
            // settle and is cleared when the leaving entry unloads.
            currentOnBack.value()
        },
        onCancel = {
            presentation.pendingSettleVelocity = 0f
            backScope.launch {
                presentation.animatedTop.settleTo(target = topIndex.toFloat())
                presentation.gesture = null
            }
        },
    )

    // Build entries for the current back stack and reconcile. The build runs only when the back-stack
    // key list changes; entry instances for surviving keys are reused by the presentation. The
    // reconcile pass classifies the change against the PREVIOUS back-stack content keys (kept in a
    // remembered holder, not the presented set — the latter still holds exiting keys) and precomputes a
    // stable contentKey -> back-stack-index map so the render loop never rebuilds NavEntry instances to
    // recover an index (no O(n^2)).
    val currentKeyList = backStack.toList()
    // Plain (non-snapshot) holder for the previous classification input; bookkeeping only, never read
    // by UI, so it must not be snapshot state (no cross-phase back-write).
    val previousContentKeys = remember { arrayOfNulls<List<Any>>(1) }
    // Persistent contentKey -> back-stack index. Current entries get their fresh index on each
    // reconcile; a leaving (popped/replaced) entry KEEPS its last index here so it can still render
    // its exit animation, and is pruned only when it has finished leaving (see the unload effect
    // below). Plain (non-snapshot) map: it is mutated in lockstep with presentation.presented (a
    // snapshot list) inside the reconcile block, and the visible-window derivedStateOf re-evaluates
    // off that snapshot list rather than off this map.
    val indexByContentKey = remember { mutableMapOf<Any, Int>() }
    remember(currentKeyList, entryProvider) {
        // Disambiguate value-equal keys by their occurrence in the stack BEFORE reconcile: the
        // reconciler/render loop/saveable holder all key off contentKey, so two equal route keys
        // (e.g. Article(42) reached twice) must resolve to distinct, deterministic slot ids — the
        // first keeps its base key, later ones get an occurrence-tagged key (restore-stable).
        val seenBaseKeys = HashMap<Any, Int>()
        val currentEntries = currentKeyList.map { key ->
            val entry = entryProvider(key)
            val occurrence = seenBaseKeys.getOrElse(entry.contentKey) { 0 }
            seenBaseKeys[entry.contentKey] = occurrence + 1
            if (occurrence > 0) entry.disambiguateOccurrence(occurrence)
            entry
        }
        val newContentKeys = currentEntries.map { it.contentKey }
        presentation.reconcile(currentEntries, navReconcile(previousContentKeys[0] ?: emptyList(), newContentKeys))
        previousContentKeys[0] = newContentKeys
        currentEntries.forEachIndexed { index, e -> indexByContentKey[e.contentKey] = index }
    }

    // Unload entries that have finished leaving — either they slid fully off the front edge
    // (relativeDepth <= -1, the pop case) or a current entry now occupies their index (the replace
    // case, where the driver never moves past them). Such entries are no longer (or never were) in the
    // visible window, so no NavEntryHost runs to self-unload; prune them here at the layout level: drop
    // the saved state and remove from the presentation set + index map. Each entry's per-entry
    // ViewModelStore is cleared automatically by its host's RememberObserver.onForgotten when the host
    // leaves composition.
    val finishedLeaving by remember(presentation, indexByContentKey) {
        derivedStateOf {
            val top = presentation.animatedTop.value
            val currentSize = backStack.size
            val presented = presentation.presented
            // Hand-rolled scan: this calculation re-runs on every animation frame (it must, so the
            // derived state can compare results), and the common nothing-leaving case has to stay
            // allocation-free — no filter list, no closure, no boxing.
            var result: MutableList<NavEntry<*>>? = null
            for (i in presented.indices) {
                val e = presented[i]
                if (!e.presentation.isRemoving) continue
                val index = indexByContentKey[e.contentKey]
                if (index == null || top - index <= -1f || index < currentSize) {
                    (result ?: ArrayList<NavEntry<*>>(presented.size).also { result = it }).add(e)
                }
            }
            result ?: emptyList()
        }
    }
    if (finishedLeaving.isNotEmpty()) {
        SideEffect {
            finishedLeaving.fastForEach { e ->
                stateHolder.removeState(e.contentKey)
                indexByContentKey.remove(e.contentKey)
                presentation.unload(e)
            }
            // A committed back gesture resolves here (its leaving entry just unloaded); release
            // the gesture context that was kept frozen through the settle.
            presentation.gesture = null
        }
    }

    // Drive the single shared float towards the new top index via the shared spring. The renderer only
    // sets the target; the interactive gesture layer may snapToFinger it instead.
    presentation.animateTopTo(backStack.lastIndex.toFloat())

    // Effective interactive dismiss direction of the CURRENT top entry: a per-route override wins,
    // else its governing transition declares the natural direction. Rebuilding one entry to read
    // its metadata is O(1) and only happens when the stack, the global transition, or the entry
    // provider changes.
    val topDismissDirection = remember(currentKeyList, transition, entryProvider) {
        val topEntry = entryProvider(currentKeyList.last())
        topEntry.swipeDismissOrNull() ?: (topEntry.transitionOrNull() ?: transition).dismissDirection
    }

    var layoutSize by remember { mutableStateOf(IntSize.Zero) }
    val layoutDirection = LocalLayoutDirection.current
    val density = LocalDensity.current

    Box(
        // The unified swipe recognizer lives on the DISPLAY CONTAINER, not on the top entry's host:
        // the host's hit-rect follows its graphicsLayer translation, so early in a push most of the
        // screen belongs to the covered (input-blocked) layer below and an interrupting back-swipe
        // could never engage. The container sees every pointer on the Initial pass parent-first —
        // before any entry's nested scroll or input-blocking modifier — keeping the two-phase
        // claiming contract (invariant 4) intact while making the whole screen grabbable at any
        // point of an in-flight transition. At rest the behavior is unchanged (the settled top host
        // was full-screen anyway).
        modifier = modifier
            .onSizeChanged { layoutSize = it }
            .navSwipeDismiss(
                // Never on the root (nothing to pop); NavSwipeDirection.None keeps it disabled.
                enabled = topIndex > 0,
                direction = topDismissDirection,
                animatedTop = presentation.animatedTop,
                topIndex = topIndex,
                onCommit = currentOnBack.value,
                onCancel = {},
                onGesture = { presentation.gesture = it },
            ),
    ) {
        // Backdrop behind every entry (reference shell behavior): a solid layer in the page
        // background color, so the area revealed around a scaled-down entry reads as the page
        // background extending outward instead of whatever sits behind the navigation host. At
        // rest it is fully covered by the opaque top entry.
        if (effects.backdropColor.isSpecified) {
            Box(modifier = Modifier.fillMaxSize().background(effects.backdropColor))
        }

        // Visible window (spec §4.4 / §9): an entry is visible when -1 < d <= opaqueDepth. The window
        // depth is the MAX opaqueDepth across the global transition and every presented entry's
        // per-route override — the renderer keeps a layer alive while ANY transition in play would, so
        // a Modal override (opaqueDepth 2f) keeps the layer below it visible even under a shallower
        // global transition. Over-keeping a hidden layer is cheap; under-keeping would blank it out.
        // The derived calculation re-runs each animation frame (it must, so the derived state can
        // compare results) but notifies readers only when the membership actually changes — i.e.
        // when a window boundary is crossed; per-frame floats are otherwise read lazily in
        // graphicsLayer. Hand-rolled max/filter keeps the per-frame re-run free of closures,
        // iterators, and boxed floats; the only allocation left is the result list.
        val visibleEntries by remember(presentation, transition, indexByContentKey) {
            derivedStateOf {
                val top = presentation.animatedTop.value
                val presented = presentation.presented
                var windowDepth = transition.opaqueDepth
                for (i in presented.indices) {
                    val depth = (presented[i].transitionOrNull() ?: transition).opaqueDepth
                    if (depth > windowDepth) windowDepth = depth
                }
                var result: MutableList<NavEntry<*>>? = null
                for (i in presented.indices) {
                    val entry = presented[i]
                    val index = indexByContentKey[entry.contentKey] ?: continue
                    if (isVisibleAt(relativeDepth(top, index), windowDepth)) {
                        (result ?: ArrayList<NavEntry<*>>(presented.size).also { result = it }).add(entry)
                    }
                }
                result ?: emptyList()
            }
        }

        // Highest visible layer; the fullscreen dim scrim renders just beneath it.
        val topMostVisibleIndex = visibleEntries.fold(-1) { acc, e ->
            maxOf(acc, indexByContentKey[e.contentKey] ?: -1)
        }

        visibleEntries.fastForEach { entry ->
            key(entry.contentKey) {
                // Local non-null guard, NOT a non-local `return@key`. A non-local return out of an
                // inline @Composable lambda makes the Kotlin compiler emit a synthetic
                // `$$$$$NON_LOCAL_RETURN$$$$$."<anonymous>"()` marker; the Compose Hot Reload javaagent
                // then materialises that into a method literally named "<anonymous>", which the JVM
                // rejects at class load (ClassFormatError). Entries reaching here always have an index
                // (they survived the visibleEntries filter over the same map), so the body always runs.
                indexByContentKey[entry.contentKey]?.let { entryIndex ->
                    // Boundary ownership (§4.3): the entry's own transition governs while it is at the
                    // top (d <= 0); the upper neighbour's transition governs the covered treatment
                    // (0 < d <= 1). NavEntryHost picks per relativeDepth inside the deferred
                    // graphicsLayer so the choice costs no recomposition.
                    val ownTransition = entry.transitionOrNull() ?: transition
                    // Resolve the upper neighbour from the PRESENTED set (which still holds a
                    // popped-but-animating-out entry), NOT the live back stack. A leaving entry keeps its
                    // last index in indexByContentKey, so index + 1 still points at it until it unloads.
                    // Using backStack here would, the instant an upper entry with a per-route transition
                    // (e.g. a bottom-up Modal) is popped, revert this covered layer to the global transition
                    // mid-dismiss — for a Modal over the horizontal-slide default that wrongly parallaxes the
                    // layer below sideways while the modal is still sliding down (§4.3 boundary ownership).
                    val upperEntry = presentation.presented.firstOrNull { indexByContentKey[it.contentKey] == entryIndex + 1 }
                    val upperTransition = upperEntry?.transitionOrNull() ?: transition

                    if (effects.dimAmount > 0f && entryIndex == topMostVisibleIndex && entryIndex > 0) {
                        // Fullscreen dim scrim UNDER the top-most visible layer (reference shell
                        // behavior): it must cover the entering page AND the backdrop revealed
                        // around it when a card-style transition scales layers down — a scrim
                        // inside the entry host would shrink with the page and leave the
                        // surroundings undimmed.
                        //
                        // The scrim is part of the covered treatment, so the top entry's governing
                        // transition owns its curve (§4.3 boundary ownership, NavTransition.scrimFraction),
                        // evaluated against the covered layer directly below (depth 0 = revealed,
                        // 1 = covered). The effects layer only caps the darkness (dimAmount). The
                        // fraction is read inside the deferred graphicsLayer, so the per-frame float
                        // never recomposes this layout.
                        val belowEntry = presentation.presented.firstOrNull { indexByContentKey[it.contentKey] == entryIndex - 1 }
                        val scrimScope = LiveNavTransitionScope(
                            presentation = presentation,
                            entryIndex = entryIndex - 1,
                            isRemoving = belowEntry?.presentation?.isRemoving == true,
                            change = presentation.change,
                            layoutSize = layoutSize,
                            layoutDirection = layoutDirection,
                            density = density,
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    alpha = effects.dimAmount * ownTransition.scrimFraction(scrimScope).coerceIn(0f, 1f)
                                }.background(Color.Black),
                        )
                    }

                    NavEntryHost(
                        entry = entry,
                        entryIndex = entryIndex,
                        presentation = presentation,
                        stateHolder = stateHolder,
                        ownTransition = ownTransition,
                        upperTransition = upperTransition,
                        change = presentation.change,
                        effects = effects,
                        layoutSize = layoutSize,
                        layoutDirection = layoutDirection,
                        density = density,
                    )
                }
            }
        }
    }
}

/**
 * Bit layout of the packed per-host depth-bucket mask (see [NavEntryHost]): bits 0..2 carry the
 * [Lifecycle.State] ordinal of the lifecycle ceiling, the flag bits the three boolean gates. The
 * mask stays below 64 so the boxed derived value always hits the small-integer cache — the
 * per-frame derived recompute allocates nothing.
 */
private const val DEPTH_BUCKET_LIFECYCLE_MASK = 0b111
private const val DEPTH_BUCKET_GOVERNS_OWN = 1 shl 3
private const val DEPTH_BUCKET_CLIP_CORNERS = 1 shl 4
private const val DEPTH_BUCKET_BLOCK_INPUT = 1 shl 5

/**
 * Renders one presented entry as a pure function of [NavPresentation.animatedTop].
 *
 * Per-frame reads of the driving float happen **only** inside deferred `graphicsLayer { }` blocks
 * (here and inside the transition's own `transformEntry`) and inside one threshold-bucketed
 * [derivedStateOf]. The host's composition scope subscribes to the bucket mask, not the raw float,
 * so a frame's float write re-runs only that pure calculation and the host recomposes only when
 * the depth crosses a threshold (a handful of times per transition), never per frame. The host:
 * - derives the packed depth buckets: lifecycle ceiling, governing-transition side, corner-clip
 *   gate, input-block gate — every composition consumer of the depth is threshold-level;
 * - selects the governing transition (own when d <= 0, upper when 0 < d) per §4.3 and applies its
 *   `transformEntry` with a [LiveNavTransitionScope] (per-frame deferred read);
 * - layers the orthogonal [effects] via [NavDisplayEffects]'s per-depth helpers
 *   (`shouldClipCornersAt` / `shouldBlockInputAt`; the dim scrim is a fullscreen sibling rendered
 *   by the layout, not a per-host overlay);
 * - scopes the content with the shared [stateHolder]'s `EntryStateContent`, a per-entry lifecycle
 *   owner, and a per-entry view-model store owner (Phase 5, real signatures);
 * - keeps composition identity via the surrounding `key(contentKey)` + a cached `movableContentOf`.
 *
 * @param stateHolder the single saveable-state holder created once in [NavDisplayLayout] and shared
 *   across all hosts (stable identity, §0.4).
 */
@Composable
private fun NavEntryHost(
    entry: NavEntry<*>,
    entryIndex: Int,
    presentation: NavPresentation,
    stateHolder: NavSaveableStateHolder,
    ownTransition: NavTransition,
    upperTransition: NavTransition,
    change: NavChange,
    effects: NavDisplayEffects,
    layoutSize: IntSize,
    layoutDirection: LayoutDirection,
    density: Density,
) {
    // Cache a single movableContentOf per host so the entry's content keeps its state if the host is
    // moved within the call hierarchy across recompositions.
    val movableContent = remember {
        movableContentOf<@Composable () -> Unit> { inner -> inner() }
    }

    // Composition-time depth decisions, bucketed. All four composition consumers of the driving
    // float — the lifecycle ceiling, the governing-transition side, the corner-clip gate and the
    // input-block gate — change only when the depth crosses a threshold, never per frame. Packing
    // them into one derived Int keeps this host subscribed to the buckets instead of the raw float:
    // each frame's float write re-runs only this calculation (pure math, allocation-free) while the
    // host recomposes solely on an actual threshold crossing. Raw per-frame floats are read only
    // inside the deferred graphicsLayer blocks installed below.
    val depthBuckets by remember(presentation, entryIndex, entry, effects) {
        derivedStateOf {
            val d = presentation.animatedTop.value - entryIndex
            var mask = navMaxLifecycleFor(entry.presentation, d).ordinal
            if (d <= 0f) mask = mask or DEPTH_BUCKET_GOVERNS_OWN
            if (effects.shouldClipCornersAt(d)) mask = mask or DEPTH_BUCKET_CLIP_CORNERS
            if (effects.shouldBlockInputAt(d)) mask = mask or DEPTH_BUCKET_BLOCK_INPUT
            mask
        }
    }

    // Per-entry state scopes. The view-model store owner and lifecycle owner are remembered per host;
    // the saveable holder is the shared one passed from NavDisplayLayout.
    val vmOwner = rememberNavEntryViewModelStoreOwner()
    val maxLifecycle = Lifecycle.State.entries[depthBuckets and DEPTH_BUCKET_LIFECYCLE_MASK]
    val lifecycleOwner = rememberNavEntryLifecycleOwner(maxLifecycle)

    // Choose the governing transition at the bucketed depth: own transition while at or entering
    // the top (d <= 0), the upper neighbour's transition while covered (0 < d). The per-frame
    // visual is then a pure deferred read inside the transition's own graphicsLayer.
    val activeTransition = if ((depthBuckets and DEPTH_BUCKET_GOVERNS_OWN) != 0) ownTransition else upperTransition
    val entryModifier = with(activeTransition) {
        Modifier.transformEntry(
            LiveNavTransitionScope(
                presentation = presentation,
                entryIndex = entryIndex,
                isRemoving = entry.presentation.isRemoving,
                change = change,
                layoutSize = layoutSize,
                layoutDirection = layoutDirection,
                density = density,
            ),
        )
    }

    val clipModifier = if ((depthBuckets and DEPTH_BUCKET_CLIP_CORNERS) != 0) {
        // Squircle-clip the transitioning top entry. The radius comes from the effects, so callers
        // can follow the platform screen corner (e.g. rememberNavSystemCornerRadius); the corner
        // set follows the effects' clip mode. absoluteSquircleClip uses physical corners (never
        // flipped by layout direction), so the leading side is picked per layout direction.
        val r = effects.cornerClipRadius
        when (effects.cornerClipMode) {
            // Card-style transitions scale the whole page: every corner meets visible background.
            NavCornerClipMode.All -> Modifier.absoluteSquircleClip(topLeft = r, topRight = r, bottomRight = r, bottomLeft = r)

            // Slide-style transitions: only the corners that meet the screen edge as the entering
            // page slides in — left for LTR, right for RTL — matching the reference navigation.
            NavCornerClipMode.Leading ->
                if (layoutDirection == LayoutDirection.Ltr) {
                    Modifier.absoluteSquircleClip(topLeft = r, topRight = 0.dp, bottomRight = 0.dp, bottomLeft = r)
                } else {
                    Modifier.absoluteSquircleClip(topLeft = 0.dp, topRight = r, bottomRight = r, bottomLeft = 0.dp)
                }
        }
    } else {
        Modifier
    }

    val blockInputModifier = if ((depthBuckets and DEPTH_BUCKET_BLOCK_INPUT) != 0) {
        Modifier.pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    val event = awaitPointerEvent(pass = PointerEventPass.Initial)
                    event.changes.fastForEach { it.consume() }
                }
            }
        }
    } else {
        Modifier
    }

    Box(modifier = entryModifier.then(clipModifier).then(blockInputModifier)) {
        ProvideNavEntryViewModelStore(vmOwner) {
            ProvideNavEntryLifecycle(lifecycleOwner) {
                stateHolder.EntryStateContent(entry.contentKey) {
                    movableContent { entry.Content() }
                }
            }
        }
    }
}

/**
 * Host composable that renders and animates a navigation back stack with continuous-depth float
 * transitions.
 *
 * This is the primary overload: routes are registered via the [content] DSL ([NavEntryBuilder.entry]).
 * The [transition] is the global default; individual routes may override it via `entry(transition = …)`.
 *
 * @param backStack the live back stack to render (a [androidx.compose.runtime.snapshots.SnapshotStateList] of [NavKey]).
 * @param modifier modifier applied to the host container.
 * @param onBack callback for a system/predictive back; defaults to popping the last entry.
 * @param transition the global default [NavTransition]; per-route overrides win.
 * @param effects orthogonal visual effects (corner clip / dim / input blocking).
 * @param content the route-registration DSL block.
 */
@Composable
fun <T : NavKey> NavDisplay(
    backStack: NavBackStack<T>,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = { backStack.removeLastOrNull() },
    transition: NavTransition = NavTransitions.Cupertino,
    effects: NavDisplayEffects = NavDisplayEffects(),
    enablePredictiveBack: Boolean = true,
    content: NavEntryBuilder<T>.() -> Unit,
) {
    val provider = remember(content) { entryProvider(content) }
    NavDisplay(
        backStack = backStack,
        entryProvider = provider,
        modifier = modifier,
        onBack = onBack,
        transition = transition,
        effects = effects,
        enablePredictiveBack = enablePredictiveBack,
    )
}

/**
 * Convenience overload driving the back stack through a [NavController].
 *
 * @param navController the controller whose [NavController.backStack] is rendered.
 * @param modifier modifier applied to the host container.
 * @param onBack callback for a system/predictive back; defaults to [NavController.pop].
 * @param transition the global default [NavTransition]; per-route overrides win.
 * @param effects orthogonal visual effects.
 * @param content the route-registration DSL block.
 */
@Composable
@NonRestartableComposable
fun <T : NavKey> NavDisplay(
    navController: NavController<T>,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = { navController.pop() },
    transition: NavTransition = NavTransitions.Cupertino,
    effects: NavDisplayEffects = NavDisplayEffects(),
    enablePredictiveBack: Boolean = true,
    content: NavEntryBuilder<T>.() -> Unit,
) {
    NavDisplay(
        backStack = navController.backStack,
        modifier = modifier,
        onBack = onBack,
        transition = transition,
        effects = effects,
        enablePredictiveBack = enablePredictiveBack,
        content = content,
    )
}

/**
 * Low-level overload taking a prebuilt [entryProvider] instead of the DSL lambda.
 *
 * Declared `internal` because its signature exposes the `internal` [NavEntry] type; the public DSL
 * overloads above forward to it within the module.
 *
 * @param backStack the live back stack to render.
 * @param entryProvider lookup that builds a [NavEntry] for each [NavKey].
 * @param modifier modifier applied to the host container.
 * @param onBack callback for a system/predictive back; defaults to popping the last entry.
 * @param transition the global default [NavTransition]; per-route overrides win.
 * @param effects orthogonal visual effects.
 */
@Composable
internal fun <T : NavKey> NavDisplay(
    backStack: NavBackStack<T>,
    entryProvider: (T) -> NavEntry<*>,
    modifier: Modifier = Modifier,
    onBack: () -> Unit = { backStack.removeLastOrNull() },
    transition: NavTransition = NavTransitions.Cupertino,
    effects: NavDisplayEffects = NavDisplayEffects(),
    enablePredictiveBack: Boolean = true,
) {
    require(backStack.isNotEmpty()) { "NavDisplay back stack cannot be empty" }
    NavDisplayLayout(
        backStack = backStack,
        entryProvider = entryProvider,
        onBack = onBack,
        transition = transition,
        effects = effects,
        modifier = modifier,
        enablePredictiveBack = enablePredictiveBack,
    )
}
