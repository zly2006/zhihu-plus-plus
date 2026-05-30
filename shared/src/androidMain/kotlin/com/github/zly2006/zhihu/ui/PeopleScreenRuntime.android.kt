package com.github.zly2006.zhihu.ui

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.shared.platform.androidUserMessageSink
import com.github.zly2006.zhihu.ui.components.OpenImageDialog

private const val WEBVIEW_ACTIVITY_CLASS = "com.github.zly2006.zhihu.WebviewActivity"

@Composable
actual fun rememberPeopleScreenRuntime(): PeopleScreenRuntime {
    val context = LocalContext.current
    return remember(context) {
        val userMessages = androidUserMessageSink(context)
        PeopleScreenRuntime(
            showShortMessage = { message ->
                userMessages.showShortMessage(message)
            },
            openWebUrl = { url ->
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, url.toUri()).setClassName(context, WEBVIEW_ACTIVITY_CLASS),
                )
            },
            openImage = { url ->
                OpenImageDialog(
                    context,
                    AccountData.httpClient(context),
                    url,
                ).show()
            },
        )
    }
}
