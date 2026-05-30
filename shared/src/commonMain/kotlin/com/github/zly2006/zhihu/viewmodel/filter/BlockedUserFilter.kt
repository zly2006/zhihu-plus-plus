package com.github.zly2006.zhihu.viewmodel.filter

import com.github.zly2006.zhihu.viewmodel.PaginationEnvironment

/**
 * Returns the set of currently blocked user IDs for the given environment.
 */
fun PaginationEnvironment.fetchBlockedUserIds(): Set<String> = blockedUserIds()
