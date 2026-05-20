package com.github.zly2006.zhihu.shared.login

import platform.Foundation.NSDate

internal actual fun currentTimeMillis(): Long = (NSDate().timeIntervalSince1970 * 1000).toLong()
