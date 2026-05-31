package com.github.zly2006.zhihu.shared.util

actual object Log {
    actual fun d(
        tag: String,
        message: String,
        throwable: Throwable?,
    ) = log { android.util.Log.d(tag, message, throwable) }

    actual fun i(
        tag: String,
        message: String,
        throwable: Throwable?,
    ) = log { android.util.Log.i(tag, message, throwable) }

    actual fun w(
        tag: String,
        message: String,
        throwable: Throwable?,
    ) = log { android.util.Log.w(tag, message, throwable) }

    actual fun e(
        tag: String,
        message: String,
        throwable: Throwable?,
    ) = log { android.util.Log.e(tag, message, throwable) }
}

private inline fun log(action: () -> Int) {
    action()
}
