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

package com.hrm.latex.renderer.utils

import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.hrm.latex.renderer.model.LatexFontFamilies
import com.hrm.latex.renderer.model.RenderContext

/**
 * 字体类别，用于选择对应的字体家族
 *
 * KaTeX 字体体系下的类别划分：
 * - ROMAN → KaTeX_Main (正文、数字、标点、运算符)
 * - MATH_ITALIC → KaTeX_Math (数学变量)
 * - SYMBOL → KaTeX_Main (KaTeX 中运算符在 Main 字体中)
 * - EXTENSION → KaTeX_Size1 (大型定界符)
 * - 其他特殊字体类别映射到对应的 KaTeX 字体
 */
enum class FontCategory {
    ROMAN,           // 正文文本 (KaTeX_Main)
    MATH_ITALIC,     // 数学变量 (KaTeX_Math)
    SYMBOL,          // 运算符、小型定界符 (KaTeX_Main)
    EXTENSION,       // 大型运算符、大型定界符 (KaTeX_Size1)
    SANS_SERIF,      // 无衬线 (KaTeX_SansSerif)
    MONOSPACE,       // 等宽 (KaTeX_Typewriter)
    BLACKBOARD_BOLD, // 黑板粗体 (KaTeX_AMS)
    CALLIGRAPHIC,    // 花体 (KaTeX_Caligraphic)
    FRAKTUR,         // 哥特体 (KaTeX_Fraktur)
    SCRIPT           // 手写花体 (KaTeX_Script)
}

/**
 * 符号渲染信息
 *
 * KaTeX 字体使用标准 Unicode 编码，texGlyph 直接是 Unicode 字符。
 *
 * @param texGlyph 用于渲染的 Unicode 字符
 * @param fontCategory 应使用的字体类别
 * @param fontStyle 字体样式（Normal 或 Italic）
 */
data class SymbolRenderInfo(
    val texGlyph: String,
    val fontCategory: FontCategory,
    val fontStyle: FontStyle = FontStyle.Normal
)

/**
 * 集中管理字体选择和符号字体路由。
 *
 * KaTeX 字体使用标准 Unicode 编码，所有符号直接使用 Unicode 字符渲染，
 * 无需 CM 字体的 TeX 编码映射。
 *
 * 符号路由规则：
 * - 希腊字母（小写）→ KaTeX_Math (Italic)
 * - 希腊字母（大写）→ KaTeX_Main (Normal)
 * - 运算符、关系符 → KaTeX_Main (Normal)
 * - 大型运算符 → KaTeX_Main (Normal)，Display 模式下字号放大
 * - 定界符 → KaTeX_Main (行内) 或 KaTeX_Size1~4 (放大)
 */
internal object FontResolver {

    // =========================================================================
    // 符号命令名 → Unicode 字符 + 字体类别 映射表
    //
    // KaTeX 字体使用标准 Unicode，直接映射到 Unicode 字符即可。
    // =========================================================================

    /** 快捷构造 Main 字体 (运算符/关系符/大写希腊) */
    private fun main(unicode: String, style: FontStyle = FontStyle.Normal): SymbolRenderInfo =
        SymbolRenderInfo(unicode, FontCategory.ROMAN, style)

    /** 快捷构造 Math 字体 (数学变量/小写希腊) */
    private fun math(unicode: String, style: FontStyle = FontStyle.Italic): SymbolRenderInfo =
        SymbolRenderInfo(unicode, FontCategory.MATH_ITALIC, style)

    /** 快捷构造 Main 字体的符号类 (运算符在 KaTeX_Main 中) */
    private fun sym(unicode: String): SymbolRenderInfo =
        SymbolRenderInfo(unicode, FontCategory.SYMBOL, FontStyle.Normal)

