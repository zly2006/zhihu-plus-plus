package com.hrm.markdown.renderer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import com.hrm.markdown.parser.MarkdownParser
import com.hrm.markdown.parser.ast.Document
import com.hrm.markdown.runtime.MarkdownDirectivePipeline
import com.hrm.markdown.runtime.MarkdownDirectiveRegistry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * 流式文档解析状态。
 * - 流式模式：增量追加解析，避免闪烁
 * - 非流式模式：全量异步解析
 */
@Composable
internal fun rememberStreamingDocument(
    markdown: String,
    isStreaming: Boolean,
    config: MarkdownConfig = MarkdownConfig.Default,
    runtimePipeline: MarkdownDirectivePipeline = MarkdownDirectivePipeline(MarkdownDirectiveRegistry.Empty),
): Document? {
    val parser = remember(config) {
        MarkdownParser(
            flavour = config.flavour,
            customEmojiMap = config.customEmojiMap,
            enableAsciiEmoticons = config.enableAsciiEmoticons,
            enableLinting = config.enableLinting,
            appendCoalesceThreshold = config.appendCoalesceThreshold,
        )
    }
    var state by remember(parser, runtimePipeline) { mutableStateOf(StreamingDocumentState<Document>()) }

    LaunchedEffect(markdown, isStreaming, parser, runtimePipeline) {
        state = updateStreamingDocumentState(
            markdown = markdown,
            isStreaming = isStreaming,
            state = state,
            beginStream = parser::beginStream,
            append = parser::append,
            endStream = parser::endStream,
            parse = { value ->
                withContext(Dispatchers.Default) {
                    parser.parse(runtimePipeline.transform(value).markdown)
                }
            }
        )
    }

    return state.document
}

@Composable
internal fun rememberRenderDocument(
    document: Document,
    isStreaming: Boolean,
): Document {
    val latestDocument by rememberUpdatedState(document)
    var throttledDocument by remember { mutableStateOf(document) }

    LaunchedEffect(isStreaming) {
        if (!isStreaming) {
            throttledDocument = latestDocument
            return@LaunchedEffect
        }

        while (true) {
            withFrameNanos { }
            delay(16L)
            val upstream = latestDocument
            if (upstream !== throttledDocument) {
                throttledDocument = upstream
            }
        }
    }

    // 仅在流式期间使用节流后的 document；流结束后直接消费最终 document，
    // 避免 endStream() 后 renderer 仍停留在旧的流式快照。
    return if (isStreaming) throttledDocument else document
}

internal data class StreamingDocumentState<T>(
    val lastParsedLength: Int = 0,
    val document: T? = null,
    val wasStreaming: Boolean = false,
    val lastNonStreamingMarkdown: String = "",
)

internal suspend fun <T> updateStreamingDocumentState(
    markdown: String,
    isStreaming: Boolean,
    state: StreamingDocumentState<T>,
    beginStream: () -> Unit,
    append: (String) -> T,
    endStream: () -> T,
    parse: suspend (String) -> T?,
): StreamingDocumentState<T> {
    var nextState = state

    if (isStreaming && !nextState.wasStreaming) {
        beginStream()
        nextState = nextState.copy(
            lastParsedLength = 0,
            document = null,
            wasStreaming = true,
        )
    }

    if (isStreaming) {
        if (markdown.length > nextState.lastParsedLength) {
            val chunk = markdown.substring(nextState.lastParsedLength)
            if (chunk.isNotEmpty()) {
                nextState = nextState.copy(
                    document = append(chunk),
                    lastParsedLength = markdown.length,
                )
            }
        }
        return nextState.copy(wasStreaming = true)
    }

    if (nextState.wasStreaming) {
        if (markdown.length > nextState.lastParsedLength) {
            val chunk = markdown.substring(nextState.lastParsedLength)
            if (chunk.isNotEmpty()) {
                nextState = nextState.copy(
                    document = append(chunk),
                    lastParsedLength = markdown.length,
                )
            }
        }
        return nextState.copy(
            document = endStream(),
            lastParsedLength = markdown.length,
            wasStreaming = false,
            lastNonStreamingMarkdown = markdown,
        )
    }

    if (markdown == nextState.lastNonStreamingMarkdown) {
        return nextState.copy(wasStreaming = false)
    }

    return nextState.copy(
        document = parse(markdown),
        lastParsedLength = markdown.length,
        wasStreaming = false,
        lastNonStreamingMarkdown = markdown,
    )
}
