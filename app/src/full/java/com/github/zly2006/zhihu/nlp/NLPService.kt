package com.github.zly2006.zhihu.nlp

import android.util.Log
import com.hankcs.hanlp.HanLP
import com.hankcs.hanlp.seg.common.Term
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * NLP服务，用于中文文本的关键词提取和语义相似度计算
 */
object NLPService {
    private const val TAG = "NLPService"
    private const val MAX_KEYWORD_CANDIDATES = 60
    private const val KEYWORD_CANDIDATE_MULTIPLIER = 5
    private const val MMR_LAMBDA = 0.65
    private const val MAX_EMBEDDING_TEXT_LENGTH = 2048
    private const val MIN_KEYWORD_LENGTH = 2
    private const val MAX_SEGMENT_COUNT = 48
    private const val MAX_SEGMENT_CHAR_LENGTH = 160
    private const val SEGMENT_WINDOW_STEP = 90
    private const val MIN_SEGMENT_CHAR_LENGTH = 4
    private val SENTENCE_SPLIT_REGEX = "[。！？!?\\n]+".toRegex()

    private data class SegmentedTextContext(
        val originalText: String,
        val segments: List<String>,
        val segmentEmbeddings: List<FloatArray?>,
    )

    /**
     * 从文本中提取关键词
     */
    suspend fun extractKeywords(text: String, topN: Int = 5): List<String> = withContext(Dispatchers.Default) {
        extractKeywordsInternal(text, topN).map { it.keyword }
    }

    /**
     * 从文本中提取关键词及其权重
     */
    suspend fun extractKeywordsWithWeight(text: String, topN: Int = 5): List<KeywordWithWeight> = withContext(Dispatchers.Default) {
        extractKeywordsInternal(text, topN)
    }

    private suspend fun extractKeywordsInternal(text: String, topN: Int): List<KeywordWithWeight> {
        if (text.isBlank() || topN <= 0) return emptyList()
        val candidates = generateKeywordCandidates(text, topN)
        val debugTrace = NlpDebugTrace(normalizedText = text.trim(), candidateKeywords = candidates)
        logDebug("extractKeywordsInternal textLen=${text.length} topN=$topN candidates=${candidates.size}")
        if (candidates.isEmpty()) return emptyList()

        val mmrResult = selectKeywordsWithMMR(text, candidates, topN, debugTrace)
        if (mmrResult.isNotEmpty()) {
            logDebug("MMR selected=${mmrResult.map { it.keyword }} trace=$debugTrace")
            return mmrResult
        }

        // Fallback：使用HanLP结果
        val fallback = candidates.take(topN)
        val denominator = max(1, fallback.size - 1)
        val fallbackResult = fallback.mapIndexed { index, keyword ->
            KeywordWithWeight(keyword, 1.0 - index.toDouble() / max(1, denominator))
        }
        logDebug("Fallback keywords=${fallbackResult.map { it.keyword }} trace=$debugTrace")
        return fallbackResult
    }