    /**
     * 符号命令名 → SymbolRenderInfo 映射表
     *
     * KaTeX 中大部分符号都在 KaTeX_Main 字体中（标准 Unicode），
     * 希腊字母小写在 KaTeX_Math 中。
     */
    private val symbolMap: Map<String, SymbolRenderInfo> = buildMap {
        // === 小写希腊字母 → KaTeX_Math (Italic) ===
        put("alpha", math("α"))
        put("beta", math("β"))
        put("gamma", math("γ"))
        put("delta", math("δ"))
        put("epsilon", math("ϵ"))
        put("varepsilon", math("ε"))
        put("zeta", math("ζ"))
        put("eta", math("η"))
        put("theta", math("θ"))
        put("vartheta", math("ϑ"))
        put("iota", math("ι"))
        put("kappa", math("κ"))
        put("lambda", math("λ"))
        put("mu", math("μ"))
        put("nu", math("ν"))
        put("xi", math("ξ"))
        put("omicron", math("ο"))
        put("pi", math("π"))
        put("varpi", math("ϖ"))
        put("rho", math("ρ"))
        put("varrho", math("ϱ"))
        put("sigma", math("σ"))
        put("varsigma", math("ς"))
        put("tau", math("τ"))
        put("upsilon", math("υ"))
        put("phi", math("ϕ"))
        put("varphi", math("φ"))
        put("chi", math("χ"))
        put("psi", math("ψ"))
        put("omega", math("ω"))

        // === AMS 希腊字母变体 → KaTeX_Math (Italic) ===
        put("digamma", math("ϝ"))
        put("varkappa", math("ϰ"))

        // === 大写希腊字母 → KaTeX_Main (Normal) ===
        put("Alpha", main("A"))
        put("Beta", main("B"))
        put("Gamma", main("Γ"))
        put("Delta", main("Δ"))
        put("Epsilon", main("E"))
        put("Zeta", main("Z"))
        put("Eta", main("H"))
        put("Theta", main("Θ"))
        put("Iota", main("I"))
        put("Kappa", main("K"))
        put("Lambda", main("Λ"))
        put("Mu", main("M"))
        put("Nu", main("N"))
        put("Xi", main("Ξ"))
        put("Omicron", main("O"))
        put("Pi", main("Π"))
        put("Rho", main("P"))
        put("Sigma", main("Σ"))
        put("Tau", main("T"))
        put("Upsilon", main("Υ"))
        put("Phi", main("Φ"))
        put("Chi", main("X"))
        put("Psi", main("Ψ"))
        put("Omega", main("Ω"))

        // === 大写希腊字母斜体变体 → KaTeX_Math (Italic) ===
        put("varGamma", math("Γ"))
        put("varDelta", math("Δ"))
        put("varTheta", math("Θ"))
        put("varLambda", math("Λ"))
        put("varXi", math("Ξ"))
        put("varPi", math("Π"))
        put("varSigma", math("Σ"))
        put("varUpsilon", math("Υ"))
        put("varPhi", math("Φ"))
        put("varPsi", math("Ψ"))
        put("varOmega", math("Ω"))

        // === 箭头 → KaTeX_Main ===
        put("leftarrow", main("←"))
        put("gets", main("←"))
        put("rightarrow", main("→"))
        put("to", main("→"))
        put("uparrow", main("↑"))
        put("downarrow", main("↓"))
        put("leftrightarrow", main("↔"))
        put("updownarrow", main("↕"))
        put("Leftarrow", main("⇐"))
        put("Rightarrow", main("⇒"))
        put("Uparrow", main("⇑"))
        put("Downarrow", main("⇓"))
        put("Leftrightarrow", main("⇔"))
        put("Updownarrow", main("⇕"))
        put("nearrow", main("↗"))
        put("searrow", main("↘"))
        put("nwarrow", main("↖"))
        put("swarrow", main("↙"))
        put("mapsto", main("↦"))
        put("longrightarrow", main("⟶"))
        put("longleftarrow", main("⟵"))
        put("longleftrightarrow", main("⟷"))
        put("Longleftarrow", main("⟸"))
        put("Longrightarrow", main("⟹"))
        put("Longleftrightarrow", main("⟺"))
        put("longmapsto", main("⟼"))
        put("leftharpoonup", main("↼"))
        put("leftharpoondown", main("↽"))
        put("rightharpoonup", main("⇀"))
        put("rightharpoondown", main("⇁"))
        put("rightleftharpoons", main("⇌"))
        put("leftrightharpoons", main("⇋"))
        put("hookrightarrow", main("↪"))
        put("hookleftarrow", main("↩"))

        // === 二元运算符 → KaTeX_Main ===
        put("plus", main("+"))
        put("minus", main("−"))
        put("times", main("×"))
        put("div", main("÷"))
        put("pm", main("±"))
        put("mp", main("∓"))
        put("cdot", main("⋅"))
        put("cdotp", main("⋅"))
        put("ast", main("∗"))
        put("star", main("⋆"))
        put("circ", main("∘"))
        put("bullet", main("•"))
        put("diamond", main("◇"))
        put("bigcirc", main("○"))
        put("oplus", main("⊕"))
        put("ominus", main("⊖"))
        put("otimes", main("⊗"))
        put("oslash", main("⊘"))
        put("odot", main("⊙"))
        put("amalg", main("∐"))
        put("wr", main("≀"))
        put("uplus", main("⊎"))
        put("sqcup", main("⊔"))
        put("sqcap", main("⊓"))
        put("cap", main("∩"))
        put("cup", main("∪"))
        put("wedge", main("∧"))
        put("land", main("∧"))
        put("vee", main("∨"))
        put("lor", main("∨"))
        put("setminus", main("∖"))
        put("backslash", main("∖"))

        // === AMS 二元运算符 → KaTeX_AMS ===
        put("dotplus", SymbolRenderInfo("∔", FontCategory.BLACKBOARD_BOLD))
        put("smallsetminus", SymbolRenderInfo("∖", FontCategory.BLACKBOARD_BOLD))
        put("barwedge", SymbolRenderInfo("⌅", FontCategory.BLACKBOARD_BOLD))
        put("veebar", SymbolRenderInfo("⌆", FontCategory.BLACKBOARD_BOLD))
        put("doublebarwedge", SymbolRenderInfo("⩞", FontCategory.BLACKBOARD_BOLD))
        put("boxminus", SymbolRenderInfo("⊟", FontCategory.BLACKBOARD_BOLD))
        put("boxplus", SymbolRenderInfo("⊞", FontCategory.BLACKBOARD_BOLD))
        put("boxtimes", SymbolRenderInfo("⊠", FontCategory.BLACKBOARD_BOLD))
        put("boxdot", SymbolRenderInfo("⊡", FontCategory.BLACKBOARD_BOLD))
        put("leftthreetimes", SymbolRenderInfo("⋋", FontCategory.BLACKBOARD_BOLD))
        put("rightthreetimes", SymbolRenderInfo("⋌", FontCategory.BLACKBOARD_BOLD))
        put("curlywedge", SymbolRenderInfo("⋏", FontCategory.BLACKBOARD_BOLD))
        put("curlyvee", SymbolRenderInfo("⋎", FontCategory.BLACKBOARD_BOLD))
        put("circleddash", SymbolRenderInfo("⊝", FontCategory.BLACKBOARD_BOLD))
        put("circledast", SymbolRenderInfo("⊛", FontCategory.BLACKBOARD_BOLD))
        put("circledcirc", SymbolRenderInfo("⊚", FontCategory.BLACKBOARD_BOLD))
        put("centerdot", SymbolRenderInfo("·", FontCategory.BLACKBOARD_BOLD))
        put("intercal", SymbolRenderInfo("⊺", FontCategory.BLACKBOARD_BOLD))
        put("divideontimes", SymbolRenderInfo("⋇", FontCategory.BLACKBOARD_BOLD))
        put("rtimes", SymbolRenderInfo("⋊", FontCategory.BLACKBOARD_BOLD))
        put("ltimes", SymbolRenderInfo("⋉", FontCategory.BLACKBOARD_BOLD))

        // === 关系符号 → KaTeX_Main ===
        put("leq", main("≤"))
        put("le", main("≤"))
        put("geq", main("≥"))
        put("ge", main("≥"))
        put("neq", main("≠"))
        put("ne", main("≠"))
        put("equiv", main("≡"))
        put("approx", main("≈"))
        put("cong", main("≅"))
        put("sim", main("∼"))
        put("simeq", main("≃"))
        put("propto", main("∝"))
        put("varpropto", SymbolRenderInfo("∝", FontCategory.BLACKBOARD_BOLD))
        put("asymp", main("≍"))
        put("ll", main("≪"))
        put("gg", main("≫"))
        put("prec", main("≺"))
        put("succ", main("≻"))
        put("preceq", main("≼"))
        put("succeq", main("≽"))
        put("subset", main("⊂"))
        put("supset", main("⊃"))
        put("subseteq", main("⊆"))
        put("supseteq", main("⊇"))
        put("sqsubseteq", main("⊑"))
        put("sqsupseteq", main("⊒"))
        put("in", main("∈"))
        put("notin", main("∉"))
        put("ni", main("∋"))
        put("owns", main("∋"))
        put("vdash", main("⊢"))
        put("dashv", main("⊣"))
        put("perp", main("⊥"))
        put("parallel", main("∥"))
        put("mid", main("∣"))

        // === AMS 否定关系符号 → KaTeX_AMS ===
        put("nless", SymbolRenderInfo("≮", FontCategory.BLACKBOARD_BOLD))
        put("ngtr", SymbolRenderInfo("≯", FontCategory.BLACKBOARD_BOLD))
        put("nleq", SymbolRenderInfo("≰", FontCategory.BLACKBOARD_BOLD))
        put("ngeq", SymbolRenderInfo("≱", FontCategory.BLACKBOARD_BOLD))
        put("nleqslant", SymbolRenderInfo("≰", FontCategory.BLACKBOARD_BOLD))
        put("ngeqslant", SymbolRenderInfo("≱", FontCategory.BLACKBOARD_BOLD))
        put("nsubseteq", SymbolRenderInfo("⊈", FontCategory.BLACKBOARD_BOLD))
        put("nsupseteq", SymbolRenderInfo("⊉", FontCategory.BLACKBOARD_BOLD))
        put("nprec", SymbolRenderInfo("⊀", FontCategory.BLACKBOARD_BOLD))
        put("nsucc", SymbolRenderInfo("⊁", FontCategory.BLACKBOARD_BOLD))
        put("ncong", SymbolRenderInfo("≇", FontCategory.BLACKBOARD_BOLD))
        put("nsim", SymbolRenderInfo("≁", FontCategory.BLACKBOARD_BOLD))
        put("nmid", SymbolRenderInfo("∤", FontCategory.BLACKBOARD_BOLD))
        put("nparallel", SymbolRenderInfo("∦", FontCategory.BLACKBOARD_BOLD))
        put("nvdash", SymbolRenderInfo("⊬", FontCategory.BLACKBOARD_BOLD))
        put("nvDash", SymbolRenderInfo("⊭", FontCategory.BLACKBOARD_BOLD))
        put("nVdash", SymbolRenderInfo("⊮", FontCategory.BLACKBOARD_BOLD))
        put("nVDash", SymbolRenderInfo("⊯", FontCategory.BLACKBOARD_BOLD))
        put("ntriangleleft", SymbolRenderInfo("⋪", FontCategory.BLACKBOARD_BOLD))
        put("ntriangleright", SymbolRenderInfo("⋫", FontCategory.BLACKBOARD_BOLD))
        put("ntrianglelefteq", SymbolRenderInfo("⋬", FontCategory.BLACKBOARD_BOLD))
        put("ntrianglerighteq", SymbolRenderInfo("⋭", FontCategory.BLACKBOARD_BOLD))
        put("lneq", SymbolRenderInfo("⪇", FontCategory.BLACKBOARD_BOLD))
        put("gneq", SymbolRenderInfo("⪈", FontCategory.BLACKBOARD_BOLD))
        put("lnsim", SymbolRenderInfo("⋦", FontCategory.BLACKBOARD_BOLD))
        put("gnsim", SymbolRenderInfo("⋧", FontCategory.BLACKBOARD_BOLD))
        put("precnsim", SymbolRenderInfo("⋨", FontCategory.BLACKBOARD_BOLD))
        put("succnsim", SymbolRenderInfo("⋩", FontCategory.BLACKBOARD_BOLD))
        put("subsetneq", SymbolRenderInfo("⊊", FontCategory.BLACKBOARD_BOLD))
        put("supsetneq", SymbolRenderInfo("⊋", FontCategory.BLACKBOARD_BOLD))
        put("varsubsetneq", SymbolRenderInfo("⊊", FontCategory.BLACKBOARD_BOLD))
        put("varsupsetneq", SymbolRenderInfo("⊋", FontCategory.BLACKBOARD_BOLD))
        put("subsetneqq", SymbolRenderInfo("⫋", FontCategory.BLACKBOARD_BOLD))
        put("supsetneqq", SymbolRenderInfo("⫌", FontCategory.BLACKBOARD_BOLD))
        put("varsubsetneqq", SymbolRenderInfo("⫋", FontCategory.BLACKBOARD_BOLD))
        put("varsupsetneqq", SymbolRenderInfo("⫌", FontCategory.BLACKBOARD_BOLD))
        put("nsubset", SymbolRenderInfo("⊄", FontCategory.BLACKBOARD_BOLD))
        put("nsupset", SymbolRenderInfo("⊅", FontCategory.BLACKBOARD_BOLD))
        put("nsubseteqq", SymbolRenderInfo("⊈", FontCategory.BLACKBOARD_BOLD))
        put("nsupseteqq", SymbolRenderInfo("⊉", FontCategory.BLACKBOARD_BOLD))

        // === AMS 额外关系符号 → KaTeX_AMS ===
        put("leqslant", SymbolRenderInfo("⩽", FontCategory.BLACKBOARD_BOLD))
        put("geqslant", SymbolRenderInfo("⩾", FontCategory.BLACKBOARD_BOLD))
        put("eqslantless", SymbolRenderInfo("⪕", FontCategory.BLACKBOARD_BOLD))
        put("eqslantgtr", SymbolRenderInfo("⪖", FontCategory.BLACKBOARD_BOLD))
        put("lessgtr", SymbolRenderInfo("≶", FontCategory.BLACKBOARD_BOLD))
        put("gtrless", SymbolRenderInfo("≷", FontCategory.BLACKBOARD_BOLD))
        put("lesssim", SymbolRenderInfo("≲", FontCategory.BLACKBOARD_BOLD))
        put("gtrsim", SymbolRenderInfo("≳", FontCategory.BLACKBOARD_BOLD))
        put("lessapprox", SymbolRenderInfo("⪅", FontCategory.BLACKBOARD_BOLD))
        put("gtrapprox", SymbolRenderInfo("⪆", FontCategory.BLACKBOARD_BOLD))
        put("precsim", SymbolRenderInfo("≾", FontCategory.BLACKBOARD_BOLD))
        put("succsim", SymbolRenderInfo("≿", FontCategory.BLACKBOARD_BOLD))
        put("precapprox", SymbolRenderInfo("⪷", FontCategory.BLACKBOARD_BOLD))
        put("succapprox", SymbolRenderInfo("⪸", FontCategory.BLACKBOARD_BOLD))
        put("trianglelefteq", SymbolRenderInfo("⊴", FontCategory.BLACKBOARD_BOLD))
        put("trianglerighteq", SymbolRenderInfo("⊵", FontCategory.BLACKBOARD_BOLD))
        put("vDash", SymbolRenderInfo("⊨", FontCategory.BLACKBOARD_BOLD))
        put("Vdash", SymbolRenderInfo("⊩", FontCategory.BLACKBOARD_BOLD))
        put("Vvdash", SymbolRenderInfo("⊪", FontCategory.BLACKBOARD_BOLD))
        put("models", SymbolRenderInfo("⊧", FontCategory.BLACKBOARD_BOLD))

        // === 杂项符号 → KaTeX_Main ===
        put("infty", main("∞"))
        put("partial", main("∂"))
        put("nabla", main("∇"))
        put("forall", main("∀"))
        put("exists", main("∃"))
        put("nexists", main("∄"))
        put("neg", main("¬"))
        put("lnot", main("¬"))
        put("emptyset", main("∅"))
        put("varnothing", main("∅"))
        put("top", main("⊤"))
        put("bot", main("⊥"))
        put("prime", main("′"))
        put("hbar", main("ℏ"))
        put("ell", main("ℓ"))
        put("wp", main("℘"))
        put("Re", main("ℜ"))
        put("Im", main("ℑ"))
        put("aleph", main("ℵ"))
        put("not", main("/"))       // negation slash
        put("triangle", main("△"))
        put("S", main("§"))
        put("P", main("¶"))
        put("dagger", main("†"))
        put("ddagger", main("‡"))
        put("clubsuit", main("♣"))
        put("diamondsuit", main("♢"))
        put("heartsuit", main("♡"))
        put("spadesuit", main("♠"))
        put("imath", math("ı"))
        put("jmath", math("ȷ"))

        // === 省略号 → KaTeX_Main ===
        put("ldots", main("…"))
        put("cdots", main("⋯"))
        put("vdots", main("⋮"))
        put("ddots", main("⋱"))

        // === 其他缺失符号 → KaTeX_Main ===
        put("therefore", main("∴"))
        put("because", main("∵"))
        put("angle", main("∠"))
        put("degree", main("°"))
        put("triangleright", main("▷"))
        put("triangleleft", main("◁"))

        // === 定界符 → KaTeX_Main ===
        put("lbrace", main("{"))
        put("lacc", main("{"))
        put("rbrace", main("}"))
        put("racc", main("}"))
        put("langle", main("⟨"))
        put("rangle", main("⟩"))
        put("lfloor", main("⌊"))
        put("rfloor", main("⌋"))
        put("lceil", main("⌈"))
        put("rceil", main("⌉"))
        put("vert", main("∣"))
        put("Vert", main("∥"))
        put("lvert", main("∣"))
        put("rvert", main("∣"))
        put("lVert", main("∥"))
        put("rVert", main("∥"))
        put("lgroup", main("⟮"))
        put("rgroup", main("⟯"))
        put("lmoustache", main("⎰"))
        put("rmoustache", main("⎱"))
        put("lbrack", main("["))
        put("rbrack", main("]"))
        put("lsqbrack", main("["))
        put("rsqbrack", main("]"))

        // === 标点与杂项 ===
        put("faculty", main("!"))
        put("mathsharp", main("#"))
        put("textdollar", main("$"))
        put("textpercent", main("%"))
        put("textampersand", main("&"))
        put("colon", main(":"))
        put("semicolon", main(";"))
        put("equals", main("="))
        put("Relbar", main("="))
        put("question", main("?"))
        put("matharobase", main("@"))
        put("plus", main("+"))
        put("textminus", main("-"))
        put("textfractionsolidus", main("/"))
        put("slashdel", main("/"))
        put("ldotp", main("."))
        put("normaldot", main("."))
        put("comma", main(","))
        put("lt", main("<"))
        put("gt", main(">"))
        put("slash", main("/"))
        put("smallint", main("∫"))
        put("sqrt", main("√"))
        put("surdsign", main("√"))

        // === 装饰重音符号 → KaTeX_Main ===
        put("hat", main("^"))
        put("dot", main("˙"))
        put("grave", main("`"))
        put("acute", main("´"))
        put("check", main("ˇ"))
        put("breve", main("˘"))
        put("bar", main("¯"))
        put("mathring", main("˚"))
        put("bmathring", main("˚"))
        put("tilde", main("~"))
        put("ddot", main("¨"))
        put("vec", main("⃗"))
        put("flat", main("♭"))
        put("natural", main("♮"))
        put("sharp", main("♯"))
        put("smile", main("⌣"))
        put("frown", main("⌢"))

        // === 大型运算符 → KaTeX_Main (Display 模式字号放大) ===
        put("sum", main("∑"))
        put("prod", main("∏"))
        put("coprod", main("∐"))
        put("int", main("∫"))
        put("oint", main("∮"))
        put("bigcup", main("⋃"))
        put("bigcap", main("⋂"))
        put("bigvee", main("⋁"))
        put("bigwedge", main("⋀"))
        put("bigoplus", main("⨁"))
        put("bigotimes", main("⨂"))
        put("bigsqcup", main("⨆"))
        put("bigodot", main("⨀"))
        put("biguplus", main("⨄"))
        put("bigtriangleup", main("△"))
        put("bigtriangledown", main("▽"))
        put("iiiint", main("⨌"))
        put("idotsint", main("∫⋯∫"))
        put("oiint", main("∬"))
        put("oiiint", main("∭"))

        // === 宽装饰 ===
        put("widehat", main("^"))
        put("widetilde", main("~"))

        // === AMS 额外常用符号 ===
        put("checkmark", SymbolRenderInfo("✓", FontCategory.BLACKBOARD_BOLD))
        put("complement", SymbolRenderInfo("∁", FontCategory.BLACKBOARD_BOLD))
        put("eth", SymbolRenderInfo("ð", FontCategory.BLACKBOARD_BOLD))
        put("mho", SymbolRenderInfo("℧", FontCategory.BLACKBOARD_BOLD))
        put("twoheadrightarrow", SymbolRenderInfo("↠", FontCategory.BLACKBOARD_BOLD))
        put("twoheadleftarrow", SymbolRenderInfo("↞", FontCategory.BLACKBOARD_BOLD))
        put("leftleftarrows", SymbolRenderInfo("⇇", FontCategory.BLACKBOARD_BOLD))
        put("rightrightarrows", SymbolRenderInfo("⇉", FontCategory.BLACKBOARD_BOLD))
        put("leftrightarrows", SymbolRenderInfo("⇆", FontCategory.BLACKBOARD_BOLD))
        put("rightleftarrows", SymbolRenderInfo("⇄", FontCategory.BLACKBOARD_BOLD))
        put("Lsh", SymbolRenderInfo("↰", FontCategory.BLACKBOARD_BOLD))
        put("Rsh", SymbolRenderInfo("↱", FontCategory.BLACKBOARD_BOLD))
        put("upharpoonleft", SymbolRenderInfo("↿", FontCategory.BLACKBOARD_BOLD))
        put("upharpoonright", SymbolRenderInfo("↾", FontCategory.BLACKBOARD_BOLD))
        put("downharpoonleft", SymbolRenderInfo("⇃", FontCategory.BLACKBOARD_BOLD))
        put("downharpoonright", SymbolRenderInfo("⇂", FontCategory.BLACKBOARD_BOLD))
        put("rightarrowtail", SymbolRenderInfo("↣", FontCategory.BLACKBOARD_BOLD))
        put("leftarrowtail", SymbolRenderInfo("↢", FontCategory.BLACKBOARD_BOLD))
        put("looparrowright", SymbolRenderInfo("↬", FontCategory.BLACKBOARD_BOLD))
        put("looparrowleft", SymbolRenderInfo("↫", FontCategory.BLACKBOARD_BOLD))
        put("curvearrowright", SymbolRenderInfo("↷", FontCategory.BLACKBOARD_BOLD))
        put("curvearrowleft", SymbolRenderInfo("↶", FontCategory.BLACKBOARD_BOLD))
        put("circlearrowright", SymbolRenderInfo("↻", FontCategory.BLACKBOARD_BOLD))
        put("circlearrowleft", SymbolRenderInfo("↺", FontCategory.BLACKBOARD_BOLD))
        put("dashrightarrow", SymbolRenderInfo("⇢", FontCategory.BLACKBOARD_BOLD))
        put("dashleftarrow", SymbolRenderInfo("⇠", FontCategory.BLACKBOARD_BOLD))
        put("multimap", SymbolRenderInfo("⊸", FontCategory.BLACKBOARD_BOLD))
        put("leftrightsquigarrow", SymbolRenderInfo("↭", FontCategory.BLACKBOARD_BOLD))
        put("rightsquigarrow", SymbolRenderInfo("⇝", FontCategory.BLACKBOARD_BOLD))
        put("Rrightarrow", SymbolRenderInfo("⇛", FontCategory.BLACKBOARD_BOLD))
        put("Lleftarrow", SymbolRenderInfo("⇚", FontCategory.BLACKBOARD_BOLD))
        put("rightarrowback", SymbolRenderInfo("⇠", FontCategory.BLACKBOARD_BOLD))
        put("twoheadrightarrowtail", SymbolRenderInfo("⤳", FontCategory.BLACKBOARD_BOLD))
        put("nleftarrow", SymbolRenderInfo("↚", FontCategory.BLACKBOARD_BOLD))
        put("nrightarrow", SymbolRenderInfo("↛", FontCategory.BLACKBOARD_BOLD))
        put("nLeftarrow", SymbolRenderInfo("⇍", FontCategory.BLACKBOARD_BOLD))
        put("nRightarrow", SymbolRenderInfo("⇏", FontCategory.BLACKBOARD_BOLD))
        put("nLeftrightarrow", SymbolRenderInfo("⇎", FontCategory.BLACKBOARD_BOLD))
        put("nleftrightarrow", SymbolRenderInfo("↮", FontCategory.BLACKBOARD_BOLD))
        put("Subset", SymbolRenderInfo("⋐", FontCategory.BLACKBOARD_BOLD))
        put("Supset", SymbolRenderInfo("⋑", FontCategory.BLACKBOARD_BOLD))
        put("Cap", SymbolRenderInfo("⋒", FontCategory.BLACKBOARD_BOLD))
        put("Cup", SymbolRenderInfo("⋓", FontCategory.BLACKBOARD_BOLD))
        put("pitchfork", SymbolRenderInfo("⋔", FontCategory.BLACKBOARD_BOLD))
        put("lessdot", SymbolRenderInfo("⋖", FontCategory.BLACKBOARD_BOLD))
        put("gtrdot", SymbolRenderInfo("⋗", FontCategory.BLACKBOARD_BOLD))
        put("lll", SymbolRenderInfo("⋘", FontCategory.BLACKBOARD_BOLD))
        put("ggg", SymbolRenderInfo("⋙", FontCategory.BLACKBOARD_BOLD))
        put("eqcirc", SymbolRenderInfo("≖", FontCategory.BLACKBOARD_BOLD))
        put("circeq", SymbolRenderInfo("≗", FontCategory.BLACKBOARD_BOLD))
        put("triangleq", SymbolRenderInfo("≜", FontCategory.BLACKBOARD_BOLD))
        put("bumpeq", SymbolRenderInfo("≏", FontCategory.BLACKBOARD_BOLD))
        put("Bumpeq", SymbolRenderInfo("≎", FontCategory.BLACKBOARD_BOLD))
        put("doteqdot", SymbolRenderInfo("≑", FontCategory.BLACKBOARD_BOLD))
        put("fallingdotseq", SymbolRenderInfo("≒", FontCategory.BLACKBOARD_BOLD))
        put("risingdotseq", SymbolRenderInfo("≓", FontCategory.BLACKBOARD_BOLD))
        put("backsim", SymbolRenderInfo("∽", FontCategory.BLACKBOARD_BOLD))
        put("backsimeq", SymbolRenderInfo("⋍", FontCategory.BLACKBOARD_BOLD))
        put("between", SymbolRenderInfo("≬", FontCategory.BLACKBOARD_BOLD))
        put("bowtie", SymbolRenderInfo("⋈", FontCategory.BLACKBOARD_BOLD))
        put("smallsmile", main("⌣"))
        put("smallfrown", main("⌢"))
        put("sqsubset", SymbolRenderInfo("⊏", FontCategory.BLACKBOARD_BOLD))
        put("sqsupset", SymbolRenderInfo("⊐", FontCategory.BLACKBOARD_BOLD))
        put("vartriangleleft", SymbolRenderInfo("⊲", FontCategory.BLACKBOARD_BOLD))
        put("vartriangleright", SymbolRenderInfo("⊳", FontCategory.BLACKBOARD_BOLD))
        put("blacktriangleleft", SymbolRenderInfo("◀", FontCategory.BLACKBOARD_BOLD))
        put("blacktriangleright", SymbolRenderInfo("▶", FontCategory.BLACKBOARD_BOLD))
        put("blacktriangle", SymbolRenderInfo("▲", FontCategory.BLACKBOARD_BOLD))
        put("blacktriangledown", SymbolRenderInfo("▼", FontCategory.BLACKBOARD_BOLD))
        put("blacksquare", SymbolRenderInfo("■", FontCategory.BLACKBOARD_BOLD))
        put("square", SymbolRenderInfo("□", FontCategory.BLACKBOARD_BOLD))
        put("lozenge", SymbolRenderInfo("◊", FontCategory.BLACKBOARD_BOLD))
        put("circledS", SymbolRenderInfo("Ⓢ", FontCategory.BLACKBOARD_BOLD))
        put("measuredangle", SymbolRenderInfo("∡", FontCategory.BLACKBOARD_BOLD))
        put("sphericalangle", SymbolRenderInfo("∢", FontCategory.BLACKBOARD_BOLD))
        put("backprime", SymbolRenderInfo("‵", FontCategory.BLACKBOARD_BOLD))
        put("Finv", SymbolRenderInfo("Ⅎ", FontCategory.BLACKBOARD_BOLD))
        put("Game", SymbolRenderInfo("⅁", FontCategory.BLACKBOARD_BOLD))
        put("diagup", SymbolRenderInfo("╱", FontCategory.BLACKBOARD_BOLD))
        put("diagdown", SymbolRenderInfo("╲", FontCategory.BLACKBOARD_BOLD))
        put("beth", SymbolRenderInfo("ℶ", FontCategory.BLACKBOARD_BOLD))
        put("gimel", SymbolRenderInfo("ℷ", FontCategory.BLACKBOARD_BOLD))
        put("daleth", SymbolRenderInfo("ℸ", FontCategory.BLACKBOARD_BOLD))
        put("hslash", main("ℏ"))
        put("blacklozenge", SymbolRenderInfo("⧫", FontCategory.BLACKBOARD_BOLD))
        put("bigstar", main("★"))
        put("Bbbk", SymbolRenderInfo("𝕜", FontCategory.BLACKBOARD_BOLD))
    }

