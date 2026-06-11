// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0

package top.yukonga.miuix.kmp.nav.core

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp

/**
 * The device screen's corner radius, intended for [NavDisplayEffects.cornerClipRadius] so a
 * full-window navigation entry is clipped to match the rounded screen corner as it slides in.
 *
 * Android returns the system rounded-corner radius (bottom-left position), or `0.dp` when the
 * platform reports none (flat-corner screens or API < 31). Skiko targets (Desktop / iOS / macOS /
 * Web) return `0.dp`, since there is no OS-level screen corner to match — yielding no rounding.
 */
@Composable
expect fun rememberNavSystemCornerRadius(): Dp
