package com.github.zly2006.zhihu.viewmodel.local

import androidx.room.Room
import java.io.File

fun getLocalContentDatabase(databaseFile: File): LocalContentDatabase =
    buildLocalContentDatabase(
        Room.databaseBuilder<LocalContentDatabase>(
            name = databaseFile.absolutePath,
        ),
    )
