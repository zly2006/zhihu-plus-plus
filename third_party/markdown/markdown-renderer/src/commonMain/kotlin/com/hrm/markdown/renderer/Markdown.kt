package com.hrm.markdown.renderer

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.hrm.codehigh.theme.CodeTheme
import com.hrm.markdown.parser.ast.Document
import com.hrm.markdown.runtime.MarkdownDirectivePlugin
import com.hrm.markdown.runtime.MarkdownDirectiveRegistry
import com.hrm.markdown.runtime.MarkdownDirectivePipeline

/**
 * Markdown 渲染器的顶层 Composable 入口。
 *
 * 自动异步解析 Markdown 文本并渲染。
 *
 * 普通用法：
 * ```
 * Markdown(markdown = "# Hello World")
 * ```
 *
 * 流式用法（LLM 逐 token 输出场景）：
 * ```
 * var text by remember { mutableStateOf("") }
 * var isStreaming by remember { mutableStateOf(true) }
 *
 * LaunchedEffect(Unit) {
 *     tokens.collect { token ->
 *         text += token
 *     }
 *     isStreaming = false
 * }
 *
 * Markdown(markdown = text, isStreaming = isStreaming)
 * ```
 *
 * @param markdown 原始 Markdown 文本
 * @param isStreaming 是否处于流式生成中。为 true 时启用增量解析，避免全量重解析导致的闪烁
 * @param theme 可选的自定义主题，默认跟随系统日夜间模式
 * @param config 解析配置，控制 Markdown 方言（Flavour）和解析行为，默认使用 [MarkdownConfig.Default]（ExtendedFlavour 全功能）
 * @param scrollState 滚动状态，外部可控制滚动位置
 * @param enablePagination 是否启用分页加载，适合超长文档（> 500 段落）
 * @param enableScroll 是否启用 Markdown 内部滚动容器
 * @param enableSelection 是否启用文本选择。关闭后会在非流式场景自动切换为 LazyColumn 渲染长文档
 * @param initialBlockCount 分页模式下初始渲染的块数量
 * @param header Markdown 内容前方插槽，会和正文处于同一滚动容器中
 * @param footer Markdown 内容后方插槽，会和正文处于同一滚动容器中
 * @param imageContent 自定义图片渲染组件，null 则使用默认占位渲染
 * @param onLinkClick 链接点击回调
 * @param directivePlugins Markdown 指令插件列表，用于接入输入转换器和 directive 自定义渲染器
 */
@Composable
fun Markdown(
    markdown: String,
    modifier: Modifier = Modifier,
    theme: MarkdownTheme = MarkdownTheme.auto(),
    codeTheme: CodeTheme? = null,
    config: MarkdownConfig = MarkdownConfig.Default,
    scrollState: ScrollState = rememberScrollState(),
    isStreaming: Boolean = false,
    enablePagination: Boolean = false,
    enableScroll: Boolean = true,
    enableSelection: Boolean = true,
    initialBlockCount: Int = 100,
    header: (@Composable () -> Unit)? = null,
    footer: (@Composable () -> Unit)? = null,
    imageContent: MarkdownImageRenderer? = null,
    onLinkClick: ((String) -> Unit)? = null,
    directivePlugins: List<MarkdownDirectivePlugin> = emptyList(),
) {
    val directiveRegistry = remember(directivePlugins) { MarkdownDirectiveRegistry(directivePlugins) }
    val runtimePipeline = remember(directiveRegistry) { MarkdownDirectivePipeline(directiveRegistry) }
    val effectiveStreaming = isStreaming && runtimePipeline.supportsStreamingFastPath
    val document = rememberStreamingDocument(
        markdown = markdown,
        isStreaming = effectiveStreaming,
        config = config,
        runtimePipeline = runtimePipeline,
    )

    if (document == null) {
        if (effectiveStreaming) return
        MarkdownLoading(modifier = modifier)
    } else {
        MarkdownDocumentRenderer(
            document = document,
            modifier = modifier,
            theme = theme,
            codeTheme = codeTheme,
            config = config,
            scrollState = scrollState,
            isStreaming = effectiveStreaming,
            enablePagination = enablePagination,
            enableScroll = enableScroll,
            enableSelection = enableSelection,
            initialBlockCount = initialBlockCount,
            header = header,
            footer = footer,
            imageContent = imageContent,
            onLinkClick = onLinkClick,
            directiveRegistry = directiveRegistry,
        )
    }
}

/**
 * Markdown 渲染器的顶层 Composable 入口，支持传入自定义 AST，供需要高度定制的场景使用。
 *
 * @param document Markdown AST
 * @param isStreaming 是否处于流式生成中。为 true 时启用增量解析，避免全量重解析导致的闪烁
 * @param theme 可选的自定义主题，默认跟随系统日夜间模式
 * @param config 解析配置，控制 Markdown 方言（Flavour）和解析行为，默认使用 [MarkdownConfig.Default]（ExtendedFlavour 全功能）
 * @param scrollState 滚动状态，外部可控制滚动位置
 * @param enablePagination 是否启用分页加载，适合超长文档（> 500 段落）
 * @param enableScroll 是否启用 Markdown 内部滚动容器
 * @param enableSelection 是否启用文本选择。关闭后会在非流式场景自动切换为 LazyColumn 渲染长文档
 * @param initialBlockCount 分页模式下初始渲染的块数量
 * @param header Markdown 内容前方插槽，会和正文处于同一滚动容器中
 * @param footer Markdown 内容后方插槽，会和正文处于同一滚动容器中
 * @param imageContent 自定义图片渲染组件，null 则使用默认占位渲染
 * @param onLinkClick 链接点击回调
 * @param directivePlugins Markdown 指令插件列表，用于接入 directive 自定义渲染器
 */
@Composable
fun Markdown(
    document: Document,
    modifier: Modifier = Modifier,
    theme: MarkdownTheme = MarkdownTheme.auto(),
    codeTheme: CodeTheme? = null,
    config: MarkdownConfig = MarkdownConfig.Default,
    scrollState: ScrollState = rememberScrollState(),
    isStreaming: Boolean = false,
    enablePagination: Boolean = false,
    enableScroll: Boolean = true,
    enableSelection: Boolean = true,
    initialBlockCount: Int = 100,
    header: (@Composable () -> Unit)? = null,
    footer: (@Composable () -> Unit)? = null,
    imageContent: MarkdownImageRenderer? = null,
    onLinkClick: ((String) -> Unit)? = null,
    directivePlugins: List<MarkdownDirectivePlugin> = emptyList(),
) {
    val directiveRegistry = remember(directivePlugins) { MarkdownDirectiveRegistry(directivePlugins) }
    MarkdownDocumentRenderer(
        document = document,
        modifier = modifier,
        theme = theme,
        codeTheme = codeTheme,
        config = config,
        scrollState = scrollState,
        isStreaming = isStreaming,
        enablePagination = enablePagination,
        enableScroll = enableScroll,
        enableSelection = enableSelection,
        initialBlockCount = initialBlockCount,
        header = header,
        footer = footer,
        imageContent = imageContent,
        onLinkClick = onLinkClick,
        directiveRegistry = directiveRegistry,
    )
}

@Composable
private fun MarkdownLoading(
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}
