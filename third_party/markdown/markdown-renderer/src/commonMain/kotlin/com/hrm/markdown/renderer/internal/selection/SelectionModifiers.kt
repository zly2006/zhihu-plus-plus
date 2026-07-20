package com.hrm.markdown.renderer.internal.selection

import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import com.hrm.markdown.renderer.LocalMarkdownTheme

/**
 * 把一个可选 inline block 接入自研选区层：
 * - [onGloballyPositioned] 把当前 block 的 [LayoutCoordinates] 注册到坐标表（支持 window↔local 换算）；
 * - [DisposableEffect] 在 block 滚出屏 / 离开组合时反注册（支持 LazyColumn 虚拟化）；
 * - [drawWithContent] 按控制器给出的 block-local 高亮矩形绘制选中背景。
 */
internal fun Modifier.selectableBlock(
    stableId: Long,
    controller: MarkdownSelectionController?,
): Modifier {
    if (controller == null) return this
    return this.composed {
        val highlightColor = LocalMarkdownTheme.current.linkColor.copy(alpha = 0.3f)
        DisposableEffect(stableId, controller) {
            onDispose { controller.registry.unregister(stableId) }
        }
        this
            .onGloballyPositioned { controller.registry.register(stableId, it) }
            .drawWithContent {
                drawContent()
                val highlightBoxes = if (controller.state.range == null) {
                    emptyList()
                } else {
                    controller.highlightBoxesFor(stableId)
                }
                for (box in highlightBoxes) {
                    drawRect(
                        color = highlightColor,
                        topLeft = Offset(box.left, box.top),
                        size = Size(box.width, box.height),
                    )
                }
            }
    }
}
