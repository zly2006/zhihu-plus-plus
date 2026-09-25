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

import kotlinx.coroutines.runBlocking
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** 标准库算出的 "abc" 摘要，用来和自实现 SHA-256 交叉验证。 */
private const val SHA256_OF_ABC = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"

class DownloadVerifierTest {
    @Test
    fun parseDigestAcceptsOnlySha256Hex() {
        assertEquals(SHA256_OF_ABC, DownloadVerifier.parseDigest("sha256:$SHA256_OF_ABC"))
        assertEquals(SHA256_OF_ABC, DownloadVerifier.parseDigest("  sha256:${SHA256_OF_ABC.uppercase()}  "))

        assertNull(DownloadVerifier.parseDigest(null))
        assertNull(DownloadVerifier.parseDigest(""))
        assertNull(DownloadVerifier.parseDigest(SHA256_OF_ABC))
        assertNull(DownloadVerifier.parseDigest("sha512:$SHA256_OF_ABC"))
        assertNull(DownloadVerifier.parseDigest("sha256:not-a-digest"))
        assertNull(DownloadVerifier.parseDigest("sha256:${SHA256_OF_ABC.dropLast(1)}"))
    }

    /** 显式声明 Unit：assertIs 会返回被断言的实例，直接用作表达式体会让测试方法返回非 void。 */
    @Test
    fun failsWhenFileIsMissing(): Unit = runBlocking {
        val missing = Path(createTempDirectory("verifier-missing").resolve("update.apk").toString())

        assertIs<VerificationOutcome.Failed>(
            DownloadVerifier.verify(missing, expectedSize = 3, expectedDigest = SHA256_OF_ABC),
        )
    }

    @Test
    fun reportsAppliedLayersWhenSizeAndDigestBothMatch() = runBlocking {
        val outcome = DownloadVerifier.verify(tempFileWith("abc"), expectedSize = 3, expectedDigest = SHA256_OF_ABC)

        assertEquals(VerificationOutcome.Passed(listOf("文件大小", "SHA256 摘要")), outcome)
    }

    @Test
    fun failsWhenSizeDiffersFromExpected() = runBlocking {
        val outcome = DownloadVerifier.verify(tempFileWith("abc"), expectedSize = 4, expectedDigest = null)

        val failed = assertIs<VerificationOutcome.Failed>(outcome)
        assertTrue(failed.reason.contains("文件大小不符"), failed.reason)
    }

    @Test
    fun failsWhenDigestDiffersFromExpected() = runBlocking {
        val outcome = DownloadVerifier.verify(tempFileWith("abc"), expectedSize = 3, expectedDigest = "0".repeat(64))

        val failed = assertIs<VerificationOutcome.Failed>(outcome)
        assertTrue(failed.reason.contains("SHA256 摘要不符"), failed.reason)
    }

    /** 元数据没有摘要的旧资产只能降级为大小校验，但不能被当成"已做摘要校验"。 */
    @Test
    fun degradesToSizeOnlyWhenMetadataHasNoDigest() = runBlocking {
        assertEquals(
            VerificationOutcome.Passed(listOf("文件大小")),
            DownloadVerifier.verify(tempFileWith("abc"), expectedSize = 3, expectedDigest = null),
        )
    }

    @Test
    fun degradesToDigestOnlyWhenMetadataHasNoSize() = runBlocking {
        assertEquals(
            VerificationOutcome.Passed(listOf("SHA256 摘要")),
            DownloadVerifier.verify(tempFileWith("abc"), expectedSize = null, expectedDigest = SHA256_OF_ABC),
        )
    }
}

private fun tempFileWith(content: String): Path {
    val bytes = content.encodeToByteArray()
    val path = Path(createTempDirectory("download-verifier").resolve("update.apk").toString())
    SystemFileSystem.sink(path).buffered().use { sink -> sink.write(bytes, 0, bytes.size) }
    return path
}
