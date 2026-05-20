package com.github.zly2006.zhihu.shared.data

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable

@Serializable
class CollectionItem(
    val created: String,
    val content: Feed.Target,
)

@Stable
data class CollectionHtmlExportDialogState(
    val phaseText: String,
    val totalCount: Int,
    val processedCount: Int,
    val successCount: Int,
    val skippedCount: Int,
    val failedCount: Int,
    val currentTitle: String = "",
    val isIndeterminate: Boolean = false,
    val isCompleted: Boolean = false,
    val resultMessage: String? = null,
    val zipFilePath: String? = null,
) {
    val progress: Float
        get() = if (totalCount <= 0) 0f else (processedCount.toFloat() / totalCount.toFloat()).coerceIn(0f, 1f)
}
