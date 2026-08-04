package com.hrm.markdown.renderer

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.hrm.codehigh.theme.CodeTheme
import com.hrm.markdown.parser.ast.Document
import com.hrm.markdown.runtime.MarkdownDirectiveRegistry

@Composable
internal fun MarkdownDocumentRenderer(
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
    deferOffscreenBlocks: Boolean = true,
    initialBlockCount: Int = 100,
    header: (@Composable () -> Unit)? = null,
    footer: (@Composable () -> Unit)? = null,
    imageContent: MarkdownImageRenderer? = null,
    onLinkClick: ((String) -> Unit)? = null,
    directiveRegistry: MarkdownDirectiveRegistry = MarkdownDirectiveRegistry.Empty,
) {
    val renderMode = remember(enableSelection, enableScroll, isStreaming) {
        resolveMarkdownRenderMode(
            enableSelection = enableSelection,
            enableScroll = enableScroll,
            isStreaming = isStreaming,
        )
    }
    val lazyListState = rememberLazyListState()
    val renderDocument = rememberRenderDocument(
        document = document,
        isStreaming = isStreaming,
    )
    val renderState = rememberMarkdownBlockRenderState(
        document = renderDocument,
        renderMode = renderMode,
        enablePagination = enablePagination,
        initialBlockCount = initialBlockCount,
        scrollState = scrollState,
        isStreaming = isStreaming,
        hasHeader = header != null,
    )
    val navigationHandlers = rememberMarkdownNavigationHandlers(
        renderMode = renderMode,
        enableScroll = enableScroll,
        scrollState = scrollState,
        lazyListState = lazyListState,
        renderState = renderState,
        onLinkClick = onLinkClick,
    )

    ProvideMarkdownTheme(theme) {
        ProvideRendererContext(
            document = renderDocument,
            onLinkClick = onLinkClick,
            onFootnoteClick = navigationHandlers.onFootnoteClick,
            onFootnoteBackClick = navigationHandlers.onFootnoteBackClick,
            footnoteNavigationState = navigationHandlers.footnoteNavigationState,
            imageContent = imageContent,
            config = config,
            codeTheme = codeTheme,
            isStreaming = isStreaming,
            directiveRegistry = directiveRegistry,
        ) {
            MarkdownDocumentLayout(
                renderMode = renderMode,
                renderState = renderState,
                modifier = modifier,
                enableScroll = enableScroll,
                scrollState = scrollState,
                lazyListState = lazyListState,
                deferOffscreenBlocks = deferOffscreenBlocks,
                header = header,
                footer = footer,
            )
        }
    }
}
