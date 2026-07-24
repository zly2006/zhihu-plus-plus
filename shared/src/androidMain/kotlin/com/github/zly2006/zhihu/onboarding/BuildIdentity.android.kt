/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 */

package com.github.zly2006.zhihu.onboarding

import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

internal actual fun currentEpochMillis(): Long = System.currentTimeMillis()

@Composable
actual fun rememberBuildIdentity(): BuildIdentity {
    val context = LocalContext.current.applicationContext
    return remember(context) { context.readBuildIdentity() }
}

private fun Context.readBuildIdentity(): BuildIdentity {
    val versionName =
        runCatching {
            packageManager.getPackageInfo(packageName, 0).versionName
        }.getOrNull()
            .orEmpty()
            .ifBlank { "unknown" }

    val meta =
        runCatching {
            packageManager
                .getApplicationInfo(packageName, PackageManager.GET_META_DATA)
                .metaData
        }.getOrNull()

    val gitHash = meta.metaString("com.github.zly2006.zhihu.GIT_HASH")
    val commitHash =
        meta
            .metaString("com.github.zly2006.zhihu.COMMIT_HASH")
            .ifBlank { gitHash }
            .ifBlank { "unknown" }
    val shortHash =
        when {
            gitHash.isNotBlank() -> gitHash.take(8)
            commitHash != "unknown" && commitHash.length >= 8 -> commitHash.take(8)
            commitHash != "unknown" && commitHash.length >= 7 -> commitHash.take(7)
            else -> "unknown"
        }
    // Manifest contract: BUILD_TIME; accept BUILD_TIME_UTC_MILLIS as alias.
    val buildTime =
        meta.metaLong("com.github.zly2006.zhihu.BUILD_TIME").takeIf { it > 0L }
            ?: meta.metaLong("com.github.zly2006.zhihu.BUILD_TIME_UTC_MILLIS")

    return BuildIdentity(
        commitHash = commitHash,
        shortHash = shortHash,
        buildTimeUtcMillis = buildTime,
        versionName = versionName,
    )
}

/** Manifest meta-data is injected as string placeholders. */
private fun Bundle?.metaString(key: String): String =
    this?.getString(key)?.trim().orEmpty()

private fun Bundle?.metaLong(key: String): Long =
    this?.getString(key)?.toLongOrNull() ?: 0L
