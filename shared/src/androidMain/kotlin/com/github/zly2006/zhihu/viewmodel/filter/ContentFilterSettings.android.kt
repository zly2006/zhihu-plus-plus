package com.github.zly2006.zhihu.viewmodel.filter

import android.content.Context
import com.github.zly2006.zhihu.shared.platform.androidSettingsStore

fun Context.contentFilterSettings(): FeedFilterSettings =
    androidSettingsStore(this).toFeedFilterSettings()
