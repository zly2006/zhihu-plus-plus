/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
) = println("[$level][$tag] $message").also {
    throwable?.printStackTrace()
}