    /**
     * Unicode 字符 → SymbolRenderInfo 的反向映射表
     *
     * 当 LatexNode.Symbol 的 unicode 字段是 Unicode 字符时，
     * 通过此表路由到正确的字体。
     *
     * 由 symbolMap 自动生成，避免重复维护。
     */
    private val unicodeToSymbolMap: Map<String, SymbolRenderInfo> by lazy {
        buildMap {
            for ((_, info) in symbolMap) {
                // 仅保留首次出现的映射（避免别名覆盖）
                if (!containsKey(info.texGlyph)) {
                    put(info.texGlyph, info)
                }
            }
        }
    }

    /**
     * 解析符号对应的 Unicode 字符和字体信息
     *
     * KaTeX 字体使用标准 Unicode，直接返回 Unicode 字符和对应的字体类别。
     *
     * @param symbolName LaTeX 符号命令名（不含反斜杠）或 Unicode 字符
     * @param fontFamilies 已加载的字体家族集合
     * @return 符号渲染信息，null 表示该符号无需特殊字体路由
     */
    fun resolveSymbol(
        symbolName: String,
        fontFamilies: LatexFontFamilies?
    ): SymbolRenderInfo? {
        if (fontFamilies == null) return null
        // 1. 优先按命令名查找
        symbolMap[symbolName]?.let { return it }
        // 2. 按 Unicode 字符反向查找
        return unicodeToSymbolMap[symbolName]
    }

