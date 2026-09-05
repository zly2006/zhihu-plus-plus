plugins {
    alias(libs.plugins.kotlinMultiplatform)
}

// x64 编译垫片（仅编译用）：CPF Ktor 3.3.3-1.0.0 没有发布 ohosX64 变体，而共享的 NavDestination.kt
// 使用 io.ktor.http.Url 解析知乎链接。把它做成依赖 klib 而不是源码源集，是让 commonMain 编译可见的唯一方式。
// 只实现 NavDestination.kt 用到的只读 API；URL 百分号解码等行为未与 ktor 对齐，
// 深链解析（resolveContent）尚未在 x64 上验收；arm64 使用真实 ktor-http。
kotlin {
    ohosX64()
}
