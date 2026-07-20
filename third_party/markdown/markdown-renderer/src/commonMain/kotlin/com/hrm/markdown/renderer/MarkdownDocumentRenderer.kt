package com.hrm.markdown.renderer

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.rememberTextMeasurer
import com.hrm.codehigh.theme.CodeTheme
import com.hrm.latex.renderer.measure.rememberLatexMeasurer
import com.hrm.markdown.parser.ast.Document
import com.hrm.markdown.renderer.internal.MarkdownEngineHost
import com.hrm.markdown.renderer.internal.RendererFacadeState
import com.hrm.markdown.renderer.internal.compose.ComposeRenderEnvironment
import com.hrm.markdown.renderer.internal.selection.LocalMarkdownSelectionController
import com.hrm.markdown.renderer.internal.selection.SelectionToolbarHost
import com.hrm.markdown.renderer.internal.selection.markdownSelectionGestures
import com.hrm.markdown.renderer.internal.selection.rememberMarkdownSelectionController
import com.hrm.markdown.renderer.internal.selection.buildSelectionDocumentIndex
import com.hrm.markdown.runtime.MarkdownDirectiveRegistry

@Composable
internal fun MarkdownDocumentRenderer(
    document: Document,
    modifier: Modifier = Modifier,
    theme: MarkdownTheme = MarkdownTheme.auto(),
    codeTheme: CodeTheme? = null,
    config: MarkdownConfig = MarkdownConfig.Default,
    scrollState: ScrollState = rememberScrollState(),
    lazyListState: LazyListState = rememberLazyListState(),
    isStreaming: Boolean = false,
    enableScroll: Boolean = true,
    enableSelection: Boolean = true,
    header: (@Composable () -> Unit)? = null,
    footer: (@Composable () -> Unit)? = null,
    imageContent: MarkdownImageRenderer? = null,
    onLinkClick: ((String) -> Unit)? = null,
    directiveRegistry: MarkdownDirectiveRegistry = MarkdownDirectiveRegistry.Empty,
) {
    val renderMode = remember(enableScroll) {
        resolveMarkdownRenderMode(enableScroll = enableScroll)
    }
    val renderDocument = rememberRenderDocument(
        document = document,
        isStreaming = isStreaming,
    )
    ProvideMarkdownTheme(theme) {
        val engineHost = remember { MarkdownEngineHost() }
        val facadeState = remember(
            theme,
            config,
            codeTheme,
            imageContent,
            onLinkClick,
            directiveRegistry,
            isStreaming,
            enableSelection,
        ) {
            RendererFacadeState(
                theme = theme,
                config = config,
                codeTheme = codeTheme,
                imageRenderer = imageContent,
                onLinkClick = onLinkClick,
                directiveRegistry = directiveRegistry,
                isStreaming = isStreaming,
                enableSelection = enableSelection,
            )
        }
        val renderCatalog = remember(engineHost, renderDocument, facadeState) {
            engineHost.createCatalog(renderDocument, facadeState)
        }
        val density = LocalDensity.current
        val latexMeasurer = rememberLatexMeasurer()
        val diagramHostRegistry = remember { DiagramHostRegistry() }
        BoxWithConstraints(modifier = modifier) {
            val viewportWidthPx = with(density) { maxWidth.toPx() }
            val blockSpacingPx = with(density) { theme.blockSpacing.toPx() }
            val textMeasurer = rememberTextMeasurer()
            val selectionController = if (enableSelection) {
                val selectionScope = rememberCoroutineScope()
                rememberMarkdownSelectionController(selectionScope, textMeasurer)
            } else {
                null
            }
            selectionController?.bindClipboard(LocalClipboard.current)
            val navigationController = rememberMarkdownNavigationController(
                renderMode = renderMode,
                enableScroll = enableScroll,
                scrollState = scrollState,
                lazyListState = lazyListState,
                onLinkClick = onLinkClick,
            )
            val internalRenderDocument = remember(
                engineHost,
                renderCatalog,
                renderMode,
                facadeState,
            ) {
                if (renderMode == MarkdownRenderMode.StaticColumn) {
                    engineHost.compile(renderDocument, facadeState)
                } else {
                    null
                }
            }
            val layoutDocument = remember(
                engineHost,
                internalRenderDocument,
                renderMode,
                facadeState,
                viewportWidthPx,
                blockSpacingPx,
                density,
                textMeasurer,
                latexMeasurer,
                diagramHostRegistry,
            ) {
                internalRenderDocument?.let { staticDocument ->
                    engineHost.layout(
                        renderDocument = staticDocument,
                        facadeState = facadeState,
                        viewportWidth = viewportWidthPx,
                        blockSpacing = blockSpacingPx,
                        onLinkClick = navigationController.linkClickDelegate,
                        onFootnoteClick = navigationController.onFootnoteClick,
                        density = density,
                        textMeasurer = textMeasurer,
                        latexMeasurer = latexMeasurer,
                        diagramHostRegistry = diagramHostRegistry,
                    )
                }
            }
            val lazyLayoutSession = remember(
                engineHost,
                renderCatalog,
                renderMode,
                facadeState,
                viewportWidthPx,
                blockSpacingPx,
                density,
                textMeasurer,
                latexMeasurer,
                diagramHostRegistry,
                navigationController,
            ) {
                if (renderMode == MarkdownRenderMode.LazyColumn) {
                    engineHost.createLazyLayoutSession(
                        catalog = renderCatalog,
                        facadeState = facadeState,
                        viewportWidth = viewportWidthPx,
                        blockSpacing = blockSpacingPx,
                        onLinkClick = navigationController.linkClickDelegate,
                        onFootnoteClick = navigationController.onFootnoteClick,
                        density = density,
                        textMeasurer = textMeasurer,
                        latexMeasurer = latexMeasurer,
                        diagramHostRegistry = diagramHostRegistry,
                    )
                } else {
                    null
                }
            }
            navigationController.footnoteDefinitionItemIndexes = when (renderMode) {
                MarkdownRenderMode.LazyColumn -> renderCatalog.metadata.footnoteDefinitions.mapNotNull { footnote ->
                    renderCatalog.itemIndexOf(footnote.identity.stableId)?.let { index ->
                        footnote.label to index + if (header != null) 1 else 0
                    }
                }.toMap()

                MarkdownRenderMode.StaticColumn -> layoutDocument?.metadata?.footnoteDefinitionItemIndexes.orEmpty()
            }
            if (selectionController != null) {
                val selectionDocumentIndex = remember(renderDocument) {
                    buildSelectionDocumentIndex(renderDocument)
                }
                LaunchedEffect(renderMode, selectionDocumentIndex, layoutDocument) {
                    if (renderMode == MarkdownRenderMode.LazyColumn) {
                        selectionController.updateDocumentIndex(selectionDocumentIndex)
                    } else {
                        layoutDocument?.let { selectionController.updateIndex(it.blocks) }
                    }
                }
            }
            Box(
                modifier = if (selectionController != null) {
                    Modifier
                        .onGloballyPositioned { selectionController.registry.setRoot(it) }
                        .markdownSelectionGestures(selectionController)
                } else {
                    Modifier
                },
            ) {
                CompositionLocalProvider(
                    LocalMarkdownSelectionController provides selectionController,
                ) {
                    ProvideRendererContext(
                        document = renderDocument,
                        onLinkClick = onLinkClick,
                        onFootnoteClick = navigationController.onFootnoteClick,
                        onFootnoteBackClick = navigationController.onFootnoteBackClick,
                        footnoteNavigationState = navigationController.footnoteNavigationState,
                        imageContent = imageContent,
                        config = config,
                        codeTheme = codeTheme,
                        isStreaming = isStreaming,
                        directiveRegistry = directiveRegistry,
                        diagramHostRegistry = diagramHostRegistry,
                    ) {
                        val composeEnvironment = ComposeRenderEnvironment(
                            modifier = Modifier.fillMaxWidth(),
                            renderMode = renderMode,
                            enableScroll = enableScroll,
                            scrollState = scrollState,
                            lazyListState = lazyListState,
                            header = header,
                            footer = footer,
                        )
                        when (renderMode) {
                            MarkdownRenderMode.LazyColumn -> engineHost.composePainter.PaintLazy(
                                documentRevision = renderCatalog.documentIdentity.contentRevision,
                                blockCount = renderCatalog.size,
                                blockKeyAt = { index -> renderCatalog.identityAt(index).stableId },
                                blockAt = { index -> lazyLayoutSession?.layout(index) },
                                environment = composeEnvironment,
                            )

                            MarkdownRenderMode.StaticColumn -> layoutDocument?.let { staticDocument ->
                                engineHost.composePainter.Paint(
                                    document = staticDocument,
                                    environment = composeEnvironment,
                                )
                            }
                        }
                    }
                }
                if (selectionController != null) {
                    SelectionToolbarHost(selectionController)
                }
            }
        }
    }
}
