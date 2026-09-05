package com.github.zly2006.zhihu.harmonyprobe.db.sqldelight

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.util.Properties

fun newJdbcDriver(path: String): JdbcSqliteDriver =
    JdbcSqliteDriver("jdbc:sqlite:$path", Properties())

fun wrapDb(driver: SqlDriver): ContentFilterDb = ContentFilterDb(driver)
