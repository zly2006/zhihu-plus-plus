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

package com.github.zly2006.zhihu.shared.account

import com.github.zly2006.zhihu.shared.data.installZhihuCommonClientConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSString
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSURL
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.create
import platform.Foundation.dataUsingEncoding

class IosAccountStore {
    private val accountClient = defaultIosAccountClient

    fun load(): ZhihuAccountSession = accountClient.load()

    fun save(session: ZhihuAccountSession) = accountClient.save(session)

    fun clear() = accountClient.clear()

    fun httpClient(): HttpClient = accountClient.httpClient()

    fun createHttpClient(cookies: MutableMap<String, String>): HttpClient =
        accountClient.temporaryHttpClient(cookies)
}

private val defaultIosAccountClient by lazy {
    ZhihuAccountClient(
        repository = ZhihuAccountRepository(IosFileAccountSessionStore()),
        createClient = { cookies, session, onCookieChanged, _ ->
            HttpClient(Darwin) {
                installZhihuCommonClientConfig(
                    cookies = cookies,
                    userAgent = session.userAgent,
                    onCookieChanged = onCookieChanged,
                )
            }
        },
    )
}

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
private class IosFileAccountSessionStore : ZhihuAccountSessionStore {
    private val filePath: String by lazy {
        val fm = NSFileManager.defaultManager
        val urls = fm.URLsForDirectory(NSDocumentDirectory, inDomains = NSUserDomainMask)
        val docsUrl = urls.firstOrNull() as? NSURL
        val docsDir = docsUrl?.path ?: NSTemporaryDirectory()
        "$docsDir/account.json"
    }

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

    override fun delete() = NSFileManager.defaultManager.removeItemAtPath(filePath, error = null).let {}
}
