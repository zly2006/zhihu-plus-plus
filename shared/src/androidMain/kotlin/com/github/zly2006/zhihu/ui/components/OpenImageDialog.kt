package com.github.zly2006.zhihu.ui.components

import android.content.Context
import android.graphics.Color.BLACK
import android.os.Bundle
import android.view.ViewGroup
import android.view.Window
import androidx.activity.ComponentDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.DpOffset
import androidx.core.graphics.drawable.toDrawable
import androidx.core.net.toUri
import com.github.zly2006.zhihu.util.luoTianYiUrlLauncher
import com.github.zly2006.zhihu.util.saveImageToGallery
import com.github.zly2006.zhihu.util.shareImage
import io.ktor.client.HttpClient
import kotlinx.coroutines.launch
import me.saket.telephoto.zoomable.coil3.ZoomableAsyncImage

class OpenImageDialog(
    context: Context,
    private val httpClient: HttpClient,
    private val url: String,
) : ComponentDialog(context) {
    init {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setCanceledOnTouchOutside(true)
        setContentView(
            ComposeView(context).apply {
                setContent {
                    val scope = rememberCoroutineScope()
                    var showMenu by remember { mutableStateOf(false) }
                    var menuOffset by remember { mutableStateOf(Offset.Zero) }
                    val density = LocalDensity.current

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black),
                    ) {
                        // 禁用telephoto自带的震动反馈
                        CompositionLocalProvider(LocalHapticFeedback provides NoopHapticFeedback) {
                            ZoomableAsyncImage(
                                model = url,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                onClick = { dismiss() },
                                onLongClick = { offset ->
                                    menuOffset = offset
                                    showMenu = true
                                },
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            offset = with(density) {
                                DpOffset(
                                    menuOffset.x.toDp(),
                                    menuOffset.y.toDp(),
                                )
                            },
                        ) {
                            DropdownMenuItem(
                                text = { Text("保存图片") },
                                onClick = {
                                    showMenu = false
                                    scope.launch {
                                        saveImageToGallery(context, httpClient, url)
                                    }
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("分享图片") },
                                onClick = {
                                    showMenu = false
                                    scope.launch {
                                        shareImage(context, httpClient, url)
                                    }
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("在浏览器中打开") },
                                onClick = {
                                    showMenu = false
                                    luoTianYiUrlLauncher(context, url.toUri())
                                },
                            )
                        }
                    }
                }
            },
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT,
        )
        window?.setBackgroundDrawable(BLACK.toDrawable())
    }
}

private object NoopHapticFeedback : HapticFeedback {
    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) {
        // noop
    }
}
