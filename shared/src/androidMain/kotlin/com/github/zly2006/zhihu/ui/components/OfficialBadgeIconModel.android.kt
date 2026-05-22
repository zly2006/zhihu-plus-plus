package com.github.zly2006.zhihu.ui.components

import com.github.zly2006.zhihu.shared.R
import com.github.zly2006.zhihu.shared.data.DataHolder

actual fun officialBadgeIconModel(iconUrl: String): Any = if (iconUrl == DataHolder.ZH_PLUS_AUTHOR_BADGE_ICON) {
    R.drawable.ic_zh_plus_author_badge
} else {
    iconUrl
}
