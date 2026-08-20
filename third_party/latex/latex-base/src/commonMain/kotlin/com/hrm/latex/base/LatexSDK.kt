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


package com.hrm.latex.base

import com.hrm.latex.base.log.HLog
import com.hrm.latex.base.log.ILogger

/**
 * LaTeX SDK 统一入口
 */
object LatexSDK {
    private var initialized = false

    /**
     * SDK 配置
     */
    data class Config(
        val logger: ILogger? = null,
        val debug: Boolean = false
    )

    /**
     * 初始化 SDK
     * @param config SDK 配置，可选传入日志实现
     */
    fun initialize(config: Config = Config()) {
        if (initialized) {
            HLog.w("LatexSDK", "SDK already initialized")
            return
        }

        // 注入日志实现
        HLog.setLogger(config.logger)

        initialized = true
        HLog.i("LatexSDK", "LaTeX SDK initialized, debug=${config.debug}")
    }

    /**
     * 检查是否已初始化
     */
    fun isInitialized(): Boolean = initialized
}