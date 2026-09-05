package com.github.zly2006.zhihu.harmonyprobe

import com.github.zly2006.zhihu.harmonyprobe.db.sqldelight.newNativeDriver
import com.github.zly2006.zhihu.harmonyprobe.db.sqldelight.wrapDb
import kotlinx.coroutines.runBlocking

/**
 * ohosArm64 上的 P3 冒烟：CPF SQLDelight（NativeSqliteDriver / native-driver OH 变体）
 * 在 ArkTS 壳注入的沙箱目录（P3SetDatabasePath）下建库、写读、关闭。
 *
 * Room3 不在此冒烟中：其 OH 变体（room3-runtime-ohosarm64 3.0.0-alpha01-0.3.0）被 fork 改为
 * 非 suspend API，但配套 room3-compiler 未发布，无法完成 OHOS 侧代码生成（详见 P3-VALIDATION.md）；
 * Room3 的 JVM 全项验证与 Room 2.8.4 格式兼容性实验在 db-checks 完成。
 */
internal actual suspend fun p3DatabaseSmoke(): String {
    val dir = p3DatabaseDir
    if (dir.isNullOrEmpty()) {
        return "P3 DB：沙箱路径未注入（P3SetDatabasePath 未被宿主调用）"
    }
    return runBlocking {
        val sqldelight = runCatching {
            val driver = newNativeDriver("$dir/p3_sqldelight.db")
            try {
                val db = wrapDb(driver)
                db.blocked_keywordsQueries.insertKeyword("p3-smoke", "EXACT_MATCH", 0, 0, 1)
                db.content_open_eventsQueries.insertOpenEvent("article", "p3", null, "smoke", 1)
                db.blocked_keywordsQueries.countKeywords().executeAsOne() to
                    db.content_open_eventsQueries.countOpenEvents().executeAsOne()
            } finally {
                driver.close()
            }
        }
        buildString {
            append("P3 DB（arm64）· SQLDelight(2.2.1-1.0.0): ")
            append(sqldelight.fold({ "OK ${it.first}词/${it.second}事件" }, { "失败 ${it::class.simpleName}: ${it.message}" }))
            append(" · Room3: OHOS 侧缺配套 compiler，未参与（JVM 全项验证通过）")
        }
    }
}
