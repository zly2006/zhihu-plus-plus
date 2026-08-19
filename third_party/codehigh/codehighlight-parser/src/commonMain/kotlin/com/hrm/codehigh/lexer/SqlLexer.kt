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
import com.hrm.codehigh.ast.TokenType

/**
 * SQL 词法分析器（大小写不敏感）。
 * 标记为 internal，外部通过 LanguageRegistry 访问。
 */
internal object SqlLexer : BaseLexer() {

    private val keywords = setOf(
        "SELECT", "FROM", "WHERE", "JOIN", "LEFT", "RIGHT", "INNER", "OUTER",
        "FULL", "CROSS", "ON", "GROUP", "BY", "ORDER", "HAVING", "INSERT",
        "INTO", "VALUES", "UPDATE", "SET", "DELETE", "CREATE", "DROP", "ALTER",
        "TABLE", "INDEX", "VIEW", "DATABASE", "SCHEMA", "AS", "AND", "OR",
        "NOT", "IN", "LIKE", "BETWEEN", "IS", "NULL", "DISTINCT", "LIMIT",
        "OFFSET", "UNION", "ALL", "INTERSECT", "EXCEPT", "EXISTS", "CASE",
        "WHEN", "THEN", "ELSE", "END", "WITH", "RECURSIVE", "RETURNING",
        "PRIMARY", "KEY", "FOREIGN", "REFERENCES", "UNIQUE", "CHECK",
        "DEFAULT", "CONSTRAINT", "AUTO_INCREMENT", "AUTOINCREMENT",
        "SERIAL", "IDENTITY", "SEQUENCE", "TRIGGER", "PROCEDURE", "FUNCTION",
        "BEGIN", "COMMIT", "ROLLBACK", "TRANSACTION", "SAVEPOINT",
        "GRANT", "REVOKE", "PRIVILEGES", "TO", "TRUNCATE", "EXPLAIN",
        "ANALYZE", "VACUUM", "REINDEX", "ATTACH", "DETACH"
    )

    private val builtins = setOf(
        "COUNT", "SUM", "AVG", "MAX", "MIN", "COALESCE", "NULLIF", "CAST",
        "CONVERT", "NOW", "DATE", "TIME", "DATETIME", "TIMESTAMP",
        "SUBSTRING", "SUBSTR", "UPPER", "LOWER", "TRIM", "LTRIM", "RTRIM",
        "LENGTH", "LEN", "REPLACE", "CONCAT", "CONCAT_WS", "GROUP_CONCAT",
        "STRING_AGG", "ARRAY_AGG", "JSON_AGG", "ROW_NUMBER", "RANK",
        "DENSE_RANK", "NTILE", "LAG", "LEAD", "FIRST_VALUE", "LAST_VALUE",
        "OVER", "PARTITION", "ROWS", "RANGE", "UNBOUNDED", "PRECEDING",
        "FOLLOWING", "CURRENT", "ROW", "ABS", "CEIL", "FLOOR", "ROUND",
        "MOD", "POWER", "SQRT", "LOG", "EXP", "SIGN", "ISNULL", "IFNULL",
        "NVL", "DECODE", "IIF", "IF", "YEAR", "MONTH", "DAY", "HOUR",
        "MINUTE", "SECOND", "DATEADD", "DATEDIFF", "DATEPART", "EXTRACT",
        "TO_DATE", "TO_CHAR", "TO_NUMBER", "FORMAT", "CHARINDEX", "INSTR",
        "POSITION", "LOCATE", "LPAD", "RPAD", "REPEAT", "REVERSE", "SPACE",
        "STR", "CHAR", "ASCII", "UNICODE", "NCHAR", "SOUNDEX", "DIFFERENCE"
    )

