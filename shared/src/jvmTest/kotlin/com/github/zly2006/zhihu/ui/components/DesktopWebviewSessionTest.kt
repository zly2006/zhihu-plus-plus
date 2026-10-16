package com.github.zly2006.zhihu.ui.components

import com.sun.net.httpserver.HttpServer
import javafx.application.Platform
import java.net.InetSocketAddress
import java.util.concurrent.CompletableFuture
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DesktopWebviewSessionTest {
    @AfterTest
    fun stopToolkit() = Platform.exit()

    @Test
    fun sessionSharesWebKitCookieStoreAndSkipsDuplicateLoads() {
        ensureToolkit()
        val requests = LinkedBlockingQueue<String>()
        val reportedCookies = LinkedBlockingQueue<Map<String, String>>()
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            requests.put(exchange.requestHeaders.getFirst("Cookie").orEmpty())
            exchange.responseHeaders.add("Content-Type", "text/html; charset=utf-8")
            // 知乎的登录凭据是 HttpOnly，document.cookie 读不到，只有 WebKit 的 Cookie 存储能看到。
            exchange.responseHeaders.add("Set-Cookie", "z_c0=authenticated==; HttpOnly; Path=/")
            val body = "<html><body id='content'>loaded</body></html>".toByteArray()
            exchange.sendResponseHeaders(200, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }
        server.start()
        val loaded = CompletableFuture<String?>()
        val session = onFx { DesktopWebviewSession(loaded::complete, reportedCookies::put) }
        try {
            val url = "http://127.0.0.1:${server.address.port}/"
            onFx { session.load(url, "WebviewRegression", mapOf("initial" to "before-request")) }
            assertEquals(url, loaded.get(20, TimeUnit.SECONDS))
            assertTrue(
                awaitRequest(requests).contains("initial=before-request"),
                "初始 cookie 必须出现在首个网络请求中",
            )
            assertEquals("authenticated==", awaitCookies(reportedCookies, "z_c0")["z_c0"])
            onFx {
                val engine = session.view.engine
                assertEquals("loaded", engine.executeScript("document.getElementById('content').textContent"))
                assertTrue(!(engine.executeScript("document.cookie") as String).contains("z_c0="))
                engine.executeScript("setTimeout(function(){document.cookie='async=changed; Path=/'}, 100)")
            }
            assertEquals("changed", awaitCookies(reportedCookies, "async")["async"])

            onFx { session.load(url, "WebviewRegression", emptyMap()) }
            assertEquals(null, requests.poll(1, TimeUnit.SECONDS), "相同 URL 不应重复加载")

            onFx {
                session.close()
                session.load("${url}after-close", "WebviewRegression", emptyMap())
            }
            assertEquals(null, requests.poll(1, TimeUnit.SECONDS), "关闭后的会话不应再加载页面")
        } finally {
            onFx { session.close() }
            server.stop(0)
        }
    }

    private fun awaitRequest(queue: LinkedBlockingQueue<String>): String =
        queue.poll(10, TimeUnit.SECONDS) ?: error("页面没有发出请求")

    private fun awaitCookies(queue: LinkedBlockingQueue<Map<String, String>>, key: String): Map<String, String> {
        val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
        while (System.nanoTime() < deadline) {
            val cookies = queue.poll(1, TimeUnit.SECONDS) ?: continue
            if (key in cookies) return cookies
        }
        error("没有上报 cookie $key")
    }

    companion object {
        private var initialized = false

        @Synchronized
        private fun ensureToolkit() {
            if (initialized) return
            val ready = CompletableFuture<Unit>()
            Platform.startup {
                Platform.setImplicitExit(false)
                ready.complete(Unit)
            }
            ready.get(20, TimeUnit.SECONDS)
            initialized = true
        }

        private fun <T> onFx(action: () -> T): T {
            val result = CompletableFuture<T>()
            Platform.runLater {
                try {
                    result.complete(action())
                } catch (error: Throwable) {
                    result.completeExceptionally(error)
                }
            }
            return result.get(20, TimeUnit.SECONDS)
        }
    }
}
