/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 */

package com.github.zly2006.zhihu.reading

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.media.app.NotificationCompat.MediaStyle
import com.fleeksoft.ksoup.Ksoup
import com.github.zly2006.zhihu.shared.comment.decodeZhihuCommentData
import com.github.zly2006.zhihu.shared.util.Log
import com.github.zly2006.zhihu.viewmodel.SharedAndroidPaginationEnvironment
import com.github.zly2006.zhihu.viewmodel.getOrFetchContentDetail
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.Locale

class ContentReadingService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val environment by lazy {
        SharedAndroidPaginationEnvironment(applicationContext, allowGuestAccess = true)
    }
    private val audioManager by lazy { getSystemService(AudioManager::class.java) }
    private val wakeLock by lazy {
        getSystemService(PowerManager::class.java)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "$packageName:content-reading")
            .apply { setReferenceCounted(false) }
    }
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var audioFocusRequest: AudioFocusRequest

    private var textToSpeech: TextToSpeech? = null
    private var ttsReady = false
    private var ttsInitializing = false
    private var ttsError: String? = null
    private var engineLabel = ""
    private var availableEngineLabels = emptyList<String>()
    private var selectedEngineName: String? = null
    private var queue = emptyList<ReadingQueueItem>()
    private var queueSourceId: String? = null
    private var preferences = ReadingPreferences()
    private var playbackSpeed = DEFAULT_READING_PLAYBACK_SPEED
    private var currentIndex = -1
    private var currentChunks = emptyList<String>()
    private var currentChunkIndex = 0
    private var currentLoadIncludesTransition = false
    private var playbackStatus = ReadingPlaybackStatus.Idle
    private var errorMessage: String? = null
    private var loadingJob: Job? = null
    private var playbackGeneration = 0L
    private var ttsInitializationGeneration = 0L
    private var utteranceSequence = 0L
    private var activeUtteranceId: String? = null
    private var playWhenReady = false
    private var resumeOnFocusGain = false
    private var engineRecoveryAttempts = 0
    private var synthesisRecoveryAttempts = 0
    private var isDestroyed = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        createMediaSession()
        createAudioFocusRequest()
        initializeTextToSpeech()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        when (intent?.action) {
            ACTION_START -> {
                val request = AndroidReadingPlayerBridge.consumeStartRequest()
                if (request != null) {
                    startSession(request)
                } else if (queue.isEmpty()) {
                    stopSelfResult(startId)
                }
            }
            ACTION_TOGGLE -> togglePlayback()
            ACTION_PLAY -> resumePlayback()
            ACTION_PAUSE -> pausePlayback(abandonAudioFocus = true)
            ACTION_PREVIOUS -> playPrevious()
            ACTION_NEXT -> playNext()
            ACTION_PLAY_AT -> playAt(intent.getIntExtra(EXTRA_INDEX, -1))
            ACTION_SET_PLAYBACK_SPEED -> setPlaybackSpeed(
                intent.getFloatExtra(EXTRA_PLAYBACK_SPEED, DEFAULT_READING_PLAYBACK_SPEED),
            )
            ACTION_STOP -> stopSession()
        }
        if (intent?.action != ACTION_START && queue.isEmpty()) {
            stopSelfResult(startId)
        }
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        isDestroyed = true
        playbackGeneration++
        ttsInitializationGeneration++
        playWhenReady = false
        resumeOnFocusGain = false
        loadingJob?.cancel()
        activeUtteranceId = null
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        abandonAudioFocus()
        releaseWakeLock()
        mediaSession.release()
        serviceScope.cancel()
        if (AndroidReadingPlayerBridge.state.value.hasSession) {
            AndroidReadingPlayerBridge.publish(ReadingPlayerState())
        }
        super.onDestroy()
    }

    private fun startSession(request: ReadingStartRequest) {
        playbackGeneration++
        playWhenReady = true
        resumeOnFocusGain = false
        engineRecoveryAttempts = 0
        synthesisRecoveryAttempts = 0
        loadingJob?.cancel()
        activeUtteranceId = null
        textToSpeech?.stop()
        preferences = request.preferences.normalized()
        playbackSpeed = normalizeReadingPlaybackSpeed(request.playbackSpeed)
        if (ttsReady) {
            textToSpeech?.setSpeechRate(effectiveSpeechRate(selectedEngineName))
        }
        queueSourceId = request.sourceId
        queue = request.queue
            .distinctBy(ReadingQueueItem::key)
            .take(preferences.queueLimit)
        if (queue.isEmpty()) {
            stopSession()
            return
        }
        currentIndex = request.startIndex.coerceIn(queue.indices)
        currentChunks = emptyList()
        currentChunkIndex = 0
        currentLoadIncludesTransition = false
        errorMessage = null
        playbackStatus = if (ttsReady) ReadingPlaybackStatus.Loading else ReadingPlaybackStatus.Initializing
        mediaSession.isActive = true
        publishState()
        if (ttsReady) {
            loadCurrentItem(includeTransition = false)
        } else if (ttsError != null || !ttsInitializing) {
            restartTextToSpeech()
        }
    }

    private fun togglePlayback() {
        when (playbackStatus) {
            ReadingPlaybackStatus.Playing,
            ReadingPlaybackStatus.Loading,
            ReadingPlaybackStatus.Initializing,
            -> pausePlayback(abandonAudioFocus = true)
            ReadingPlaybackStatus.Paused,
            ReadingPlaybackStatus.Error,
            -> resumePlayback()
            else -> Unit
        }
    }

    private fun resumePlayback() {
        if (queue.isEmpty() || currentIndex !in queue.indices) return
        if (playWhenReady && playbackStatus in PLAYING_STATUSES) return
        if (ttsError != null) {
            engineRecoveryAttempts = 0
            synthesisRecoveryAttempts = 0
        }
        playWhenReady = true
        resumeOnFocusGain = false
        if (!ttsReady) {
            playbackStatus = ReadingPlaybackStatus.Initializing
            errorMessage = null
            publishState()
            if (ttsError != null || !ttsInitializing) restartTextToSpeech()
            return
        }
        acquireWakeLock()
        errorMessage = null
        if (currentChunks.isEmpty()) {
            loadCurrentItem(includeTransition = currentLoadIncludesTransition)
            return
        }
        if (!requestAudioFocus()) {
            playWhenReady = false
            playbackStatus = ReadingPlaybackStatus.Paused
            errorMessage = "无法获取音频焦点"
            releaseWakeLock()
            publishState()
            return
        }
        speakCurrentChunk()
    }

    private fun pausePlayback(
        abandonAudioFocus: Boolean,
        resumeWhenFocusGained: Boolean = false,
    ) {
        if (queue.isEmpty()) return
        playbackGeneration++
        playWhenReady = false
        resumeOnFocusGain = resumeWhenFocusGained
        loadingJob?.cancel()
        activeUtteranceId = null
        textToSpeech?.stop()
        playbackStatus = ReadingPlaybackStatus.Paused
        if (abandonAudioFocus) abandonAudioFocus()
        releaseWakeLock()
        publishState()
    }

    private fun playPrevious() {
        if (currentIndex <= 0) return
        playAt(currentIndex - 1)
    }

    private fun playNext() {
        if (currentIndex !in 0 until queue.lastIndex) return
        playAt(currentIndex + 1)
    }

    private fun playAt(index: Int) {
        if (index !in queue.indices) return
        playbackGeneration++
        loadingJob?.cancel()
        activeUtteranceId = null
        textToSpeech?.stop()
        currentIndex = index
        currentChunks = emptyList()
        currentChunkIndex = 0
        currentLoadIncludesTransition = false
        playWhenReady = true
        resumeOnFocusGain = false
        engineRecoveryAttempts = 0
        synthesisRecoveryAttempts = 0
        if (ttsReady) {
            loadCurrentItem(includeTransition = false)
        } else {
            playbackStatus = ReadingPlaybackStatus.Initializing
            errorMessage = null
            publishState()
            if (ttsError != null || !ttsInitializing) restartTextToSpeech()
        }
    }

    private fun setPlaybackSpeed(speed: Float) {
        val normalizedSpeed = normalizeReadingPlaybackSpeed(speed)
        if (normalizedSpeed == playbackSpeed) return

        playbackSpeed = normalizedSpeed
        val engine = textToSpeech
        if (ttsReady && engine != null) {
            engine.setSpeechRate(effectiveSpeechRate(selectedEngineName))
        }
        if (
            playbackStatus == ReadingPlaybackStatus.Playing &&
            playWhenReady &&
            currentChunkIndex in currentChunks.indices
        ) {
            activeUtteranceId = null
            engine?.stop()
            speakCurrentChunk()
        } else {
            publishState()
        }
    }

    private fun loadCurrentItem(includeTransition: Boolean) {
        currentLoadIncludesTransition = includeTransition
        if (!playWhenReady) {
            playbackStatus = ReadingPlaybackStatus.Paused
            releaseWakeLock()
            publishState()
            return
        }
        if (currentIndex !in queue.indices) {
            stopSession()
            return
        }

        playbackGeneration++
        acquireWakeLock()
        val generation = playbackGeneration
        loadingJob?.cancel()
        activeUtteranceId = null
        textToSpeech?.stop()
        currentChunks = emptyList()
        currentChunkIndex = 0
        playbackStatus = ReadingPlaybackStatus.Loading
        errorMessage = null
        publishState()

        loadingJob = serviceScope.launch {
            try {
                val resolved = withContext(Dispatchers.Default) {
                    resolveReadingContent(queue[currentIndex])
                }
                if (generation != playbackGeneration) return@launch

                val resolvedItem = resolved.first
                val content = resolved.second
                queue = queue.toMutableList().also { it[currentIndex] = resolvedItem.withoutBody() }
                val speechText = buildReadingSpeechText(content, preferences)
                if (speechText.isBlank()) error("没有可朗读的字段")
                val transition = if (includeTransition && currentIndex > 0) {
                    renderReadingTransition(
                        template = preferences.transitionText,
                        nextItem = resolvedItem,
                        nextIndex = currentIndex,
                        total = queue.size,
                    )
                } else {
                    ""
                }
                currentChunks = splitReadingSpeechIntoChunks(
                    listOf(transition, speechText).filter(String::isNotBlank).joinToString("\n"),
                )
                currentLoadIncludesTransition = false

                if (!requestAudioFocus()) {
                    playWhenReady = false
                    playbackStatus = ReadingPlaybackStatus.Paused
                    errorMessage = "无法获取音频焦点"
                    releaseWakeLock()
                    publishState()
                    return@launch
                }
                speakCurrentChunk()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (!isDestroyed && generation == playbackGeneration && playWhenReady) {
                    skipFailedItem(error)
                }
            }
        }
    }

    private suspend fun resolveReadingContent(
        item: ReadingQueueItem,
    ): Pair<ReadingQueueItem, ResolvedReadingContent> {
        val resolvedItem = if (item.bodyHtml != null) {
            item
        } else {
            val content = environment.getOrFetchContentDetail(item.toDestination())
                ?: error("内容加载失败")
            content.toReadingQueueItem(item.toDestination()) ?: error("不支持的内容类型")
        }
        val comments = if (preferences.shouldLoadComments) {
            fetchComments(resolvedItem)
        } else {
            emptyList()
        }
        return resolvedItem to ResolvedReadingContent(
            contentType = resolvedItem.contentType,
            title = resolvedItem.title,
            author = resolvedItem.author,
            body = Ksoup.parse(resolvedItem.bodyHtml.orEmpty()).text(),
            publishedAt = resolvedItem.publishedAt,
            voteUpCount = resolvedItem.voteUpCount,
            comments = comments,
        )
    }

    private suspend fun fetchComments(item: ReadingQueueItem): List<ReadingComment> {
        val baseUrl = when (item.contentType) {
            ReadingContentType.Answer -> "https://www.zhihu.com/api/v4/comment_v5/answers/${item.id}/root_comment"
            ReadingContentType.Article -> "https://www.zhihu.com/api/v4/comment_v5/articles/${item.id}/root_comment"
            ReadingContentType.Pin -> "https://www.zhihu.com/api/v4/comment_v5/pins/${item.id}/root_comment"
            ReadingContentType.Question -> "https://www.zhihu.com/api/v4/comment_v5/questions/${item.id}/root_comment"
        }
        val comments = mutableListOf<ReadingComment>()
        val blockedUserIds = environment.blockedUserIds()
        var nextUrl: String? = "$baseUrl?order_by=${preferences.commentOrder.apiValue}&limit=${minOf(20, preferences.commentCount)}"
        val visitedUrls = mutableSetOf<String>()

        while (nextUrl != null && comments.size < preferences.commentCount && visitedUrls.add(nextUrl)) {
            val remaining = preferences.commentCount - comments.size
            val response = environment.fetchJson(
                url = nextUrl,
                include = "data[*].content,author",
            ) ?: break
            comments += decodeZhihuCommentData(response, Int.MAX_VALUE)
                .filterNot { comment -> comment.author.id in blockedUserIds }
                .map { comment ->
                    ReadingComment(
                        author = comment.author.name,
                        body = Ksoup.parse(comment.content).text(),
                    )
                }.take(remaining)
            val paging = response["paging"]?.jsonObject
            val isEnd = paging?.get("is_end")?.jsonPrimitive?.booleanOrNull == true
            nextUrl = if (isEnd) {
                null
            } else {
                paging?.get("next")?.jsonPrimitive?.contentOrNull
            }
        }
        return comments.take(preferences.commentCount)
    }

    private fun speakCurrentChunk() {
        if (!playWhenReady || isDestroyed) return
        if (currentChunkIndex !in currentChunks.indices || currentIndex !in queue.indices) {
            onCurrentItemFinished()
            return
        }
        val utteranceId = "reading_${playbackGeneration}_${currentIndex}_${currentChunkIndex}_${++utteranceSequence}"
        activeUtteranceId = utteranceId
        playbackStatus = ReadingPlaybackStatus.Playing
        errorMessage = null
        publishState()
        val result = textToSpeech?.speak(
            currentChunks[currentChunkIndex],
            TextToSpeech.QUEUE_FLUSH,
            Bundle().apply {
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1f)
            },
            utteranceId,
        ) ?: TextToSpeech.ERROR
        if (result == TextToSpeech.ERROR) {
            recoverTextToSpeech(TextToSpeech.ERROR_SERVICE)
        }
    }

    private fun onCurrentItemFinished() {
        if (!playWhenReady || isDestroyed) return
        if (currentIndex < queue.lastIndex) {
            currentIndex++
            engineRecoveryAttempts = 0
            synthesisRecoveryAttempts = 0
            loadCurrentItem(includeTransition = true)
        } else {
            stopSession()
        }
    }

    private fun skipFailedItem(error: Exception) {
        if (isDestroyed || !playWhenReady) return
        Log.e(TAG, "Failed to read queue item ${queue.getOrNull(currentIndex)}", error)
        Toast
            .makeText(
                applicationContext,
                "已跳过无法朗读的内容：${error.message ?: "未知错误"}",
                Toast.LENGTH_SHORT,
            ).show()
        if (currentIndex < queue.lastIndex) {
            currentIndex++
            engineRecoveryAttempts = 0
            synthesisRecoveryAttempts = 0
            errorMessage = error.message
            loadCurrentItem(includeTransition = false)
        } else {
            stopSession()
        }
    }

    private fun stopSession() {
        if (isDestroyed) return
        playbackGeneration++
        playWhenReady = false
        resumeOnFocusGain = false
        engineRecoveryAttempts = 0
        synthesisRecoveryAttempts = 0
        loadingJob?.cancel()
        activeUtteranceId = null
        textToSpeech?.stop()
        abandonAudioFocus()
        releaseWakeLock()
        queue = emptyList()
        queueSourceId = null
        currentIndex = -1
        currentChunks = emptyList()
        currentChunkIndex = 0
        currentLoadIncludesTransition = false
        playbackStatus = ReadingPlaybackStatus.Idle
        errorMessage = null
        mediaSession.isActive = false
        AndroidReadingPlayerBridge.publish(
            ReadingPlayerState(
                engineLabel = engineLabel,
                availableEngineLabels = availableEngineLabels,
                playbackSpeed = playbackSpeed,
            ),
        )
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun initializeTextToSpeech(
        engineName: String? = null,
        allowPreferredSelection: Boolean = engineName == null,
    ) {
        if (isDestroyed) return
        ttsReady = false
        ttsInitializing = true
        ttsError = null
        val initializationGeneration = ++ttsInitializationGeneration
        val listener = TextToSpeech.OnInitListener { status ->
            serviceScope.launch {
                if (isDestroyed || initializationGeneration != ttsInitializationGeneration) return@launch
                val engine = textToSpeech ?: return@launch
                if (status != TextToSpeech.SUCCESS) {
                    if (engineName != null) {
                        Log.w(TAG, "Preferred TTS engine unavailable: $engineName")
                        selectedEngineName = null
                        restartTextToSpeech(engineName = null, allowPreferredSelection = false)
                        return@launch
                    }
                    handleTextToSpeechInitializationError("系统 TTS 初始化失败")
                    return@launch
                }

                availableEngineLabels = engine.engines.map { it.name }
                if (engineName == null && allowPreferredSelection) {
                    val preferredEngine = when {
                        PICO_TTS_ENGINE in availableEngineLabels -> PICO_TTS_ENGINE
                        SHERPA_TTS_ENGINE in availableEngineLabels -> SHERPA_TTS_ENGINE
                        else -> null
                    }
                    if (preferredEngine != null) {
                        selectedEngineName = preferredEngine
                        restartTextToSpeech(engineName = preferredEngine)
                        return@launch
                    }
                }

                val languageSupported = runCatching { configureTextToSpeech(engine, engineName) }
                    .getOrElse { error ->
                        Log.e(TAG, "Failed to configure TTS", error)
                        false
                    }
                if (!languageSupported) {
                    if (engineName != null) {
                        Log.w(TAG, "Preferred TTS engine does not support speech language: $engineName")
                        selectedEngineName = null
                        restartTextToSpeech(engineName = null, allowPreferredSelection = false)
                        return@launch
                    }
                    handleTextToSpeechInitializationError("系统 TTS 不支持当前语言")
                    return@launch
                }
                engine.setOnUtteranceProgressListener(createUtteranceProgressListener(initializationGeneration))
                selectedEngineName = engineName
                engineLabel = engineName ?: engine.defaultEngine.orEmpty()
                ttsReady = true
                ttsInitializing = false
                ttsError = null
                errorMessage = null
                when {
                    queue.isEmpty() -> {
                        playbackStatus = ReadingPlaybackStatus.Idle
                        publishState()
                    }
                    playWhenReady -> continuePlaybackAfterTextToSpeechReady()
                    else -> {
                        playbackStatus = ReadingPlaybackStatus.Paused
                        publishState()
                    }
                }
            }
        }
        textToSpeech = if (engineName == null) {
            TextToSpeech(this, listener)
        } else {
            TextToSpeech(this, listener, engineName)
        }
    }

    private fun configureTextToSpeech(
        engine: TextToSpeech,
        engineName: String?,
    ): Boolean {
        val languageResult = engine.setLanguage(Locale.CHINESE)
        if (languageResult == TextToSpeech.LANG_MISSING_DATA || languageResult == TextToSpeech.LANG_NOT_SUPPORTED) {
            val fallbackResult = engine.setLanguage(Locale.getDefault())
            if (fallbackResult == TextToSpeech.LANG_MISSING_DATA || fallbackResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                return false
            }
        }
        engine.setSpeechRate(effectiveSpeechRate(engineName))
        engine.setPitch(1f)
        engine.setAudioAttributes(
            AudioAttributes
                .Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build(),
        )
        return true
    }

    private fun effectiveSpeechRate(engineName: String?): Float =
        (if (engineName == SHERPA_TTS_ENGINE) SHERPA_BASE_SPEECH_RATE else DEFAULT_BASE_SPEECH_RATE) * playbackSpeed

    private fun continuePlaybackAfterTextToSpeechReady() {
        if (currentChunks.isEmpty()) {
            loadCurrentItem(includeTransition = currentLoadIncludesTransition)
            return
        }
        acquireWakeLock()
        if (!requestAudioFocus()) {
            playWhenReady = false
            playbackStatus = ReadingPlaybackStatus.Paused
            errorMessage = "无法获取音频焦点"
            releaseWakeLock()
            publishState()
            return
        }
        speakCurrentChunk()
    }

    private fun createUtteranceProgressListener(
        initializationGeneration: Long,
    ): UtteranceProgressListener = object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {
            serviceScope.launch {
                if (!isCurrentUtterance(initializationGeneration, utteranceId)) return@launch
                playbackStatus = ReadingPlaybackStatus.Playing
                publishState()
            }
        }

        override fun onDone(utteranceId: String?) {
            serviceScope.launch {
                if (!isCurrentUtterance(initializationGeneration, utteranceId)) return@launch
                activeUtteranceId = null
                engineRecoveryAttempts = 0
                synthesisRecoveryAttempts = 0
                currentChunkIndex++
                if (currentChunkIndex < currentChunks.size) {
                    speakCurrentChunk()
                } else {
                    onCurrentItemFinished()
                }
            }
        }

        @Suppress("OVERRIDE_DEPRECATION")
        override fun onError(utteranceId: String?) {
            onError(utteranceId, TextToSpeech.ERROR)
        }

        override fun onError(utteranceId: String?, errorCode: Int) {
            serviceScope.launch {
                if (!isCurrentUtterance(initializationGeneration, utteranceId)) return@launch
                activeUtteranceId = null
                when (errorCode) {
                    TextToSpeech.ERROR_INVALID_REQUEST ->
                        skipFailedItem(IllegalStateException("TTS 无法合成当前内容（$errorCode）"))

                    TextToSpeech.ERROR_SYNTHESIS -> recoverSynthesisFailure(errorCode)

                    else -> recoverTextToSpeech(errorCode)
                }
            }
        }
    }

    private fun isCurrentUtterance(
        initializationGeneration: Long,
        utteranceId: String?,
    ): Boolean = !isDestroyed &&
        playWhenReady &&
        initializationGeneration == ttsInitializationGeneration &&
        utteranceId != null &&
        utteranceId == activeUtteranceId

    private fun handleTextToSpeechInitializationError(message: String) {
        ttsReady = false
        ttsInitializing = false
        ttsError = message
        playWhenReady = false
        playbackStatus = if (queue.isEmpty()) ReadingPlaybackStatus.Idle else ReadingPlaybackStatus.Error
        errorMessage = message.takeIf { queue.isNotEmpty() }
        abandonAudioFocus()
        releaseWakeLock()
        publishState()
    }

    private fun restartTextToSpeech(
        engineName: String? = selectedEngineName,
        allowPreferredSelection: Boolean = engineName == null,
    ) {
        if (isDestroyed) return
        activeUtteranceId = null
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        textToSpeech = null
        ttsReady = false
        ttsInitializing = false
        ttsError = null
        initializeTextToSpeech(engineName, allowPreferredSelection)
    }

    private fun recoverTextToSpeech(errorCode: Int) {
        if (isDestroyed || !playWhenReady) return
        if (engineRecoveryAttempts >= MAX_TTS_ENGINE_RECOVERY_ATTEMPTS) {
            val message = "TTS 引擎连续失败（$errorCode）"
            Log.e(TAG, message)
            ttsReady = false
            ttsInitializing = false
            ttsError = message
            playWhenReady = false
            playbackStatus = ReadingPlaybackStatus.Error
            errorMessage = message
            abandonAudioFocus()
            releaseWakeLock()
            publishState()
            return
        }

        engineRecoveryAttempts++
        restartTextToSpeechForRecovery()
    }

    private fun recoverSynthesisFailure(errorCode: Int) {
        if (isDestroyed || !playWhenReady) return
        if (synthesisRecoveryAttempts >= MAX_TTS_ENGINE_RECOVERY_ATTEMPTS) {
            skipFailedItem(IllegalStateException("TTS 无法合成当前内容（$errorCode）"))
            return
        }

        synthesisRecoveryAttempts++
        restartTextToSpeechForRecovery()
    }

    private fun restartTextToSpeechForRecovery() {
        playbackGeneration++
        loadingJob?.cancel()
        activeUtteranceId = null
        playbackStatus = ReadingPlaybackStatus.Initializing
        errorMessage = null
        abandonAudioFocus()
        releaseWakeLock()
        publishState()
        restartTextToSpeech()
    }

    private fun createMediaSession() {
        mediaSession = MediaSessionCompat(this, TAG).apply {
            setFlags(
                MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or
                    MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS,
            )
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() = resumePlayback()

                override fun onPause() = pausePlayback(abandonAudioFocus = true)

                override fun onSkipToPrevious() = playPrevious()

                override fun onSkipToNext() = playNext()

                override fun onStop() = stopSession()
            })
        }
    }

    private fun createAudioFocusRequest() {
        val attributes = AudioAttributes
            .Builder()
            .setUsage(AudioAttributes.USAGE_MEDIA)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()
        audioFocusRequest = AudioFocusRequest
            .Builder(AudioManager.AUDIOFOCUS_GAIN)
            .setAudioAttributes(attributes)
            .setOnAudioFocusChangeListener { focusChange ->
                serviceScope.launch {
                    when (focusChange) {
                        AudioManager.AUDIOFOCUS_GAIN -> {
                            val shouldResume = resumeOnFocusGain
                            resumeOnFocusGain = false
                            if (shouldResume) {
                                resumePlayback()
                            }
                        }
                        AudioManager.AUDIOFOCUS_LOSS -> {
                            resumeOnFocusGain = false
                            pausePlayback(abandonAudioFocus = false)
                        }
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
                        AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK,
                        -> {
                            val shouldResume = playWhenReady && playbackStatus in PLAYING_STATUSES
                            pausePlayback(
                                abandonAudioFocus = false,
                                resumeWhenFocusGained = shouldResume,
                            )
                        }
                    }
                }
            }.build()
    }

    private fun requestAudioFocus(): Boolean = audioManager.requestAudioFocus(audioFocusRequest) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED

    private fun abandonAudioFocus() {
        runCatching { audioManager.abandonAudioFocusRequest(audioFocusRequest) }
    }

    private fun acquireWakeLock() {
        if (!wakeLock.isHeld) wakeLock.acquire()
    }

    private fun releaseWakeLock() {
        if (wakeLock.isHeld) wakeLock.release()
    }

    private fun publishState() {
        if (isDestroyed) return
        val state = ReadingPlayerState(
            status = playbackStatus,
            queue = queue.map(ReadingQueueItem::withoutBody),
            currentIndex = currentIndex,
            currentChunkIndex = currentChunkIndex,
            totalChunks = currentChunks.size,
            errorMessage = errorMessage,
            engineLabel = engineLabel,
            availableEngineLabels = availableEngineLabels,
            sourceId = queueSourceId,
            playbackSpeed = playbackSpeed,
        )
        AndroidReadingPlayerBridge.publish(state)
        updateMediaSession(state)
        if (state.hasSession) {
            ServiceCompat.startForeground(
                this,
                NOTIFICATION_ID,
                buildNotification(state),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
            )
        }
    }

    private fun updateMediaSession(state: ReadingPlayerState) {
        val item = state.currentItem
        if (item != null) {
            mediaSession.setMetadata(
                MediaMetadataCompat
                    .Builder()
                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, item.displayTitle)
                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, item.author)
                    .putString(MediaMetadataCompat.METADATA_KEY_ALBUM, item.contentType.displayName)
                    .putString(
                        MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION,
                        "${item.contentType.displayName} · ${state.currentIndex + 1} / ${state.queue.size}",
                    ).build(),
            )
            mediaSession.setSessionActivity(contentPendingIntent(item))
        }

        val actions = PlaybackStateCompat.ACTION_PLAY or
            PlaybackStateCompat.ACTION_PAUSE or
            PlaybackStateCompat.ACTION_PLAY_PAUSE or
            PlaybackStateCompat.ACTION_STOP or
            (if (state.canPlayPrevious) PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS else 0L) or
            (if (state.canPlayNext) PlaybackStateCompat.ACTION_SKIP_TO_NEXT else 0L)
        val playbackState = when (state.status) {
            ReadingPlaybackStatus.Playing -> PlaybackStateCompat.STATE_PLAYING
            ReadingPlaybackStatus.Loading,
            ReadingPlaybackStatus.Initializing,
            -> PlaybackStateCompat.STATE_BUFFERING
            ReadingPlaybackStatus.Paused -> PlaybackStateCompat.STATE_PAUSED
            ReadingPlaybackStatus.Error -> PlaybackStateCompat.STATE_ERROR
            ReadingPlaybackStatus.Idle -> PlaybackStateCompat.STATE_STOPPED
        }
        mediaSession.setPlaybackState(
            PlaybackStateCompat
                .Builder()
                .setActions(actions)
                .setState(
                    playbackState,
                    PlaybackStateCompat.PLAYBACK_POSITION_UNKNOWN,
                    if (state.status == ReadingPlaybackStatus.Playing) state.playbackSpeed else 0f,
                ).apply {
                    state.errorMessage?.let { setErrorMessage(PlaybackStateCompat.ERROR_CODE_APP_ERROR, it) }
                }.build(),
        )
    }

    private fun buildNotification(state: ReadingPlayerState): Notification {
        val item = state.currentItem
        val playPauseAction = if (state.isActivelyPlaying) {
            notificationAction(
                icon = android.R.drawable.ic_media_pause,
                title = "暂停",
                action = ACTION_PAUSE,
            )
        } else {
            notificationAction(
                icon = android.R.drawable.ic_media_play,
                title = "继续",
                action = ACTION_PLAY,
            )
        }
        val notificationActions = buildList {
            if (state.canPlayPrevious) {
                add(notificationAction(android.R.drawable.ic_media_previous, "上一条", ACTION_PREVIOUS))
            }
            add(playPauseAction)
            if (state.canPlayNext) {
                add(notificationAction(android.R.drawable.ic_media_next, "下一条", ACTION_NEXT))
            }
            add(notificationAction(android.R.drawable.ic_menu_close_clear_cancel, "停止", ACTION_STOP))
        }
        val compactActionIndices = notificationActions
            .indices
            .take(minOf(3, notificationActions.lastIndex))
            .toIntArray()
        return NotificationCompat
            .Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(item?.displayTitle ?: "连续朗读")
            .setContentText(
                buildString {
                    item?.author?.takeIf(String::isNotBlank)?.let {
                        append(it)
                        append(" · ")
                    }
                    item?.contentType?.displayName?.let(::append)
                    if (state.hasSession) append(" · ${state.currentIndex + 1} / ${state.queue.size}")
                },
            ).setContentIntent(item?.let(::contentPendingIntent))
            .setDeleteIntent(commandPendingIntent(ACTION_STOP, 40))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .also { builder -> notificationActions.forEach(builder::addAction) }
            .setStyle(
                MediaStyle()
                    .setMediaSession(mediaSession.sessionToken)
                    .setShowActionsInCompactView(*compactActionIndices)
                    .setShowCancelButton(true)
                    .setCancelButtonIntent(commandPendingIntent(ACTION_STOP, 41)),
            ).build()
    }

    private fun notificationAction(
        icon: Int,
        title: String,
        action: String,
    ): NotificationCompat.Action = NotificationCompat.Action(
        icon,
        title,
        commandPendingIntent(action, action.hashCode()),
    )

    private fun commandPendingIntent(
        action: String,
        requestCode: Int,
    ): PendingIntent = PendingIntent.getService(
        this,
        requestCode,
        commandIntent(this, action),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    private fun contentPendingIntent(item: ReadingQueueItem): PendingIntent {
        val url = when (item.contentType) {
            ReadingContentType.Answer -> item.questionId?.let { "https://www.zhihu.com/question/$it/answer/${item.id}" }
                ?: "https://www.zhihu.com/answer/${item.id}"
            ReadingContentType.Article -> "https://zhuanlan.zhihu.com/p/${item.id}"
            ReadingContentType.Pin -> "https://www.zhihu.com/pin/${item.id}"
            ReadingContentType.Question -> "https://www.zhihu.com/question/${item.id}"
        }
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            setClassName(packageName, MAIN_ACTIVITY_CLASS_NAME)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(READING_NOTIFICATION_INTENT_EXTRA, true)
        }
        return PendingIntent.getActivity(
            this,
            item.key.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "内容朗读",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "连续朗读的播放控制"
            setShowBadge(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        const val ACTION_START = "com.github.zly2006.zhihu.reading.START"
        const val ACTION_TOGGLE = "com.github.zly2006.zhihu.reading.TOGGLE"
        const val ACTION_PLAY = "com.github.zly2006.zhihu.reading.PLAY"
        const val ACTION_PAUSE = "com.github.zly2006.zhihu.reading.PAUSE"
        const val ACTION_PREVIOUS = "com.github.zly2006.zhihu.reading.PREVIOUS"
        const val ACTION_NEXT = "com.github.zly2006.zhihu.reading.NEXT"
        const val ACTION_PLAY_AT = "com.github.zly2006.zhihu.reading.PLAY_AT"
        const val ACTION_SET_PLAYBACK_SPEED = "com.github.zly2006.zhihu.reading.SET_PLAYBACK_SPEED"
        const val ACTION_STOP = "com.github.zly2006.zhihu.reading.STOP"
        const val EXTRA_INDEX = "index"
        const val EXTRA_PLAYBACK_SPEED = "playback_speed"

        private const val TAG = "ContentReadingService"
        private const val MAIN_ACTIVITY_CLASS_NAME = "com.github.zly2006.zhihu.MainActivity"
        const val READING_NOTIFICATION_INTENT_EXTRA = "com.github.zly2006.zhihu.reading.NOTIFICATION_CONTENT"
        private const val NOTIFICATION_CHANNEL_ID = "content_reading"
        private const val NOTIFICATION_ID = 2206
        private const val MAX_TTS_ENGINE_RECOVERY_ATTEMPTS = 1
        private const val PICO_TTS_ENGINE = "com.svox.pico"
        private const val SHERPA_TTS_ENGINE = "com.k2fsa.sherpa.onnx.tts.engine"
        private const val SHERPA_BASE_SPEECH_RATE = 1.1f
        private const val DEFAULT_BASE_SPEECH_RATE = 0.9f

        private val PLAYING_STATUSES = setOf(
            ReadingPlaybackStatus.Initializing,
            ReadingPlaybackStatus.Loading,
            ReadingPlaybackStatus.Playing,
        )

        fun commandIntent(
            context: Context,
            action: String,
        ): Intent = Intent(context, ContentReadingService::class.java).setAction(action)
    }
}
