/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.github.zly2006.zhihu.viewmodel.local

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import kotlin.jvm.java

fun LocalRecommendationEngine(context: Context): LocalRecommendationEngine {
    val dao = getLocalContentDatabase(context).contentDao()
    val contentInitializer = LocalContentInitializer(context)
    val taskScheduler = TaskScheduler(context)
    val crawlingExecutor = CrawlingExecutor(context)
    return LocalRecommendationEngine(
        dao = dao,
        feedGenerator = FeedGenerator(dao),
        userBehaviorAnalyzer = UserBehaviorAnalyzer(dao),
        initializeContentIfNeeded = { contentInitializer.initializeIfNeeded() },
        startScheduling = { taskScheduler.startScheduling() },
        stopScheduling = { taskScheduler.stopScheduling() },
        executeTask = { task -> crawlingExecutor.executeTask(task) },
        isNetworkAvailable = { isLocalRecommendationNetworkAvailable(context) },
        logWarning = { message -> Log.w("LocalRecommendationEngine", message) },
        logError = { message, throwable -> Log.e("LocalRecommendationEngine", message, throwable) },
    )
}

private fun isLocalRecommendationNetworkAvailable(context: Context): Boolean = try {
    val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
    val activeNetwork = connectivityManager.activeNetwork
    val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
    networkCapabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
} catch (_: Exception) {
    false
}
