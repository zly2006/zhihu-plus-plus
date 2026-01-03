package com.github.zly2006.zhihu.nlp

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 关键词分析器
 * 提供完整的关键词提取、去重、过滤逻辑
 */
object KeywordAnalyzer {
    /**
     * 从Feed内容中提取关键词，标题权重更高
     * @param title 标题（必填）
     * @param excerpt 摘要
     * @param content 正文内容
     * @param topN 返回前N个关键词
     * @return 去重和过滤后的关键词列表
     */
    suspend fun extractFromFeed(
        title: String,
        excerpt: String? = null,
        content: String? = null,
        topN: Int = 10,
    ): List<String> = withContext(Dispatchers.Default) {
        if (title.isBlank()) return@withContext emptyList()

        try {
            // 构建加权文本：标题重复3次以提高权重
            val weightedText = buildWeightedText(title, excerpt, content)

            // 提取关键词（提取更多用于去重和过滤）
            val extractedKeywords = NLPService.extractKeywords(weightedText, topN * 3)

            // 去重、过滤和排序
            return@withContext processKeywords(extractedKeywords, topN)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * 从Feed内容中提取关键词及权重
     * @param title 标题（必填）
     * @param excerpt 摘要
     * @param content 正文内容
     * @param topN 返回前N个关键词
     * @return 去重和过滤后的关键词及权重列表
     */
    suspend fun extractFromFeedWithWeight(
        title: String,
        excerpt: String? = null,
        content: String? = null,
        topN: Int = 10,
    ): List<KeywordWithWeight> = withContext(Dispatchers.Default) {
        if (title.isBlank()) return@withContext emptyList()

        try {
            // 构建加权文本
            val weightedText = buildWeightedText(title, excerpt, content)

            // 提取关键词及权重（提取更多用于去重和过滤）
            val keywordsWithWeight = NLPService.extractKeywordsWithWeight(weightedText, topN * 3)

            // 去重和过滤
            return@withContext processKeywordsWithWeight(keywordsWithWeight, topN)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * 构建加权文本
     * 标题重复3次，摘要1次，正文取前500字
     */
    private fun buildWeightedText(
        title: String,
        excerpt: String?,
        content: String?,
    ): String = buildString {
        // 标题权重最高，重复3次
        repeat(3) {
            append(title)
            append(" ")
        }

        // 添加摘要
        if (!excerpt.isNullOrBlank()) {
            append(excerpt)
            append(" ")
        }

        // 添加正文前500字
        if (!content.isNullOrBlank()) {
            append(content.take(500))
        }
    }

    /**
     * 处理关键词：去重、过滤、排序
     */
    private fun processKeywords(keywords: List<String>, topN: Int): List<String> = keywords
        .asSequence()
        .distinct() // 去重
        .filter { it.length >= 2 } // 过滤过短的词（至少2个字符）
        .filter { !isStopWord(it) } // 过滤停用词
        .filter { !isNumberOnly(it) } // 过滤纯数字
        .take(topN)
        .toList()

    /**
     * 处理带权重的关键词：去重、过滤、排序
     */
    private fun processKeywordsWithWeight(
        keywordsWithWeight: List<KeywordWithWeight>,
        topN: Int,
    ): List<KeywordWithWeight> {
        // 使用Map去重，保留权重最高的
        val keywordMap = mutableMapOf<String, Double>()
        for ((keyword, weight) in keywordsWithWeight) {
            val current = keywordMap[keyword] ?: 0.0
            if (weight > current) {
                keywordMap[keyword] = weight
            }
        }

        return keywordMap
            .asSequence()
            .filter { (keyword, _) -> keyword.length >= 2 } // 过滤过短的词
            .filter { (keyword, _) -> !isStopWord(keyword) } // 过滤停用词
            .filter { (keyword, _) -> !isNumberOnly(keyword) } // 过滤纯数字
            .map { (keyword, weight) -> KeywordWithWeight(keyword, weight) }
            .sortedByDescending { it.weight } // 按权重降序
            .take(topN)
            .toList()
    }

    /**
     * 判断是否为停用词
     */
    private fun isStopWord(word: String): Boolean {
        // 常见停用词列表
        val stopWords = setOf(
            "的",
            "了",
            "在",
            "是",
            "我",
            "有",
            "和",
            "就",
            "不",
            "人",
            "都",
            "一",
            "一个",
            "上",
            "也",
            "很",
            "到",
            "说",
            "要",
            "去",
            "你",
            "会",
            "着",
            "没有",
            "看",
            "好",
            "自己",
            "这",
            "个",
            "们",
            "那",
            "来",
            "么",
            "吗",
            "时",
            "大",
            "地",
            "为",
            "子",
            "中",
            "你们",
            "对",
            "生",
            "能",
            "而",
            "子",
            "得",
            "与",
            "她",
            "他",
            "以",
            "及",
            "于",
            "用",
            "就是",
            "比",
            "啊",
            "呢",
            "吧",
            "呀",
            "嘛",
            "哦",
            "哪",
            "什么",
            "怎么",
        )
        return word in stopWords
    }

    /**
     * 判断是否为纯数字
     */
    private fun isNumberOnly(word: String): Boolean = word.all { it.isDigit() }

    /**
     * 合并多个关键词列表并去重
     */
    fun mergeAndDeduplicate(vararg keywordLists: List<String>): List<String> = keywordLists
        .asSequence()
        .flatten()
        .distinct()
        .filter { it.length >= 2 }
        .filter { !isStopWord(it) }
        .filter { !isNumberOnly(it) }
        .toList()
}
