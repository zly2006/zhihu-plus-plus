package com.github.zly2006.zhihu.harmonyprobe.db.checks

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import java.io.File

fun tempDbDir(tag: String): File =
    File(System.getProperty("java.io.tmpdir"), "p3-db-checks/$tag-${System.nanoTime()}").apply {
        deleteRecursively()
        mkdirs()
    }

/** 读 sqlite_master 的建表/建索引语句，供 C4 DDL 一致性比对。 */
fun readDdl(driver: SqlDriver): Map<String, String> {
    val rows = driver.executeQuery(
        null,
        "SELECT name, sql FROM sqlite_master WHERE type IN ('table', 'index') " +
            "AND name NOT LIKE 'sqlite_%' AND name != 'room_master_table'",
        { cursor ->
            val result = mutableMapOf<String, String>()
            while (cursor.next().value) {
                val name = cursor.getString(0) ?: continue
                result[name] = cursor.getString(1) ?: ""
            }
            QueryResult.Value(result)
        },
        0,
        null,
    )
    @Suppress("UNCHECKED_CAST")
    return rows.value as Map<String, String>
}

/** 归一化 DDL：去反引号/方括号引用符、去全部空白、小写，并移除两种主键写法（Room 的表级
 * PRIMARY KEY(col) 子句与 SQLDelight 的列级 PRIMARY KEY 标记）；主键语义由 readTableInfo 断言。 */
fun normalizeDdl(sql: String): String =
    sql.replace("`", "")
        .replace("[", "")
        .replace("]", "")
        .replace(Regex("\\s+"), "")
        .lowercase()
        .replace(Regex(",primarykey\\([a-z_]+\\)"), "")
        .replace("primarykey,", ",")
        .replace("primarykey)", ")")

/** 读 PRAGMA table_info 的结构信息（name:type:notNull:dflt:pk），做跨栈结构断言。 */
fun readTableInfo(driver: SqlDriver, table: String): List<String> {
    val rows = driver.executeQuery(
        null,
        "PRAGMA table_info($table)",
        { cursor ->
            val list = mutableListOf<String>()
            while (cursor.next().value) {
                val name = cursor.getString(1)
                val type = cursor.getString(2)
                val notNull = cursor.getLong(3)
                val dflt = cursor.getString(4)
                val pk = cursor.getLong(5)
                list.add("$name:$type:notNull=$notNull:dflt=$dflt:pk=$pk")
            }
            QueryResult.Value(list)
        },
        0,
        null,
    )
    @Suppress("UNCHECKED_CAST")
    return rows.value as List<String>
}
