// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package top.yukonga.miuix.kmp.nav.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer

/**
 * The navigation back stack: a [SnapshotStateList] of route keys [T] (a `@Serializable` route
 * hierarchy implementing [NavKey]).
 *
 * Generic over the route type so the stack can only ever hold keys of that hierarchy — pushing an
 * unrelated [NavKey] is a compile error, and the saver below encodes the exact element type with no
 * unchecked cast. Callers may operate the list directly (add/removeAt/...) or via a [NavController].
 * Persistence across configuration changes and process death is provided by [rememberNavBackStack].
 */
public typealias NavBackStack<T> = SnapshotStateList<T>

/**
 * Non-composable constructor for a [NavBackStack], primarily for tests and off-composition setup.
 */
public fun <T : NavKey> navBackStackOf(vararg elements: T): NavBackStack<T> = elements.toList().toMutableStateList()

/** Json used to (de)serialize the back stack. Keys must be `@Serializable`. */
@PublishedApi
internal val NavBackStackJson: Json = Json { ignoreUnknownKeys = true }

/**
 * Builds a reflection-free [Saver] for a back stack whose keys are all of type [T].
 *
 * [elementsSerializer] is the compiler-generated serializer for `List<T>`, obtained at the call
 * site via the reified `serializer<List<T>>()`. For a `@Serializable sealed` route hierarchy this
 * is a closed-polymorphic serializer that needs no `SerializersModule` and works on every target
 * (no JVM reflection, no `InternalSerializationApi`).
 *
 * Non-`@Serializable` keys surface a `SerializationException` rather than silently dropping data
 * (design spec §12 known risk).
 */
@PublishedApi
internal fun <T : NavKey> navBackStackSaver(elementsSerializer: KSerializer<List<T>>): Saver<NavBackStack<T>, String> = Saver(
    save = { stack -> NavBackStackJson.encodeToString(elementsSerializer, stack.toList()) },
    restore = { encoded -> NavBackStackJson.decodeFromString(elementsSerializer, encoded).toMutableStateList() },
)

/**
 * Remembers a [NavBackStack] seeded with [elements], persisted via [rememberSaveable].
 *
 * The key type [T] is captured reflection-free so persistence works on every target. When seeding
 * with a single concrete key, pass the route supertype explicitly so the whole hierarchy can be
 * encoded, e.g. `rememberNavBackStack<Route>(Route.Home)`.
 *
 * ```kotlin
 * val backStack = rememberNavBackStack<Route>(Route.Home)
 * ```
 */
@Composable
public inline fun <reified T : NavKey> rememberNavBackStack(vararg elements: T): NavBackStack<T> {
    // T is fixed by the reified call site, so the serializer never changes -> keyless remember.
    val saver = remember { navBackStackSaver(serializer<List<T>>()) }
    return rememberSaveable(saver = saver) { elements.toList().toMutableStateList() }
}
