package com.github.zly2006.zhihu.data

import androidx.room.RoomDatabase

/**
 * Platform-specific Room database driver configuration.
 * Android: uses framework SQLite (no-op, Room default).
 * JVM/Desktop: uses BundledSQLiteDriver for bundled native SQLite.
 */
expect fun <T : RoomDatabase> RoomDatabase.Builder<T>.applyPlatformDriver(): RoomDatabase.Builder<T>
