// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package top.yukonga.miuix.kmp.nav.runtime

/**
 * Pure classifier of a back-stack change over `contentKey` lists.
 *
 * Algorithm (design spec §8):
 * - `common` = [commonPrefixLength] of [old] and [new];
 * - `removed` = old.size - common, `added` = new.size - common;
 * - classify (the first matching arm wins, so pure add/remove are decided before the mixed arm):
 *   - identical lists -> [NavChange.None];
 *   - added > 0 && removed == 0 -> [NavChange.Push] (added == 1) else [NavChange.MultiPush]
 *     (regardless of `common`, so pushing onto an empty stack is still a [NavChange.Push]);
 *   - removed > 0 && added == 0 -> [NavChange.Pop] (removed == 1) else [NavChange.MultiPop];
 *   - added > 0 && removed > 0 -> [NavChange.Replace] only when it is a one-for-one top swap
 *     (common == new.size - 1 && removed == 1 && added == 1); this also covers swapping a lone
 *     root such as `["a"] -> ["b"]` (common == 0 == new.size - 1). Every other mixed add+remove
 *     (including a no-shared-prefix multi-entry replacement) -> [NavChange.ReplaceAll].
 *
 * The reconciler caller additionally marks removed entries as exiting and drives
 * `animatedTop` target = `new.lastIndex`; that side of the work lives in the rendering layer.
 */
internal fun navReconcile(old: List<Any>, new: List<Any>): NavChange {
    val common = commonPrefixLength(old, new)
    val removed = old.size - common
    val added = new.size - common
    return when {
        removed == 0 && added == 0 -> NavChange.None
        removed == 0 -> if (added == 1) NavChange.Push else NavChange.MultiPush(added)
        added == 0 -> if (removed == 1) NavChange.Pop else NavChange.MultiPop(removed)
        common == new.size - 1 && removed == 1 && added == 1 -> NavChange.Replace
        else -> NavChange.ReplaceAll
    }
}

/**
 * Length of the longest common prefix of [old] and [new] compared by element equality.
 *
 * Both lists are `contentKey` lists; equality (not identity) decides a match so that value-stable
 * keys recompose-survive correctly.
 */
internal fun commonPrefixLength(old: List<Any>, new: List<Any>): Int {
    val limit = minOf(old.size, new.size)
    var i = 0
    while (i < limit && old[i] == new[i]) i++
    return i
}
