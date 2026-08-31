/*
 * Copyright (c) 2026 huarangmeng
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */


package com.hrm.latex.base.log

/**
 * 日志门面，通过 LatexSDK 初始化时注入实现
 */
object HLog {
    @PublishedApi
    internal var loggerImpl: ILogger? = null

    /**
     * 设置日志实现，由 SDK 初始化时调用
     */
    fun setLogger(logger: ILogger?) {
        loggerImpl = logger
    }

    /** 是否有日志实现（用于调用方提前判断，避免无谓的参数构造） */
    val isEnabled: Boolean get() = loggerImpl != null

    fun v(tag: String, message: String) {
        loggerImpl?.v(tag, message)
    }

    fun d(tag: String, message: String) {
        loggerImpl?.d(tag, message)
    }

    fun i(tag: String, message: String) {
        loggerImpl?.i(tag, message)
    }

    fun w(tag: String, message: String) {
        loggerImpl?.w(tag, message)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        loggerImpl?.e(tag, message, throwable)
    }

    // --- Lazy 重载：message lambda 仅在日志启用时求值 ---
    // 解决热路径中 HLog.d(TAG, "...$variable...") 即使日志关闭也会执行字符串模板的问题

    inline fun v(tag: String, message: () -> String) {
        loggerImpl?.v(tag, message())
    }

    inline fun d(tag: String, message: () -> String) {
        loggerImpl?.d(tag, message())
    }

    inline fun i(tag: String, message: () -> String) {
        loggerImpl?.i(tag, message())
    }

    inline fun w(tag: String, message: () -> String) {
        loggerImpl?.w(tag, message())
    }

    inline fun e(tag: String, throwable: Throwable? = null, message: () -> String) {
        loggerImpl?.e(tag, message(), throwable)
    }
}
