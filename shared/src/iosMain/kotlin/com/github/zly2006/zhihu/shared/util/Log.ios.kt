package com.github.zly2006.zhihu.shared.util

actual object Log {
    actual fun d(
        tag: String,
        message: String,
        throwable: Throwable?,
    ) = printLog("DEBUG", tag, message, throwable)

    actual fun i(
        tag: String,
        message: String,
        throwable: Throwable?,
    ) = printLog("INFO", tag, message, throwable)

    actual fun w(
        tag: String,
        message: String,
        throwable: Throwable?,
    ) = printLog("WARN", tag, message, throwable)

    actual fun e(
        tag: String,
        message: String,
        throwable: Throwable?,
    ) = printLog("ERROR", tag, message, throwable)
}

private fun printLog(
    level: String,
    tag: String,
    message: String,
    throwable: Throwable?,
) {
    println("[$level][$tag] $message")
    throwable?.printStackTrace()
}
