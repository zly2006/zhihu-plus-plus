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

package com.github.zly2006.zhihu.editor

data class UploadedZhihuImage(
    val url: String,
    val originalUrl: String,
    val watermark: Boolean? = null,
    val watermarkMode: String? = null,
    val watermarkUrl: String? = null,
    val rawWidth: Int,
    val rawHeight: Int,
    val imageId: String? = null,
)

class UnknownImageFormatException(
    message: String = "无法识别图片格式，已取消上传",
) : IllegalArgumentException(message)

internal enum class ZhihuImageUploadSource(
    val apiValue: String,
) {
    Article("article"),
    Pin("pin"),
}
