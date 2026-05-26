package com.github.zly2006.zhihu.viewmodel.filter

internal val desktopKeywordSemanticMatcher = KeywordSemanticMatcher { text, blockedPhrases, threshold ->
    val normalizedText = text.lowercase()
    val textTokens = extractDesktopSemanticTokens(normalizedText).toSet()
    blockedPhrases.mapNotNull { phrase ->
        val normalizedPhrase = phrase.lowercase().trim()
        val similarity = when {
            normalizedPhrase.isBlank() -> 0.0
            normalizedText.contains(normalizedPhrase) -> 1.0
            else -> {
                val phraseTokens = extractDesktopSemanticTokens(normalizedPhrase)
                if (phraseTokens.isEmpty()) {
                    0.0
                } else {
                    phraseTokens.count { it in textTokens }.toDouble() / phraseTokens.size.toDouble()
                }
            }
        }
        if (similarity >= threshold) phrase to similarity else null
    }
}

private fun extractDesktopSemanticTokens(text: String): List<String> =
    Regex("[\\p{L}\\p{N}_\\u4e00-\\u9fff]{2,}")
        .findAll(text)
        .map { it.value.trim() }
        .filter { it.length >= 2 }
        .toList()
