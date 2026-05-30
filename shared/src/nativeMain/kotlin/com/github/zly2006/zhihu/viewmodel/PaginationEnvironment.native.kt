package com.github.zly2006.zhihu.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

// TODO: iOS 分页环境完整实现
@Composable
actual fun rememberPaginationEnvironment(allowGuestAccess: Boolean): PaginationEnvironment =
    remember(allowGuestAccess) { IosPaginationEnvironment() }

private class IosPaginationEnvironment : PaginationEnvironment {
    override fun httpClient(): HttpClient = error("HTTP client not available on iOS") // TODO: iOS HTTP 客户端

    override suspend fun fetchJson(url: String, include: String): JsonObject? = null // TODO: iOS JSON 数据获取

    override fun logDecodeFailure(tag: String?, item: JsonElement, error: Exception) = Unit // TODO: iOS 解码失败日志

    override suspend fun handleFetchFailure(tag: String?, error: Exception) = Unit // TODO: iOS 获取失败处理

    override fun configureSignedRequest(builder: HttpRequestBuilder) = Unit // TODO: iOS 签名请求配置
}
