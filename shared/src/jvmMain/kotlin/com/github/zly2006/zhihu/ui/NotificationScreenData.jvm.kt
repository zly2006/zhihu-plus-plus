package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.github.zly2006.zhihu.shared.desktop.DesktopAccountStore
import com.github.zly2006.zhihu.shared.notification.NotificationSettingsStore
import com.github.zly2006.zhihu.shared.notification.rememberNotificationSettingsStore
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.shared.util.signZhihuFetchRequest
import com.github.zly2006.zhihu.viewmodel.NotificationPaginationEnvironment
import com.github.zly2006.zhihu.viewmodel.NotificationViewModel
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

@Composable
actual fun rememberNotificationScreenData(): NotificationScreenData {
    val userMessages = rememberUserMessageSink()
    val settingsStore = rememberNotificationSettingsStore()
    val viewModel = viewModel<NotificationViewModel>()
    val coroutineScope = rememberCoroutineScope()
    val store = remember { DesktopAccountStore() }
    val cookies = remember(store) { store.load().cookies }
    val client = remember(store, cookies) { store.createHttpClient(cookies) }
    DisposableEffect(client) {
        onDispose { client.close() }
    }
    val environment = remember(client, cookies, settingsStore) {
        JvmNotificationPaginationEnvironment(
            client = client,
            cookies = cookies,
            notificationSettingsStore = settingsStore,
            showMessage = userMessages::showMessage,
        )
    }
    return NotificationScreenData(
        notifications = viewModel.allData.filter { viewModel.shouldShowNotification(settingsStore, it) },
        totalItemCount = viewModel.allData.size,
        unreadCount = viewModel.unreadCount,
        isLoading = viewModel.isLoading,
        isEnd = viewModel.isEnd,
        showDebugCopy = false,
        refresh = { viewModel.refresh(environment) },
        loadMore = { viewModel.loadMore(environment) },
        markAsRead = { id -> viewModel.markAsRead(id) },
        markAllAsRead = {
            coroutineScope.launch {
                viewModel.markAllAsRead(environment)
                userMessages.showMessage("已全部标记为已读")
            }
        },
        copyDebugData = {
            val debugData = Json.encodeToString(viewModel.debugData)
            runCatching {
                Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(debugData), null)
            }
            userMessages.showMessage("已复制调试数据")
        },
        showMessage = { message -> userMessages.showMessage(message) },
    )
}

@Composable
actual fun NotificationDebugCopyButton(
    visible: Boolean,
    onClick: () -> Unit,
) {
}

private class JvmNotificationPaginationEnvironment(
    private val client: HttpClient,
    private val cookies: Map<String, String>,
    override val notificationSettingsStore: NotificationSettingsStore,
    private val showMessage: (String) -> Unit,
) : NotificationPaginationEnvironment {
    override fun httpClient(): HttpClient = client

    override suspend fun fetchJson(
        url: String,
        include: String,
    ): JsonObject =
        client
            .get(url) {
                if (include.isNotEmpty()) {
                    parameter("include", include)
                }
                configureSignedRequest(this)
            }.body()

    override fun logDecodeFailure(
        tag: String?,
        item: JsonElement,
        error: Exception,
    ) {
        println("${tag ?: "NotificationViewModel"} failed to decode item: $item")
        error.printStackTrace()
    }

    override suspend fun handleFetchFailure(
        tag: String?,
        error: Exception,
    ) {
        println("${tag ?: "NotificationViewModel"} failed to fetch notifications")
        error.printStackTrace()
        showMessage("加载失败: ${error.message}")
    }

    override fun configureSignedRequest(builder: HttpRequestBuilder) {
        builder.signZhihuFetchRequest(dc0 = cookies["d_c0"] ?: "")
    }
}
