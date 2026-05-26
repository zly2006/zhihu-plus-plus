package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import com.github.zly2006.zhihu.shared.desktop.DesktopAccountStore
import com.github.zly2006.zhihu.shared.notification.NotificationSettingsStore
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.shared.util.Log
import com.github.zly2006.zhihu.shared.util.signZhihuFetchRequest
import com.github.zly2006.zhihu.viewmodel.NotificationPaginationEnvironment
import com.github.zly2006.zhihu.viewmodel.NotificationViewModel
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.parameter
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

@Composable
actual fun rememberNotificationScreenRuntime(
    viewModel: NotificationViewModel,
    settingsStore: NotificationSettingsStore,
): NotificationScreenRuntime {
    val userMessages = rememberUserMessageSink()
    val store = remember { DesktopAccountStore() }
    val cookies = remember(store) { store.load().cookies }
    val client = remember(store, cookies) { store.createHttpClient(cookies) }
    DisposableEffect(client) {
        onDispose { client.close() }
    }
    val environment = remember(store, client, cookies, settingsStore, userMessages) {
        JvmNotificationPaginationEnvironment(
            store = store,
            client = client,
            notificationSettingsStore = settingsStore,
            showMessage = userMessages::showMessage,
        )
    }
    return NotificationScreenRuntime(
        environment = environment,
        showDebugCopy = true,
        copyDebugText = { _, text ->
            runCatching {
                Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
            }
        },
    )
}

private class JvmNotificationPaginationEnvironment(
    private val store: DesktopAccountStore,
    private val client: HttpClient,
    override val notificationSettingsStore: NotificationSettingsStore,
    private val showMessage: (String) -> Unit,
) : NotificationPaginationEnvironment {
    override fun httpClient(): HttpClient = client

    override suspend fun fetchJson(
        url: String,
        include: String,
    ): JsonObject =
        store
            .fetchAuthenticatedJson(url) {
                if (include.isNotEmpty()) {
                    parameter("include", include)
                }
                configureSignedRequest(this)
            } ?: error("No notification response body")

    override fun logDecodeFailure(
        tag: String?,
        item: JsonElement,
        error: Exception,
    ) {
        Log.e(tag ?: "NotificationViewModel", "Failed to decode item: $item", error)
    }

    override suspend fun handleFetchFailure(
        tag: String?,
        error: Exception,
    ) {
        Log.e(tag ?: "NotificationViewModel", "Failed to fetch notifications", error)
        showMessage("加载失败: ${error.message}")
    }

    override fun configureSignedRequest(builder: HttpRequestBuilder) {
        builder.signZhihuFetchRequest(dc0 = store.load().cookies["d_c0"] ?: "")
    }
}
