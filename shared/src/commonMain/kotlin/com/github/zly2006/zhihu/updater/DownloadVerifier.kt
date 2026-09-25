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

import com.github.zly2006.zhihu.util.Sha256
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem

/** 下载物校验结论。 */
sealed interface VerificationOutcome {
    /**
     * 通过校验，可以进入安装流程。
     *
     * [layers] 是本次实际生效的校验层级，用于向用户说明下载物为什么可信。
     */
    data class Passed(
        val layers: List<String>,
    ) : VerificationOutcome

    /** 未通过校验，[reason] 用于向用户展示失败原因。 */
    data class Failed(
        val reason: String,
    ) : VerificationOutcome
}

/**
 * 下载完成后的安装门禁：任何来源的下载物在通过校验前不得进入安装流程。
 *
 * 平台签名门禁（如 Android 的 APK 签名）由平台实现叠加在公共校验之上。
 */
fun interface DownloadedFileVerifier {
    suspend fun verify(file: Path, expectedSize: Long?, expectedDigest: String?): VerificationOutcome
}

/**
 * 公共下载物校验：文件大小比对 + 可选 SHA256 摘要。
 *
 * 摘要取自 GitHub Releases 资产元数据的 `digest` 字段。元数据没有摘要时（旧资产）自动降级为
 * 只做大小比对，由调用方再叠加平台签名门禁；这里不会把"拿不到摘要"当成校验通过。
 */
object DownloadVerifier : DownloadedFileVerifier {
    override suspend fun verify(file: Path, expectedSize: Long?, expectedDigest: String?): VerificationOutcome {
        val actualSize = SystemFileSystem.metadataOrNull(file)?.size
            ?: return VerificationOutcome.Failed("下载文件不存在，无法校验")

        val layers = mutableListOf<String>()
        if (expectedSize != null) {
            if (actualSize != expectedSize) {
                return VerificationOutcome.Failed("文件大小不符：期望 $expectedSize 字节，实际 $actualSize 字节")
            }
            layers += "文件大小"
        }
        if (expectedDigest != null) {
            val actualDigest = withContext(Dispatchers.Default) { sha256Hex(file) }
            if (!actualDigest.equals(expectedDigest, ignoreCase = true)) {
                return VerificationOutcome.Failed("SHA256 摘要不符：期望 $expectedDigest，实际 $actualDigest")
            }
            layers += "SHA256 摘要"
        }
        return VerificationOutcome.Passed(layers)
    }

    /**
     * 解析 Releases 资产 `digest` 字段（形如 `sha256:<hex>`）中的摘要值。
     *
     * 只接受 sha256 前缀加 64 位十六进制值；字段缺失或算法不是 sha256 时返回 null，让调用方降级到
     * 其他校验层级，而不是拿一个无法验证的值去误判下载物。
     */
    fun parseDigest(assetDigest: String?): String? {
        val value = assetDigest
            ?.trim()
            ?.lowercase()
            ?.substringAfter("$SHA256_PREFIX:", "")
            ?.takeIf { it.isNotEmpty() }
            ?: return null
        return value.takeIf { it.length == SHA256_HEX_LENGTH && it.all { char -> char.digitToIntOrNull(16) != null } }
    }
}

/** 分块读取文件计算 SHA-256，避免把上百 MB 的安装包整体读进内存。 */
private fun sha256Hex(file: Path): String {
    val sha256 = Sha256()
    val buffer = ByteArray(SHA256_READ_BUFFER_BYTES)
    SystemFileSystem.source(file).buffered().use { source ->
        while (true) {
            val read = source.readAtMostTo(buffer, 0, buffer.size)
            if (read <= 0) break
            sha256.update(buffer, 0, read)
        }
    }
    return sha256.digestHex()
}

private const val SHA256_PREFIX = "sha256"
private const val SHA256_HEX_LENGTH = 64
private const val SHA256_READ_BUFFER_BYTES = 64 * 1024
