// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package top.yukonga.miuix.kmp.nav.internal

/**
 * Internal module marker for `:miuix-nav`.
 *
 * This file exists so the otherwise source-light scaffold produces a valid klib across all
 * targets before the public API (NavKey / NavDisplay / reconciler / transitions) lands in later
 * phases. It carries no public surface and is safe to keep as the module fills in.
 */
internal const val NAV_MODULE_NAME: String = "miuix-nav"