    /**
     * 根据 SymbolRenderInfo 获取实际的 FontFamily
     */
    fun getFontForSymbol(
        info: SymbolRenderInfo,
        fontFamilies: LatexFontFamilies?
    ): FontFamily? {
        return getFont(info.fontCategory, fontFamilies)
    }

    /**
     * 解析给定字体类别应使用的字体家族
     */
    fun resolve(
        category: FontCategory,
        fontFamilies: LatexFontFamilies?
    ): FontFamily? {
        return getFont(category, fontFamilies)
    }

    /**
     * 将定界符 Unicode 字符转换为渲染用字符
     *
     * KaTeX 字体使用标准 Unicode 编码，直接返回原字符。
     */
    fun resolveDelimiterGlyph(delimiter: String, fontFamilies: LatexFontFamilies?): String {
        return delimiter
    }

    /**
     * 获取定界符渲染上下文
     *
     * KaTeX 策略：根据缩放比例选择 Main / Size1 / Size2 / Size3 / Size4 字体。
     * 每个 Size 字体包含独立设计的定界符字形，笔画粗细一致，无需 FontWeight 补偿。
     *
     * @param context 当前渲染上下文
     * @param delimiter 定界符字符
     * @param scale 缩放比例（1.0 = 原始大小）
     */
    fun delimiterContext(
        context: RenderContext,
        delimiter: String = "(",
        scale: Float = 1.0f
    ): RenderContext {
        val fontFamily = resolveDelimiterFont(context.fontFamilies, scale)
            ?: context.fontFamily

        return context.copy(
            fontStyle = FontStyle.Normal,
            fontFamily = fontFamily,
            fontWeight = FontWeight.Normal
        )
    }

