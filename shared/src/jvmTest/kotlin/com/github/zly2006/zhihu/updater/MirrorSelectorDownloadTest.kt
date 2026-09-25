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

package com.github.zly2006.zhihu.updater

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.runBlocking
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray
import java.security.MessageDigest
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

private const val MIRROR_HOST = "ghfast.top"
private const val OFFICIAL_HOST = "github.com"

class MirrorSelectorDownloadTest {
    private val payload = ByteArray(4_096) { index -> (index * 31 % 251).toByte() }
    private val payloadDigest = sha256Hex(payload)
    private val officialUrl = "https://$OFFICIAL_HOST/zly2006/zhihu-plus-plus/releases/download/0.30/zhihu%2B%2B-lite.apk"

    @Test
    fun downloadsFromMirrorAndReportsAppliedVerificationLayers() = runBlocking {
        val engine = MockEngine { request ->
            if (request.url.host == MIRROR_HOST) serve(request, payload) else error("不应回退到官方直连")
        }

        val result = selector().download(HttpClient(engine), request(tempDestination()), DownloadVerifier)

        val verified = assertIs<DownloadResult.Verified>(result)
        assertEquals(MIRROR_HOST, verified.source.name)
        assertEquals(listOf("文件大小", "SHA256 摘要"), verified.layers)
    }

    @Test
    fun fallsBackToOfficialDirectDownloadWhenMirrorProbeFails() = runBlocking {
        val engine = MockEngine { request ->
            when {
                request.url.host == MIRROR_HOST -> respond(
                    ByteReadChannel(ByteArray(0)),
                    HttpStatusCode.NotFound,
                    headersOf(),
                )

                request.url.host == OFFICIAL_HOST -> serve(request, payload)
                else -> error("未知来源 ${request.url.host}")
            }
        }

        val destination = tempDestination()
        val result = selector().download(HttpClient(engine), request(destination), DownloadVerifier)

        val verified = assertIs<DownloadResult.Verified>(result)
        assertEquals(OFFICIAL_DOWNLOAD_SOURCE_NAME, verified.source.name)
        assertContentEquals(payload, readBytes(destination))
    }

    @Test
    fun disablingMirrorsNeverTouchesMirrorHosts() = runBlocking {
        val hosts = mutableListOf<String>()
        val engine = MockEngine { request ->
            hosts += request.url.host
            serve(request, payload)
        }

        val result = selector().download(
            HttpClient(engine),
            request(tempDestination()).copy(mirrorEnabled = false),
            DownloadVerifier,
        )

        assertEquals(OFFICIAL_DOWNLOAD_SOURCE_NAME, assertIs<DownloadResult.Verified>(result).source.name)
        assertEquals(listOf(OFFICIAL_HOST), hosts.distinct())
    }

    @Test
    fun poisonedMirrorIsRejectedAndFullDownloadRestartsFromOfficial() = runBlocking {
        val poisoned = payload.copyOf().also { it[0] = (it[0] + 1).toByte() }
        val officialRanges = mutableListOf<String?>()
        val engine = MockEngine { request ->
            if (request.url.host == MIRROR_HOST) {
                serve(request, poisoned)
            } else {
                officialRanges += request.headers[HttpHeaders.Range]
                serve(request, payload)
            }
        }

        val destination = tempDestination()
        val result = selector().download(HttpClient(engine), request(destination), DownloadVerifier)

        assertEquals(OFFICIAL_DOWNLOAD_SOURCE_NAME, assertIs<DownloadResult.Verified>(result).source.name)
        // 校验失败后必须丢弃投毒内容重新全量下载，不能拿坏字节做断点续传。
        assertEquals(listOf<String?>(null), officialRanges)
        assertContentEquals(payload, readBytes(destination))
    }

