package com.github.zly2006.zhihu.data

import androidx.room.RoomDatabase

actual fun <T : RoomDatabase> RoomDatabase.Builder<T>.applyPlatformDriver(): RoomDatabase.Builder<T> {
    TODO("Not yet implemented")
}
