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

package com.hrm.latex.renderer.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily

/**
 * LaTeX 字体家族配置（基于 KaTeX 字体）
 * LaTeX Font Families Configuration (KaTeX-based)
 *
 * 定义了 LaTeX 渲染所需的核心字体家族。
 * KaTeX 字体使用标准 Unicode 编码，无需 TeX 编码映射。
 *
 * ## 使用场景速查 | Quick Reference
 *
 * | 字段 Field | 实际字体 Font | LaTeX 命令 | 使用场景 Use Case |
 * |-----------|--------------|-----------|------------------|
 * | `main` | KaTeX_Main (Regular/Bold/Italic/BoldItalic) | `\text{}`, `\mathrm{}` | 正文文本、数字、标点、运算符 |
 * | `math` | KaTeX_Math (Italic/BoldItalic) | (默认) | 数学变量: x, y, α, β |
 * | `ams` | KaTeX_AMS-Regular | `\mathbb{}` | AMS 符号、黑板粗体: ℝ, ℕ, ℤ |
 * | `sansSerif` | KaTeX_SansSerif (Regular/Bold/Italic) | `\mathsf{}` | 无衬线文本 |
 * | `monospace` | KaTeX_Typewriter-Regular | `\mathtt{}` | 等宽打字机体 |
 * | `caligraphic` | KaTeX_Caligraphic (Regular/Bold) | `\mathcal{}` | 花体 |
 * | `fraktur` | KaTeX_Fraktur (Regular/Bold) | `\mathfrak{}` | 哥特体 |
 * | `script` | KaTeX_Script-Regular | `\mathscr{}` | 手写花体 |
 * | `size1~size4` | KaTeX_Size1~4 | `\big`, `\Big`, `\bigg`, `\Bigg` | 定界符尺寸 |
 */
