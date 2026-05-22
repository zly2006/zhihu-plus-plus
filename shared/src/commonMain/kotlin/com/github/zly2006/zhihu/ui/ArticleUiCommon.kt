package com.github.zly2006.zhihu.ui

import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.theme.ThemeManager
import com.materialkolor.ktx.harmonize
import kotlin.math.abs

private const val SCROLL_THRESHOLD = 10
val ScrollThresholdDp = SCROLL_THRESHOLD.dp

@Composable
fun rememberBottomBarAvoidingBringIntoViewSpec(
    obscuredBottomPx: Float,
): BringIntoViewSpec {
    val density = LocalDensity.current
    return remember(obscuredBottomPx) {
        object : BringIntoViewSpec {
            override fun calculateScrollDistance(
                offset: Float,
                size: Float,
                containerSize: Float,
            ): Float {
                val effectiveContainerSize = (containerSize - obscuredBottomPx).coerceAtLeast(0f)
                val effectiveContainerTop = density.run { 110.dp.toPx() }
                val trailingEdge = offset + size
                return when {
                    offset >= effectiveContainerTop && trailingEdge <= effectiveContainerSize -> 0f
                    offset < effectiveContainerTop && trailingEdge > effectiveContainerSize -> 0f
                    abs(offset) < abs(trailingEdge + effectiveContainerTop - effectiveContainerSize) -> offset - effectiveContainerTop
                    else -> trailingEdge + effectiveContainerTop - effectiveContainerSize
                }
            }
        }
    }
}

private val VoteUpNeutralContent = Color(0xFF3671EE)
private val VoteUpNeutralContentDark = Color(0xFF628DF7)

@Composable
fun voteUpNeutralContent() = if (ThemeManager.isDarkTheme()) VoteUpNeutralContentDark else VoteUpNeutralContent

@Composable
fun voteUpNeutralContentDuo3() = if (ThemeManager.isDarkTheme()) {
    VoteUpNeutralContentDark.harmonize(MaterialTheme.colorScheme.primary)
} else {
    VoteUpNeutralContent.harmonize(MaterialTheme.colorScheme.primary)
}

@Composable
fun voteUpActiveButtonColors() = ButtonDefaults.buttonColors(
    containerColor = voteUpNeutralContent(),
    contentColor = Color.White,
)

@Composable
fun voteUpNeutralButtonColors() = ButtonDefaults.buttonColors(
    containerColor = MaterialTheme.colorScheme.secondaryContainer,
    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArticleTopAppBar(
    title: @Composable (expanded: Boolean) -> Unit,
    subtitle: (@Composable (expanded: Boolean) -> Unit)?,
    navigationIcon: @Composable () -> Unit,
    actions: @Composable RowScope.() -> Unit,
    titleHorizontalAlignment: Alignment.Horizontal = Alignment.Start,
    collapsedHeight: Dp = TopAppBarDefaults.TopAppBarExpandedHeight,
    expandedHeight: Dp = TopAppBarDefaults.TopAppBarExpandedHeight,
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors(),
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    TopAppBar(
        title = {
            Column(
                horizontalAlignment = titleHorizontalAlignment,
            ) {
                title(false)
                subtitle?.invoke(false)
            }
        },
        navigationIcon = navigationIcon,
        actions = actions,
        expandedHeight = maxOf(collapsedHeight, expandedHeight),
        windowInsets = windowInsets,
        colors = colors,
        scrollBehavior = scrollBehavior,
    )
}
