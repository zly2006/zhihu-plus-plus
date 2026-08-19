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

package com.hrm.latex.renderer.export

import androidx.compose.ui.graphics.ImageBitmap
import com.hrm.latex.renderer.model.LatexConfig

/**
 * 导出图片格式
 */
enum class ImageFormat {
    PNG,
    JPEG,
    WEBP
}

/**
 * SVG 中数学文本的输出方式。
 */
enum class SvgTextMode {
    /**
     * 将字形轮廓写成 SVG path。文件可独立分发，且不会因目标环境缺少 KaTeX 字体而变形。
     */
    PATH,

    /**
     * 保留 SVG text。文件更小且文字可选择，但显示端必须提供公式使用的字体。
     */
    TEXT
}

/**
 * SVG 矢量导出配置。
 *
 * SVG 与光栅导出使用独立配置，避免把像素压缩质量等无意义参数带入矢量 API。
 *
 * @property scale 输出画布和字号的缩放倍率。SVG 本身与分辨率无关，通常保持 1 即可；
 *   需要特定物理尺寸时可以调整该值
 * @property transparentBackground 是否输出透明背景；false 时使用 [LatexConfig] 解析后的背景色
 * @property textMode 字形路径或可选择文本
 * @property prettyPrint 是否格式化 SVG XML
 */
data class SvgExportConfig(
    val scale: Float = 1f,
    val transparentBackground: Boolean = true,
    val textMode: SvgTextMode = SvgTextMode.PATH,
    val prettyPrint: Boolean = true
)

/**
 * SVG 矢量导出结果。
 *
 * @property svg UTF-8 SVG 文本，可直接写入 `.svg` 文件或嵌入 Web 页面
 * @property bytes 与 [svg] 相同内容的 UTF-8 字节
 * @property width SVG viewport 宽度
 * @property height SVG viewport 高度
 */
data class SvgExportResult(
    val svg: String,
    val bytes: ByteArray,
    val width: Int,
    val height: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SvgExportResult) return false
        return svg == other.svg &&
                bytes.contentEquals(other.bytes) &&
                width == other.width &&
                height == other.height
    }

    override fun hashCode(): Int {
        var result = svg.hashCode()
        result = 31 * result + bytes.contentHashCode()
        result = 31 * result + width
        result = 31 * result + height
        return result
    }
}

/**
 * 导出配置
 *
 * @property scale 缩放倍率（1.0 = 1x，2.0 = 2x 高清，3.0 = 3x 超高清）
 * @property format 导出图片格式
 * @property transparentBackground 是否使用透明背景（false 时使用 LatexConfig 中配置的背景色）。
 *   注意：JPEG 格式不支持透明背景，即使设置为 true 也会自动使用不透明背景。
 * @property quality 图片压缩质量（1~100），对 JPEG 和 WEBP 有效，PNG 始终为无损压缩
 */
data class ExportConfig(
    val scale: Float = 2f,
    val format: ImageFormat = ImageFormat.PNG,
    val transparentBackground: Boolean = false,
    val quality: Int = 90
)

/**
 * 导出结果
 *
 * 包含渲染后的 [ImageBitmap]（可直接在 Compose 中展示）以及编码后的字节数据。
 * 一次导出同时产出 bitmap 和 bytes，避免重复渲染。
 *
 * @property imageBitmap 渲染后的图片 Bitmap
 * @property bytes 编码后的图片字节数组（格式由 [ExportConfig.format] 决定）；编码失败时为 null
 * @property format 实际使用的导出格式
 */
data class ExportResult(
    val imageBitmap: ImageBitmap,
    val bytes: ByteArray?,
    val format: ImageFormat
) {
    /** 图片宽度（像素） */
    val width: Int get() = imageBitmap.width

    /** 图片高度（像素） */
    val height: Int get() = imageBitmap.height

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as ExportResult
        return imageBitmap == other.imageBitmap &&
                bytes.contentEquals(other.bytes) &&
                format == other.format
    }

    override fun hashCode(): Int {
        var result = imageBitmap.hashCode()
        result = 31 * result + (bytes?.contentHashCode() ?: 0)
        result = 31 * result + format.hashCode()
        return result
    }
}

/**
 * 将 [ImageBitmap] 编码为指定格式的字节数组。
 *
 * 各平台实现：
 * - Android: 使用 Bitmap.compress()
 * - JVM/iOS/JS/WasmJS: 使用 Skia Image.encodeToData()
 *
 * @param format 目标图片格式
 * @param quality 图片压缩质量（1~100），对 JPEG 和 WEBP 有效
 */
expect fun ImageBitmap.encodeToFormat(format: ImageFormat, quality: Int = 100): ByteArray?
