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

package com.hrm.codehigh.lexer

import com.hrm.codehigh.ast.CodeToken

/**
 * 词法分析器接口，定义代码分词的核心方法。
 * 对外公开，支持外部注入自定义 Lexer 实现。
 */
public interface Lexer {
    /**
     * 对代码字符串进行词法分析，返回 Token 列表。
     * 空输入返回空列表，不抛出异常。
     * 所有 Token 的 range 覆盖原始字符串完整范围（无遗漏字符）。
     *
     * @param code 待分析的代码字符串
     * @return Token 列表
     */
    public fun tokenize(code: String): List<CodeToken>
}
