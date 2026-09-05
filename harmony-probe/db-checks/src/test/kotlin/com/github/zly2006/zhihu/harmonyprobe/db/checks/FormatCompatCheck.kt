package com.github.zly2006.zhihu.harmonyprobe.db.checks

import com.github.zly2006.zhihu.harmonyprobe.db.legacy.LegacySubsetKeyword
import com.github.zly2006.zhihu.harmonyprobe.db.legacy.LegacySubsetOpenEvent
import com.github.zly2006.zhihu.harmonyprobe.db.legacy.LegacySubsetUser
import com.github.zly2006.zhihu.harmonyprobe.db.legacy.buildLegacyContentFilterDatabase
import com.github.zly2006.zhihu.harmonyprobe.db.legacy.buildLegacySubsetDatabase
import com.github.zly2006.zhihu.harmonyprobe.db.room3.buildContentFilterDb3
import com.github.zly2006.zhihu.harmonyprobe.db.sqldelight.newJdbcDriver
import com.github.zly2006.zhihu.harmonyprobe.db.sqldelight.wrapDb
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 「能否维持 Android/Desktop 现有数据库格式」实验：
 * - C1 用生产 Room 2-8-4 版本线（db-legacy-room2）产出完整 v6 数据库并写入生产实体行；
 * - C2 用 SQLDelight 直接打开该文件读取（零迁移共存）；
 * - C3 用 Room3 打开 Room 2-8-4 创建的同 DDL 文件，观察 identity hash / 版本线行为；
 * - C4 三方（Room 2-8-4 与 Room3 与 SQLDelight）v2 子集 DDL 一致性比对；
 * - C5 C3 失败时的显式迁移桥：清空 room_master_table 后 Room3 接管，数据不丢。
 */
class FormatCompatCheck {

    @Test
    fun `C1 - 生产 Room 2-8-4 写入完整 v6 数据库`() {
        val dir = tempDbDir("compat-c1")
        val file = File(dir, "content_filter.db")
        runBlocking {
            val db = buildLegacyContentFilterDatabase(file.absolutePath)
            try {
                db.legacyKeywordDao().insert(
                    com.github.zly2006.zhihu.viewmodel.filter.BlockedKeyword(keyword = "生产词一"),
                )
                db.legacyUserDao().insert(
                    com.github.zly2006.zhihu.viewmodel.filter.BlockedUser(userId = "u-9", userName = "生产用户"),
                )
                db.legacyOpenEventDao().insert(
                    com.github.zly2006.zhihu.viewmodel.filter.ContentOpenEvent(
                        contentType = "answer",
                        contentId = "q-1",
                        openFrom = "feed",
                    ),
                )
                assertEquals(1, db.legacyKeywordDao().count())
            } finally {
                db.close()
            }
            assertTrue(file.exists(), "生产格式文件应已生成")
            println("C1 生成生产 Room 2-8-4 v6 文件：${file.absolutePath}（${file.length()} 字节）")
        }
    }

    @Test
    fun `C2 - SQLDelight 直接读取生产 Room 2-8-4 文件（零迁移）`() {
        val dir = tempDbDir("compat-c2")
        val file = File(dir, "content_filter.db")
        runBlocking {
            // 先生产 v6 文件 + 三类行
            val legacy = buildLegacyContentFilterDatabase(file.absolutePath)
            try {
                legacy.legacyKeywordDao().insert(com.github.zly2006.zhihu.viewmodel.filter.BlockedKeyword(keyword = "兼容词"))
                legacy.legacyUserDao().insert(com.github.zly2006.zhihu.viewmodel.filter.BlockedUser(userId = "u-9", userName = "兼容用户"))
                legacy.legacyOpenEventDao().insert(
                    com.github.zly2006.zhihu.viewmodel.filter.ContentOpenEvent(contentType = "article", contentId = "p-1", openFrom = "daily"),
                )
            } finally {
                legacy.close()
            }

            // SQLDelight 不执行 Schema.create，直接打开既有文件查询
            val driver = newJdbcDriver(file.absolutePath)
            try {
                val db = wrapDb(driver)
                val keywords = db.blocked_keywordsQueries.selectAllKeywords().executeAsList()
                assertEquals(1, keywords.size)
                assertEquals("兼容词", keywords.first().keyword)
                assertEquals("EXACT_MATCH", keywords.first().keywordType)

                val user = db.blocked_usersQueries.selectBlockedUserByUserId("u-9").executeAsOne()
                assertEquals("兼容用户", user.userName)

                val events = db.content_open_eventsQueries.selectRecentOpenEvents(10).executeAsList()
                assertEquals(1, events.size)
                assertEquals("p-1", events.first().contentId)
                println("C2 SQLDelight 零迁移读取生产 v6 文件：通过")
            } finally {
                driver.close()
            }
        }
    }

