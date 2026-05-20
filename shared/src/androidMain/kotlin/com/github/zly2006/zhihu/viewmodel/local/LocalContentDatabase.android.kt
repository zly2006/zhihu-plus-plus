package com.github.zly2006.zhihu.viewmodel.local

import android.content.Context
import androidx.room.Room

private const val LOCAL_CONTENT_DATABASE_NAME = "local_content_database"

@Volatile
private var localContentDatabase: LocalContentDatabase? = null

fun getLocalContentDatabase(context: Context): LocalContentDatabase =
    localContentDatabase ?: synchronized(LocalContentDatabase::class) {
        localContentDatabase ?: buildLocalContentDatabase(
            Room.databaseBuilder<LocalContentDatabase>(
                context.applicationContext,
                LOCAL_CONTENT_DATABASE_NAME,
            ),
        ).also {
            localContentDatabase = it
        }
    }
