@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hrm.markdown.renderer.selection

import androidx.collection.LongObjectMap
import androidx.compose.foundation.text.selection.LocalSelectionRegistrar
import androidx.compose.foundation.text.selection.Selectable
import androidx.compose.foundation.text.selection.Selection
import androidx.compose.foundation.text.selection.SelectionAdjustment
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.text.selection.SelectionLayoutBuilder
import androidx.compose.foundation.text.selection.SelectionRegistrar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange

/**
 * AndroidX [SelectionContainer] whose selectable registrations outlive their layouts.
 *
 * AndroidX normally unregisters a text node when its composable leaves the tree. Deferred Markdown
 * intentionally removes only the expensive view while scrolling, so unregistering there must not
 * erase the document selection. This container registers one stable proxy per AndroidX selectable
 * id and reconnects that proxy to the real text node when the view is composed again. Text, layout,
 * selection calculation, drawing, handles, toolbar and clipboard behavior all remain AndroidX's;
 * there is no parallel text or coordinate layer.
 */
@Composable
fun PersistentSelectionContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    SelectionContainer(modifier) {
        val androidxRegistrar = checkNotNull(LocalSelectionRegistrar.current)
        val persistentRegistrar = remember(androidxRegistrar) {
            PersistentSelectionRegistrar(androidxRegistrar)
        }
        CompositionLocalProvider(
            LocalSelectionRegistrar provides persistentRegistrar,
            content = content,
        )
    }
}

/**
 * Gives every selectable in one deferred Markdown block a stable slot across disposal.
 *
 * AndroidX only saves a selectable id while that selectable is part of the active selection. A
 * completely unrelated paragraph therefore receives a new id after an off-screen round trip. The
 * outer registrar must not keep both generations, otherwise full-document selection copies the
 * paragraph repeatedly. A block scope reuses ids by composition order while reserving only ids
 * that were selected when the block was disposed and will therefore be restored by AndroidX.
 */
@Composable
internal fun PersistentSelectionScope(
    scopeKey: Any,
    fallbackCoordinates: LayoutCoordinates? = null,
    content: @Composable () -> Unit,
) {
    val registrar = LocalSelectionRegistrar.current
    val persistentRegistrar = when (registrar) {
        is PersistentSelectionRegistrar -> registrar
        is ScopedPersistentSelectionRegistrar -> registrar.persistentRegistrar
        else -> null
    }
    if (persistentRegistrar == null) {
        content()
        return
    }

    val scopedRegistrar = remember(persistentRegistrar, scopeKey) {
        persistentRegistrar.scoped(scopeKey).also { it.beginComposition() }
    }
    scopedRegistrar.fallbackCoordinates = fallbackCoordinates
    CompositionLocalProvider(
        LocalSelectionRegistrar provides scopedRegistrar,
        content = content,
    )
}

private class PersistentSelectionRegistrar(
    private val androidxRegistrar: SelectionRegistrar,
) : SelectionRegistrar {
    private val selectables = mutableMapOf<Long, PersistentSelectable>()
    private val scopes = mutableMapOf<Any, ScopedPersistentSelectionRegistrar>()

    fun scoped(scopeKey: Any): ScopedPersistentSelectionRegistrar =
        scopes.getOrPut(scopeKey) { ScopedPersistentSelectionRegistrar(this) }

    override val subselections: LongObjectMap<Selection>
        get() = androidxRegistrar.subselections

    override fun subscribe(selectable: Selectable): Selectable =
        subscribe(selectable) { null }

    fun subscribe(
        selectable: Selectable,
        fallbackCoordinates: () -> LayoutCoordinates?,
    ): Selectable {
        val persistentSelectable = selectables[selectable.selectableId]
        if (persistentSelectable != null) {
            persistentSelectable.attach(selectable)
            persistentSelectable.setFallbackCoordinates(fallbackCoordinates)
        } else {
            PersistentSelectable(selectable, fallbackCoordinates).also {
                selectables[selectable.selectableId] = it
                androidxRegistrar.subscribe(it)
            }
        }
        // SelectionController keeps the value returned here for drawing and unsubscribe. Returning
        // the persistent proxy would make the real text node retain another node's lifecycle.
        return selectable
    }

    override fun unsubscribe(selectable: Selectable) {
        detach(selectable)
    }

    fun detach(selectable: Selectable): Boolean =
        selectables[selectable.selectableId]?.detach(selectable) == true

    override fun nextSelectableId(): Long = androidxRegistrar.nextSelectableId()

    override fun notifyPositionChange(selectableId: Long) {
        androidxRegistrar.notifyPositionChange(selectableId)
    }

    override fun notifySelectionUpdateStart(
        layoutCoordinates: LayoutCoordinates,
        startPosition: Offset,
        adjustment: SelectionAdjustment,
        isInTouchMode: Boolean,
    ) {
        androidxRegistrar.notifySelectionUpdateStart(
            layoutCoordinates,
            startPosition,
            adjustment,
            isInTouchMode,
        )
    }

    override fun notifySelectionUpdateSelectAll(selectableId: Long, isInTouchMode: Boolean) {
        androidxRegistrar.notifySelectionUpdateSelectAll(selectableId, isInTouchMode)
    }

    override fun notifySelectionUpdate(
        layoutCoordinates: LayoutCoordinates,
        newPosition: Offset,
        previousPosition: Offset,
        isStartHandle: Boolean,
        adjustment: SelectionAdjustment,
        isInTouchMode: Boolean,
    ): Boolean = androidxRegistrar.notifySelectionUpdate(
        layoutCoordinates,
        newPosition,
        previousPosition,
        isStartHandle,
        adjustment,
        isInTouchMode,
    )

    override fun notifySelectionUpdateEnd() {
        androidxRegistrar.notifySelectionUpdateEnd()
    }

    override fun notifySelectableChange(selectableId: Long) {
        androidxRegistrar.notifySelectableChange(selectableId)
    }
}