    private suspend fun calculatePhraseMatchScoreForContext(
        phraseKeywords: String,
        segmentedContext: SegmentedTextContext,
    ): PhraseMatchResult {
        val normalizedPhrase = phraseKeywords.trim()
        if (normalizedPhrase.isEmpty()) {
            return PhraseMatchResult(
                phrase = phraseKeywords,
                normalizedPhrase = normalizedPhrase,
                keywords = emptyList(),
                phraseEmbeddingDim = null,
                bestScore = 0.0,
                segmentDebug = emptyList(),
            )
        }
        val keywords = normalizedPhrase
            .split("\\s+".toRegex())
            .filter { it.isNotBlank() }

        logDebug("calculatePhraseMatchScore phrase='$normalizedPhrase' keywords=${keywords.joinToString()}")
        val phraseEmbedding = encodeTextOrNull(normalizedPhrase)
        var bestScore = 0.0
        val segmentDebug = mutableListOf<SegmentMatchDebug>()
        if (phraseEmbedding != null) {
            var hasSegmentEmbedding = false
            segmentedContext.segmentEmbeddings.forEachIndexed { idx, embedding ->
                if (embedding != null) {
                    hasSegmentEmbedding = true
                    val score = cosineToScore(cosineSimilarity(phraseEmbedding, embedding))
                    segmentDebug.add(
                        SegmentMatchDebug(
                            index = idx,
                            segmentTextPreview = segmentedContext.segments.getOrNull(idx)?.take(80) ?: "",
                            segmentLength = segmentedContext.segments.getOrNull(idx)?.length ?: 0,
                            embeddingDim = embedding.size,
                            score = score,
                        ),
                    )
                    logDebug(
                        "phrase ($normalizedPhrase) vs segment#$idx score=$score segmentLen=${segmentedContext.segments.getOrNull(idx)?.length ?: 0}" +
                            " (${segmentedContext.segments.getOrNull(idx)})",
                    )
                    if (score > bestScore) {
                        bestScore = score
                    }
                } else {
                    segmentDebug.add(
                        SegmentMatchDebug(
                            index = idx,
                            segmentTextPreview = segmentedContext.segments.getOrNull(idx)?.take(80) ?: "",
                            segmentLength = segmentedContext.segments.getOrNull(idx)?.length ?: 0,
                            embeddingDim = null,
                            score = null,
                        ),
                    )
                    logDebug("phrase vs segment#$idx missing embedding")
                }
            }
            if (!hasSegmentEmbedding) {
                logDebug("phrase bestScore=0.0 (no segment embeddings)")
            } else {
                logDebug("phrase bestScore=$bestScore")
            }
        } else {
            logDebug("phrase embedding null for '$normalizedPhrase'")
        }

        return PhraseMatchResult(
            phrase = phraseKeywords,
            normalizedPhrase = normalizedPhrase,
            keywords = keywords,
            phraseEmbeddingDim = phraseEmbedding?.size,
            bestScore = bestScore,
            segmentDebug = segmentDebug,
        )
    }

    private suspend fun buildSegmentedContext(text: String): SegmentedTextContext {
        val normalized = text.trim()
        val segments = splitTextIntoSegments(normalized)
            .ifEmpty {
                val fallback = normalized.take(MAX_SEGMENT_CHAR_LENGTH)
                if (fallback.isNotEmpty()) listOf(fallback) else emptyList()
            }.take(MAX_SEGMENT_COUNT)

        val embeddings = segments.mapIndexed { idx, segment ->
            val emb = encodeTextOrNull(segment)
            val preview = previewEmbedding(emb)
            logDebug("segment#$idx len=${segment.length} embedding=${emb?.size ?: 0} preview=$preview")
            emb
        }

        logDebug("buildSegmentedContext normalizedLen=${normalized.length} segments=${segments.size}")
        return SegmentedTextContext(
            originalText = normalized,
            segments = segments,
            segmentEmbeddings = embeddings,
        )
    }

    private fun splitTextIntoSegments(text: String): List<String> {
        if (text.isBlank()) return emptyList()
        logDebug("splitTextIntoSegments len=${text.length}")
        val rawSentences = SENTENCE_SPLIT_REGEX
            .split(text)
            .map { it.trim() }
            .filter { it.length >= MIN_SEGMENT_CHAR_LENGTH }

        val segments = mutableListOf<String>()
        for (sentence in rawSentences) {
            if (sentence.isEmpty()) continue
            if (sentence.length <= MAX_SEGMENT_CHAR_LENGTH) {
                logDebug("sentence kept len=${sentence.length} text='${sentence.take(80)}'")
                segments.add(sentence)
            } else {
                var start = 0
                logDebug("sentence sliding len=${sentence.length} text='${sentence.take(80)}'")
                while (start < sentence.length) {
                    val end = min(sentence.length, start + MAX_SEGMENT_CHAR_LENGTH)
                    val chunk = sentence.substring(start, end)
                    segments.add(chunk)
                    logDebug("chunk start=$start end=$end len=${chunk.length} text='${chunk.take(80)}'")
                    if (segments.size >= MAX_SEGMENT_COUNT) return segments
                    start += SEGMENT_WINDOW_STEP
                }
            }
            if (segments.size >= MAX_SEGMENT_COUNT) break
        }

        if (segments.isEmpty()) {
            val fallback = text.take(MAX_SEGMENT_CHAR_LENGTH)
            if (fallback.isNotEmpty()) {
                logDebug("segments empty, fallback len=${fallback.length}")
                segments.add(fallback)
            }
        }

        logDebug("splitTextIntoSegments result count=${segments.size} lengths=${segments.map { it.length }}")
        return segments
    }

