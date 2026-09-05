package com.github.zly2006.zhihu.harmonyprobe

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import kotlin.experimental.ExperimentalNativeApi

@OptIn(ExperimentalNativeApi::class)
@CName("P2UsesNativeNetwork")
fun p2UsesNativeNetwork(): Boolean = usesNativeNetwork

@OptIn(ExperimentalNativeApi::class)
@CName("P2HandleBack")
fun p2HandleBack(): Boolean {
    if (!P2State.showArticle && !P2State.showStress) return false
    P2State.showArticle = false
    P2State.showStress = false
    return true
}

@OptIn(ExperimentalNativeApi::class, ExperimentalForeignApi::class)
@CName("P2ApplyHomeJson")
fun p2ApplyHomeJson(value: CPointer<ByteVar>?) {
    value?.toKString()?.let(P2State::applyHomeJson)
}

@OptIn(ExperimentalNativeApi::class, ExperimentalForeignApi::class)
@CName("P2ApplyDetailJson")
fun p2ApplyDetailJson(value: CPointer<ByteVar>?) {
    value?.toKString()?.let(P2State::applyDetailJson)
}

@OptIn(ExperimentalNativeApi::class, ExperimentalForeignApi::class)
@CName("P2ApplyImageBase64")
fun p2ApplyImageBase64(value: CPointer<ByteVar>?) {
    value?.toKString()?.let(P2State::applyImageBase64)
}

@OptIn(ExperimentalNativeApi::class, ExperimentalForeignApi::class)
@CName("P2ApplySessionStatus")
fun p2ApplySessionStatus(value: CPointer<ByteVar>?) {
    value?.toKString()?.let(P2State::applySessionStatus)
}

@OptIn(ExperimentalNativeApi::class, ExperimentalForeignApi::class)
@CName("P2ApplyError")
fun p2ApplyError(value: CPointer<ByteVar>?) {
    value?.toKString()?.let(P2State::applyError)
}
