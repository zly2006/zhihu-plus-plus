package com.github.zly2006.zhihu.viewmodel.filter

import com.github.zly2006.zhihu.viewmodel.PaginationEnvironment
import com.github.zly2006.zhihu.viewmodel.androidContext
import kotlinx.coroutines.runBlocking

actual fun PaginationEnvironment.fetchBlockedUserIds(): Set<String> {
    val ctx = androidContext() ?: return emptySet()
    return runBlocking { getBlocklistManager(ctx).getAllBlockedUsers().map { it.userId }.toSet() }
}
