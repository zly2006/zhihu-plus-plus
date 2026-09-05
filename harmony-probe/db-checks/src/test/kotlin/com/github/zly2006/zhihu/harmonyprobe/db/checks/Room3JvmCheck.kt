package com.github.zly2006.zhihu.harmonyprobe.db.checks

import androidx.room3.withWriteTransaction
import com.github.zly2006.zhihu.harmonyprobe.db.room3.BlockedKeyword3Entity
import com.github.zly2006.zhihu.harmonyprobe.db.room3.BlockedKeyword3V1Entity
import com.github.zly2006.zhihu.harmonyprobe.db.room3.BlockedUser3Entity
import com.github.zly2006.zhihu.harmonyprobe.db.room3.ContentFilterDb3
import com.github.zly2006.zhihu.harmonyprobe.db.room3.ContentOpenEvent3Entity
import com.github.zly2006.zhihu.harmonyprobe.db.room3.MIGRATION_1_2
import com.github.zly2006.zhihu.harmonyprobe.db.room3.buildContentFilterDb3
import com.github.zly2006.zhihu.harmonyprobe.db.room3.buildContentFilterDb3V1
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * CPF Room3（androidx.room3:room3-runtime-ohosarm64 3.0.0-alpha01-0.3.0 / JVM 3.0.0-alpha01）
 * 在 JVM（BundledSQLiteDriver + 文件路径）上的同项验证。
 */
class Room3JvmCheck {

    private fun newDb(dir: File): ContentFilterDb3 =
        buildContentFilterDb3(File(dir, "content_filter.db").absolutePath)

    @Test
    fun `A1 - 基础 CRUD：自增主键、文本主键、索引表`() = runBlocking {
        val db = newDb(tempDbDir("room3-a1"))
        try {
            val k1 = db.keywordDao().insert(BlockedKeyword3Entity(keyword = "广告"))
            val k2 = db.keywordDao().insert(BlockedKeyword3Entity(keyword = "引流", keywordType = "NLP_SEMANTIC"))
            assertTrue(k2 > k1, "自增主键应递增")
            assertEquals(2, db.keywordDao().count())

            db.userDao().upsert(BlockedUser3Entity(userId = "u-1", userName = "用户一"))
            db.userDao().upsert(BlockedUser3Entity(userId = "u-1", userName = "用户一改"))
            assertEquals(1, db.userDao().count(), "文本主键 upsert 应覆盖而非新增")
            assertEquals("用户一改", db.userDao().getByUserId("u-1")?.userName)

            db.openEventDao().insert(ContentOpenEvent3Entity(contentType = "answer", contentId = "a", openFrom = "feed"))
            db.openEventDao().insert(ContentOpenEvent3Entity(contentType = "article", contentId = "b", openFrom = "daily"))
            assertEquals(2, db.openEventDao().count())
        } finally {
            db.close()
        }
    }

    @Test
    fun `A2 - 事务回滚：异常后数据不变`() = runBlocking {
        val db = newDb(tempDbDir("room3-a2"))
        try {
            db.keywordDao().insert(BlockedKeyword3Entity(keyword = "原有"))
            val result = runCatching {
                withTransactionCompat(db) {
                    db.keywordDao().insert(BlockedKeyword3Entity(keyword = "应回滚"))
                    error("强制回滚")
                }
            }
            assertTrue(result.isFailure, "事务内抛异常应向外传播")
            assertEquals(1, db.keywordDao().count(), "事务回滚后行数不应变化")
        } finally {
            db.close()
        }
    }

    @Test
    fun `A3 - Flow 观察：insert 后观察流更新`() = runBlocking {
        val db = newDb(tempDbDir("room3-a3"))
        try {
            assertEquals(0, db.keywordDao().observeAll().first().size)
            db.keywordDao().insert(BlockedKeyword3Entity(keyword = "观察目标"))
            val rows = withTimeout(10_000) { db.keywordDao().observeAll().first { it.isNotEmpty() } }
            assertEquals("观察目标", rows.first().keyword)
        } finally {
            db.close()
        }
    }

