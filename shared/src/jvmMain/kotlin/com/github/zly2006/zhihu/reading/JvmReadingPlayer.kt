/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.github.zly2006.zhihu.reading

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember

@Composable
actual fun rememberReadingPlayerController(): ReadingPlayerController = remember {
    object : ReadingPlayerController {
        override val state: State<ReadingPlayerState> = mutableStateOf(ReadingPlayerState())
        override val isSupported: Boolean = false

        override fun start(request: ReadingStartRequest) = Unit

        override fun togglePlayPause() = Unit

        override fun playPrevious() = Unit

        override fun playNext() = Unit

        override fun playAt(index: Int) = Unit

        override fun setPlaybackSpeed(speed: Float) = Unit

        override fun stop() = Unit
    }
}
