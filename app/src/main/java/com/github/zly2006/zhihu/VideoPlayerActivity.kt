/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
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

package com.github.zly2006.zhihu

import android.content.ContentValues
import android.content.res.Configuration
import android.graphics.SurfaceTexture
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.Surface
import android.view.TextureView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.github.zly2006.zhihu.shared.platform.androidSettingsStore
import com.github.zly2006.zhihu.util.enableEdgeToEdgeCompat
import kotlinx.coroutines.delay
import kotlin.math.abs

class VideoPlayerActivity : ComponentActivity() {
    private var player: MediaPlayer? = null
    private var videoId: Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val videoUrl = intent.getStringExtra("video_url") ?: run {
            finish()
            return
        }
        videoId = intent.getLongExtra("video_id", 0)
        val settingsStore = androidSettingsStore(this)
        val savedPosition = if (videoId != 0L) {
            settingsStore.getLong("video_progress_$videoId", 0)
        } else {
            0
        }

        enableEdgeToEdgeCompat()
        setContent {
            val toolbarColor = Color(0xFF1A1A2E).copy(alpha = 0.85f)
            val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
            val isFastForwardingState = remember { mutableStateOf(false) }

            LaunchedEffect(isLandscape) {
                val ctrl = WindowInsetsControllerCompat(window, window.decorView)
                if (isLandscape) {
                    ctrl.hide(WindowInsetsCompat.Type.systemBars())
                    ctrl.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                } else {
                    ctrl.show(WindowInsetsCompat.Type.systemBars())
                }
            }

            Box(Modifier.fillMaxSize().background(Color.Black)) {
                VideoPlayerView(
                    videoUrl = videoUrl,
                    savedPosition = savedPosition,
                    isFastForwardingState = isFastForwardingState,
                    onPlayerReady = { p -> player = p },
                )
                if (isLandscape) {
                    LandscapeOverlay(onBack = { finish() })
                } else {
                    PortraitOverlay(toolbarColor, onBack = { finish() })
                }

                AnimatedVisibility(
                    visible = isFastForwardingState.value,
                    enter = fadeIn(tween(150)),
                    exit = fadeOut(tween(300)),
                    modifier = Modifier.align(Alignment.TopCenter),
                ) {
                    Surface(
                        modifier = Modifier.padding(top = 56.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = Color.Black.copy(alpha = 0.35f),
                    ) {
                        Text(
                            "2x",
                            color = Color.White.copy(alpha = 0.85f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        )
                    }
                }
            }

            DisposableEffect(Unit) { onDispose { saveCurrentProgress() } }
        }
    }

    override fun onPause() {
        super.onPause()
        saveCurrentProgress()
    }

    override fun onDestroy() {
        super.onDestroy()
        saveCurrentProgress()
        player?.release()
        player = null
    }

    private fun saveCurrentProgress() {
        val p = player ?: return
        if (videoId == 0L) return
        try {
            val pos = p.currentPosition.toLong()
            val d = p.duration.toLong()
            if (pos > 1000 && d > 0) {
                val store = androidSettingsStore(this)
                if (d - pos > 3000) {
                    store.putLong("video_progress_$videoId", pos)
                } else {
                    store.remove("video_progress_$videoId")
                }
            }
        } catch (_: IllegalStateException) {
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PortraitOverlay(toolbarColor: Color, onBack: () -> Unit) {
    val c = if (toolbarColor.luminance() > 0.5f) Color.Black else Color.White
    TopAppBar(
        title = { Text("视频播放", color = c) },
        navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = c) } },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = toolbarColor, navigationIconContentColor = c, titleContentColor = c),
        modifier = Modifier.statusBarsPadding(),
    )
}

@Composable
private fun LandscapeOverlay(onBack: () -> Unit) {
    Surface(onClick = onBack, modifier = Modifier.padding(12.dp).size(40.dp), shape = CircleShape, color = Color.Black.copy(alpha = 0.5f)) {
        Box(contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回", tint = Color.White, modifier = Modifier.size(24.dp))
        }
    }
}

