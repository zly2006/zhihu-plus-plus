package com.github.zly2006.zhihu.viewmodel.local

import android.content.Context

fun FeedGenerator(context: Context): FeedGenerator =
    FeedGenerator(getLocalContentDatabase(context).contentDao())
