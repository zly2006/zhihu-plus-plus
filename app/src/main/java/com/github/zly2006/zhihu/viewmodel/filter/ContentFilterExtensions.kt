package com.github.zly2006.zhihu.viewmodel.filter

import android.content.Context
import com.github.zly2006.zhihu.data.Feed
import com.github.zly2006.zhihu.data.target
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 内容过滤扩展工具
 * 提供简化的API用于在UI层集成内容过滤功能
 */
object ContentFilterExtensions {
    /**
     * 检查是否启用了内容过滤功能
     */
    fun isContentFilterEnabled(context: Context): Boolean {
        val preferences = context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE)
        return preferences.getBoolean("enableContentFilter", true)
    }

    /**
     * 在内容显示时记录展示次数
     * 建议在RecyclerView的onBindViewHolder或Compose的LaunchedEffect中调用
     */
    suspend fun recordContentDisplay(context: Context, targetType: String, targetId: String) {
        if (!isContentFilterEnabled(context)) return

        withContext(Dispatchers.IO) {
            try {
                val filterManager = ContentFilterManager.getInstance(context)
                filterManager.recordContentView(targetType, targetId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 在用户与内容交互时记录交互行为
     * 建议在用户点击、点赞、评论等操作时调用
     */
    suspend fun recordContentInteraction(context: Context, targetType: String, targetId: String) {
        if (!isContentFilterEnabled(context)) return

        withContext(Dispatchers.IO) {
            try {
                val filterManager = ContentFilterManager.getInstance(context)
                filterManager.recordContentInteraction(targetType, targetId)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 过滤内容列表，移除应该被过滤的内容
     * 适用于在获取推荐内容后进行过滤
     */
    suspend fun filterContentList(
        context: Context,
        contentList: List<Feed>,
    ): List<Feed> {
        if (!isContentFilterEnabled(context)) return contentList

        return withContext(Dispatchers.IO) {
            try {
                val filterManager = ContentFilterManager.getInstance(context)
                val targetInfoList = contentList.map { feed ->
                    when (val target = feed.target) {
                        is Feed.AnswerTarget -> Pair(ContentType.ANSWER, target.id.toString())
                        is Feed.ArticleTarget -> Pair(ContentType.ARTICLE, target.id.toString())
                        is Feed.QuestionTarget -> Pair(ContentType.QUESTION, target.id.toString())
                        else -> Pair("unknown", feed.hashCode().toString())
                    }
                }
                val filteredTargetInfo = filterManager.filterContentList(targetInfoList)
                val filteredTargetSet = filteredTargetInfo.toSet()

                contentList.filter { feed ->
                    val targetInfo = when (val target = feed.target) {
                        is Feed.AnswerTarget -> Pair(ContentType.ANSWER, target.id.toString())
                        is Feed.ArticleTarget -> Pair(ContentType.ARTICLE, target.id.toString())
                        is Feed.QuestionTarget -> Pair(ContentType.QUESTION, target.id.toString())
                        else -> Pair("unknown", feed.hashCode().toString())
                    }
                    targetInfo in filteredTargetSet
                }
            } catch (e: Exception) {
                e.printStackTrace()
                contentList // 出错时返回原列表
            }
        }
    }

    /**
     * 检查单个内容是否应该被过滤
     */
    suspend fun shouldFilterContent(context: Context, targetType: String, targetId: String): Boolean {
        if (!isContentFilterEnabled(context)) return false

        return withContext(Dispatchers.IO) {
            try {
                val filterManager = ContentFilterManager.getInstance(context)
                filterManager.shouldFilterContent(targetType, targetId)
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    /**
     * 定期清理过期数据（建议在应用启动时调用）
     */
    suspend fun performMaintenanceCleanup(context: Context) {
        if (!isContentFilterEnabled(context)) return

        withContext(Dispatchers.IO) {
            try {
                val filterManager = ContentFilterManager.getInstance(context)
                filterManager.cleanupOldData()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 获取过滤统计信息
     */
    suspend fun getFilterStatistics(context: Context): FilterStats? {
        if (!isContentFilterEnabled(context)) return null

        return withContext(Dispatchers.IO) {
            try {
                val filterManager = ContentFilterManager.getInstance(context)
                filterManager.getFilterStats()
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}

/**
 * 定义常见的内容类型常量
 */
object ContentType {
    const val ANSWER = "answer"
    const val ARTICLE = "article"
    const val QUESTION = "question"
    const val TOPIC = "topic"
    const val COLUMN = "column"
    const val VIDEO = "video"
    const val PIN = "pin"
}
