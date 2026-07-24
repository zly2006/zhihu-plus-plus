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

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object AndroidReadingPlayerBridge {
    private val mutableState = MutableStateFlow(ReadingPlayerState())
    val state: StateFlow<ReadingPlayerState> = mutableState

    @Volatile
    private var pendingStartRequest: ReadingStartRequest? = null

    @Synchronized
    fun prepareStart(request: ReadingStartRequest) {
        pendingStartRequest = request
    }

    @Synchronized
    fun consumeStartRequest(): ReadingStartRequest? = pendingStartRequest.also {
        pendingStartRequest = null
    }

    fun publish(state: ReadingPlayerState) {
        mutableState.value = state
    }
}

private class AndroidReadingPlayerController(
    private val context: Context,
    override val state: State<ReadingPlayerState>,
) : ReadingPlayerController {
    override val isSupported: Boolean = true

    override fun start(request: ReadingStartRequest) {
        val normalizedPreferences = request.preferences.normalized()
        val normalizedQueue = request.queue
            .distinctBy(ReadingQueueItem::key)
            .take(normalizedPreferences.queueLimit)
        if (normalizedQueue.isEmpty()) return

        AndroidReadingPlayerBridge.prepareStart(
            request.copy(
                queue = normalizedQueue,
                preferences = normalizedPreferences,
                startIndex = request.startIndex.coerceIn(normalizedQueue.indices),
                playbackSpeed = normalizeReadingPlaybackSpeed(request.playbackSpeed),
            ),
        )
        ContextCompat.startForegroundService(
            context,
            ContentReadingService.commandIntent(context, ContentReadingService.ACTION_START),
        )
    }

    override fun togglePlayPause() = send(ContentReadingService.ACTION_TOGGLE)

    override fun playPrevious() = send(ContentReadingService.ACTION_PREVIOUS)

    override fun playNext() = send(ContentReadingService.ACTION_NEXT)

    override fun playAt(index: Int) {
        context.startService(
            ContentReadingService
                .commandIntent(context, ContentReadingService.ACTION_PLAY_AT)
                .putExtra(ContentReadingService.EXTRA_INDEX, index),
        )
    }

    override fun setPlaybackSpeed(speed: Float) {
        context.startService(
            ContentReadingService
                .commandIntent(context, ContentReadingService.ACTION_SET_PLAYBACK_SPEED)
                .putExtra(ContentReadingService.EXTRA_PLAYBACK_SPEED, normalizeReadingPlaybackSpeed(speed)),
        )
    }

    override fun stop() = send(ContentReadingService.ACTION_STOP)

    private fun send(action: String) {
        context.startService(ContentReadingService.commandIntent(context, action))
    }
}

@Composable
actual fun rememberReadingPlayerController(): ReadingPlayerController {
    val context = LocalContext.current
    val state = AndroidReadingPlayerBridge.state.collectAsState()
    return remember(context, state) {
        AndroidReadingPlayerController(context, state)
    }
}
