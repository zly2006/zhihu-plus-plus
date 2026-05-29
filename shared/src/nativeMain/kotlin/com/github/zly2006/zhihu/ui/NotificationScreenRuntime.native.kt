package com.github.zly2006.zhihu.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.github.zly2006.zhihu.shared.notification.NotificationSettingsStore
import com.github.zly2006.zhihu.viewmodel.NotificationPaginationEnvironment
import com.github.zly2006.zhihu.viewmodel.NotificationViewModel
import io.ktor.client.request.HttpRequestBuilder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

// TODO: iOS 通知页面完整实现
@Composable
actual fun rememberNotificationScreenRuntime(
    viewModel: NotificationViewModel,
    settingsStore: NotificationSettingsStore,
): NotificationScreenRuntime = remember(settingsStore) {
    NotificationScreenRuntime(
        environment = IosNotificationPaginationEnvironment(settingsStore),
        showDebugCopy = false,
        copyDebugText = { _, _ -> },
    )
}

private class IosNotificationPaginationEnvironment(
    override val notificationSettingsStore: NotificationSettingsStore,
) : NotificationPaginationEnvironment {
    // TODO: iOS HTTP 客户端
    override fun httpClient() = error("HTTP client not available on iOS yet")

    // TODO: iOS 通知数据获取
    override suspend fun fetchJson(url: String, include: String): JsonObject = error("fetchJson not available on iOS yet")

    // TODO: iOS 解码失败日志
    override fun logDecodeFailure(tag: String?, item: JsonElement, error: Exception) = Unit

    // TODO: iOS 获取失败处理
    override suspend fun handleFetchFailure(tag: String?, error: Exception) = Unit

    // TODO: iOS 签名请求配置
    override fun configureSignedRequest(builder: HttpRequestBuilder) = Unit
}
