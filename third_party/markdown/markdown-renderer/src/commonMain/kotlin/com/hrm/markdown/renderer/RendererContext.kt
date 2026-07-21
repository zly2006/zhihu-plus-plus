package com.hrm.markdown.renderer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import com.hrm.codehigh.theme.CodeTheme
import com.hrm.markdown.parser.ast.Document
import com.hrm.markdown.runtime.MarkdownDirectiveRegistry

/**
 * 链接点击回调，通过 [compositionLocalOf] 在组件树中传递。
 *
 * 使用非 static 版本，配合 [ProvideRendererContext] 中的 [rememberUpdatedState] 包装：
 * - Provider 始终提供同一个 lambda 包装器引用（只创建一次）
 * - 包装器内部通过 State 读取最新的 onLinkClick
 * - compositionLocalOf 比较引用发现没变 → **跳过所有下游重组**
 */
internal val LocalOnLinkClick = compositionLocalOf<((String) -> Unit)?> { null }
internal val LocalOnFootnoteClick = compositionLocalOf<((String) -> Unit)?> { null }
internal val LocalOnFootnoteBackClick = compositionLocalOf<((String) -> Unit)?> { null }

/**
 * 文档引用，通过 [compositionLocalOf] 在组件树中传递。
 * 使用非 static 版本是因为流式场景下 document 每个 token 都变化，
 * 但只有极少数组件（如 TocPlaceholderRenderer）需要读取它。
 * [compositionLocalOf] 只会通知**真正读取了它的组件**重组，
 * 而不会像 staticCompositionLocalOf 那样无条件使整个子树失效。
 */
internal val LocalRendererDocument = compositionLocalOf { Document() }

/**
 * Markdown 渲染配置，通过 CompositionLocal 传递。
 */
internal val LocalMarkdownConfig = compositionLocalOf { MarkdownConfig.Default }
internal val LocalFootnoteNavigationState = compositionLocalOf<FootnoteNavigationState?> { null }

internal val LocalCodeHighlightTheme = compositionLocalOf<CodeTheme?> { null }
internal val LocalIsStreaming = compositionLocalOf { false }
internal val LocalMarkdownDirectiveRegistry = compositionLocalOf { MarkdownDirectiveRegistry.Empty }

@Composable
internal fun ProvideRendererContext(
    document: Document,
    onLinkClick: ((String) -> Unit)?,
    onFootnoteClick: ((String) -> Unit)? = null,
    onFootnoteBackClick: ((String) -> Unit)? = null,
    footnoteNavigationState: FootnoteNavigationState? = null,
    imageContent: MarkdownImageRenderer? = null,
    config: MarkdownConfig = MarkdownConfig.Default,
    codeTheme: CodeTheme? = null,
    isStreaming: Boolean = false,
    directiveRegistry: MarkdownDirectiveRegistry = MarkdownDirectiveRegistry.Empty,
    content: @Composable () -> Unit,
) {
    // 用 rememberUpdatedState 包装 onLinkClick：
    // - stableOnLinkClick 始终是一个 remember 的稳定 lambda 引用（对象不变）
    // - 即使当前 onLinkClick 为 null，wrapper 也只是空操作，不会锁死后续更新
    // - wrapper 内部通过 State 读取始终最新的 onLinkClick
    // - compositionLocalOf 比较引用 === 发现没变 → 跳过下游重组
    val currentOnLinkClick = rememberUpdatedState(onLinkClick)
    val currentOnFootnoteClick = rememberUpdatedState(onFootnoteClick)
    val currentOnFootnoteBackClick = rememberUpdatedState(onFootnoteBackClick)
    val stableOnLinkClick: (String) -> Unit = remember {
        { url: String -> currentOnLinkClick.value?.invoke(url) }
    }
    val stableOnFootnoteClick: (String) -> Unit = remember {
        { label: String -> currentOnFootnoteClick.value?.invoke(label) }
    }
    val stableOnFootnoteBackClick: (String) -> Unit = remember {
        { label: String -> currentOnFootnoteBackClick.value?.invoke(label) }
    }

    CompositionLocalProvider(
        LocalOnLinkClick provides stableOnLinkClick,
        LocalOnFootnoteClick provides stableOnFootnoteClick,
        LocalOnFootnoteBackClick provides stableOnFootnoteBackClick,
        LocalRendererDocument provides document,
        LocalImageRenderer provides imageContent,
        LocalMarkdownConfig provides config,
        LocalFootnoteNavigationState provides footnoteNavigationState,
        LocalCodeHighlightTheme provides codeTheme,
        LocalIsStreaming provides isStreaming,
        LocalMarkdownDirectiveRegistry provides directiveRegistry,
    ) {
        content()
    }
}