    override fun tokenize(code: String): List<CodeToken> {
        if (code.isEmpty()) return emptyList()
        val tokens = mutableListOf<CodeToken>()
        var pos = 0

        while (pos < code.length) {
            val c = code[pos]

            // 单行注释 --
            if (pos + 1 < code.length && code[pos] == '-' && code[pos + 1] == '-') {
                val start = pos
                while (pos < code.length && code[pos] != '\n') pos++
                tokens.add(CodeToken(TokenType.COMMENT, code.substring(start, pos), start until pos))
                continue
            }

            // 多行注释
            if (pos + 1 < code.length && code[pos] == '/' && code[pos + 1] == '*') {
                val start = pos
                pos += 2
                while (pos + 1 < code.length && !(code[pos] == '*' && code[pos + 1] == '/')) pos++
                if (pos + 1 < code.length) {
                    pos += 2
                } else {
                    pos = code.length
                }
                tokens.add(CodeToken(TokenType.COMMENT, code.substring(start, pos), start until pos))
                continue
            }

            // 单引号字符串
            if (c == '\'') {
                val start = pos
                pos++
                while (pos < code.length && !(code[pos] == '\'' && (pos + 1 >= code.length || code[pos + 1] != '\''))) {
                    if (code[pos] == '\'' && pos + 1 < code.length && code[pos + 1] == '\'') {
                        pos += 2 // 转义的单引号 ''
                    } else {
                        pos++
                    }
                }
                if (pos < code.length && code[pos] == '\'') pos++
                tokens.add(CodeToken(TokenType.STRING, code.substring(start, pos), start until pos))
                continue
            }

            // 双引号标识符（表名、列名）
            if (c == '"') {
                val start = pos
                pos++
                while (pos < code.length && code[pos] != '"') pos++
                if (pos < code.length) pos++
                tokens.add(CodeToken(TokenType.IDENTIFIER, code.substring(start, pos), start until pos))
                continue
            }

            // 反引号标识符（MySQL）
            if (c == '`') {
                val start = pos
                pos++
                while (pos < code.length && code[pos] != '`') pos++
                if (pos < code.length) pos++
                tokens.add(CodeToken(TokenType.IDENTIFIER, code.substring(start, pos), start until pos))
                continue
            }

            // 数字字面量
            if (c.isDigit() || (c == '-' && pos + 1 < code.length && code[pos + 1].isDigit())) {
                val start = pos
                if (c == '-') pos++
                while (pos < code.length && code[pos].isDigit()) pos++
                if (pos < code.length && code[pos] == '.') {
                    pos++
                    while (pos < code.length && code[pos].isDigit()) pos++
                }
                if (pos < code.length && (code[pos] == 'e' || code[pos] == 'E')) {
                    pos++
                    if (pos < code.length && (code[pos] == '+' || code[pos] == '-')) pos++
                    while (pos < code.length && code[pos].isDigit()) pos++
                }
                tokens.add(CodeToken(TokenType.NUMBER, code.substring(start, pos), start until pos))
                continue
            }

            // 参数占位符 ? 或 :name 或 $1
            if (c == '?' || (c == ':' && pos + 1 < code.length && code[pos + 1].isLetter()) ||
                (c == '$' && pos + 1 < code.length && code[pos + 1].isDigit())) {
                val start = pos
                pos++
                if (c == ':' || c == '$') {
                    while (pos < code.length && (code[pos].isLetterOrDigit() || code[pos] == '_')) pos++
                }
                tokens.add(CodeToken(TokenType.VARIABLE, code.substring(start, pos), start until pos))
                continue
            }

            // 标识符、关键字、内置函数
            if (c.isLetter() || c == '_') {
                val start = pos
                while (pos < code.length && (code[pos].isLetterOrDigit() || code[pos] == '_')) pos++
                val word = code.substring(start, pos)
                val wordUpper = word.uppercase()
                val type = when {
                    wordUpper in keywords -> TokenType.KEYWORD
                    wordUpper in builtins -> TokenType.BUILTIN
                    pos < code.length && code[pos] == '(' -> TokenType.FUNCTION
                    else -> TokenType.IDENTIFIER
                }
                tokens.add(CodeToken(type, word, start until pos))
                continue
            }

            // 运算符
            if (c in "+-*/%=!<>&|^~") {
                val start = pos
                val twoChar = if (pos + 1 < code.length) code.substring(pos, pos + 2) else ""
                when {
                    twoChar in setOf("!=", "<>", "<=", ">=", "||", "::", "->") -> pos += 2
                    else -> pos++
                }
                tokens.add(CodeToken(TokenType.OPERATOR, code.substring(start, pos), start until pos))
                continue
            }

            // 标点符号
            if (c in "{}()[];,.*") {
                tokens.add(CodeToken(TokenType.PUNCTUATION, c.toString(), pos until pos + 1))
                pos++
                continue
            }

            // 其他字符
            tokens.add(CodeToken(TokenType.PLAIN, c.toString(), pos until pos + 1))
            pos++
        }

        return tokens
    }
}
