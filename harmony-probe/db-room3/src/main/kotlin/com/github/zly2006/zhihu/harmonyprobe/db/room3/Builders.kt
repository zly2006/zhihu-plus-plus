package com.github.zly2006.zhihu.harmonyprobe.db.room3

import androidx.room3.Room
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL

/**
 * 对齐生产 shared-local-db buildContentFilterDatabase 中 migration2To3 的语义：
 * 给 blocked_keywords 增加 keywordType 列（NOT NULL DEFAULT 'EXACT_MATCH'）。
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override suspend fun migrate(connection: SQLiteConnection) {
        connection.execSQL(
            "ALTER TABLE `blocked_keywords` ADD COLUMN `keywordType` TEXT NOT NULL DEFAULT 'EXACT_MATCH'",
        )
    }
}

fun buildContentFilterDb3(path: String): ContentFilterDb3 =
    Room.databaseBuilder<ContentFilterDb3>(path)
        .addMigrations(MIGRATION_1_2)
        .setDriver(BundledSQLiteDriver())
        .build()

fun buildContentFilterDb3V1(path: String): ContentFilterDb3V1 =
    Room.databaseBuilder<ContentFilterDb3V1>(path)
        .setDriver(BundledSQLiteDriver())
        .build()
