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

package com.github.zly2006.zhihu.account

import com.github.zly2006.zhihu.platform.nativeAccountFilePath
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding

internal val defaultNativeAccountStore by lazy {
    ZhihuAccountStore(
        repository = ZhihuAccountRepository(NativeFileAccountSessionStore()),
    )
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private class NativeFileAccountSessionStore : ZhihuAccountSessionStore {
    private val filePath: String by lazy(::nativeAccountFilePath)

    override fun readText(): String? {
        val fm = NSFileManager.defaultManager
        if (!fm.fileExistsAtPath(filePath)) return null
        val data = fm.contentsAtPath(filePath) ?: return null
        val nsString = NSString.create(data = data, encoding = NSUTF8StringEncoding) ?: return null
        return nsString.toString()
    }

    override fun writeText(text: String) {
        val fm = NSFileManager.defaultManager
        val parentPath = filePath.substringBeforeLast("/")
        if (!fm.fileExistsAtPath(parentPath)) {
            fm.createDirectoryAtPath(parentPath, withIntermediateDirectories = true, attributes = null, error = null)
        }
        val nsString = NSString.create(string = text)
        fm.createFileAtPath(filePath, contents = nsString.dataUsingEncoding(NSUTF8StringEncoding), attributes = null)
    }

    override fun delete() {
        NSFileManager.defaultManager.removeItemAtPath(filePath, error = null)
    }
}
