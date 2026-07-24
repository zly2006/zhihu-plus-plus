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

@Composable
actual fun rememberBuildIdentity(): BuildIdentity =
    remember {
        val commitFromEnv = System.getenv("ZHPLUS_COMMIT_HASH")?.trim()?.takeIf { it.isNotEmpty() }
        val commitFromProp = System.getProperty("zhplus.commitHash")?.trim()?.takeIf { it.isNotEmpty() }
        val commit = commitFromEnv ?: commitFromProp ?: "unknown"

        val shortFromEnv = System.getenv("ZHPLUS_SHORT_HASH")?.trim()?.takeIf { it.isNotEmpty() }
        val short = when {
            shortFromEnv != null -> shortFromEnv
            commit != "unknown" && commit.length >= 8 -> commit.take(8)
            commit != "unknown" && commit.length >= 7 -> commit.take(7)
            else -> "unknown"
        }

        val buildTimeFromEnv = System.getenv("ZHPLUS_BUILD_TIME_UTC_MILLIS")?.toLongOrNull()
        val buildTimeFromProp = System.getProperty("zhplus.buildTimeUtcMillis")?.toLongOrNull()
        val buildTime = buildTimeFromEnv ?: buildTimeFromProp ?: 0L

        val versionFromEnv = System.getenv("ZHPLUS_VERSION_NAME")?.trim()?.takeIf { it.isNotEmpty() }
        val versionFromProp = System.getProperty("zhplus.versionName")?.trim()?.takeIf { it.isNotEmpty() }
        val version = versionFromEnv ?: versionFromProp ?: "desktop"

        BuildIdentity(
            commitHash = commit,
            shortHash = short,
            buildTimeUtcMillis = buildTime,
            versionName = version,
        )
    }
