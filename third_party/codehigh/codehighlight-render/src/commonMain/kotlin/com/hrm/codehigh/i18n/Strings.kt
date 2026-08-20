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

package com.hrm.codehigh.i18n

import kotlin.math.max

/**
 * 多语言字符串支持
 * 支持中文/英文，不支持的语言回退到英文
 */
internal object Strings {
    /**
     * 获取当前语言代码
     * 在 JVM/Android 上使用系统语言，其他平台默认英文
     */
    private val languageCode: String by lazy {
        getSystemLanguage()
    }

    private fun getSystemLanguage(): String {
        return try {
            // 尝试获取系统语言
            val locale = PlatformLocale.current()
            locale.language.lowercase()
        } catch (e: Exception) {
            "en"
        }
    }

    /**
     * 是否为中文环境
     */
    private val isChinese: Boolean
        get() = languageCode.startsWith("zh")

    /**
     * 收起按钮文本
     */
    fun collapse(): String = if (isChinese) "▲ 收起" else "▲ Collapse"

    /**
     * 展开按钮文本
     * @param hiddenLines 隐藏的行数
     */
    fun expand(hiddenLines: Int): String {
        val lines = max(0, hiddenLines)
        return if (isChinese) {
            "▼ 展开 ($lines 行)"
        } else {
            "▼ Expand ($lines lines)"
        }
    }

    /**
     * 复制按钮文本
     */
    fun copy(): String = if (isChinese) "复制" else "Copy"

    /**
     * 已复制提示文本
     */
    fun copied(): String = if (isChinese) "已复制" else "Copied"

    /**
     * 行号列标题
     */
    fun lineNumber(): String = if (isChinese) "行" else "Line"
}

/**
 * 平台相关的 Locale 获取
 */
internal expect object PlatformLocale {
    fun current(): LocaleInfo
}

/**
 * 语言环境信息
 */
internal data class LocaleInfo(
    val language: String,
    val country: String = ""
)
