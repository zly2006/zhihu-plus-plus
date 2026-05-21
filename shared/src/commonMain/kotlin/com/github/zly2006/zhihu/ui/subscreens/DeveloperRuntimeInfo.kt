/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 */

package com.github.zly2006.zhihu.ui.subscreens

import com.github.zly2006.zhihu.ui.TtsState

data class DeveloperRuntimeInfo(
    val continuousUsageDurationMs: Long = 0L,
    val ttsState: TtsState = TtsState.Uninitialized,
    val currentTtsEngineLabel: String = "未初始化",
    val availableTtsEngineLabels: List<String> = emptyList(),
)

interface DeveloperRuntimeInfoProvider {
    val developerRuntimeInfo: DeveloperRuntimeInfo
}