    private fun generateKeywordCandidates(text: String, topN: Int): List<String> {
        if (text.isBlank()) return emptyList()
        val poolSize = max(topN * KEYWORD_CANDIDATE_MULTIPLIER, topN + 3)
        val maxPoolSize = min(MAX_KEYWORD_CANDIDATES * 2, poolSize)
        val result = HanLP
            .extractKeyword(text, maxPoolSize)
            .mapNotNull { it?.trim() }
            .filter { it.length >= MIN_KEYWORD_LENGTH }
            .distinct()
        logDebug("generateKeywordCandidates topN=$topN pool=$maxPoolSize result=${result.size} list=$result")
        return result
    }

    private suspend fun selectKeywordsWithMMR(
        text: String,
        candidates: List<String>,
        topN: Int,
        debugTrace: NlpDebugTrace?,
    ): List<KeywordWithWeight> {
        val documentEmbedding = encodeTextOrNull(text) ?: return emptyList()
        val limitedCandidates = candidates.take(min(candidates.size, MAX_KEYWORD_CANDIDATES))
        if (limitedCandidates.isEmpty()) return emptyList()

        val candidateEmbeddings = buildMap {
            for (candidate in limitedCandidates) {
                val embedding = encodeTextOrNull(candidate)
                if (embedding != null) {
                    put(candidate, embedding)
                } else {
                    logDebug("candidate embedding null for '$candidate'")
                }
            }
        }
        logDebug("selectKeywordsWithMMR limited=${limitedCandidates.size} withEmbeddings=${candidateEmbeddings.size}")
        if (candidateEmbeddings.isEmpty()) return emptyList()

        val remaining = candidateEmbeddings.toMutableMap()
        val selected = mutableListOf<KeywordWithWeight>()

        while (selected.size < topN && remaining.isNotEmpty()) {
            var bestKey: String? = null
            var bestRelevance = 0.0
            var bestScore = Double.NEGATIVE_INFINITY
            remaining.forEach { (word, embedding) ->
                val relevance = cosineSimilarity(documentEmbedding, embedding)
                val redundancy = selected.maxOfOrNull { keyword ->
                    val otherEmbedding = candidateEmbeddings[keyword.keyword]
                    if (otherEmbedding != null) cosineSimilarity(embedding, otherEmbedding) else 0.0
                } ?: 0.0
                val score = MMR_LAMBDA * relevance - (1 - MMR_LAMBDA) * redundancy
                debugTrace?.mmrIterations?.add(
                    KeywordSelectionDebug(
                        iteration = selected.size,
                        candidate = word,
                        relevance = relevance,
                        redundancy = redundancy,
                        score = score,
                    ),
                )
                logDebug("MMR iter=${selected.size} word='$word' rel=$relevance red=$redundancy score=$score")
                if (score > bestScore) {
                    bestScore = score
                    bestKey = word
                    bestRelevance = relevance
                }
            }

            val keyword = bestKey ?: break
            val kw = KeywordWithWeight(keyword, cosineToScore(bestRelevance))
            selected.add(kw)
            debugTrace?.selectedKeywords?.add(kw)
            remaining.remove(keyword)
            logDebug("MMR pick='$keyword' score=$bestScore rel=$bestRelevance remaining=${remaining.size}")
        }

        return selected
    }

    private suspend fun encodeTextOrNull(text: String): FloatArray? {
        val normalized = truncateForEmbedding(text)
        if (normalized.isBlank()) return null
        return try {
            val embedding = SentenceEmbeddingManager.encode(normalized)
            logDebug("encodeText len=${normalized.length} dim=${embedding.size} text='${normalized.take(80)}'")
            embedding
        } catch (e: Exception) {
            Log.e(TAG, "encodeText failed len=${normalized.length} text='${normalized.take(80)}'", e)
            null
        }
    }

