/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

@file:OptIn(kotlinx.cinterop.BetaInteropApi::class, kotlinx.cinterop.ExperimentalForeignApi::class)

package com.github.zly2006.zhihu.account

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.readBytes
import kotlinx.cinterop.reinterpret
import org.jetbrains.skia.Image
import platform.AppKit.NSBitmapImageFileType.NSBitmapImageFileTypePNG
import platform.AppKit.NSBitmapImageRep
import platform.AppKit.NSCIImageRep
import platform.AppKit.NSImage
import platform.AppKit.representationUsingType
import platform.CoreGraphics.CGAffineTransformMakeScale
import platform.CoreImage.CIFilter
import platform.CoreImage.filterWithName
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding
import platform.Foundation.setValue
import platform.darwin.NSObject

actual fun generateQrLoginBitmap(content: String): ImageBitmap {
    val messageData = NSString.create(string = content).dataUsingEncoding(NSUTF8StringEncoding)
        ?: error("无法编码二维码内容")
    val filter = CIFilter.filterWithName("CIQRCodeGenerator")
        ?: error("macOS 不支持二维码生成器")
    (filter as NSObject).setValue(messageData, forKey = "inputMessage")
    (filter as NSObject).setValue("M", forKey = "inputCorrectionLevel")
    val outputImage = filter.outputImage
        ?.imageByApplyingTransform(CGAffineTransformMakeScale(10.0, 10.0))
        ?: error("无法生成二维码图像")
    val imageRepresentation = NSCIImageRep.imageRepWithCIImage(outputImage)
    val image = NSImage(size = imageRepresentation.size)
    image.addRepresentation(imageRepresentation)
    val tiffData = image.TIFFRepresentation ?: error("无法读取二维码图像")
    val bitmapRepresentation = NSBitmapImageRep.imageRepWithData(tiffData)
        ?: error("无法转换二维码图像")
    val pngData = bitmapRepresentation.representationUsingType(
        storageType = NSBitmapImageFileTypePNG,
        properties = emptyMap<Any?, Any?>(),
    ) ?: error("无法编码二维码图像")
    val bytes = pngData.bytes?.reinterpret<ByteVar>()?.readBytes(pngData.length.toInt())
        ?: error("二维码图像数据为空")
    return Image.makeFromEncoded(bytes).toComposeImageBitmap()
}
