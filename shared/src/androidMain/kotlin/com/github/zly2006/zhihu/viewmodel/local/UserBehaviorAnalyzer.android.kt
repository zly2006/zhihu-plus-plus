package com.github.zly2006.zhihu.viewmodel.local

import android.content.Context

fun UserBehaviorAnalyzer(context: Context): UserBehaviorAnalyzer =
    UserBehaviorAnalyzer(getLocalContentDatabase(context).contentDao())