    @Test
    fun `C3 - Room3 打开 Room 2-8-4 创建的同 DDL 文件（identity hash 实验）`() {
        val dir = tempDbDir("compat-c3")
        val file = File(dir, "content_filter.db")
        runBlocking {
            val legacy = buildLegacySubsetDatabase(file.absolutePath)
            try {
                legacy.subsetKeywordDao().insert(LegacySubsetKeyword(keyword = "跨版本词"))
                legacy.subsetUserDao().insert(LegacySubsetUser(userId = "u-3", userName = "子集用户"))
                legacy.subsetOpenEventDao().insert(
                    LegacySubsetOpenEvent(contentType = "answer", contentId = "a-1", openFrom = "feed"),
                )
            } finally {
                legacy.close()
            }

            val outcome = runCatching {
                val db3 = buildContentFilterDb3(file.absolutePath)
                try {
                    db3.keywordDao().count()
                } finally {
                    db3.close()
                }
            }
            outcome
                .onSuccess { println("C3 Room3 直接打开 Room 2-8-4 文件：成功（count=${it}），identity hash 兼容") }
                .onFailure {
                    println("C3 Room3 直接打开 Room 2-8-4 文件：失败 —— ${it::class.simpleName}: ${it.message}")
                }
            // 两种结果都记录：成功 = 直接兼容；失败 = 需要 C5 的显式迁移桥。文件本身不得被破坏。
            assertTrue(file.exists(), "实验不应删除原文件")
        }
    }

