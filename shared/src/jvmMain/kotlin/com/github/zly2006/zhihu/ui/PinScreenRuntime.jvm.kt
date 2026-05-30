package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.desktop.DesktopAccountStore
import com.github.zly2006.zhihu.shared.pin.PinLinkCardPreview
import com.github.zly2006.zhihu.shared.platform.rememberSettingsStore
import com.github.zly2006.zhihu.ui.components.handleShareAction
import com.github.zly2006.zhihu.ui.components.rememberShareDialogRuntime
import com.github.zly2006.zhihu.viewmodel.DesktopPaginationEnvironment

@Composable
actual fun rememberPinScreenRuntime(): PinScreenRuntime {
    val settings = rememberSettingsStore()
    val shareRuntime = rememberShareDialogRuntime()
    val store = remember { DesktopAccountStore() }
    val environment = remember(store) { DesktopPaginationEnvironment(store) }
    return remember(settings, shareRuntime, environment) {
        PinScreenRuntime(
            handleShareAction = { pin, onShowDialog ->
                handleShareAction(pin, settings, shareRuntime, onShowDialog)
            },
            fetchLinkCardPreview = { linkCard ->
                fetchDesktopLinkCardPreview(environment, linkCard)
            },
        )
    }
}

@Composable
actual fun PinHtmlWebViewContent(html: String) = Unit // TODO: desktop Pin WebView

actual fun supportsPinHtmlWebView(): Boolean = false

private suspend fun fetchDesktopLinkCardPreview(
    environment: DesktopPaginationEnvironment,
    linkCard: DataHolder.Pin.ContentLinkCard,
): PinLinkCardPreview? = fetchPinLinkCardPreview(linkCard) { destination ->
    environment.getContentDetail(destination)
}
