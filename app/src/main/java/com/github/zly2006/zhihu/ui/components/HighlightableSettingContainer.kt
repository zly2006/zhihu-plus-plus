package com.github.zly2006.zhihu.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Wraps a settings item so it can be scrolled into view and briefly highlighted
 * when navigated to via [highlightedKey].
 *
 * @param settingKey   The preference key that identifies this item.
 * @param highlightedKey The key received from the nav destination (empty means no target).
 * @param onPositioned Callback reporting this item's root-Y coordinate for scroll math.
 */
@Composable
fun HighlightableSettingContainer(
    settingKey: String,
    highlightedKey: String,
    onPositioned: (rootY: Int) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val isTarget = settingKey.isNotEmpty() && settingKey == highlightedKey
    var highlighted by remember { mutableStateOf(isTarget) }

    LaunchedEffect(isTarget) {
        if (isTarget) {
            highlighted = true
            delay(2000)
            highlighted = false
        }
    }

    val highlightAlpha by animateFloatAsState(
        targetValue = if (highlighted) 0.35f else 0f,
        animationSpec = tween(durationMillis = if (highlighted) 300 else 1200),
        label = "setting_highlight",
    )

    Column(
        modifier = modifier
            .onGloballyPositioned { coords ->
                onPositioned(coords.positionInRoot().y.toInt())
            }.background(
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = highlightAlpha),
                shape = RoundedCornerShape(8.dp),
            ),
        content = content,
    )
}

/**
 * Returns a [Modifier] that records the composable's root-Y coordinate into [positions]
 * for the given [key], to be used together with [launchScrollToSetting].
 */
fun Modifier.trackSettingPosition(key: String, positions: MutableMap<String, Int>): Modifier =
    this.onGloballyPositioned { coords ->
        positions[key] = coords.positionInRoot().y.toInt()
    }

/**
 * Suspends and scrolls [scrollState] so the item matching [setting] is visible.
 * Call from a [LaunchedEffect] in the host screen.
 *
 * @param setting          The navigation setting key (empty = no-op).
 * @param itemPositions    Map from setting key → root-Y of each item.
 * @param columnRootY      Root-Y of the scrollable Column itself.
 * @param scrollState      The scroll state of the Column.
 */
suspend fun launchScrollToSetting(
    setting: String,
    itemPositions: Map<String, Int>,
    columnRootY: Int,
    scrollState: ScrollState,
) {
    if (setting.isEmpty()) return
    delay(200) // allow layout to complete
    itemPositions[setting]?.let { itemRootY ->
        scrollState.animateScrollTo(maxOf(0, itemRootY - columnRootY))
    }
}

/** Remembers a mutable map and an integer that together track scroll-target positions. */
@Composable
fun rememberSettingPositions(): Pair<MutableMap<String, Int>, () -> (Int)> {
    val positions = remember { mutableMapOf<String, Int>() }
    var columnRootY by remember { mutableIntStateOf(0) }
    return Pair(positions) { columnRootY }
}