data class LatexFontFamilies(
    // === 主字体 Main Font (文本 + 基本数学符号) ===
    val main: FontFamily,             // KaTeX_Main — \text{}, \mathrm{}, 数字, 标点, 运算符, 定界符
    // === 数学字体 Math Font ===
    val math: FontFamily,             // KaTeX_Math — 数学变量默认字体 (斜体)
    // === AMS 符号字体 ===
    val ams: FontFamily,              // KaTeX_AMS — \mathbb{}, AMS 扩展符号
    // === 无衬线 & 等宽 ===
    val sansSerif: FontFamily,        // KaTeX_SansSerif — \mathsf{}
    val monospace: FontFamily,        // KaTeX_Typewriter — \mathtt{}
    // === 装饰字体 ===
    val caligraphic: FontFamily,      // KaTeX_Caligraphic — \mathcal{}
    val fraktur: FontFamily,          // KaTeX_Fraktur — \mathfrak{}
    val script: FontFamily,           // KaTeX_Script — \mathscr{}
    // === 定界符尺寸字体 ===
    val size1: FontFamily,            // KaTeX_Size1 — \big
    val size2: FontFamily,            // KaTeX_Size2 — \Big
    val size3: FontFamily,            // KaTeX_Size3 — \bigg
    val size4: FontFamily,            // KaTeX_Size4 — \Bigg

    // === 字体字节数据 Font Bytes（用于精确 glyph bounds 测量） ===
    /** 主字体字节数据 — 精确墨迹边界测量所需 */
    val mainBytes: ByteArray? = null,
    /** 数学字体字节数据 — 数学斜体精确测量所需 */
    val mathBytes: ByteArray? = null,
    /** AMS 字体字节数据 */
    val amsBytes: ByteArray? = null,
    /** Size1 字体字节数据 — 定界符精确测量所需 */
    val size1Bytes: ByteArray? = null,
    val size2Bytes: ByteArray? = null,
    val size3Bytes: ByteArray? = null,
    val size4Bytes: ByteArray? = null,

    ) {
    /** 根据字体类别获取对应的 FontFamily */
    fun getFont(category: String): FontFamily? = when (category) {
        "main" -> main
        "math" -> math
        "ams" -> ams
        "sansSerif" -> sansSerif
        "monospace" -> monospace
        "caligraphic" -> caligraphic
        "fraktur" -> fraktur
        "script" -> script
        "size1" -> size1
        "size2" -> size2
        "size3" -> size3
        "size4" -> size4
        else -> null
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as LatexFontFamilies

        if (main != other.main) return false
        if (math != other.math) return false
        if (ams != other.ams) return false
        if (sansSerif != other.sansSerif) return false
        if (monospace != other.monospace) return false
        if (caligraphic != other.caligraphic) return false
        if (fraktur != other.fraktur) return false
        if (script != other.script) return false
        if (size1 != other.size1) return false
        if (size2 != other.size2) return false
        if (size3 != other.size3) return false
        if (size4 != other.size4) return false
        if (!mainBytes.contentEquals(other.mainBytes)) return false
        if (!mathBytes.contentEquals(other.mathBytes)) return false
        if (!amsBytes.contentEquals(other.amsBytes)) return false
        if (!size1Bytes.contentEquals(other.size1Bytes)) return false
        if (!size2Bytes.contentEquals(other.size2Bytes)) return false
        if (!size3Bytes.contentEquals(other.size3Bytes)) return false
        if (!size4Bytes.contentEquals(other.size4Bytes)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = main.hashCode()
        result = 31 * result + math.hashCode()
        result = 31 * result + ams.hashCode()
        result = 31 * result + sansSerif.hashCode()
        result = 31 * result + monospace.hashCode()
        result = 31 * result + caligraphic.hashCode()
        result = 31 * result + fraktur.hashCode()
        result = 31 * result + script.hashCode()
        result = 31 * result + size1.hashCode()
        result = 31 * result + size2.hashCode()
        result = 31 * result + size3.hashCode()
        result = 31 * result + size4.hashCode()
        result = 31 * result + (mainBytes?.contentHashCode() ?: 0)
        result = 31 * result + (mathBytes?.contentHashCode() ?: 0)
        result = 31 * result + (amsBytes?.contentHashCode() ?: 0)
        result = 31 * result + (size1Bytes?.contentHashCode() ?: 0)
        result = 31 * result + (size2Bytes?.contentHashCode() ?: 0)
        result = 31 * result + (size3Bytes?.contentHashCode() ?: 0)
        result = 31 * result + (size4Bytes?.contentHashCode() ?: 0)
        return result
    }
}

fun createLatexFontFamilies(font: Font, fontBytes: ByteArray): LatexFontFamilies {
    val family = FontFamily(font)
    return LatexFontFamilies(
        main = family,
        math = family,
        ams = family,
        sansSerif = family,
        monospace = family,
        caligraphic = family,
        fraktur = family,
        script = family,
        size1 = family,
        size2 = family,
        size3 = family,
        size4 = family,
        mainBytes = fontBytes,
        mathBytes = fontBytes,
        amsBytes = fontBytes,
        size1Bytes = fontBytes,
        size2Bytes = fontBytes,
        size3Bytes = fontBytes,
        size4Bytes = fontBytes,
    )
}

/**
 * 获取默认的 LaTeX 字体家族。fork 不打包字体；宿主可通过 [LatexConfig.mathFont]
 * 传入下载后的字体，未提供时使用系统字体降级。
 */
@Composable
internal fun defaultLatexFontFamilies(): LatexFontFamilies {
    val fallback = FontFamily.Default
    return LatexFontFamilies(
        main = fallback,
        math = fallback,
        ams = fallback,
        sansSerif = fallback,
        monospace = fallback,
        caligraphic = fallback,
        fraktur = fallback,
        script = fallback,
        size1 = fallback,
        size2 = fallback,
        size3 = fallback,
        size4 = fallback,
    )
}
