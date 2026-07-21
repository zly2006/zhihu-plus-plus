package com.hrm.markdown.renderer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * 代码块诊断配置，用于控制变量法定位流式抖动来源。
 *
 * 通过 [LocalCodeBlockDiagConfig] 在组件树中传递，
 * [com.hrm.markdown.renderer.block.CodeBlockCanvas] 消费这些配置来切换不同的渲染路径。
 *
 * **仅用于开发调试，生产环境使用默认值即可。**
 */
@Immutable
data class CodeBlockDiagConfig(
    /** 开关③：替换代码块为固定高度的占位框（不渲染任何代码内容） */
    val usePlaceholder: Boolean = false,
    /** 开关④：去掉透明 Text 层（禁用选中复制能力，排除 Text 层对布局的影响） */
    val disableSelectionLayer: Boolean = false,
    /** 开关⑤：固定代码块高度（300dp），排除高度变化传导到外层的影响 */
    val useFixedHeight: Boolean = false,
)

val LocalCodeBlockDiagConfig = staticCompositionLocalOf { CodeBlockDiagConfig() }

@Composable
fun ProvideCodeBlockDiagConfig(
    config: CodeBlockDiagConfig,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(LocalCodeBlockDiagConfig provides config) {
        content()
    }
}
