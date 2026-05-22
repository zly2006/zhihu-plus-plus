package com.github.zly2006.zhihu.ui.components

import android.content.Context
import android.graphics.Color.BLACK
import android.os.Bundle
import android.view.ViewGroup
import android.view.Window
import androidx.activity.ComponentDialog
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
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

                    OpenImagePreviewContent(
                        url = url,
                        onDismiss = { dismiss() },
                        onSaveImage = {
                            scope.launch {
                                saveImageToGallery(context, httpClient, url)
                            }
                        },
                        onShareImage = {
                            scope.launch {
                                shareImage(context, httpClient, url)
                            }
                        },
                        onOpenInBrowser = {
                            luoTianYiUrlLauncher(context, url.toUri())
                        },
                    ) { imageUrl, onClick, onLongClick ->
                        ZoomableAsyncImage(
                            model = imageUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            onClick = { onClick() },
                            onLongClick = onLongClick,
                        )
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
