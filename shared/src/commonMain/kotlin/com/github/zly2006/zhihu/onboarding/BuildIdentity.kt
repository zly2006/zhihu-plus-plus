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

/**
 * 当前 APK/产物的构建身份。
 *
 * 用 [commitHash] + [buildTimeUtcMillis] 检测「是否为用户尚未看过的新构建」。
 * [shortHash] 仅用于 UI 展示；匹配时优先完整 hash，再回落到 short 前缀。
 */
data class BuildIdentity(
    val commitHash: String,
    val shortHash: String,
    val buildTimeUtcMillis: Long,
    val versionName: String,
) {
    val displayLabel: String
        get() {
            val hash = shortHash.ifBlank { commitHash.take(8).ifBlank { "unknown" } }
            val time = if (buildTimeUtcMillis > 0L) {
                " · build $buildTimeUtcMillis"
            } else {
                ""
            }
            return "$versionName ($hash)$time"
        }

    fun matches(
        seenCommit: String,
        seenBuildTime: Long,
    ): Boolean {
        if (seenCommit.isBlank()) return false
        val commitEqual =
            commitHash.equals(seenCommit, ignoreCase = true) ||
                shortHash.equals(seenCommit, ignoreCase = true) ||
                commitHash.startsWith(seenCommit, ignoreCase = true) ||
                seenCommit.startsWith(shortHash, ignoreCase = true)
        if (!commitEqual) return false
        // 若任一侧缺少 build time，仅按 commit 判定。
        if (buildTimeUtcMillis <= 0L || seenBuildTime <= 0L) return true
        return buildTimeUtcMillis == seenBuildTime
    }
}

@Composable
expect fun rememberBuildIdentity(): BuildIdentity
