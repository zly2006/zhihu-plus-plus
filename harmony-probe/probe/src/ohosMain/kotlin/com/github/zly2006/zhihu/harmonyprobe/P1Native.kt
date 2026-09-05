package com.github.zly2006.zhihu.harmonyprobe

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import kotlin.experimental.ExperimentalNativeApi

@OptIn(ExperimentalNativeApi::class)
@CName("P1HandleBack")
fun p1HandleBack(): Boolean = P1ShellState.handleBack()

@OptIn(ExperimentalNativeApi::class, ExperimentalForeignApi::class)
@CName("P1ApplyColorMode")
fun p1ApplyColorMode(value: CPointer<ByteVar>?) {
    value?.toKString()?.let { P1ShellState.applyColorMode(it.equals("dark", ignoreCase = true)) }
}

/** P3：由 ArkTS 壳注入应用沙箱 filesDir 路径，作为原生数据库的存储目录。 */
internal var p3DatabaseDir: String? = null
    private set

@OptIn(ExperimentalNativeApi::class, ExperimentalForeignApi::class)
@CName("P3SetDatabasePath")
fun p3SetDatabasePath(value: CPointer<ByteVar>?) {
    value?.toKString()?.let { p3DatabaseDir = it }
}