    /**
     * 根据缩放比例选择定界符字体
     *
     * KaTeX 定界符字体选择规则：
     * - scale <= 1.0  → Main (行内默认大小)
     * - scale <= 1.2  → Size1 (\big)
     * - scale <= 1.8  → Size2 (\Big)
     * - scale <= 2.4  → Size3 (\bigg)
     * - scale > 2.4   → Size4 (\Bigg)
     */
    fun resolveDelimiterFont(
        fontFamilies: LatexFontFamilies?,
        scale: Float
    ): FontFamily? {
        if (fontFamilies == null) return null
        return when {
            scale <= 1.0f -> fontFamilies.main
            scale <= 1.2f -> fontFamilies.size1
            scale <= 1.8f -> fontFamilies.size2
            scale <= 2.4f -> fontFamilies.size3
            else -> fontFamilies.size4
        }
    }

    /**
     * \big/\Big/\bigg/\Bigg 手动大小命令到 Size 字体的直接映射
     *
     * 手动大小定界符直接使用对应的 Size 字体字形（不缩放 fontSize），
     * 因为 KaTeX Size1~4 字体的字形已按正确大小设计。
     */
    fun manualDelimiterFont(
        fontFamilies: LatexFontFamilies?,
        scaleFactor: Float
    ): FontFamily? {
        if (fontFamilies == null) return null
        return when {
            scaleFactor <= 1.0f -> fontFamilies.main
            scaleFactor <= 1.2f -> fontFamilies.size1   // \big  = 1.2
            scaleFactor <= 1.8f -> fontFamilies.size2   // \Big  = 1.8
            scaleFactor <= 2.4f -> fontFamilies.size3   // \bigg = 2.4
            else -> fontFamilies.size4                   // \Bigg = 3.0
        }
    }

