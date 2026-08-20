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

package com.hrm.codehigh.ast

/**
 * Token 类型枚举，定义代码高亮中所有可能的 Token 分类
 */
enum class TokenType {
    /** 关键字：fun、class、if、def、import 等 */
    KEYWORD,

    /** 字符串字面量：单引号、双引号、三引号、模板字符串 */
    STRING,

    /** 数字字面量：整数、浮点数、十六进制、二进制 */
    NUMBER,

    /** 注释：单行 //、#，多行 /* */、""" """ */
    COMMENT,

    /** 运算符：+、-、*、/、=、==、!=、&&、|| 等 */
    OPERATOR,

    /** 标点符号：{、}、(、)、[、]、;、,、. */
    PUNCTUATION,

    /** 标识符：变量名、函数名、类名等通用标识 */
    IDENTIFIER,

    /** 类型名：Int、String、Boolean、List、Map 等 */
    TYPE,

    /** 函数调用名（可与 IDENTIFIER 合并，按语言特性决定） */
    FUNCTION,

    /** 变量名（部分语言可区分变量与标识符） */
    VARIABLE,

    /** 常量名：const、val、全大写命名等 */
    CONSTANT,

    /** 注解/装饰器：@Override、@Composable、@property */
    ANNOTATION,

    /** 装饰器：Python @decorator，与 ANNOTATION 语义相近 */
    DECORATOR,

    /** 内置函数/类型：println、print、len、range、None、true、false */
    BUILTIN,

    /** 纯文本：无法分类的字符，降级兜底 */
    PLAIN
}
