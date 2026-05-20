package com.github.zly2006.zhihu.shared.pin

import com.github.zly2006.zhihu.shared.data.DataHolder

data class PinScreenUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val pinContent: DataHolder.Pin? = null,
    val isLiked: Boolean = false,
    val likeCount: Int = 0,
)

data class PinLinkCardPreview(
    val title: String,
    val preview: String,
)
