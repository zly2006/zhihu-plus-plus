package com.github.zly2006.zhihu.shared.account

import com.github.zly2006.zhihu.shared.data.installZhihuCommonClientConfig
import io.ktor.client.HttpClient
import io.ktor.client.engine.darwin.Darwin
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
    private val repository = ZhihuAccountRepository(IosFileAccountSessionStore())

    fun load(): ZhihuAccountSession = repository.load()

    fun save(session: ZhihuAccountSession) = repository.save(session)

    fun clear() = repository.clear()

    fun createHttpClient(cookies: MutableMap<String, String>): HttpClient = HttpClient(Darwin) {
        val savedData = load()
        installZhihuCommonClientConfig(
            cookies = cookies,
            userAgent = savedData.userAgent,
            onCookieChanged = {
                save(savedData.copy(cookies = cookies.toMutableMap()))
            },
        )
    }
}

@OptIn(ExperimentalForeignApi::class)
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
