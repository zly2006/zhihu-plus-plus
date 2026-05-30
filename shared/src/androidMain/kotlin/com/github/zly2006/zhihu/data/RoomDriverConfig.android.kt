package com.github.zly2006.zhihu.data

import androidx.room.RoomDatabase

/**
 * Android: framework SQLite is the default, no explicit driver needed.
 */
actual fun <T : RoomDatabase> RoomDatabase.Builder<T>.applyPlatformDriver(): RoomDatabase.Builder<T> = this
