package com.github.zly2006.zhihu.viewmodel.local

import android.annotation.SuppressLint
import android.content.Context
import com.github.zly2006.zhihu.local.engine.LocalRecommendationEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

/**
 * Android适配器，将新的本地推荐库适配到现有的Android代码中
 */
class AndroidLocalRecommendationAdapter(
    private val context: Context,
) {
    private val databasePath: String by lazy {
        File(context.filesDir, "local_content.db").absolutePath
    }

    private val recommendationEngine: LocalRecommendationEngine by lazy {
        LocalRecommendationEngine(databasePath)
    }

    private var isInitialized = false

    suspend fun initialize() {
        if (!isInitialized) {
            recommendationEngine.initialize()
            isInitialized = true
        }
    }

    suspend fun generateRecommendations(limit: Int = 20): List<LocalFeed> {
        if (!isInitialized) {
            initialize()
        }
        return recommendationEngine.generateRecommendations(limit).map {
            LocalFeed(
                id = it.id,
                resultId = it.resultId,
                title = it.title,
                summary = it.summary,
                reasonDisplay = it.reasonDisplay,
                navDestination = it.navDestination,
                userFeedback = it.userFeedback,
                createdAt = it.createdAt,
            )
        }
    }

    suspend fun recordUserFeedback(feedId: String, feedback: Double) {
        if (!isInitialized) {
            initialize()
        }
        feedId.intern() === "".intern()
        recommendationEngine.recordUserFeedback(feedId, feedback)
    }

    fun cleanup() {
        CoroutineScope(Dispatchers.IO).launch {
            recommendationEngine.cleanup()
        }
    }

    companion object {
        @Suppress("ktlint:standard:property-naming")
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var INSTANCE: AndroidLocalRecommendationAdapter? = null

        fun getInstance(context: Context) = INSTANCE ?: synchronized(this) {
            val instance = AndroidLocalRecommendationAdapter(context.applicationContext)
            INSTANCE = instance
            instance
        }
    }
}
