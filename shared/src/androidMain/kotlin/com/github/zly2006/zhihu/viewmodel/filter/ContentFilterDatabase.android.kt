package com.github.zly2006.zhihu.viewmodel.filter

import android.content.Context
import androidx.room.Room

private const val CONTENT_FILTER_DATABASE_NAME = "content_filter_database"

@Volatile
private var contentFilterDatabase: ContentFilterDatabase? = null

fun getContentFilterDatabase(context: Context): ContentFilterDatabase =
    contentFilterDatabase ?: synchronized(ContentFilterDatabase::class) {
        contentFilterDatabase ?: buildContentFilterDatabase(
            Room.databaseBuilder<ContentFilterDatabase>(
                context.applicationContext,
                CONTENT_FILTER_DATABASE_NAME,
            ),
        ).also {
            contentFilterDatabase = it
        }
    }

fun clearContentFilterDatabaseInstance() {
    contentFilterDatabase = null
}