    /**
     * 根据缩放比例计算补偿后的 FontWeight
     */
    fun compensatedFontWeight(baseWeight: Int, scaleFactor: Float): FontWeight {
        val compensated = when {
            scaleFactor <= 1.0f -> baseWeight
            scaleFactor >= 2.0f -> 100
            else -> {
                val t = (scaleFactor - 1.0f) / 1.0f
                (baseWeight - t * (baseWeight - 100)).toInt().coerceIn(100, baseWeight)
            }
        }
        return FontWeight(compensated)
    }

    // =========================================================================
    // 私有工具方法
    // =========================================================================

    private fun getFont(category: FontCategory, fontFamilies: LatexFontFamilies?): FontFamily? {
        if (fontFamilies == null) return null
        return when (category) {
            FontCategory.ROMAN -> fontFamilies.main
            FontCategory.MATH_ITALIC -> fontFamilies.math
            FontCategory.SYMBOL -> fontFamilies.main      // KaTeX: 运算符在 Main 中
            FontCategory.EXTENSION -> fontFamilies.size1   // KaTeX: 大型定界符用 Size1
            FontCategory.SANS_SERIF -> fontFamilies.sansSerif
            FontCategory.MONOSPACE -> fontFamilies.monospace
            FontCategory.BLACKBOARD_BOLD -> fontFamilies.ams
            FontCategory.CALLIGRAPHIC -> fontFamilies.caligraphic
            FontCategory.FRAKTUR -> fontFamilies.fraktur
            FontCategory.SCRIPT -> fontFamilies.script
        }
    }
}
