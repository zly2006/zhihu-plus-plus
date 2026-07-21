package com.hrm.markdown.renderer

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.launch

@Stable
internal data class MarkdownNavigationHandlers(
    val footnoteNavigationState: FootnoteNavigationState,
    val onFootnoteClick: (String) -> Unit,
    val onFootnoteBackClick: (String) -> Unit,
)

@Composable
internal fun rememberMarkdownNavigationHandlers(
    renderMode: MarkdownRenderMode,
    enableScroll: Boolean,
    scrollState: ScrollState,
    lazyListState: LazyListState,
    renderState: MarkdownBlockRenderState,
    onLinkClick: ((String) -> Unit)?,
): MarkdownNavigationHandlers {
    val footnoteNavigationState = remember { FootnoteNavigationState() }
    val coroutineScope = rememberCoroutineScope()
    val currentOnLinkClick = rememberUpdatedState(onLinkClick)
    val currentScrollState = rememberUpdatedState(scrollState)
    val currentLazyListState = rememberUpdatedState(lazyListState)
    val currentRenderState = rememberUpdatedState(renderState)

    val onFootnoteClick = remember(
        footnoteNavigationState,
        renderMode,
        enableScroll,
        scrollState,
        lazyListState,
        renderState.effectivePagination,
        renderState.footnoteDefinitionItemIndexes,
    ) {
        { label: String ->
            coroutineScope.launch {
                when (renderMode) {
                    MarkdownRenderMode.LazyColumn -> {
                        val lazyState = currentLazyListState.value
                        footnoteNavigationState.rememberLazyListPosition(
                            label = label,
                            index = lazyState.firstVisibleItemIndex,
                            offset = lazyState.firstVisibleItemScrollOffset,
                        )
                        val targetIndex = currentRenderState.value.footnoteDefinitionItemIndexes[label]
                        if (enableScroll && targetIndex != null) {
                            lazyState.animateScrollToItem(targetIndex)
                            withFrameNanos { }
                        }
                        if (!footnoteNavigationState.bringDefinitionIntoView(label)) {
                            currentOnLinkClick.value?.invoke("#fn-$label")
                        }
                    }

                    else -> {
                        footnoteNavigationState.rememberReturnPosition(label, currentScrollState.value.value)

                        if (currentRenderState.value.effectivePagination && !footnoteNavigationState.hasDefinition(label)) {
                            currentRenderState.value.expandAllBlocks()
                            withFrameNanos { }
                        }

                        if (!footnoteNavigationState.hasDefinition(label)) {
                            footnoteNavigationState.requestDefinition(label)
                            withFrameNanos { }
                        }

                        val broughtIntoView = footnoteNavigationState.bringDefinitionIntoView(label)
                        footnoteNavigationState.clearDefinitionRequest(label)
                        if (!broughtIntoView) {
                            currentOnLinkClick.value?.invoke("#fn-$label")
                        }
                    }
                }
            }
            Unit
        }
    }
    val onFootnoteBackClick = remember(footnoteNavigationState, enableScroll, renderMode, scrollState, lazyListState) {
        { label: String ->
            coroutineScope.launch {
                val returnPosition = footnoteNavigationState.getReturnPosition(label)
                if (returnPosition != null && enableScroll) {
                    when (returnPosition) {
                        is FootnoteReturnPosition.Scroll ->
                            currentScrollState.value.animateScrollTo(returnPosition.value)

                        is FootnoteReturnPosition.LazyList ->
                            currentLazyListState.value.animateScrollToItem(returnPosition.index, returnPosition.offset)
                    }
                    withFrameNanos { }
                    footnoteNavigationState.bringReferenceIntoView(label)
                    return@launch
                }

                if (!footnoteNavigationState.bringReferenceIntoView(label)) {
                    footnoteNavigationState.requestReference(label)
                    withFrameNanos { }
                    footnoteNavigationState.bringReferenceIntoView(label)
                    footnoteNavigationState.clearReferenceRequest(label)
                }
            }
            Unit
        }
    }

    return MarkdownNavigationHandlers(
        footnoteNavigationState = footnoteNavigationState,
        onFootnoteClick = onFootnoteClick,
        onFootnoteBackClick = onFootnoteBackClick,
    )
}
