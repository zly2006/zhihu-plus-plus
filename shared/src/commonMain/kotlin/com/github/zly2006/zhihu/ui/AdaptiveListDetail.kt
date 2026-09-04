/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License (version 3 only).
 */

package com.github.zly2006.zhihu.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.CommentHolder
import com.github.zly2006.zhihu.navigation.NavDestination
import com.github.zly2006.zhihu.navigation.Pin
import com.github.zly2006.zhihu.navigation.Question
import org.jetbrains.compose.resources.painterResource
import zhihu.shared.generated.resources.Res
import zhihu.shared.generated.resources.ic_launcher_foreground

internal const val LANDSCAPE_LIST_PANE_RATIO_KEY = "landscape_list_pane_ratio"
internal const val DEFAULT_LIST_PANE_RATIO = 1f / 3f
internal val MIN_LIST_PANE_WIDTH = 320.dp
internal val MIN_DETAIL_PANE_WIDTH = 480.dp
internal val LIST_DETAIL_DIVIDER_WIDTH = 16.dp

internal val LocalSelectedContentDestination = compositionLocalOf<NavDestination?> { null }

internal fun NavDestination.isReadingDestination(): Boolean =
    this is Article || this is Question || this is Pin || this is CommentHolder

internal fun normalizedListPaneWidth(
    availableWidth: Dp,
    requestedRatio: Float,
): Dp {
    val usableWidth = availableWidth - LIST_DETAIL_DIVIDER_WIDTH
    return (usableWidth * requestedRatio.coerceIn(0f, 1f)).coerceIn(
        minimumValue = MIN_LIST_PANE_WIDTH,
        maximumValue = usableWidth - MIN_DETAIL_PANE_WIDTH,
    )
}

@Composable
internal fun ListDetailDivider(
    onDrag: (Float) -> Unit,
    onAdjustBy: (Float) -> Unit,
    onDragStopped: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .width(LIST_DETAIL_DIVIDER_WIDTH)
            .fillMaxHeight()
            .testTag("list_detail_divider")
            .semantics {
                contentDescription = "调整列表和详情宽度"
                customActions = listOf(
                    CustomAccessibilityAction("增加列表宽度") {
                        onAdjustBy(0.05f)
                        onDragStopped()
                        true
                    },
                    CustomAccessibilityAction("减小列表宽度") {
                        onAdjustBy(-0.05f)
                        onDragStopped()
                        true
                    },
                )
            }.draggable(
                state = rememberDraggableState(onDelta = onDrag),
                orientation = Orientation.Horizontal,
                onDragStopped = { onDragStopped() },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .width(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant),
        )
    }
}

@Composable
internal fun EmptyDetailPane(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .testTag("detail_pane_empty"),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().padding(32.dp),
        ) {
            Image(
                painter = painterResource(Res.drawable.ic_launcher_foreground),
                contentDescription = null,
                modifier = Modifier.size(96.dp).alpha(0.72f),
            )
            Text(
                text = "选择左侧内容",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
