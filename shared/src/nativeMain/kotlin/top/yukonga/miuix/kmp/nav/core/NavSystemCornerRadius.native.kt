// Copyright 2026, compose-miuix-ui contributors
// SPDX-License-Identifier: Apache-2.0
//
// Vendored from compose-miuix-ui/miuix feature/miuix-nav-v1 (skikoMain actual).
// iOS has no OS-level screen corner to match here, so no rounding is applied.

package top.yukonga.miuix.kmp.nav.core

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
actual fun rememberNavSystemCornerRadius(): Dp = 0.dp
