package com.hrm.markdown.renderer

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier

// Retains the upstream public image contract. The host supplies Coil on arm64 and
// NetworkKit on x64 through imageContent; no fictitious x64 Coil implementation.
@Immutable
data class MarkdownImageData(
    val url: String,
    val altText: String,
    val title: String? = null,
    val width: Int? = null,
    val height: Int? = null,
    val attributes: Map<String, String> = emptyMap(),
) {
    val align: String? get() = attributes["align"]
    val loading: String? get() = attributes["loading"]
    val cssClasses: List<String> get() = attributes["class"]?.split(" ")?.filter(String::isNotEmpty) ?: emptyList()
    val cssId: String? get() = attributes["id"]
}

typealias MarkdownImageRenderer = @Composable (data: MarkdownImageData, modifier: Modifier) -> Unit
internal val LocalImageRenderer = compositionLocalOf<MarkdownImageRenderer?> { null }

@Composable
internal fun DefaultMarkdownImage(data: MarkdownImageData, modifier: Modifier = Modifier) {
    Text("图片加载器未配置：${data.altText.ifBlank { data.url }}", modifier)
}
