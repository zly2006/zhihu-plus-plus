package com.hrm.markdown.renderer

import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateMapOf

internal sealed interface FootnoteReturnPosition {
    data class Scroll(val value: Int) : FootnoteReturnPosition
    data class LazyList(val index: Int, val offset: Int) : FootnoteReturnPosition
}

@Stable
internal class FootnoteNavigationState {
    private val definitionRequesters = mutableMapOf<String, BringIntoViewRequester>()
    private val returnPositions = mutableStateMapOf<String, FootnoteReturnPosition>()

    fun registerDefinition(label: String, requester: BringIntoViewRequester) {
        definitionRequesters[label] = requester
    }

    fun unregisterDefinition(label: String, requester: BringIntoViewRequester) {
        if (definitionRequesters[label] === requester) {
            definitionRequesters.remove(label)
        }
    }

    fun hasDefinition(label: String): Boolean = definitionRequesters.containsKey(label)

    fun rememberReturnPosition(label: String, scrollValue: Int) {
        returnPositions[label] = FootnoteReturnPosition.Scroll(scrollValue)
    }

    fun rememberLazyListPosition(label: String, index: Int, offset: Int) {
        returnPositions[label] = FootnoteReturnPosition.LazyList(index, offset)
    }

    fun hasReturnPosition(label: String): Boolean = returnPositions.containsKey(label)

    fun getReturnPosition(label: String): FootnoteReturnPosition? = returnPositions[label]

    suspend fun bringDefinitionIntoView(label: String): Boolean {
        val requester = definitionRequesters[label] ?: return false
        requester.bringIntoView()
        return true
    }
}
