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

/**
 * 语言词法分析器注册表，管理语言标识符到 Lexer 的映射。
 * 对外公开 get() 和 register() 方法，其余实现细节标记为 internal。
 */
object LanguageRegistry {

    /** 语言标识符到 Lexer 的映射表，internal 不对外暴露 */
    internal val registry: MutableMap<String, Lexer> = mutableMapOf()

    /** 别名映射表，internal 不对外暴露 */
    internal val aliases: MutableMap<String, String> = mutableMapOf()

    /** 是否已完成默认注册 */
    private var defaultsRegistered = false

    /**
     * 注册语言词法分析器。
     *
     * @param lang 语言标识符（小写）
     * @param lexer 对应的词法分析器
     */
    fun register(lang: String, lexer: Lexer) {
        registry[lang.lowercase()] = lexer
    }

    /**
     * 注册语言别名。
     *
     * @param alias 别名（如 "js"）
     * @param canonical 规范名称（如 "javascript"）
     */
    fun registerAlias(alias: String, canonical: String) {
        aliases[alias.lowercase()] = canonical.lowercase()
    }

    /**
     * 按语言标识符获取词法分析器。
     * 支持别名查找，未知语言返回 null。
     *
     * @param lang 语言标识符
     * @return 对应的词法分析器，未找到时返回 null
     */
    fun get(lang: String): Lexer? {
        ensureDefaultsRegistered()
        val normalized = lang.lowercase().trim()
        val canonical = aliases[normalized] ?: normalized
        return registry[canonical]
    }

    /**
     * 获取词法分析器，未知语言降级为 PlainTextLexer。
     * 对外公开，供渲染层使用。
     */
    fun getOrPlain(lang: String): Lexer {
        return get(lang) ?: PlainTextLexer
    }

    /**
     * 确保默认语言已注册（懒加载）。
     */
    private fun ensureDefaultsRegistered() {
        if (!defaultsRegistered) {
            registerDefaults()
            defaultsRegistered = true
        }
    }

    /**
     * 注册所有内置语言词法分析器。
     * 模块初始化时内部调用，标记为 internal。
     */
    internal fun registerDefaults() {
        // 系统语言
        register("kotlin", KotlinLexer)
        registerAlias("kt", "kotlin")
        registerAlias("kts", "kotlin")

        register("java", JavaLexer)

        register("swift", SwiftLexer)

        // 脚本语言
        register("python", PythonLexer)
        registerAlias("py", "python")

        register("javascript", JavaScriptLexer)
        registerAlias("js", "javascript")
        registerAlias("jsx", "javascript")

        register("typescript", TypeScriptLexer)
        registerAlias("ts", "typescript")
        registerAlias("tsx", "typescript")

        register("ruby", RubyLexer)
        registerAlias("rb", "ruby")

        register("php", PhpLexer)
        registerAlias("phtml", "php")
        registerAlias("php3", "php")
        registerAlias("php4", "php")
        registerAlias("php5", "php")

        register("dart", DartLexer)

        register("scala", ScalaLexer)
        registerAlias("sc", "scala")

        register("lua", LuaLexer)
        registerAlias("luau", "lua")

        register("haskell", HaskellLexer)
        registerAlias("hs", "haskell")

        register("elixir", ElixirLexer)
        registerAlias("ex", "elixir")
        registerAlias("exs", "elixir")

        // 系统/底层语言
        register("go", GoLexer)
        register("rust", RustLexer)
        registerAlias("rs", "rust")
        register("c", CLexer)
        register("cpp", CppLexer)
        registerAlias("c++", "cpp")
        registerAlias("cxx", "cpp")
        registerAlias("cc", "cpp")

        // 数据/配置语言
        register("sql", SqlLexer)
        register("json", JsonLexer)
        register("yaml", YamlLexer)
        registerAlias("yml", "yaml")
        register("r", RLangLexer)
        registerAlias("rscript", "r")
        register("toml", TomlLexer)
        register("dockerfile", DockerfileLexer)
        registerAlias("docker", "dockerfile")

        // 标记/样式语言
        register("bash", BashLexer)
        registerAlias("sh", "bash")
        registerAlias("shell", "bash")
        register("diff", DiffLexer)
        registerAlias("patch", "diff")
        register("xml", XmlLexer)
        register("html", HtmlLexer)
        registerAlias("htm", "html")
        register("css", CssLexer)
    }
}
