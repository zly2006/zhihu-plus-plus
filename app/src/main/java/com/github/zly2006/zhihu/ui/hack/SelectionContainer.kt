@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")

package com.github.zly2006.zhihu.ui.hack

import androidx.compose.foundation.ComposeFoundationFlags
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.internal.isWriteSupported
import androidx.compose.foundation.internal.toClipEntry
import androidx.compose.foundation.text.ContextMenuArea
import androidx.compose.foundation.text.detectDownAndDragGesturesWithObserver
import androidx.compose.foundation.text.rememberClipboardEventsHandler
import androidx.compose.foundation.text.selection.LocalSelectionRegistrar
import androidx.compose.foundation.text.selection.SelectedTextType
import androidx.compose.foundation.text.selection.Selection
import androidx.compose.foundation.text.selection.SelectionHandle
import androidx.compose.foundation.text.selection.SelectionManager
import androidx.compose.foundation.text.selection.SelectionRegistrarImpl
import androidx.compose.foundation.text.selection.SimpleLayout
import androidx.compose.foundation.text.selection.rememberPlatformSelectionBehaviors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.util.fastForEach
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

// hack
internal val LocalSelectionManager = compositionLocalOf<SelectionManager> {
    error("LocalSelectionManager not provided, SelectionContainer not hacked?")
}

/**
 * Selection Composable.
 *
 * The selection composable wraps composables and let them to be selectable. It paints the selection
 * area with start and end handles.
 */
@Suppress("ComposableLambdaParameterNaming")
@Composable
internal fun SelectionContainer(
    /** A [Modifier] for SelectionContainer. */
    modifier: Modifier = Modifier,
    /** Current Selection status. */
    selection: Selection?,
    /** A function containing customized behaviour when selection changes. */
    onSelectionChange: (Selection?) -> Unit,
    registrarImpl: SelectionRegistrarImpl,
    manager: SelectionManager,
    children: @Composable () -> Unit,
) {
    val clipboard = LocalClipboard.current
    val coroutineScope = rememberCoroutineScope()
    manager.hapticFeedBack = LocalHapticFeedback.current
    manager.onCopyHandler =
        remember(coroutineScope, clipboard) {
            if (clipboard.isWriteSupported()) {
                { textToCopy ->
                    coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
                        clipboard.setClipEntry(textToCopy.toClipEntry())
                    }
                }
            } else null
        }
    manager.textToolbar = LocalTextToolbar.current
    manager.onSelectionChange = onSelectionChange
    manager.selection = selection
    @OptIn(ExperimentalFoundationApi::class)
    if (ComposeFoundationFlags.isSmartSelectionEnabled) {
        manager.platformSelectionBehaviors =
            rememberPlatformSelectionBehaviors(SelectedTextType.StaticText, null)
        manager.coroutineScope = coroutineScope
    }

    rememberClipboardEventsHandler(
        onCopy = { manager.getSelectedText() },
        isEnabled = manager.isNonEmptySelection(),
    )

    /*
     * Need a layout for selection gestures that span multiple text children.
     *
     * b/372053402: SimpleLayout must be the top layout in this composable because
     *     the modifier argument must be applied to the top layout in case it contains
     *     something like `Modifier.weight`.
     */
    SimpleLayout(modifier = modifier.then(manager.modifier)) {
        ContextMenuArea(manager) {
            CompositionLocalProvider(LocalSelectionRegistrar provides registrarImpl) {
                children()
                if (
                    manager.isInTouchMode &&
                    manager.hasFocus &&
                    !manager.isTriviallyCollapsedSelection()
                ) {
                    manager.selection?.let {
                        listOf(true, false).fastForEach { isStartHandle ->
                            val observer =
                                remember(isStartHandle) {
                                    manager.handleDragObserver(isStartHandle)
                                }

                            val positionProvider: () -> Offset =
                                remember(isStartHandle) {
                                    if (isStartHandle) {
                                        { manager.startHandlePosition ?: Offset.Unspecified }
                                    } else {
                                        { manager.endHandlePosition ?: Offset.Unspecified }
                                    }
                                }

                            val direction =
                                if (isStartHandle) {
                                    it.start.direction
                                } else {
                                    it.end.direction
                                }

                            val lineHeight =
                                if (isStartHandle) {
                                    manager.startHandleLineHeight
                                } else {
                                    manager.endHandleLineHeight
                                }
                            SelectionHandle(
                                offsetProvider = positionProvider,
                                isStartHandle = isStartHandle,
                                direction = direction,
                                handlesCrossed = it.handlesCrossed,
                                lineHeight = lineHeight,
                                modifier =
                                    Modifier.pointerInput(observer) {
                                        detectDownAndDragGesturesWithObserver(observer)
                                    },
                            )
                        }
                    }
                }
            }
        }
    }

    DisposableEffect(manager) {
        onDispose {
            manager.onRelease()
            manager.hasFocus = false
        }
    }
}
