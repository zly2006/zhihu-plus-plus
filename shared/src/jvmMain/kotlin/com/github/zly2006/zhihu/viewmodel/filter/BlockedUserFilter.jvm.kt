package com.github.zly2006.zhihu.viewmodel.filter

import com.github.zly2006.zhihu.viewmodel.PaginationEnvironment

// TODO: JVM 拉黑用户ID获取
actual fun PaginationEnvironment.fetchBlockedUserIds(): Set<String> = emptySet()
