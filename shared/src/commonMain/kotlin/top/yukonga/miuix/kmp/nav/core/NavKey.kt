// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package top.yukonga.miuix.kmp.nav.core

/**
 * Marker interface for navigation destination keys.
 *
 * A [NavKey] is a pure tag carrying no behavior. User route hierarchies implement it and should
 * annotate the hierarchy with `@Serializable` (kotlinx.serialization) so the back stack can be
 * persisted across configuration changes and process death via [rememberNavBackStack].
 *
 * ```kotlin
 * @Serializable
 * sealed interface Route : NavKey {
 *     @Serializable data object Home : Route
 *     @Serializable data class Detail(val id: String) : Route
 * }
 * ```
 *
 * Keys that are not `@Serializable` degrade to in-memory-only and will not survive process death.
 */
public interface NavKey
