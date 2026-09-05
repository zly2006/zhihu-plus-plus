package com.github.zly2006.zhihu.harmonyprobe

/**
 * x86_64 模拟器上的真实能力边界：CPF Room3 与 SQLDelight 均未发布 ohosX64 变体，
 * 原生 DB 在 x64 上不可链接。这里明确报告"不支持"，不做任何假实现。
 */
internal actual suspend fun p3DatabaseSmoke(): String =
    "P3 DB（x64）：CPF Room3 / SQLDelight 未发布 ohosX64 变体，原生 DB 不支持；" +
        "选型验证在宿主机 JVM 与 ohosArm64 编译链接中完成"
