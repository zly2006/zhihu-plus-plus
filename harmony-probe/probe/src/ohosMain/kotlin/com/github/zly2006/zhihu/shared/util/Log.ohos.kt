package com.github.zly2006.zhihu.shared.util

actual object Log {
    actual fun d(
        tag: String,
        message: String,
        throwable: Throwable?,
    ) {
        print("D/$tag: $message")
        throwable?.let { print(" ($it)") }
        println()
    }

    actual fun i(
        tag: String,
        message: String,
        throwable: Throwable?,
    ) {
        print("I/$tag: $message")
        throwable?.let { print(" ($it)") }
        println()
    }

    actual fun w(
        tag: String,
        message: String,
        throwable: Throwable?,
    ) {
        print("W/$tag: $message")
        throwable?.let { print(" ($it)") }
        println()
    }

    actual fun e(
        tag: String,
        message: String,
        throwable: Throwable?,
    ) {
        print("E/$tag: $message")
        throwable?.let { print(" ($it)") }
        println()
    }
}
