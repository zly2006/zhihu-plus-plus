package com.hrm.markdown.renderer

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.neverEqualPolicy
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import com.hrm.markdown.parser.ast.BlankLine
import com.hrm.markdown.parser.ast.Document
import com.hrm.markdown.parser.ast.FootnoteDefinition
import com.hrm.markdown.parser.ast.Node
import com.hrm.markdown.parser.log.HLog
import com.hrm.markdown.renderer.block.blockRenderRevision

private const val TAG_RENDER = "MarkdownRender"

@Stable
internal data class MarkdownBlockRenderState(
    val blockNodes: List<Node>,
    val renderBlocks: List<Node>,
    val effectivePagination: Boolean,
    val footnoteDefinitionItemIndexes: Map<String, Int>,
    val expandAllBlocks: () -> Unit,
)

@Composable
internal fun rememberMarkdownBlockRenderState(
    document: Document,
    renderMode: MarkdownRenderMode,
    enablePagination: Boolean,
    initialBlockCount: Int,
    scrollState: ScrollState,
    isStreaming: Boolean,
    hasHeader: Boolean,
): MarkdownBlockRenderState {
    // 使用结构性比较缓存 blockNodes：
    // 每次 token 到达都产生新的 Document 对象，但大部分 children 的引用没变。
    // 流式期间仅在块结构变化，或同一块的 renderRevision 变化时才刷新状态，
    // 避免每个 token 都强制推整列 blockNodes。
    val blockNodesState = remember { mutableStateOf(emptyList<Node>(), neverEqualPolicy()) }
    val blockNodeRevisionsState = remember { mutableStateOf(emptyList<Long>()) }
    val newChildren = document.children
    val newFiltered = newChildren.filter { it !is BlankLine }
    val currentList = blockNodesState.value
    val newRevisions = newFiltered.map(::blockRenderRevision)
    val shouldRefreshBlockNodes = !structurallyEqual(currentList, newFiltered) ||
        !revisionsEqual(blockNodeRevisionsState.value, newRevisions)
    if (shouldRefreshBlockNodes) {
        HLog.d(TAG_RENDER) { "blockNodes updated: ${currentList.size} -> ${newFiltered.size}" }
        blockNodesState.value = newFiltered.toList()
        blockNodeRevisionsState.value = newRevisions
    }

    val blockNodes = blockNodesState.value
    val effectivePagination = enablePagination && renderMode != MarkdownRenderMode.LazyColumn
    val paginationStateKey = if (effectivePagination && !isStreaming) document else Unit
    var visibleBlockCount by remember(paginationStateKey, initialBlockCount) {
        mutableIntStateOf(initialVisibleBlockCount(initialBlockCount, blockNodes.size))
    }
    val effectiveVisibleBlockCount = visibleBlockCount.coerceAtMost(blockNodes.size)

    // 监听滚动位置，接近底部时自动加载更多块
    LaunchedEffect(scrollState, effectivePagination, blockNodes.size) {
        if (!effectivePagination) return@LaunchedEffect

        snapshotFlow {
            if (scrollState.maxValue > 0) {
                scrollState.value.toFloat() / scrollState.maxValue.toFloat()
            } else {
                0f
            }
        }.collect { scrollProgress ->
            // 滚动到 80% 时加载下一批
            if (scrollProgress > 0.8f && visibleBlockCount < blockNodes.size) {
                val increment = 50 // 每次加载 50 个块
                visibleBlockCount = (visibleBlockCount + increment).coerceAtMost(blockNodes.size)
            }
        }
    }

    val renderBlocks: List<Node> = run {
        if (!effectivePagination || effectiveVisibleBlockCount >= blockNodes.size) return@run blockNodes
        if (effectiveVisibleBlockCount <= 0) return@run emptyList()
        blockNodes.subList(0, effectiveVisibleBlockCount)
    }
    val footnoteDefinitionItemIndexes = remember(blockNodes, hasHeader, renderMode) {
        if (renderMode != MarkdownRenderMode.LazyColumn) {
            emptyMap()
        } else {
            buildMap {
                val headerOffset = if (hasHeader) 1 else 0
                blockNodes.forEachIndexed { index, node ->
                    if (node is FootnoteDefinition) {
                        put(node.label, index + headerOffset)
                    }
                }
            }
        }
    }
    val currentBlockCount = rememberUpdatedState(blockNodes.size)
    val expandAllBlocks: () -> Unit = remember {
        { visibleBlockCount = currentBlockCount.value }
    }

    return MarkdownBlockRenderState(
        blockNodes = blockNodes,
        renderBlocks = renderBlocks,
        effectivePagination = effectivePagination,
        footnoteDefinitionItemIndexes = footnoteDefinitionItemIndexes,
        expandAllBlocks = expandAllBlocks,
    )
}

/**
 * 结构性比较两个节点列表：
 * - 长度相同
 * - 每个位置的节点引用相同（=== 引用比较）
 *
 * 这比 `remember(document)` 更精确：当 Document 对象每次都是新的，
 * 但 children 列表的结构（对象引用）没变时，返回 true → 避免不必要的重组。
 */
private fun structurallyEqual(a: List<Node>, b: List<Node>): Boolean {
    if (a.size != b.size) return false
    for (i in a.indices) {
        if (a[i] !== b[i]) return false
    }
    return true
}

private fun revisionsEqual(a: List<Long>, b: List<Long>): Boolean {
    if (a.size != b.size) return false
    for (i in a.indices) {
        if (a[i] != b[i]) return false
    }
    return true
}

private fun initialVisibleBlockCount(initialBlockCount: Int, totalBlockCount: Int): Int {
    return initialBlockCount.coerceAtLeast(0).coerceAtMost(totalBlockCount.coerceAtLeast(0))
}
