package com.github.zly2006.zhihu.ui

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.github.zly2006.zhihu.data.getContentDetail
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.Pin
import com.github.zly2006.zhihu.navigation.Question
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.pin.PinLinkCardPreview
import com.github.zly2006.zhihu.shared.platform.rememberSettingsStore
import com.github.zly2006.zhihu.ui.components.WebviewComp
import com.github.zly2006.zhihu.ui.components.handleShareAction
import com.github.zly2006.zhihu.ui.components.rememberShareDialogRuntime
import com.github.zly2006.zhihu.ui.components.setupUpWebviewClient
import org.jsoup.Jsoup

@Composable
actual fun rememberPinScreenRuntime(): PinScreenRuntime {
    val context = LocalContext.current
    val settings = rememberSettingsStore()
    val shareRuntime = rememberShareDialogRuntime()
    return remember(context, settings, shareRuntime) {
        PinScreenRuntime(
            handleShareAction = { pin, onShowDialog ->
                handleShareAction(pin, settings, shareRuntime, onShowDialog)
            },
            fetchLinkCardPreview = { linkCard ->
                fetchAndroidLinkCardPreview(context, linkCard)
            },
        )
    }
}

@Composable
actual fun PinHtmlWebViewContent(html: String) {
    WebviewComp {
        it.isVerticalScrollBarEnabled = false
        it.setupUpWebviewClient()
        it.loadZhihu(
            "https://www.zhihu.com",
            Jsoup.parse(html),
        )
    }
}

actual fun supportsPinHtmlWebView(): Boolean = true

private suspend fun fetchAndroidLinkCardPreview(
    context: Context,
    linkCard: DataHolder.Pin.ContentLinkCard,
): PinLinkCardPreview? = fetchPinLinkCardPreview(linkCard) { destination ->
    when (destination) {
        is Article -> {
            DataHolder.getContentDetail(context, destination)
        }
        is Question -> {
            DataHolder.getContentDetail(context, destination)
        }
        is Pin -> {
            DataHolder.getContentDetail(context, destination)
        }
        else -> null
    }
}
