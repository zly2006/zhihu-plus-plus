package com.hrm.markdown.renderer.internal.selection

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** 选区当前由哪一端在跟随手指延展。 */
internal enum class SelectionActiveHandle { None, Start, End }

/**
 * 选区运行期状态：仅保存归一化后的逻辑范围与活动端点，
 * 不持有任何屏幕坐标，便于在滚动 / 重排后保持有效。
 */
@Stable
internal class MarkdownSelectionState {
    var range: SelectionRange? by mutableStateOf(null)
    var activeHandle: SelectionActiveHandle by mutableStateOf(SelectionActiveHandle.None)
    var toolbarRequestKey: Int by mutableStateOf(0)

    val hasSelection: Boolean get() = range != null

    fun clear() {
        range = null
        activeHandle = SelectionActiveHandle.None
        toolbarRequestKey = 0
    }
}
