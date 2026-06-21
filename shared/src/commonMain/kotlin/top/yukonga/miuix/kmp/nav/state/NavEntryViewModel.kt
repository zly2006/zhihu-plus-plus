// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package top.yukonga.miuix.kmp.nav.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner

/**
 * Per-entry [ViewModelStoreOwner] for miuix-nav.
 *
 * Each navigation entry owns an isolated [ViewModelStore] so that ViewModels scoped via
 * `viewModel()` inside that entry are retained across recomposition but cleared exactly once when
 * the entry leaves the back stack.
 *
 * Disposal happens through either path, both idempotent because [ViewModelStore.clear] tolerates
 * repeated calls:
 * - [dispose]: called explicitly by the host when the entry fully exits (relativeDepth `d <= -1`).
 * - [onForgotten]: called by Compose if the owner is dropped from the composition without an
 *   explicit dispose (a safety net).
 */
@Stable
internal class NavEntryViewModelStoreOwner :
    ViewModelStoreOwner,
    RememberObserver {
    override val viewModelStore: ViewModelStore = ViewModelStore()

    /** Clears the backing [ViewModelStore]. Safe to call more than once. */
    fun dispose() {
        viewModelStore.clear()
    }

    override fun onRemembered() = Unit

    override fun onForgotten() {
        dispose()
    }

    override fun onAbandoned() {
        dispose()
    }
}

/**
 * Remembers a [NavEntryViewModelStoreOwner] for the current entry.
 *
 * The owner survives recomposition; when it is finally forgotten/abandoned by the composition its
 * [ViewModelStore] is cleared automatically. Hosts that drive an exit animation should still call
 * [NavEntryViewModelStoreOwner.dispose] when the entry reaches `d <= -1` to clear promptly.
 */
@Composable
internal fun rememberNavEntryViewModelStoreOwner(): NavEntryViewModelStoreOwner = remember { NavEntryViewModelStoreOwner() }

/**
 * Provides [owner] as the [LocalViewModelStoreOwner] for [content], scoping `viewModel()` lookups
 * inside an entry to that entry's [ViewModelStore].
 *
 * @param owner the per-entry owner from [rememberNavEntryViewModelStoreOwner].
 * @param content the entry body that resolves ViewModels.
 */
@Composable
internal fun ProvideNavEntryViewModelStore(
    owner: NavEntryViewModelStoreOwner,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalViewModelStoreOwner provides owner, content = content)
}
