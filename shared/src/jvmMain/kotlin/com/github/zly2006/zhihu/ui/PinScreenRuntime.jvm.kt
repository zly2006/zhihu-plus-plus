package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.github.zly2006.zhihu.data.decodePinContentDetail
import com.github.zly2006.zhihu.data.decodeQuestionContentDetail
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.Pin
import com.github.zly2006.zhihu.navigation.Question
import com.github.zly2006.zhihu.shared.data.DataHolder
import com.github.zly2006.zhihu.shared.desktop.DesktopAccountStore
import com.github.zly2006.zhihu.shared.desktop.openDesktopExternalUrl
import com.github.zly2006.zhihu.shared.desktop.signedFetchJson
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
    return remember(settings, shareRuntime, store) {
        PinScreenRuntime(
            handleShareAction = { pin, onShowDialog ->
                handleShareAction(pin, settings, shareRuntime, onShowDialog)
            },
            fetchLinkCardPreview = { linkCard ->
                fetchDesktopLinkCardPreview(store, linkCard)
            },
            openExternalUrl = ::openDesktopExternalUrl,
        )
    }
}

@Composable
actual fun PinHtmlWebViewContent(html: String) {
}

actual fun supportsPinHtmlWebView(): Boolean = false

internal suspend fun fetchDesktopPinDetail(
    store: DesktopAccountStore,
    pin: Pin,
): DataHolder.Pin? = runCatching {
    val json = store.signedFetchJson("https://www.zhihu.com/api/v4/pins/${pin.id}")
        ?: return@runCatching null
    decodePinContentDetail(json)
}.getOrNull()

internal suspend fun fetchDesktopQuestionDetailForFeedBlock(
    store: DesktopAccountStore,
    question: Question,
): DataHolder.Question? = runCatching {
    val jo = store.signedFetchJson("https://www.zhihu.com/api/v4/questions/${question.questionId}?include=read_count,visit_count,answer_count,voteup_count,comment_count,follower_count,detail,excerpt,author,relationship.is_following,topics")
        ?: return@runCatching null
    decodeQuestionContentDetail(jo)
}.getOrNull()

private suspend fun fetchDesktopLinkCardPreview(
    store: DesktopAccountStore,
    linkCard: DataHolder.Pin.ContentLinkCard,
): PinLinkCardPreview? = fetchPinLinkCardPreview(linkCard) { destination ->
    when (destination) {
        is Article -> {
            DesktopPaginationEnvironment(store).getContentDetail(destination)
        }
        is Question -> {
            fetchDesktopQuestionDetailForFeedBlock(store, destination)
        }
        is Pin -> {
            fetchDesktopPinDetail(store, destination)
        }
        else -> null
    }
}
