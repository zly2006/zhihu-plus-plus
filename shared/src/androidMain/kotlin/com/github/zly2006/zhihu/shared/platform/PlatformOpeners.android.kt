package com.github.zly2006.zhihu.shared.platform

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.ui.components.OpenImageDialog
import com.github.zly2006.zhihu.util.luoTianYiUrlLauncher

private const val WEBVIEW_ACTIVITY_CLASS = "com.github.zly2006.zhihu.WebviewActivity"

@Composable
actual fun rememberExternalUrlOpener(): (String) -> Unit {
    val context = LocalContext.current
    return remember(context) {
        { url ->
            luoTianYiUrlLauncher(context, url.toUri())
        }
    }
}

@Composable
actual fun rememberZhihuWebUrlOpener(): (String) -> Unit {
    val context = LocalContext.current
    return remember(context) {
        { url ->
            context.startActivity(
                Intent(Intent.ACTION_VIEW, url.toUri()).setClassName(context, WEBVIEW_ACTIVITY_CLASS),
            )
        }
    }
}

@Composable
actual fun rememberImagePreviewOpener(): (String) -> Unit {
    val context = LocalContext.current
    return remember(context) {
        { url ->
            OpenImageDialog(
                context,
                AccountData.httpClient(context),
                url,
            ).show()
        }
    }
}
