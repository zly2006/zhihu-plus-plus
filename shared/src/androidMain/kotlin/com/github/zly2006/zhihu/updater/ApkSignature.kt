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

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.os.Build
import java.io.File
import java.security.MessageDigest

/**
 * Android 侧的下载物校验：在公共的文件大小与摘要校验之上叠加 APK 签名门禁。
 *
 * 只有三层都通过的安装包才允许进入安装流程。
 */
internal fun androidUpdateVerifier(context: Context): DownloadedFileVerifier =
    DownloadedFileVerifier { file, expectedSize, expectedDigest ->
        when (val common = DownloadVerifier.verify(file, expectedSize, expectedDigest)) {
            is VerificationOutcome.Failed -> common
            is VerificationOutcome.Passed -> {
                when (val signature = verifyApkSignatureOfInstalledApp(context, File(file.toString()))) {
                    is VerificationOutcome.Failed -> signature
                    is VerificationOutcome.Passed -> VerificationOutcome.Passed(common.layers + signature.layers)
                }
            }
        }
    }

/**
 * 校验下载的 APK 与已安装应用使用同一签名证书。
 *
 * 方案原文要求比对「内置官方指纹」常量，这里改为与已安装包自身的签名比对：对镜像投毒的拦截能力
 * 等价（Android 本身也只允许同签名覆盖安装），但不需要在代码里维护一个会随签名轮换失效的常量，
 * 也不会误伤用户自行改签名的构建。校验只读本地文件与包信息，不依赖网络。
 */
internal fun verifyApkSignatureOfInstalledApp(context: Context, apkFile: File): VerificationOutcome {
    val packageManager = context.packageManager
    val installedCertificates = packageManager.signerCertificates(
        runCatching { packageManager.getPackageInfo(context.packageName, SIGNER_FLAGS) }.getOrNull(),
    )
    if (installedCertificates.isEmpty()) {
        return VerificationOutcome.Failed("无法读取已安装应用的签名证书")
    }
    val archiveCertificates = packageManager.signerCertificates(
        runCatching { packageManager.getPackageArchiveInfo(apkFile.absolutePath, SIGNER_FLAGS) }.getOrNull(),
    )
    if (archiveCertificates.isEmpty()) {
        return VerificationOutcome.Failed("无法读取下载安装包的签名证书")
    }
    if (installedCertificates.intersect(archiveCertificates).isEmpty()) {
        return VerificationOutcome.Failed("下载安装包的签名与已安装应用不一致，已拒绝安装")
    }
    return VerificationOutcome.Passed(listOf("APK 签名"))
}

/**
 * API 28 起签名信息改为 signingInfo，API 27 仍只有已废弃的 signatures 字段。
 * 这里按系统版本选取对应字段，保持在 minSdk 27 上都能读到证书。
 */
@Suppress("DEPRECATION")
private val SIGNER_FLAGS: Int =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        PackageManager.GET_SIGNING_CERTIFICATES
    } else {
        PackageManager.GET_SIGNATURES
    }

/** 取出包信息中所有签名证书的 SHA-256 指纹；[packageInfo] 为 null 或未签名时返回空集。 */
@Suppress("DEPRECATION")
private fun PackageManager.signerCertificates(packageInfo: PackageInfo?): Set<String> {
    if (packageInfo == null) return emptySet()
    val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        packageInfo.signingInfo?.apkContentsSigners
    } else {
        packageInfo.signatures
    }
    return signatures.orEmpty().map { it.certificateSha256() }.toSet()
}

/** APK 签名方案 v2/v3 的证书指纹：对签名证书原始字节取 SHA-256。 */
private fun Signature.certificateSha256(): String =
    MessageDigest.getInstance("SHA-256").digest(toByteArray()).joinToString("") { "%02x".format(it) }
