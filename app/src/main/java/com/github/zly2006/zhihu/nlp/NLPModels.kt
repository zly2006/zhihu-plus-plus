package com.github.zly2006.zhihu.nlp

/**
 * 关键词及其权重信息
 */
data class KeywordWithWeight(
    val keyword: String,
    val weight: Double,
)

// 汇总调试信息，便于断点查看
data class NlpDebugTrace(
    val normalizedText: String,
    val candidateKeywords: List<String> = emptyList(),
    val segments: List<SegmentDebugInfo> = emptyList(),
    val mmrIterations: MutableList<KeywordSelectionDebug> = mutableListOf(),
    val selectedKeywords: MutableList<KeywordWithWeight> = mutableListOf(),
)

data class SegmentDebugInfo(
    val index: Int,
    val text: String,
    val length: Int,
    val embeddingDim: Int?,
    val embeddingPreview: String,
)

data class KeywordSelectionDebug(
    val iteration: Int,
    val candidate: String,
    val relevance: Double,
    val redundancy: Double,
    val score: Double,
)

data class SegmentMatchDebug(
    val index: Int,
    val segmentTextPreview: String,
    val segmentLength: Int,
    val embeddingDim: Int?,
    val score: Double?,
)

data class PhraseMatchResult(
    val phrase: String,
    val normalizedPhrase: String,
    val keywords: List<String>,
    val phraseEmbeddingDim: Int?,
    val bestScore: Double,
    val segmentDebug: List<SegmentMatchDebug>,
)

sealed interface ModelState {
    data object Uninitialized : ModelState

    data object Loading : ModelState

    data object Ready : ModelState

    data class Error(
        val message: String,
    ) : ModelState
}
