package com.github.zly2006.zhihu.nlp

import android.content.Context
import com.github.zly2006.zhihu.nlp.NLPService
import com.github.zly2006.zhihu.viewmodel.filter.BlockedContentRecord
import com.github.zly2006.zhihu.viewmodel.filter.BlockedKeyword
import com.github.zly2006.zhihu.viewmodel.filter.ContentFilterDatabase
import com.github.zly2006.zhihu.viewmodel.filter.KeywordType
import com.github.zly2006.zhihu.viewmodel.filter.MatchedKeywordInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/**
 * 屏蔽词管理仓库
 * 负责管理屏蔽词的增删改查和NLP过滤逻辑
 */
class BlockedKeywordRepository(
    context: Context,
) {
    private val database = ContentFilterDatabase.getDatabase(context)
    private val keywordDao = database.blockedKeywordDao()
    private val recordDao = database.blockedContentRecordDao()

    /**
     * 获取所有屏蔽词
     */
    suspend fun getAllKeywords(): List<BlockedKeyword> = withContext(Dispatchers.IO) {
        keywordDao.getAllKeywords()
    }

    /**
     * 获取精确匹配关键词
     */
    suspend fun getExactMatchKeywords(): List<BlockedKeyword> = withContext(Dispatchers.IO) {
        keywordDao.getAllKeywords().filter {
            it.getKeywordTypeEnum() == KeywordType.EXACT_MATCH
        }
    }

    /**
     * 获取NLP语义关键词
     */
    suspend fun getNLPSemanticKeywords(): List<BlockedKeyword> = withContext(Dispatchers.IO) {
        keywordDao.getAllKeywords().filter {
            it.getKeywordTypeEnum() == KeywordType.NLP_SEMANTIC
        }
    }

    /**
     * 添加屏蔽词
     */
    suspend fun addKeyword(
        keyword: String,
        keywordType: KeywordType = KeywordType.NLP_SEMANTIC,
    ): Long = withContext(Dispatchers.IO) {
        val blockedKeyword = BlockedKeyword(
            keyword = keyword.trim(),
            keywordType = keywordType.name,
            caseSensitive = false,
            isRegex = false,
            createdTime = System.currentTimeMillis(),
        )
        keywordDao.insertKeyword(blockedKeyword)
    }

    /**
     * 添加精确匹配关键词
     */
    suspend fun addExactMatchKeyword(
        keyword: String,
        caseSensitive: Boolean = false,
        isRegex: Boolean = false,
    ): Long = withContext(Dispatchers.IO) {
        val blockedKeyword = BlockedKeyword(
            keyword = keyword.trim(),
            keywordType = KeywordType.EXACT_MATCH.name,
            caseSensitive = caseSensitive,
            isRegex = isRegex,
            createdTime = System.currentTimeMillis(),
        )
        keywordDao.insertKeyword(blockedKeyword)
    }

    /**
     * 添加NLP语义短语（空格分隔的多个关键词）
     */
    suspend fun addNLPPhrase(phrase: String): Long = withContext(Dispatchers.IO) {
        val blockedKeyword = BlockedKeyword(
            keyword = phrase.trim(),
            keywordType = KeywordType.NLP_SEMANTIC.name,
            caseSensitive = false,
            isRegex = false,
            createdTime = System.currentTimeMillis(),
        )
        keywordDao.insertKeyword(blockedKeyword)
    }

    /**
     * 更新关键词
     */
    suspend fun updateKeyword(keyword: BlockedKeyword): Unit = withContext(Dispatchers.IO) {
        keywordDao.insertKeyword(keyword)
    }

    /**
     * 删除屏蔽词
     */
    suspend fun deleteKeyword(keyword: BlockedKeyword) = withContext(Dispatchers.IO) {
        keywordDao.deleteKeyword(keyword)
    }

    /**
     * 删除屏蔽词（通过ID）
     */
    suspend fun deleteKeywordById(id: Long) = withContext(Dispatchers.IO) {
        keywordDao.deleteKeywordById(id)
    }

    /**
     * 清空所有屏蔽词
     */
    suspend fun clearAllKeywords() = withContext(Dispatchers.IO) {
        keywordDao.clearAllKeywords()
    }

    /**
     * 获取屏蔽词数量
     */
    suspend fun getKeywordCount(): Int = withContext(Dispatchers.IO) {
        keywordDao.getKeywordCount()
    }

    /**
     * 检查内容是否应该被NLP语义屏蔽（标题+摘要+内容）
     * 标题权重更高，会被重复分析以提高影响力
     * @param title 标题
     * @param excerpt 摘要
     * @param content 正文内容
     * @param threshold 相似度阈值，默认0.3
     * @return Pair<是否屏蔽, 匹配的关键词列表>
     */
    suspend fun checkNLPBlockingWithWeight(
        title: String,
        excerpt: String?,
        content: String?,
        threshold: Double = 0.8,
    ): Pair<Boolean, List<MatchedKeywordInfo>> {
        if (title.isBlank()) return Pair(false, emptyList())

        val nlpKeywords = getNLPSemanticKeywords()
        if (nlpKeywords.isEmpty()) return Pair(false, emptyList())

        // 获取所有NLP短语
        val phrases = nlpKeywords.map { it.keyword }

        // 构建加权文本：标题重复3次以提高权重
        val weightedText = buildString {
            append(title)
            // 添加摘要
            if (!excerpt.isNullOrBlank()) {
                append(excerpt)
                append(" ")
            }
        }

        // 检查匹配情况
        val matches = NLPService.checkBlockedPhrases(weightedText, phrases, threshold)

        val matchedInfos = matches.map { (phrase, similarity) ->
            MatchedKeywordInfo(phrase, similarity)
        }

        return Pair(matches.isNotEmpty(), matchedInfos)
    }

    /**
     * 记录被NLP屏蔽的内容
     */
    suspend fun recordBlockedContent(
        contentId: String,
        contentType: String,
        title: String,
        excerpt: String?,
        authorName: String?,
        authorId: String?,
        matchedKeywords: List<MatchedKeywordInfo>,
    ) = withContext(Dispatchers.IO) {
        try {
            // 构建匹配关键词的JSON
            val matchedArray = JSONArray()
            val top3Matches = matchedKeywords.sortedByDescending { it.similarity }.take(3)

            for (match in top3Matches) {
                val obj = JSONObject()
                obj.put("keyword", match.keyword)
                obj.put("similarity", match.similarity)
                matchedArray.put(obj)
            }

            val record = BlockedContentRecord(
                contentId = contentId,
                contentType = contentType,
                title = title,
                excerpt = excerpt ?: "",
                authorName = authorName,
                authorId = authorId,
                blockedTime = System.currentTimeMillis(),
                blockReason = "NLP语义匹配",
                matchedKeywords = matchedArray.toString(),
            )

            recordDao.insertRecord(record)
            // 维护记录数量限制
            recordDao.maintainRecordLimit()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 获取最近被屏蔽的内容记录
     */
    suspend fun getRecentBlockedRecords(limit: Int = 100): List<BlockedContentRecord> =
        withContext(Dispatchers.IO) {
            recordDao.getRecentBlockedRecords(limit)
        }

    /**
     * 删除屏蔽记录
     */
    suspend fun deleteBlockedRecord(id: Long) = withContext(Dispatchers.IO) {
        recordDao.deleteRecord(id)
    }

    /**
     * 清空所有屏蔽记录
     */
    suspend fun clearAllBlockedRecords() = withContext(Dispatchers.IO) {
        recordDao.clearAllRecords()
    }

    /**
     * 解析匹配关键词JSON
     */
    fun parseMatchedKeywords(json: String): List<MatchedKeywordInfo> = try {
        val array = JSONArray(json)
        val result = mutableListOf<MatchedKeywordInfo>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            result.add(
                MatchedKeywordInfo(
                    keyword = obj.getString("keyword"),
                    similarity = obj.getDouble("similarity"),
                ),
            )
        }
        result
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }
}