    private fun truncateForEmbedding(text: String): String =
        text.trim().take(MAX_EMBEDDING_TEXT_LENGTH)

    private fun cosineSimilarity(
        first: FloatArray,
        second: FloatArray,
    ): Double {
        var dot = 0.0
        var magFirst = 0.0
        var magSecond = 0.0
        val size = min(first.size, second.size)
        for (i in 0 until size) {
            val v1 = first[i].toDouble()
            val v2 = second[i].toDouble()
            dot += v1 * v2
            magFirst += v1 * v1
            magSecond += v2 * v2
        }
        val denominator = sqrt(magFirst) * sqrt(magSecond)
        if (denominator == 0.0) return 0.0
        return dot / denominator
    }

    private fun cosineToScore(value: Double): Double = ((value + 1.0) / 2.0).coerceIn(0.0, 1.0)

    private fun logDebug(message: String) {
        Log.d(TAG, message)
    }

    private fun previewEmbedding(embedding: FloatArray?): String {
        if (embedding == null) return "null"
        val head = embedding.take(4).joinToString(prefix = "[", postfix = if (embedding.size > 4) ", ...]" else "]")
        return "dim=${embedding.size} sample=$head"
    }

    /**
     * 检查文本是否与屏蔽短语匹配，并返回匹配详情
     * @param text 要检查的文本
     * @param blockedPhrases 屏蔽短语列表（空格分隔的关键词）
     * @param threshold 相似度阈值，默认0.3
     * @return 匹配的短语及其相似度列表，按相似度降序排列
     */
    suspend fun checkBlockedPhrases(
        text: String,
        blockedPhrases: List<String>,
        threshold: Double = 0.8,
    ): List<Pair<String, Double>> = withContext(Dispatchers.Default) {
        if (text.isBlank() || blockedPhrases.isEmpty()) return@withContext emptyList()

        try {
            val matches = mutableListOf<Pair<String, Double>>()
            val segmentedContext = buildSegmentedContext(text)
            logDebug("checkBlockedPhrases phrases=${blockedPhrases.size} segments=${segmentedContext.segments.size}")

            for (phrase in blockedPhrases) {
                val result = calculatePhraseMatchScoreForContext(phrase, segmentedContext)
                logDebug("blockedPhrase='$phrase' similarity=${result.bestScore} threshold=$threshold shouldBlock=${result.bestScore >= threshold} detail=$result")
                if (result.bestScore >= threshold) {
                    matches.add(Pair(phrase, result.bestScore))
                }
            }

            // 按相似度降序排列
            matches.sortedByDescending { it.second }
        } catch (e: Exception) {
            Log.e(TAG, "checkBlockedPhrases failed", e)
            emptyList()
        }
    }

    /**
     * 对文本进行分词
     */
    private fun segment(text: String): List<Term> = HanLP.segment(text)

    /**
     * 判断是否应该保留该词语（过滤停用词和标点）
     */
    private fun shouldKeepTerm(term: Term): Boolean {
        val nature = term.nature.toString()
        // 保留名词、动词、形容词等实词
        return nature.startsWith("n") ||
            // 名词
            nature.startsWith("v") ||
            // 动词
            nature.startsWith("a") ||
            // 形容词
            nature.startsWith("i") ||
            // 成语
            nature.startsWith("j") ||
            // 简称
            nature.startsWith("l") // 习用语
    }

    /**
     * 提取文本摘要（用于显示）
     * @param text 输入文本
     * @param maxLength 最大长度
     * @return 摘要文本
     */
    suspend fun extractSummary(text: String, maxLength: Int = 100): String = withContext(Dispatchers.Default) {
        if (text.length <= maxLength) return@withContext text

        try {
            // 使用HanLP的自动摘要功能
            val summary = HanLP.extractSummary(text, 1)
            if (summary.isNotEmpty()) {
                summary[0].take(maxLength)
            } else {
                text.take(maxLength) + "..."
            }
        } catch (e: Exception) {
            text.take(maxLength) + "..."
        }
    }
}
