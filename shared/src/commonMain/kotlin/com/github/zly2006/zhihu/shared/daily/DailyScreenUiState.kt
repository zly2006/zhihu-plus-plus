package com.github.zly2006.zhihu.shared.daily

import com.github.zly2006.zhihu.shared.data.DailySection

data class DailyScreenUiState(
    val sections: List<DailySection> = emptyList(),
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
)
