package com.github.zly2006.zhihu.viewmodel.filter

import androidx.room.Room
import java.io.File

fun getContentFilterDatabase(databaseFile: File): ContentFilterDatabase =
    buildContentFilterDatabase(
        Room.databaseBuilder<ContentFilterDatabase>(
            name = databaseFile.absolutePath,
        ),
    )