    @Test
    fun `A4 - 并发写入：8 协程 x 25 条全部落库`() = runBlocking {
        val db = newDb(tempDbDir("room3-a4"))
        try {
            (0 until 8).map { worker ->
                launch(Dispatchers.Default) {
                    repeat(25) { i ->
                        db.keywordDao().insert(BlockedKeyword3Entity(keyword = "w$worker-$i"))
                    }
                }
            }.joinAll()
            assertEquals(200, db.keywordDao().count(), "并发写入不应丢行")
        } finally {
            db.close()
        }
    }

    @Test
    fun `A5 - 文件路径持久化：关闭重开数据保留`() {
        val dir = tempDbDir("room3-a5")
        val path = File(dir, "content_filter.db").absolutePath
        runBlocking {
            val first = buildContentFilterDb3(path)
            try {
                first.userDao().upsert(BlockedUser3Entity(userId = "persist-1", userName = "持久化用户"))
            } finally {
                first.close()
            }
            val second = buildContentFilterDb3(path)
            try {
                val user = second.userDao().getByUserId("persist-1")
                assertNotNull(user, "重开后应能读到关闭前写入的行")
                assertEquals("持久化用户", user.userName)
            } finally {
                second.close()
            }
        }
    }

    @Test
    fun `A6 - 迁移 v1 到 v2：数据保留且新列取默认值`() {
        val dir = tempDbDir("room3-a6")
        val path = File(dir, "content_filter.db").absolutePath
        runBlocking {
            val v1 = buildContentFilterDb3V1(path)
            try {
                v1.v1KeywordDao().insert(BlockedKeyword3V1Entity(keyword = "旧词一"))
                v1.v1KeywordDao().insert(BlockedKeyword3V1Entity(keyword = "旧词二"))
            } finally {
                v1.close()
            }
            val v2 = buildContentFilterDb3(path)
            try {
                // ContentFilterDb3 是 v2，打开 v1 文件触发 MIGRATION_1_2。
                assertEquals(2, v2.keywordDao().count(), "迁移后旧数据应保留")
                assertEquals("EXACT_MATCH", v2.keywordDao().observeAll().first().first().keywordType)
                v2.keywordDao().insert(BlockedKeyword3Entity(keyword = "迁移后新增"))
                assertEquals(3, v2.keywordDao().count())
            } finally {
                v2.close()
            }
        }
    }

    @Test
    fun `A7 - 异常恢复：约束冲突不损坏库，坏文件不静默`() {
        val dir = tempDbDir("room3-a7")
        runBlocking {
            // 约束冲突：文本主键重复插入
            val db = newDb(dir)
            try {
                db.userDao().insertRaw(BlockedUser3Entity(userId = "dup", userName = "第一次"))
                runCatching { db.userDao().insertRaw(BlockedUser3Entity(userId = "dup", userName = "冲突")) }
                    .onFailure { println("Room3 主键冲突异常类型：${it::class.simpleName}: ${it.message}") }
                    .let { assertTrue(it.isFailure, "主键冲突应抛异常") }
                assertEquals(1, db.userDao().count(), "冲突后库应仍然可用")
            } finally {
                db.close()
            }

            // 坏文件：随机字节伪装数据库文件
            val badFile = File(dir, "corrupt.db").apply { writeBytes(ByteArray(4096) { ('a' + it % 26).code.toByte() }) }
            val before = badFile.readBytes()
            val opened = runCatching {
                val bad = buildContentFilterDb3(badFile.absolutePath)
                try {
                    bad.keywordDao().count()
                } finally {
                    bad.close()
                }
            }
            assertTrue(opened.isFailure, "打开坏文件应报错，而不是静默成功")
            println("Room3 打开坏文件异常：${opened.exceptionOrNull()?.let { "${it::class.simpleName}: ${it.message}" }}")
            assertTrue(badFile.exists() && badFile.readBytes().contentEquals(before), "失败路径不应破坏或删除原文件")
        }
    }

    @Test
    fun `A8 - 迁移对象可重复使用（校验 MIGRATION_1_2 常量）`() {
        assertEquals(1, MIGRATION_1_2.startVersion)
        assertEquals(2, MIGRATION_1_2.endVersion)
    }
}

/** room3 alpha01 的事务 API 是 withWriteTransaction（Room2 时代的 withTransaction 已更名）。 */
private suspend fun <R> withTransactionCompat(db: ContentFilterDb3, block: suspend () -> R): R =
    db.withWriteTransaction { block() }
