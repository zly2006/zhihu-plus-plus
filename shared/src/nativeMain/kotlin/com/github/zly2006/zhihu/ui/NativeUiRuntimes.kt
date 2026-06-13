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

package com.github.zly2006.zhihu.ui
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.shared.account.IosAccountStore
import com.github.zly2006.zhihu.shared.data.RecommendationMode
import com.github.zly2006.zhihu.shared.notification.NotificationSettingsStore
import com.github.zly2006.zhihu.shared.platform.UserMessageSink
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.ui.components.rememberShareDialogRuntime
import com.github.zly2006.zhihu.viewmodel.NotificationEnvironment
import com.github.zly2006.zhihu.viewmodel.NotificationViewModel
import com.github.zly2006.zhihu.viewmodel.feed.HomeFeedViewModel
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

@Composable
actual fun rememberArticleActionsRuntime(): ArticleActionsRuntime {
    val userMessages = rememberUserMessageSink()
    val dialogShareRuntime = rememberShareDialogRuntime()
    return remember(userMessages, dialogShareRuntime) {
        object : ArticleActionsRuntime {
            override var ttsState: TtsState by mutableStateOf(TtsState.Ready)
                private set // TODO: iOS TTS 实现
            override val shareRuntime = dialogShareRuntime

            override fun toggleSpeech(title: String, content: String) =
                userMessages.showMessage("iOS TTS 暂未实现") // TODO: iOS TTS 实现

            override fun openArticleInBrowser(article: Article) = openIosUrl(articleWebUrl(article))
        }
    }
}

@Composable
actual fun rememberNotificationScreenRuntime(
    viewModel: NotificationViewModel,
    settingsStore: NotificationSettingsStore,
): NotificationScreenRuntime = remember(settingsStore) {
    NotificationScreenRuntime(
        environment = IosNotificationEnvironment(settingsStore),
        showDebugCopy = false,
    )
}

private class IosNotificationEnvironment(
    override val notificationSettingsStore: NotificationSettingsStore,
) : NotificationEnvironment {
    override fun httpClient() = error("HTTP client not available on iOS yet") // TODO: iOS HTTP 客户端

    override suspend fun fetchJson(url: String, include: String): JsonObject = error("fetchJson not available on iOS yet") // TODO: iOS 通知数据获取

    override fun logDecodeFailure(tag: String?, item: JsonElement, error: Exception) = Unit // TODO: iOS 解码失败日志

    override suspend fun handleFetchFailure(tag: String?, error: Exception) = Unit // TODO: iOS 获取失败处理

    override fun configureSignedRequest(builder: HttpRequestBuilder) = Unit // TODO: iOS 签名请求配置
}

@Composable
actual fun rememberArticleScreenRuntime(): ArticleScreenRuntime = remember {
    object : ArticleScreenRuntime {
        override val articleHost: ArticleHost? = null

        override val previewPreloader = ArticlePreviewPreloader { _, _, _, _ -> } // TODO: iOS 预加载实现
    }
}

@Composable
actual fun ArticleWebViewContent(
    article: Article,
    html: String,
    title: String,
    scrollState: ScrollState,
    rememberedScrollY: Int,
    rememberedScrollYSync: Boolean,
    onRememberedScrollYSyncChange: (Boolean) -> Unit,
    onImageLoadFailed: () -> Unit,
    onDoubleTap: () -> Unit,
) = Unit // TODO: iOS WebView 实现

actual fun Modifier.articleMarkdownSelectionWorkaround(): Modifier = this

@Composable
actual fun rememberCommentScreenRuntime(): CommentScreenRuntime {
    val userMessages = rememberUserMessageSink()
    return remember(userMessages) {
        object : CommentScreenRuntime {
            override fun saveImage(imageUrl: String) =
                userMessages.showMessage("iOS 图片保存暂未实现") // TODO: iOS 图片保存

            override fun shareImage(imageUrl: String) =
                userMessages.showMessage("iOS 图片分享暂未实现") // TODO: iOS 图片分享
        }
    }
}

@Composable
actual fun rememberCommentEmojiInlineContent(emojiKeys: Set<String>): Map<String, InlineTextContent> = emptyMap() // TODO: iOS 表情内联内容

actual fun commentEmojiInlineKey(placeholder: String): String? = null // TODO: iOS 表情内联 key

actual fun Modifier.commentSelectionWorkaround(): Modifier = this

@Composable
actual fun rememberHomeScreenRuntime(recommendationMode: RecommendationMode): HomeScreenRuntime {
    val userMessages = rememberUserMessageSink()
    val viewModel = remember { HomeFeedViewModel() }
    return remember(userMessages, viewModel) {
        HomeScreenRuntime(
            account = HomeAccountState(isLoggedIn = false, avatarUrl = null),
            updateAnnouncement = null,
            installedAtLeastThreeHours = false,
            isDebuggable = false,
            viewModel = viewModel,
            requestLogin = { userMessages.showMessage("iOS 登录暂未实现") }, // TODO: iOS 登录
            recordLocalItemOpened = { }, // TODO: iOS 本地推荐记录
            recordLocalItemFeedback = { _, _ -> false },
        )
    }
}

@Composable
actual fun rememberAccountSettingsPlatformRuntime(): AccountSettingsRuntime {
    val userMessages = rememberUserMessageSink()
    return remember(userMessages) {
        AccountSettingsRuntime(
            accountState = mutableStateOf(AccountSettingsAccountState()),
            refreshProfile = { }, // TODO: iOS 刷新用户信息
            requestLogin = { userMessages.showMessage("iOS 登录暂未实现") }, // TODO: iOS 登录
            requestQrLoginScan = { userMessages.showMessage("iOS 扫码登录暂未实现") }, // TODO: iOS 扫码登录
            logout = { }, // TODO: iOS 登出
            appVersionInfo = { "iOS" },
            selectMainTab = { }, // TODO: iOS 主 Tab 切换
        )
    }
}

@Composable
actual fun rememberPinScreenRuntime(): PinScreenRuntime =
    remember {
        PinScreenRuntime(
            fetchLinkCardPreview = { null },
        )
    }

@Composable
actual fun PinHtmlWebViewContent(html: String) = Unit // TODO: iOS 想法 WebView 实现

actual fun supportsPinHtmlWebView(): Boolean = false

@Composable
actual fun rememberBlocklistSettingsPlatformRuntime(
    userMessages: UserMessageSink,
): BlocklistSettingsRuntime = remember(userMessages) {
    BlocklistSettingsRuntime(
        requestImport = { _ -> }, // TODO: iOS 导入规则
        exportRules = { "" }, // TODO: iOS 导出规则
    )
}

@Composable
actual fun rememberZhihuHttpClient(): HttpClient {
    val store = remember { IosAccountStore() }
    val session = remember { store.load() }
    return remember(store, session) { store.createHttpClient(session.cookies.toMutableMap()) }
}

internal fun openIosUrl(url: String) {
    val nsUrl = NSURL.URLWithString(url) ?: return
    UIApplication.sharedApplication.openURL(nsUrl)
}

@Composable
actual fun QuestionDetailWebViewContent(questionId: Long, html: String) = Unit // TODO: iOS 问题 WebView 实现

actual fun supportsQuestionDetailWebView(): Boolean = false

actual fun Modifier.questionSelectionWorkaround(): Modifier = this

@Composable
actual fun ArticleImmersiveModeEffect(immersive: Boolean) = Unit

@Composable
actual fun LeaveImmersiveModeCleanup() = Unit
