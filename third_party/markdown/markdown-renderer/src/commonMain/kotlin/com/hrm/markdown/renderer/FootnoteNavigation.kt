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
    private val referenceRequesters = mutableMapOf<String, MutableList<BringIntoViewRequester>>()
    private val returnPositions = mutableStateMapOf<String, FootnoteReturnPosition>()
    private val requestedDefinitions = mutableStateMapOf<String, Unit>()
    private val requestedReferences = mutableStateMapOf<String, Unit>()

    fun registerDefinition(label: String, requester: BringIntoViewRequester) {
        definitionRequesters[label] = requester
    }

    fun unregisterDefinition(label: String, requester: BringIntoViewRequester) {
        if (definitionRequesters[label] === requester) {
            definitionRequesters.remove(label)
        }
    }

    fun hasDefinition(label: String): Boolean = definitionRequesters.containsKey(label)

    fun requestDefinition(label: String) {
        requestedDefinitions[label] = Unit
    }

    fun clearDefinitionRequest(label: String) {
        requestedDefinitions.remove(label)
    }

    fun isDefinitionRequested(label: String): Boolean = requestedDefinitions.containsKey(label)

    fun requestReference(label: String) {
        requestedReferences[label] = Unit
    }

    fun clearReferenceRequest(label: String) {
        requestedReferences.remove(label)
    }

    fun isReferenceRequested(label: String): Boolean = requestedReferences.containsKey(label)

    fun registerReference(label: String, requester: BringIntoViewRequester) {
        referenceRequesters.getOrPut(label) { mutableListOf() }.add(requester)
    }

    fun unregisterReference(label: String, requester: BringIntoViewRequester) {
        val requesters = referenceRequesters[label] ?: return
        requesters.removeAll { it === requester }
        if (requesters.isEmpty()) {
            referenceRequesters.remove(label)
        }
    }

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

    suspend fun bringReferenceIntoView(label: String): Boolean {
        val requester = referenceRequesters[label]?.firstOrNull() ?: return false
        requester.bringIntoView()
        return true
    }
}
