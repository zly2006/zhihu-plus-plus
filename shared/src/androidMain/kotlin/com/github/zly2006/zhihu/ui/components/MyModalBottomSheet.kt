/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

// From: androidx.compose.material3:material3:1.5.0-alpha17
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE", "INVISIBLE_SETTER")

package com.github.zly2006.zhihu.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.BottomSheetImpl
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheetDialog
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.Scrim
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue.Hidden
import androidx.compose.material3.SheetWindowInsets
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.internal.PredictiveBack
import androidx.compose.material3.internal.PredictiveBackHandler
import androidx.compose.material3.internal.Strings
import androidx.compose.material3.internal.getString
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.tokens.MotionSchemeKeyTokens
import androidx.compose.material3.value
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment.Companion.TopCenter
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
@ExperimentalMaterial3Api
fun MyModalBottomSheet(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
    sheetMaxWidth: Dp = BottomSheetDefaults.SheetMaxWidth,
    sheetGesturesEnabled: Boolean = true,
    shape: Shape = BottomSheetDefaults.ExpandedShape,
    containerColor: Color = BottomSheetDefaults.ContainerColor,
    contentColor: Color = contentColorFor(containerColor),
    tonalElevation: Dp = 0.dp,
    scrimColor: Color = BottomSheetDefaults.ScrimColor,
    dragHandle: @Composable (() -> Unit)? = { BottomSheetDefaults.DragHandle() },
    contentWindowInsets: @Composable () -> WindowInsets = { BottomSheetDefaults.modalWindowInsets },
    properties: ModalBottomSheetProperties = ModalBottomSheetProperties(),
    content: @Composable ColumnScope.() -> Unit,
) {
    val scope = rememberCoroutineScope()
    val animateToDismiss: () -> Unit = {
        // hack here to fix: androidx/compose/material3/SheetState.confirmValueChange is invisible
        if (sheetState.anchoredDraggableState.confirmValueChange.invoke(Hidden)) {
            scope
                .launch { sheetState.hide() }
                .invokeOnCompletion {
                    if (!sheetState.isVisible) {
                        onDismissRequest()
                    }
                }
        }
    }

    ModalBottomSheetDialog(
        properties = properties,
        contentColor = contentColor,
        onDismissRequest = {
            // Hack here by zly2006: press back button only once to dismiss.
            scope.launch { sheetState.hide() }.invokeOnCompletion { onDismissRequest() }
        },
    ) {
        Box(modifier = Modifier.fillMaxSize().imePadding().semantics { isTraversalGroup = true }) {
            val sheetWindowInsets = remember(sheetState) { SheetWindowInsets(sheetState) }
            val isScrimVisible: Boolean by remember {
                derivedStateOf { sheetState.targetValue != Hidden }
            }
            val scrimAlpha by
                animateFloatAsState(
                    targetValue = if (isScrimVisible) 1f else 0f,
                    animationSpec = MotionSchemeKeyTokens.DefaultEffects.value(),
                    label = "ScrimAlphaAnimation",
                )
            Scrim(
                contentDescription = getString(Strings.CloseSheet),
                onClick = if (properties.shouldDismissOnClickOutside) animateToDismiss else null,
                alpha = { scrimAlpha },
                color = scrimColor,
            )
            BottomSheet(
                modifier = modifier.align(TopCenter).consumeWindowInsets(sheetWindowInsets),
                state = sheetState,
                onDismissRequest = onDismissRequest,
                maxWidth = sheetMaxWidth,
                gesturesEnabled = sheetGesturesEnabled,
                backHandlerEnabled = properties.shouldDismissOnBackPress,
                shape = shape,
                containerColor = containerColor,
                contentColor = contentColor,
                tonalElevation = tonalElevation,
                dragHandle = dragHandle,
                contentWindowInsets = contentWindowInsets,
                content = content,
            )
        }
    }
    if (sheetState.hasExpandedState) {
        LaunchedEffect(sheetState) { sheetState.show() }
    }
}

@Composable
@ExperimentalMaterial3Api
fun BottomSheet(
    modifier: Modifier = Modifier,
    state: SheetState = rememberModalBottomSheetState(),
    onDismissRequest: () -> Unit = {},
    maxWidth: Dp = BottomSheetDefaults.SheetMaxWidth,
    gesturesEnabled: Boolean = true,
    backHandlerEnabled: Boolean = true,
    dragHandle: @Composable (() -> Unit)? = { BottomSheetDefaults.DragHandle() },
    contentWindowInsets: @Composable () -> WindowInsets = {
        BottomSheetDefaults.standardWindowInsets
    },
    shape: Shape = BottomSheetDefaults.ExpandedShape,
    containerColor: Color = BottomSheetDefaults.ContainerColor,
    contentColor: Color = contentColorFor(containerColor),
    tonalElevation: Dp = BottomSheetDefaults.Elevation,
    shadowElevation: Dp = 0.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val showMotion = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    val hideMotion = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
    val anchoredDraggableMotion = MaterialTheme.motionScheme.defaultSpatialSpec<Float>()
    SideEffect {
        state.showMotionSpec = showMotion
        state.hideMotionSpec = hideMotion
        state.anchoredDraggableMotionSpec = anchoredDraggableMotion
    }

    val predictiveBackProgress = remember { Animatable(initialValue = 0f) }
    val scope = rememberCoroutineScope()
    val settleToDismiss: () -> Unit = {
        // Hack here by zly2006: press back button only once to dismiss.
        scope
            .launch { state.hide() }
            .invokeOnCompletion { if (!state.isVisible) onDismissRequest() }
    }

    PredictiveBackHandler(enabled = backHandlerEnabled && state.isVisible) { progress ->
        try {
            progress.collect { backEvent ->
                predictiveBackProgress.snapTo(PredictiveBack.transform(backEvent.progress))
            }
            settleToDismiss()
        } catch (_: CancellationException) {
            predictiveBackProgress.animateTo(0f)
        }
    }
    BottomSheetImpl(
        predictiveBackProgress = predictiveBackProgress.value,
        modifier = modifier,
        state = state,
        onDismissRequest = onDismissRequest,
        maxWidth = maxWidth,
        gesturesEnabled = gesturesEnabled,
        shape = shape,
        containerColor = containerColor,
        contentColor = contentColor,
        tonalElevation = tonalElevation,
        shadowElevation = shadowElevation,
        dragHandle = dragHandle,
        contentWindowInsets = contentWindowInsets,
        content = content,
    )
}
