package com.github.zly2006.zhihu.harmonyprobe.db.sqldelight

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

/**
 * OHOS 上的生产用法：NativeSqliteDriver 内部完成建库与迁移（PRAGMA user_version）。
 *
 * 注意：CPF fork（sqliter-driver 1.3.3-0.3.0）的 DatabaseConfiguration 已不含 basePath 字段，
 * 路径随 name 传入（DatabaseFileContext.databasePath 以 name 为文件路径）；因此这里传入
 * 沙箱内完整文件路径（由 ArkTS 壳注入 filesDir 后拼接）。
 */
fun newNativeDriver(fullPath: String): SqlDriver =
    NativeSqliteDriver(ContentFilterDb.Schema, fullPath)

fun wrapDb(driver: SqlDriver): ContentFilterDb = ContentFilterDb(driver)
