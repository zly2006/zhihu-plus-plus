/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.TopLevelDestination
import com.github.zly2006.zhihu.shared.account.IosAccountStore
import com.github.zly2006.zhihu.shared.notification.NotificationSettingsStore
import com.github.zly2006.zhihu.shared.platform.UserMessageSink
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.viewmodel.NotificationEnvironment
import com.github.zly2006.zhihu.viewmodel.NotificationViewModel
import io.ktor.client.HttpClient
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

@Composable
actual fun rememberArticleTtsState(): TtsState = TtsState.Ready

@Composable
actual fun rememberArticleSpeechToggler(): (title: String, content: String) -> Unit {
    val userMessages = rememberUserMessageSink()
    return remember(userMessages) {
        { _, _ -> userMessages.showMessage("iOS TTS 暂未实现") } // TODO: iOS TTS 实现
    }
}

@Composable
actual fun rememberArticleBrowserOpener(): (Article) -> Unit = remember {
    { article -> openIosUrl(articleWebUrl(article)) }
}

@Composable
actual fun rememberNotificationEnvironment(
    viewModel: NotificationViewModel,
    settingsStore: NotificationSettingsStore,
): NotificationEnvironment = remember(settingsStore) {
    IosNotificationEnvironment(settingsStore)
}

@Composable
actual fun rememberNotificationShowDebugCopy(): Boolean = false

private class IosNotificationEnvironment(
    override val notificationSettingsStore: NotificationSettingsStore,
) : NotificationEnvironment {
    override fun httpClient() = error("HTTP client not available on iOS yet") // TODO: iOS HTTP 客户端

    override fun authenticatedCookies(): Map<String, String> = emptyMap()

    override suspend fun fetchJson(url: String, include: String): JsonObject = error("fetchJson not available on iOS yet") // TODO: iOS 通知数据获取

    override fun logDecodeFailure(tag: String?, item: JsonElement, error: Exception) = Unit // TODO: iOS 解码失败日志

    override suspend fun handleFetchFailure(tag: String?, error: Exception) = Unit // TODO: iOS 获取失败处理
}

@Composable
actual fun rememberArticleHost(): ArticleHost? = null

@Composable
actual fun ArticlePreviewPreloadEffect(
    cached: com.github.zly2006.zhihu.viewmodel.ArticleViewModel.CachedAnswerContent?,
    isNext: Boolean,
    title: String,
    onImageLoadFailed: () -> Unit,
) = Unit

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
actual fun rememberCommentEmojiInlineContent(emojiKeys: Set<String>): Map<String, InlineTextContent> = emptyMap() // TODO: iOS 表情内联内容

actual fun commentEmojiInlineKey(placeholder: String): String? = null // TODO: iOS 表情内联 key

actual fun Modifier.commentSelectionWorkaround(): Modifier = this

@Composable
actual fun rememberHomeAccountState(): HomeAccountState = HomeAccountState(
    isLoggedIn = false,
    avatarUrl = null,
)

@Composable
actual fun rememberHomeUpdateAnnouncement(): HomeUpdateAnnouncement? = null

@Composable
actual fun rememberHomeInstalledAtLeastThreeHours(): Boolean = false

@Composable
actual fun rememberHomeIsDebuggable(): Boolean = false

@Composable
actual fun rememberHomeLoginRequester(): () -> Unit {
    val userMessages = rememberUserMessageSink()
    return remember(userMessages) {
        { userMessages.showMessage("iOS 登录暂未实现") } // TODO: iOS 登录
    }
}

@Composable
actual fun rememberAccountSettingsAccountState(): androidx.compose.runtime.State<AccountSettingsAccountState> =
    remember { mutableStateOf(AccountSettingsAccountState()) }

@Composable
actual fun rememberAccountProfileRefresher(): suspend () -> Unit = remember {
    { } // TODO: iOS 刷新用户信息
}

@Composable
actual fun rememberAccountLoginRequester(): () -> Unit {
    val userMessages = rememberUserMessageSink()
    return remember(userMessages) {
        { userMessages.showMessage("iOS 登录暂未实现") } // TODO: iOS 登录
    }
}

@Composable
actual fun rememberAccountQrLoginRequester(): () -> Unit {
    val userMessages = rememberUserMessageSink()
    return remember(userMessages) {
        { userMessages.showMessage("iOS 扫码登录暂未实现") } // TODO: iOS 扫码登录
    }
}

@Composable
actual fun rememberAccountLogoutAction(): () -> Unit = remember {
    { } // TODO: iOS 登出
}

@Composable
actual fun rememberAppVersionInfo(): String = "iOS"

@Composable
actual fun rememberMainTabSelector(): (TopLevelDestination) -> Unit = remember {
    { } // TODO: iOS 主 Tab 切换
}

@Composable
actual fun ZhihuHtmlWebViewContent(html: String) = Unit // TODO: iOS HTML WebView 实现

actual fun supportsZhihuHtmlWebView(): Boolean = false

@Composable
actual fun rememberBlocklistRuleImporter(
    userMessages: UserMessageSink,
): (((String) -> Unit) -> Unit) = remember(userMessages) {
    { _ -> userMessages.showMessage("iOS 导入规则暂未实现") } // TODO: iOS 导入规则
}

@Composable
actual fun rememberBlocklistRuleExporter(): suspend () -> String = remember {
    { "" } // TODO: iOS 导出规则
}

@Composable
actual fun rememberZhihuHttpClient(): HttpClient {
    val store = remember { IosAccountStore() }
    return store.httpClient()
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
