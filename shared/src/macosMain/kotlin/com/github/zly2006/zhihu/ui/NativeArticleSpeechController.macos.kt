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

package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AppKit.NSSpeechSynthesizer
import platform.AppKit.NSSpeechSynthesizerDelegateProtocol
import platform.darwin.NSObject

@OptIn(ExperimentalForeignApi::class)
internal actual object NativeArticleSpeechController {
    private val speechDelegate = MacosSpeechDelegate()
    private val synthesizer = NSSpeechSynthesizer().apply {
        delegate = speechDelegate
    }
    private var stoppedByUser = false
    private var state by mutableStateOf(TtsState.Ready)

    actual val currentState: TtsState
        get() = state

    actual fun startSpeaking(text: String): Boolean {
        if (text.isBlank() || synthesizer.speaking) return false
        stoppedByUser = false
        state = TtsState.LoadingText
        return synthesizer.startSpeakingString(text).also { started ->
            state = if (started) TtsState.Speaking else TtsState.Error
        }
    }

    actual fun stopSpeaking() {
        stoppedByUser = true
        synthesizer.stopSpeaking()
        state = TtsState.Ready
    }

    fun didFinish(successfully: Boolean) {
        state = if (successfully || stoppedByUser) TtsState.Ready else TtsState.Error
        stoppedByUser = false
    }

    fun didFail() {
        state = TtsState.Error
        stoppedByUser = false
    }
}

@OptIn(ExperimentalForeignApi::class)
private class MacosSpeechDelegate :
    NSObject(),
    NSSpeechSynthesizerDelegateProtocol {
    override fun speechSynthesizer(
        sender: NSSpeechSynthesizer,
        didFinishSpeaking: Boolean,
    ) {
        NativeArticleSpeechController.didFinish(didFinishSpeaking)
    }

    override fun speechSynthesizer(
        sender: NSSpeechSynthesizer,
        didEncounterErrorAtIndex: ULong,
        ofString: String,
        message: String,
    ) {
        NativeArticleSpeechController.didFail()
    }
}