private fun formatTime(ms: Long): String {
    val total = ms / 1000
    return "%d:%02d".format(total / 60, total % 60)
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun VideoPlayerView(
    videoUrl: String,
    savedPosition: Long,
    isFastForwardingState: MutableState<Boolean>,
    onPlayerReady: (MediaPlayer) -> Unit,
) {
    val isPlayingState = remember { mutableStateOf(true) }
    var videoEnded by remember { mutableStateOf(false) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var controlsVisible by remember { mutableStateOf(false) }
    var screenLocked by remember { mutableStateOf(false) }
    var lockHintVisible by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    var playbackSpeed by remember { mutableFloatStateOf(1f) }
    var dragSeekPreview by remember { mutableLongStateOf(0L) }
    var videoAspectRatio by remember { mutableFloatStateOf(16f / 9f) }
    val ctx = LocalContext.current

    fun resumeOrRestart() {
        val p = mediaPlayer ?: return
        if (videoEnded) {
            p.seekTo(0)
            videoEnded = false
        }
        p.start()
        isPlayingState.value = true
    }

    fun safeSetSpeed(speed: Float) {
        try {
            mediaPlayer?.playbackParams = PlaybackParams().setSpeed(speed)
        } catch (_: Exception) {
        }
    }

    fun safeSeekTo(posMs: Long) {
        try {
            val dur = duration.coerceAtLeast(0)
            if (dur > 0) {
                val target = posMs.coerceIn(0, dur)
                mediaPlayer?.seekTo(target, MediaPlayer.SEEK_CLOSEST)
            }
        } catch (_: Exception) {
        }
    }

    fun safeGetMediaTime(fallback: Long, value: MediaPlayer.() -> Int): Long = try {
        mediaPlayer?.value()?.toLong()?.coerceAtLeast(0) ?: 0
    } catch (_: Exception) {
        fallback
    }

    LaunchedEffect(controlsVisible, isPlayingState.value) {
        while (controlsVisible || isPlayingState.value) {
            currentPosition = safeGetMediaTime(currentPosition) { this.currentPosition }
            duration = safeGetMediaTime(duration) { this.duration }
            delay(200)
        }
    }
    LaunchedEffect(isPlayingState.value) { if (!isPlayingState.value) controlsVisible = true }
    LaunchedEffect(controlsVisible, isPlayingState.value) {
        if (controlsVisible && isPlayingState.value) {
            delay(3000)
            controlsVisible = false
        }
    }
    LaunchedEffect(lockHintVisible) {
        if (lockHintVisible) {
            delay(3000)
            lockHintVisible = false
        }
    }

    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().aspectRatio(videoAspectRatio)) {
            AndroidView(
                factory = { viewCtx ->
                    TextureView(viewCtx).apply {
                        surfaceTextureListener = object : TextureView.SurfaceTextureListener {
                            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                                val existing = mediaPlayer
                                if (existing != null) {
                                    existing.setSurface(Surface(surface))
                                    return
                                }
                                val player = MediaPlayer().apply {
                                    setDataSource(viewCtx, Uri.parse(videoUrl))
                                    setSurface(Surface(surface))
                                    setOnPreparedListener {
                                        if (savedPosition > 0) seekTo(savedPosition.toInt())
                                        start()
                                        isPlayingState.value = true
                                    }
                                    setOnVideoSizeChangedListener { _, w, h ->
                                        if (w > 0 && h > 0) videoAspectRatio = w.toFloat() / h.toFloat()
                                    }
                                    setOnCompletionListener {
                                        videoEnded = true
                                        isPlayingState.value = false
                                    }
                                    setOnErrorListener { _, _, _ -> true }
                                    prepareAsync()
                                }
                                mediaPlayer = player
                                onPlayerReady(player)
                            }

                            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}

                            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                                mediaPlayer?.setSurface(null)
                                return true
                            }

                            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
                        }
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )

            val screenW = LocalContext.current.resources.displayMetrics.widthPixels
                .toFloat()
            val viewCfg = LocalViewConfiguration.current
            var fingerDown by remember { mutableStateOf(false) }

            LaunchedEffect(fingerDown) {
                if (!fingerDown) return@LaunchedEffect
                delay(viewCfg.longPressTimeoutMillis + 150L)
                if (fingerDown && !screenLocked) {
                    safeSetSpeed(2f)
                    isFastForwardingState.value = true
                }
            }
            LaunchedEffect(fingerDown, isFastForwardingState.value) {
                if (!fingerDown && isFastForwardingState.value) {
                    safeSetSpeed(playbackSpeed)
                    isFastForwardingState.value = false
                }
            }

            Box(
                Modifier.fillMaxSize().pointerInput(screenLocked) {
                    var lastTapTime = 0L
                    var tapCount = 0
                    awaitPointerEventScope {
                        while (true) {
                            var down: PointerInputChange? = null
                            while (down == null) {
                                val e = awaitPointerEvent(PointerEventPass.Main)
                                down = e.changes.firstOrNull { it.pressed }
                            }
                            val downPos = down.position
                            var totalDragX = 0f
                            var isDragging = false
                            if (!screenLocked) fingerDown = true
                            var done = false
                            while (!done) {
                                val ev = awaitPointerEvent(PointerEventPass.Main)
                                val ch = ev.changes.firstOrNull() ?: break
                                if (ch.changedToUp()) {
                                    done = true
                                    fingerDown = false
                                    if (screenLocked) {
                                        lockHintVisible = true
                                    } else if (isDragging) {
                                        val ms = (totalDragX / screenW * 15 * 1000).toLong()
                                        val mp = mediaPlayer
                                        if (mp != null) {
                                            try {
                                                val dur = mp.duration.coerceAtLeast(0)
                                                val pos = mp.currentPosition.coerceAtLeast(0)
                                                if (dur > 0) {
                                                    val target = (pos.toLong() + ms).coerceIn(0L, dur.toLong())
                                                    mp.seekTo(target, MediaPlayer.SEEK_CLOSEST)
                                                    currentPosition = mp.currentPosition.coerceAtLeast(0).toLong()
                                                }
                                            } catch (_: Exception) {
                                            }
                                        }
                                        dragSeekPreview = 0
                                        controlsVisible = true
                                    } else {
                                        val now = System.currentTimeMillis()
                                        if (now - lastTapTime < 300) tapCount++ else tapCount = 1
                                        lastTapTime = now
                                        if (tapCount >= 2) {
                                            val pl = mediaPlayer ?: return@awaitPointerEventScope
                                            if (pl.isPlaying) pl.pause() else resumeOrRestart()
                                            tapCount = 0
                                        } else {
                                            controlsVisible = !controlsVisible
                                        }
                                    }
                                } else if (!ch.pressed) {
                                    done = true
                                    fingerDown = false
                                } else {
                                    if (!isDragging && !isFastForwardingState.value && !screenLocked) {
                                        val dx = ch.position.x - downPos.x
                                        totalDragX = dx
                                        if (abs(dx) > viewConfiguration.touchSlop) {
                                            isDragging = true
                                            fingerDown = false
                                            ch.consume()
                                        }
                                    }
                                    if (isDragging) {
                                        ch.consume()
                                        totalDragX = ch.position.x - downPos.x
                                        dragSeekPreview = (totalDragX / screenW * 15 * 1000).toLong()
                                    }
                                }
                            }
                        }
                    }
                },
            )
        } // end aspect ratio Box

        AnimatedVisibility(visible = controlsVisible, enter = EnterTransition.None, exit = ExitTransition.None, modifier = Modifier.align(Alignment.BottomCenter).fillMaxHeight(0.23f)) {
            Surface(Modifier.fillMaxWidth().fillMaxHeight(), color = Color.Black.copy(alpha = 0.55f)) {
                Column(Modifier.fillMaxHeight().padding(horizontal = 12.dp, vertical = 4.dp), verticalArrangement = Arrangement.Center) {
                    Slider(
                        value = if (duration > 0) currentPosition.toFloat() / duration else 0f,
                        onValueChange = {
                            val pos = (it * duration).toLong()
                            safeSeekTo(pos)
                            currentPosition = safeGetMediaTime(currentPosition) { this.currentPosition }
                        },
                        modifier = Modifier.fillMaxWidth().height(20.dp),
                        track = {
                            val frac = if (duration > 0) currentPosition.toFloat() / duration else 0f
                            Canvas(Modifier.fillMaxWidth().height(8.dp)) {
                                val y = size.height / 2f
                                val sw = 2.dp.toPx()
                                val pe = size.width * frac
                                drawLine(Color.White.copy(alpha = 0.2f), Offset(pe, y), Offset(size.width, y), sw, cap = StrokeCap.Round)
                                drawLine(Color.White, Offset(0f, y), Offset(pe, y), sw, cap = StrokeCap.Round)
                            }
                        },
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {
                            val p = mediaPlayer ?: return@IconButton
                            if (p.isPlaying) p.pause() else resumeOrRestart()
                        }, modifier = Modifier.size(36.dp)) {
                            Icon(if (isPlayingState.value) Icons.Default.Pause else Icons.Default.PlayArrow, if (isPlayingState.value) "暂停" else "播放", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Text("${formatTime(currentPosition)} / ${formatTime(duration)}", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                        IconButton(onClick = {
                            val url = videoUrl
                            val pos = currentPosition
                            Thread {
                                try {
                                    val r = MediaMetadataRetriever()
                                    r.setDataSource(url, HashMap<String, String>())
                                    val bmp = r.getFrameAtTime(pos * 1000L, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
                                    r.release()
                                    if (bmp == null) {
                                        Handler(Looper.getMainLooper()).post { Toast.makeText(ctx, "截图失败", Toast.LENGTH_SHORT).show() }
                                        return@Thread
                                    }
                                    val v = ContentValues().apply {
                                        put(MediaStore.Images.Media.DISPLAY_NAME, "zhihu_${System.currentTimeMillis()}.jpg")
                                        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                                        put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Zhihu")
                                    }
                                    val uri = ctx.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, v)
                                    if (uri != null) {
                                        ctx.contentResolver.openOutputStream(uri)?.use { bmp.compress(android.graphics.Bitmap.CompressFormat.JPEG, 95, it) }
                                        Handler(Looper.getMainLooper()).post { Toast.makeText(ctx, "截图已保存", Toast.LENGTH_SHORT).show() }
                                    }
                                } catch (e: Exception) {
                                    Handler(Looper.getMainLooper()).post { Toast.makeText(ctx, "截图失败: ${e.message}", Toast.LENGTH_SHORT).show() }
                                }
                            }.start()
                        }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.CameraAlt, "截图", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = {
                            screenLocked = true
                            controlsVisible = false
                            lockHintVisible = true
                        }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.LockOpen, "锁定", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = {
                            val dm = ctx.getSystemService(android.content.Context.DOWNLOAD_SERVICE) as android.app.DownloadManager
                            dm.enqueue(
                                android.app.DownloadManager.Request(Uri.parse(videoUrl)).apply {
                                    setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "zhihu_video_${System.currentTimeMillis()}.mp4")
                                    setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                                },
                            )
                            Toast.makeText(ctx, "开始下载", Toast.LENGTH_SHORT).show()
                        }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Download, "下载", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        var speedMenuExpanded by remember { mutableStateOf(false) }
                        Box {
                            Text(
                                "${playbackSpeed}x",
                                color = if (playbackSpeed != 1f) Color.White else Color.White.copy(alpha = 0.6f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(8.dp).clickable { speedMenuExpanded = true },
                            )
                            DropdownMenu(expanded = speedMenuExpanded, onDismissRequest = { speedMenuExpanded = false }, modifier = Modifier.background(Color(0xFF2D2D2D), RoundedCornerShape(8.dp))) {
                                floatArrayOf(0.75f, 1f, 1.25f, 1.5f, 2f).forEach { sp ->
                                    val sel = sp == playbackSpeed
                                    DropdownMenuItem(
                                        text = { Text("${sp}x", color = if (sel) Color(0xFFA78BFA) else Color.White.copy(alpha = 0.75f), fontSize = 13.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal) },
                                        onClick = {
                                            playbackSpeed = sp
                                            safeSetSpeed(sp)
                                            speedMenuExpanded = false
                                        },
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(visible = screenLocked && lockHintVisible, enter = EnterTransition.None, exit = ExitTransition.None, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 8.dp)) {
            Surface(onClick = {
                screenLocked = false
                controlsVisible = true
                lockHintVisible = false
            }, shape = CircleShape, color = Color.Black.copy(alpha = 0.45f), modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Lock, "解锁", tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp)) }
            }
        }

        if (dragSeekPreview != 0L && duration > 0) {
            Surface(Modifier.align(Alignment.Center).padding(bottom = 80.dp), shape = RoundedCornerShape(8.dp), color = Color.Black.copy(alpha = 0.6f)) {
                Text("${if (dragSeekPreview > 0) "+" else ""}${dragSeekPreview / 1000}s  ${formatTime((currentPosition + dragSeekPreview).coerceIn(0, duration))}", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }
        }
    }
}
