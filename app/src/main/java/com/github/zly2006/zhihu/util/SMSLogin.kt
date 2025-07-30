package com.github.zly2006.zhihu.util

import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalEncodingApi::class, ExperimentalStdlibApi::class)
fun main() {
    val test = "000102030405060708090a0b0c0d0e0f"
    myEncryptV1(test)
}
