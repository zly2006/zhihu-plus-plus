// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package top.yukonga.miuix.kmp.nav.runtime

/**
 * Classification of a back-stack change, computed by [navReconcile] and surfaced to transitions
 * via `NavTransitionScope.change`. Lets callers animate a multi-level pop differently from a
 * single pop.
 */
public sealed interface NavChange {
    /** The stack is unchanged. */
    public data object None : NavChange

    /** Exactly one entry was added (added == 1, removed == 0). */
    public data object Push : NavChange

    /** Exactly one entry was removed (removed == 1, added == 0). */
    public data object Pop : NavChange

    /** More than one entry was added at once (added > 1, removed == 0). [count] == added. */
    public data class MultiPush(val count: Int) : NavChange

    /** More than one entry was removed at once (removed > 1, added == 0). [count] == removed. */
    public data class MultiPop(val count: Int) : NavChange

    /** The top entry was replaced (common == new.size - 1 && removed == 1 && added == 1). */
    public data object Replace : NavChange

    /** The whole stack (or a non-top mixed add/remove) was replaced. */
    public data object ReplaceAll : NavChange
}
