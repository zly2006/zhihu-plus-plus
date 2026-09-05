package com.github.zly2006.zhihu.harmonyprobe.db.legacy

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

fun buildLegacyContentFilterDatabase(path: String): LegacyContentFilterDatabase =
    Room.databaseBuilder<LegacyContentFilterDatabase>(path)
        .setDriver(BundledSQLiteDriver())
        .build()
