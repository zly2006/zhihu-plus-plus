/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 */

package com.github.zly2006.zhihu.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/** iOS 尚未注入构建身份；identity 使用 unknown/0 占位。 */
internal actual fun currentEpochMillis(): Long = 0L

@Composable
actual fun rememberBuildIdentity(): BuildIdentity =
    remember {
        BuildIdentity(
            commitHash = "unknown",
            shortHash = "unknown",
            buildTimeUtcMillis = 0L,
            versionName = "ios",
        )
    }
