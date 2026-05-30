package com.github.zly2006.zhihu.data

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver

/**
 * JVM/Desktop: use BundledSQLiteDriver for bundled native SQLite.
 */
actual fun <T : RoomDatabase> RoomDatabase.Builder<T>.applyPlatformDriver(): RoomDatabase.Builder<T> =
    setDriver(BundledSQLiteDriver())
