package com.github.zly2006.zhihu.harmonyprobe

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import coil3.ImageLoader
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.network.ktor3.KtorNetworkFetcherFactory
import com.github.zly2006.zhihu.shared.data.fetchDailyStory
import com.github.zly2006.zhihu.shared.data.fetchLatestDailyStories
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

internal actual val usesNativeNetwork: Boolean = true

internal actual suspend fun loadNativeDaily() {
    val client = HttpClient(CIO) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }
    try {
        val home = client.fetchLatestDailyStories()
        P2State.home = home
        val story = home.stories.maxByOrNull {
            Regex("(\\d+)\\s*分钟阅读").find(it.hint)?.groupValues?.get(1)?.toIntOrNull() ?: 0
        } ?: error("日报首页为空")
        P2State.detail = client.fetchDailyStory(story.id)
    } finally {
        client.close()
    }
}

@Composable
internal actual fun P2Cover(url: String, description: String, modifier: Modifier) {
    val context = LocalPlatformContext.current
    val client = remember { HttpClient(CIO) }
    val loader = remember(context) {
        ImageLoader.Builder(context)
            .components { add(KtorNetworkFetcherFactory(httpClient = { client })) }
            .build()
    }
    DisposableEffect(loader, client) {
        onDispose {
            loader.shutdown()
            client.close()
        }
    }
    AsyncImage(model = url, imageLoader = loader, contentDescription = description, modifier = modifier)
}
