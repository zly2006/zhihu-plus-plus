package com.github.zly2006.zhihu.harmonyprobe.db.checks

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.db.QueryResult
import com.github.zly2006.zhihu.harmonyprobe.db.sqldelight.ContentFilterDb
import com.github.zly2006.zhihu.harmonyprobe.db.sqldelight.newJdbcDriver
import com.github.zly2006.zhihu.harmonyprobe.db.sqldelight.wrapDb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * CPF SQLDelight（app.cash.sqldelight 2.2.1-1.0.0：runtime / sqlite-driver / coroutines-extensions）
 * 在 JVM（JdbcSqliteDriver + 文件路径）上的同项验证。
 *
 * 注：JVM JDBC driver 是单连接，JVM 端并发写入用 Mutex 串行化；
 * OHOS 生产路线使用 NativeSqliteDriver，其连接管理由 sqliter 驱动处理。
 */
class SqlDelightJvmCheck {

    private fun newDb(file: File): ContentFilterDb = wrapDb(newJdbcDriver(file.absolutePath))

    @Test
    fun `B1 - 建库与基础 CRUD：Schema create、自增主键、文本主键`() {
        val dir = tempDbDir("sd-b1")
        val file = File(dir, "content_filter.db")
        runBlocking {
            val driver = newJdbcDriver(file.absolutePath)
            try {
                ContentFilterDb.Schema.create(driver)
                val db = wrapDb(driver)
                db.blocked_keywordsQueries.insertKeyword("广告", "EXACT_MATCH", 0, 0, 1_000)
                db.blocked_keywordsQueries.insertKeyword("引流", "NLP_SEMANTIC", 0, 0, 2_000)
                // SQLDelight/JDBC 的 insert 返回受影响行数而非 rowid，自增验证改为读取行 id。
                val rows = db.blocked_keywordsQueries.selectAllKeywords().executeAsList()
                assertEquals(2, rows.size)
                assertTrue(rows[0].id > rows[1].id, "自增主键应递增")
                assertEquals(2, db.blocked_keywordsQueries.countKeywords().executeAsOne().toInt())

                db.blocked_usersQueries.upsertBlockedUser("u-1", "用户一", null, null, 1_000)
                db.blocked_usersQueries.upsertBlockedUser("u-1", "用户一改", null, null, 2_000)
                assertEquals(1, db.blocked_usersQueries.countBlockedUsers().executeAsOne().toInt())
                assertEquals(
                    "用户一改",
                    db.blocked_usersQueries.selectBlockedUserByUserId("u-1").executeAsOne().userName,
                )

                db.content_open_eventsQueries.insertOpenEvent("answer", "a", 10, "feed", 1_000)
                db.content_open_eventsQueries.insertOpenEvent("article", "b", null, "daily", 2_000)
                assertEquals(2, db.content_open_eventsQueries.countOpenEvents().executeAsOne().toInt())
            } finally {
                driver.close()
            }
        }
    }

    @Test
    fun `B2 - 事务回滚：异常后数据不变`() {
        val dir = tempDbDir("sd-b2")
        val file = File(dir, "content_filter.db")
        runBlocking {
            val driver = newJdbcDriver(file.absolutePath)
            try {
                ContentFilterDb.Schema.create(driver)
                val db = wrapDb(driver)
                db.blocked_keywordsQueries.insertKeyword("原有", "EXACT_MATCH", 0, 0, 1_000)
                val result = runCatching {
                    db.transaction {
                        db.blocked_keywordsQueries.insertKeyword("应回滚", "EXACT_MATCH", 0, 0, 2_000)
                        error("强制回滚")
                    }
                }
                assertTrue(result.isFailure, "事务内抛异常应向外传播")
                assertEquals(1, db.blocked_keywordsQueries.countKeywords().executeAsOne().toInt(), "事务回滚后行数不应变化")
            } finally {
                driver.close()
            }
        }
    }

    @Test
    fun `B3 - Flow 观察：insert 后观察流更新`() {
        val dir = tempDbDir("sd-b3")
        val file = File(dir, "content_filter.db")
        runBlocking {
            val driver = newJdbcDriver(file.absolutePath)
            try {
                ContentFilterDb.Schema.create(driver)
                val db = wrapDb(driver)
                val flow = db.blocked_keywordsQueries.selectAllKeywords()
                    .asFlow()
                    .mapToList(Dispatchers.Default)
                assertEquals(0, flow.first().size)
                db.blocked_keywordsQueries.insertKeyword("观察目标", "EXACT_MATCH", 0, 0, 1_000)
                val rows = withTimeout(10_000) { flow.first { it.isNotEmpty() } }
                assertEquals("观察目标", rows.first().keyword)
            } finally {
                driver.close()
            }
        }
    }

    @Test
    fun `B4 - 并发写入：8 协程 x 25 条全部落库（单连接 Mutex 串行化）`() {
        val dir = tempDbDir("sd-b4")
        val file = File(dir, "content_filter.db")
        runBlocking {
            val driver = newJdbcDriver(file.absolutePath)
            try {
                ContentFilterDb.Schema.create(driver)
                val db = wrapDb(driver)
                val mutex = Mutex()
                (0 until 8).map { worker ->
                    launch(Dispatchers.Default) {
                        repeat(25) { i ->
                            mutex.withLock {
                                db.blocked_keywordsQueries.insertKeyword("w$worker-$i", "EXACT_MATCH", 0, 0, i.toLong())
                            }
                        }
                    }
                }.joinAll()
                assertEquals(200, db.blocked_keywordsQueries.countKeywords().executeAsOne().toInt(), "并发写入不应丢行")
            } finally {
                driver.close()
            }
        }
    }

