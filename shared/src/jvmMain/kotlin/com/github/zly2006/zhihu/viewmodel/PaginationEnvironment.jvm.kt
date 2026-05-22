package com.github.zly2006.zhihu.viewmodel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.github.zly2006.zhihu.shared.desktop.DesktopAccountStore
import com.github.zly2006.zhihu.shared.util.signZhihuFetchRequest
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

class DesktopPaginationEnvironment(
    private val store: DesktopAccountStore = DesktopAccountStore(),
) : PaginationEnvironment {
    override fun httpClient(): HttpClient = store.createHttpClient(store.load().cookies)

    override suspend fun fetchJson(
        url: String,
        include: String,
    ): JsonObject {
        val account = store.load()
        return httpClient().use { client ->
            client
                .get(url) {
                    if (include.isNotEmpty()) {
                        parameter("include", include)
                    }
                    account.cookies["d_c0"]?.let { dc0 ->
                        signZhihuFetchRequest(dc0 = dc0)
                    }
                }.body()
        }
    }

    override fun logDecodeFailure(
        tag: String?,
        item: JsonElement,
        error: Exception,
    ) {
        println("${tag ?: "PaginationViewModel"} failed to decode item: $error")
    }

    override suspend fun handleFetchFailure(
        tag: String?,
        error: Exception,
    ) {
        println("${tag ?: "PaginationViewModel"} failed to fetch feeds: ${error.message}")
    }

    override fun feedDisplaySettings(): FeedDisplaySettings = FeedDisplaySettings(
        enableQualityFilter = false,
        reverseBlock = false,
    )
}

@Composable
actual fun rememberPaginationEnvironment(allowGuestAccess: Boolean): PaginationEnvironment =
    remember(allowGuestAccess) { DesktopPaginationEnvironment() }
