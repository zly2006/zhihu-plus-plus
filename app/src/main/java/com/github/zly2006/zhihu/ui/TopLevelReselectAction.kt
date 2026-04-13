package com.github.zly2006.zhihu.ui

enum class TopLevelReselectAction {
    ScrollToTop,
    Refresh,
}

fun topLevelReselectAction(
    triggerDelta: Int,
    isAtTop: Boolean,
): TopLevelReselectAction? = when {
    triggerDelta <= 0 -> null
    triggerDelta >= 2 || isAtTop -> TopLevelReselectAction.Refresh
    else -> TopLevelReselectAction.ScrollToTop
}