    @Test
    fun `C4 - Room 2-8-4 与 Room3 与 SQLDelight 三方 DDL 一致性`() {
        val dirL2 = tempDbDir("compat-c4-l2")
        val dirR3 = tempDbDir("compat-c4-r3")
        val dirSd = tempDbDir("compat-c4-sd")
        runBlocking {
            // Room 2-8-4 子集（v2，与 ContentFilterDb3 相同 DDL）
            val l2File = File(dirL2, "l2.db")
            val legacy = buildLegacySubsetDatabase(l2File.absolutePath)
            try {
                legacy.subsetKeywordDao().insert(LegacySubsetKeyword(keyword = "x"))
            } finally {
                legacy.close()
            }

            // Room3 子集
            val r3File = File(dirR3, "r3.db")
            val room3 = buildContentFilterDb3(r3File.absolutePath)
            try {
                room3.keywordDao().count()
            } finally {
                room3.close()
            }

            // SQLDelight 子集
            val sdFile = File(dirSd, "sd.db")
            val sdDriver = newJdbcDriver(sdFile.absolutePath)
            try {
                com.github.zly2006.zhihu.harmonyprobe.db.sqldelight.ContentFilterDb.Schema.create(sdDriver)
            } finally {
                sdDriver.close()
            }

            val l2Ddl = newJdbcDriver(l2File.absolutePath).let { d ->
                try { readDdl(d) } finally { d.close() }
            }
            val r3Ddl = newJdbcDriver(r3File.absolutePath).let { d ->
                try { readDdl(d) } finally { d.close() }
            }
            val sdDdl = newJdbcDriver(sdFile.absolutePath).let { d ->
                try { readDdl(d) } finally { d.close() }
            }

            val l2Norm = l2Ddl.mapValues { (_, v) -> normalizeDdl(v) }
            val r3Norm = r3Ddl.mapValues { (_, v) -> normalizeDdl(v) }
            val sdNorm = sdDdl.mapValues { (_, v) -> normalizeDdl(v) }

            // 表与索引集合
            assertEquals(setOf("blocked_keywords", "blocked_users", "content_open_events") +
                setOf("index_content_open_events_contentType_contentId", "index_content_open_events_openedAt"),
                l2Norm.keys, "Room 2-8-4 子集应包含 3 表 2 索引")
            assertEquals(l2Norm.keys, sdNorm.keys, "Room2 与 SQLDelight 对象集合应一致")
            assertEquals(l2Norm.keys, r3Norm.keys, "Room2 与 Room3 对象集合应一致")

            // 逐对象 DDL 归一化比较（主键写法差异已被归一化）
            l2Norm.forEach { (name, sql) ->
                assertEquals(sql, sdNorm[name], "对象 $name 的 DDL：Room 2-8-4 vs SQLDelight")
                assertEquals(sql, r3Norm[name], "对象 $name 的 DDL：Room 2-8-4 vs Room3")
            }

            // 结构级比较：PRAGMA table_info（列名/类型/notNull/默认值/主键位）三方一致
            listOf("blocked_keywords", "blocked_users", "content_open_events").forEach { table ->
                val l2Info = newJdbcDriver(l2File.absolutePath).let { d -> try { readTableInfo(d, table) } finally { d.close() } }
                val r3Info = newJdbcDriver(r3File.absolutePath).let { d -> try { readTableInfo(d, table) } finally { d.close() } }
                val sdInfo = newJdbcDriver(sdFile.absolutePath).let { d -> try { readTableInfo(d, table) } finally { d.close() } }
                assertEquals(l2Info, sdInfo, "表 $table 的结构：Room 2-8-4 vs SQLDelight")
                assertEquals(l2Info, r3Info, "表 $table 的结构：Room 2-8-4 vs Room3")
            }
            println("C4 三方比对：${l2Norm.size} 个对象 DDL 一致，3 张表 table_info 结构一致")
        }
    }

    @Test
    fun `C5 - 迁移桥实验：清空 room_master_table 后 Room3 接管 Room 2-8-4 文件`() {
        val dir = tempDbDir("compat-c5")
        val file = File(dir, "content_filter.db")
        runBlocking {
            val legacy = buildLegacySubsetDatabase(file.absolutePath)
            try {
                legacy.subsetKeywordDao().insert(LegacySubsetKeyword(keyword = "桥接词"))
                legacy.subsetUserDao().insert(LegacySubsetUser(userId = "u-5", userName = "桥接用户"))
            } finally {
                legacy.close()
            }

            // 先确认直接打开是否被 identity hash 拦截（同 C3）
            val direct = runCatching {
                val d = buildContentFilterDb3(file.absolutePath)
                try {
                    d.keywordDao().count()
                } finally {
                    d.close()
                }
            }
            if (direct.isSuccess) {
                println("C5 Room3 可直接接管（identity hash 兼容），无需迁移桥：count=${direct.getOrNull()}")
                return@runBlocking
            }
            println("C5 直接打开失败（${direct.exceptionOrNull()?.let { it::class.simpleName }}），尝试显式迁移桥")

            // 迁移桥：显式清空 room_master_table（用户可感知的、文档化的一次性动作，非静默）
            val bridge = newJdbcDriver(file.absolutePath)
            try {
                bridge.execute(null, "DELETE FROM room_master_table", 0, null)
            } finally {
                bridge.close()
            }

            val db3 = buildContentFilterDb3(file.absolutePath)
            try {
                assertEquals(1, db3.keywordDao().count(), "迁移桥后旧数据应完整保留")
                assertEquals("桥接词", db3.keywordDao().observeAll().first().first().keyword)
                assertEquals("桥接用户", db3.userDao().getByUserId("u-5")?.userName)
                println("C5 显式迁移桥成功：Room3 接管 Room 2-8-4 文件且数据零丢失")
            } finally {
                db3.close()
            }
        }
    }
}
