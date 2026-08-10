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

private class PersistentSelectionRegistrar(
    private val androidxRegistrar: SelectionRegistrar,
) : SelectionRegistrar {
    private val selectables = mutableMapOf<Long, PersistentSelectable>()

    override val subselections: LongObjectMap<Selection>
        get() = androidxRegistrar.subselections

    override fun subscribe(selectable: Selectable): Selectable {
        return selectables[selectable.selectableId]?.also { it.delegate = selectable }
            ?: PersistentSelectable(selectable).also {
                selectables[selectable.selectableId] = it
                androidxRegistrar.subscribe(it)
            }
    }

    override fun unsubscribe(selectable: Selectable) = Unit

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

private class PersistentSelectable(
    var delegate: Selectable,
) : Selectable {
    override val selectableId: Long
        get() = delegate.selectableId

    override fun appendSelectableInfoToBuilder(builder: SelectionLayoutBuilder) {
        delegate.appendSelectableInfoToBuilder(builder)
    }

    override fun getSelectAllSelection(): Selection? = delegate.getSelectAllSelection()

    override fun getHandlePosition(selection: Selection, isStartHandle: Boolean): Offset =
        delegate.getHandlePosition(selection, isStartHandle)

    override fun getLayoutCoordinates(): LayoutCoordinates? =
        delegate.getLayoutCoordinates()?.takeIf { it.isAttached }

    override fun textLayoutResult(): TextLayoutResult? = delegate.textLayoutResult()

    override fun getText(): AnnotatedString = delegate.getText()

    override fun getBoundingBox(offset: Int): Rect = delegate.getBoundingBox(offset)

    override fun getLineLeft(offset: Int): Float = delegate.getLineLeft(offset)

    override fun getLineRight(offset: Int): Float = delegate.getLineRight(offset)

    override fun getCenterYForOffset(offset: Int): Float = delegate.getCenterYForOffset(offset)

    override fun getRangeOfLineContaining(offset: Int): TextRange =
        delegate.getRangeOfLineContaining(offset)

    override fun getLastVisibleOffset(): Int = delegate.getLastVisibleOffset()

    override fun getLineHeight(offset: Int): Float = delegate.getLineHeight(offset)
}