private class ScopedPersistentSelectionRegistrar(
    val persistentRegistrar: PersistentSelectionRegistrar,
) : SelectionRegistrar by persistentRegistrar {
    private val selectableIds = mutableListOf<Long>()
    private val selectedIdsAtDisposal = mutableSetOf<Long>()
    private var idsRestoredThisComposition = emptySet<Long>()
    private var allocationIndex = 0
    var fallbackCoordinates: LayoutCoordinates? = null

    fun beginComposition() {
        allocationIndex = 0
        idsRestoredThisComposition = selectedIdsAtDisposal.toSet()
        selectedIdsAtDisposal.clear()
    }

    override fun nextSelectableId(): Long {
        while (allocationIndex < selectableIds.size) {
            val selectableId = selectableIds[allocationIndex++]
            if (selectableId !in idsRestoredThisComposition) return selectableId
        }
        return persistentRegistrar.nextSelectableId().also { selectableId ->
            selectableIds += selectableId
            allocationIndex = selectableIds.size
        }
    }

    override fun subscribe(selectable: Selectable): Selectable =
        persistentRegistrar.subscribe(selectable) { fallbackCoordinates }

    override fun unsubscribe(selectable: Selectable) {
        val wasSelected = persistentRegistrar.subselections.containsKey(selectable.selectableId)
        if (!persistentRegistrar.detach(selectable)) return
        if (wasSelected) {
            selectedIdsAtDisposal += selectable.selectableId
        } else {
            selectedIdsAtDisposal -= selectable.selectableId
        }
    }

}

private class PersistentSelectable(
    delegate: Selectable,
    fallbackCoordinates: () -> LayoutCoordinates?,
) : Selectable {
    override val selectableId = delegate.selectableId
    private var delegate: Selectable? = delegate
    private var fallbackCoordinates = fallbackCoordinates
    private var text = delegate.getText()
    private var selectAllSelection = delegate.getSelectAllSelection()

    fun attach(selectable: Selectable) {
        delegate = selectable
        text = selectable.getText()
        selectAllSelection = selectable.getSelectAllSelection()
    }

    fun setFallbackCoordinates(fallbackCoordinates: () -> LayoutCoordinates?) {
        this.fallbackCoordinates = fallbackCoordinates
    }

    fun detach(selectable: Selectable): Boolean {
        if (delegate !== selectable) return false
        text = selectable.getText()
        selectAllSelection = selectable.getSelectAllSelection()
        delegate = null
        return true
    }

    override fun appendSelectableInfoToBuilder(builder: SelectionLayoutBuilder) {
        delegate?.appendSelectableInfoToBuilder(builder)
    }

    override fun getSelectAllSelection(): Selection? =
        delegate?.getSelectAllSelection() ?: selectAllSelection

    override fun getHandlePosition(selection: Selection, isStartHandle: Boolean): Offset =
        delegate?.getHandlePosition(selection, isStartHandle) ?: Offset.Unspecified

    override fun getLayoutCoordinates(): LayoutCoordinates? =
        delegate?.getLayoutCoordinates()?.takeIf { it.isAttached }
            ?: fallbackCoordinates().takeIf { it?.isAttached == true }

    override fun textLayoutResult(): TextLayoutResult? = delegate?.textLayoutResult()

    override fun getText(): AnnotatedString = delegate?.getText() ?: text

    override fun getBoundingBox(offset: Int): Rect = delegate?.getBoundingBox(offset) ?: Rect.Zero

    override fun getLineLeft(offset: Int): Float = delegate?.getLineLeft(offset) ?: -1f

    override fun getLineRight(offset: Int): Float = delegate?.getLineRight(offset) ?: -1f

    override fun getCenterYForOffset(offset: Int): Float =
        delegate?.getCenterYForOffset(offset) ?: -1f

    override fun getRangeOfLineContaining(offset: Int): TextRange =
        delegate?.getRangeOfLineContaining(offset) ?: TextRange.Zero

    override fun getLastVisibleOffset(): Int = delegate?.getLastVisibleOffset() ?: text.length

    override fun getLineHeight(offset: Int): Float = delegate?.getLineHeight(offset) ?: 0f
}