    @Test
    fun `B5 - 文件路径持久化：关闭重开数据保留`() {
        val dir = tempDbDir("sd-b5")
        val file = File(dir, "content_filter.db")
        runBlocking {
            val d1 = newJdbcDriver(file.absolutePath)
            try {
                ContentFilterDb.Schema.create(d1)
                wrapDb(d1).blocked_usersQueries.upsertBlockedUser("persist-1", "持久化用户", null, null, 1_000)
            } finally {
                d1.close()
            }
            val d2 = newJdbcDriver(file.absolutePath)
            try {
                val db = wrapDb(d2)
                val user = db.blocked_usersQueries.selectBlockedUserByUserId("persist-1").executeAsOneOrNull()
                assertNotNull(user, "重开后应能读到关闭前写入的行")
                assertEquals("持久化用户", user.userName)
            } finally {
                d2.close()
            }
        }
    }

    @Test
    fun `B6 - 迁移 v1 到 v2：Schema migrate 应用 1 sqm 且数据保留`() {
        val dir = tempDbDir("sd-b6")
        val file = File(dir, "content_filter.db")
        runBlocking {
            // 手工构造 v1（blocked_keywords 无 keywordType 列，等价生产 v2 之前的形态）
            val driver = newJdbcDriver(file.absolutePath)
            driver.execute(
                null,
                "CREATE TABLE blocked_keywords (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "keyword TEXT NOT NULL, " +
                    "caseSensitive INTEGER NOT NULL, " +
                    "isRegex INTEGER NOT NULL, " +
                    "createdTime INTEGER NOT NULL)",
                0,
                null,
            )
            driver.execute(null, "PRAGMA user_version = 1", 0, null)
            driver.execute(null, "INSERT INTO blocked_keywords (keyword, caseSensitive, isRegex, createdTime) VALUES ('旧词一', 0, 0, 1000)", 0, null)
            driver.execute(null, "INSERT INTO blocked_keywords (keyword, caseSensitive, isRegex, createdTime) VALUES ('旧词二', 0, 0, 2000)", 0, null)

            ContentFilterDb.Schema.migrate(driver, 1, 2)
            driver.execute(null, "PRAGMA user_version = 2", 0, null)

            val db = wrapDb(driver)
            assertEquals(2, db.blocked_keywordsQueries.countKeywords().executeAsOne().toInt(), "迁移后旧数据应保留")
            val rows = db.blocked_keywordsQueries.selectAllKeywords().executeAsList()
            assertTrue(rows.all { it.keywordType == "EXACT_MATCH" }, "迁移后新列应取 DEFAULT 值")
            db.blocked_keywordsQueries.insertKeyword("迁移后新增", "EXACT_MATCH", 0, 0, 3_000)
            assertEquals(3, db.blocked_keywordsQueries.countKeywords().executeAsOne().toInt())
            driver.close()
        }
    }

    @Test
    fun `B7 - 异常恢复：约束冲突不损坏库，坏文件不静默`() {
        val dir = tempDbDir("sd-b7")
        runBlocking {
            // 约束冲突：文本主键重复插入（INSERT 非 OR REPLACE）
            val driver = newJdbcDriver(File(dir, "conflict.db").absolutePath)
            try {
                ContentFilterDb.Schema.create(driver)
                val db = wrapDb(driver)
                driver.execute(null, "INSERT INTO blocked_users (userId, userName, createdTime) VALUES ('dup', '第一次', 1000)", 0, null)
                val conflict = runCatching {
                    driver.execute(null, "INSERT INTO blocked_users (userId, userName, createdTime) VALUES ('dup', '冲突', 2000)", 0, null)
                }
                assertTrue(conflict.isFailure, "主键冲突应抛异常")
                println("SQLDelight 主键冲突异常类型：${conflict.exceptionOrNull()?.let { "${it::class.simpleName}: ${it.message}" }}")
                assertEquals(1, db.blocked_usersQueries.countBlockedUsers().executeAsOne().toInt(), "冲突后库应仍然可用")
            } finally {
                driver.close()
            }

            // 坏文件：随机字节伪装数据库文件
            val badFile = File(dir, "corrupt.db").apply { writeBytes(ByteArray(4096) { ('a' + it % 26).code.toByte() }) }
            val before = badFile.readBytes()
            val opened = runCatching {
                val driver = newJdbcDriver(badFile.absolutePath)
                try {
                    driver.executeQuery(null, "SELECT COUNT(*) FROM blocked_keywords", { it.next() }, 0, null)
                } finally {
                    driver.close()
                }
            }
            assertTrue(opened.isFailure, "打开坏文件应报错，而不是静默成功")
            println("SQLDelight 打开坏文件异常：${opened.exceptionOrNull()?.let { "${it::class.simpleName}: ${it.message}" }}")
            assertTrue(badFile.exists() && badFile.readBytes().contentEquals(before), "失败路径不应破坏或删除原文件")
        }
    }
}