    /**
     * 镜像把响应体提前结束时，落盘内容长度必然不足，绝不能被当成完整安装包。
     *
     * 这里只断言跨引擎都成立的行为：内容不足被判为校验失败后，必须丢弃并向下一来源重新全量下载。
     * 若引擎把提前关闭判定为传输异常，则会走断点续传分支（由真实网络的端到端用例覆盖），
     * 两条分支都安全，区别只在是否需要重新传输已下载部分。
     */
    @Test
    fun truncatedMirrorBodyIsRejectedAndFullDownloadRestartsFromOfficial() = runBlocking {
        val cutAt = 512
        val officialRanges = mutableListOf<String?>()
        val engine = MockEngine { request ->
            if (request.url.host == MIRROR_HOST) {
                when {
                    request.method == HttpMethod.Head -> serve(request, payload)
                    request.headers.contains(HttpHeaders.Range) -> serve(request, payload)
                    else -> respond(
                        ByteReadChannel(payload.copyOf(cutAt)),
                        HttpStatusCode.OK,
                        headersOf(HttpHeaders.AcceptRanges, "bytes"),
                    )
                }
            } else {
                officialRanges += request.headers[HttpHeaders.Range]
                serve(request, payload)
            }
        }

        val destination = tempDestination()
        val result = selector().download(HttpClient(engine), request(destination), DownloadVerifier)

        assertEquals(OFFICIAL_DOWNLOAD_SOURCE_NAME, assertIs<DownloadResult.Verified>(result).source.name)
        assertEquals(listOf<String?>(null), officialRanges)
        assertContentEquals(payload, readBytes(destination))
    }

    @Test
    fun reportsFailureWhenEverySourceServesMismatchedContent() = runBlocking {
        val poisoned = payload.copyOf().also { it[0] = (it[0] + 1).toByte() }
        val engine = MockEngine { request -> serve(request, poisoned) }

        val result = selector().download(HttpClient(engine), request(tempDestination()), DownloadVerifier)

        val failed = assertIs<DownloadResult.Failed>(result)
        assertTrue(failed.reason.contains("SHA256"), failed.reason)
    }

    @Test
    fun acceptsDownloadWhenMetadataCarriesNoSize() = runBlocking {
        val engine = MockEngine { request ->
            if (request.url.host == MIRROR_HOST) serve(request, payload) else error("不应回退到官方直连")
        }

        val result = selector().download(
            HttpClient(engine),
            request(tempDestination()).copy(expectedSize = null),
            DownloadVerifier,
        )

        assertEquals(listOf("SHA256 摘要"), assertIs<DownloadResult.Verified>(result).layers)
    }

    private fun selector() = MirrorSelector(listOf(MirrorCandidate("https://$MIRROR_HOST", MirrorUrlForm.Prefix)))

    private fun request(destination: Path) = MirrorDownloadRequest(
        officialUrl = officialUrl,
        destination = destination,
        expectedSize = payload.size.toLong(),
        expectedDigest = payloadDigest,
    )

    private fun tempDestination(): Path =
        Path(createTempDirectory("mirror-download").resolve("update.apk").toString())
}

/** 模拟源站：带 Range 的请求返回对应分片，其余请求返回整体内容。 */
private fun MockRequestHandleScope.serve(request: HttpRequestData, bytes: ByteArray) = run {
    val start = request.headers[HttpHeaders.Range]
        ?.removePrefix("bytes=")
        ?.substringBefore('-')
        ?.toLongOrNull()
    if (start == null) {
        respond(ByteReadChannel(bytes), HttpStatusCode.OK, headersOf(HttpHeaders.AcceptRanges, "bytes"))
    } else {
        respond(
            ByteReadChannel(bytes.copyOfRange(start.toInt(), bytes.size)),
            HttpStatusCode.PartialContent,
            headersOf(
                HttpHeaders.AcceptRanges to listOf("bytes"),
                HttpHeaders.ContentRange to listOf("bytes $start-${bytes.size - 1}/${bytes.size}"),
            ),
        )
    }
}

private fun readBytes(path: Path): ByteArray = SystemFileSystem.source(path).buffered().use { it.readByteArray() }

private fun sha256Hex(bytes: ByteArray): String =
    MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
