package com.github.zly2006.zhihu.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TabIndicatorScope
import androidx.compose.material3.TabRowDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.github.zly2006.zhihu.theme.LocalLiquidGlass

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptivePrimaryTabRow(
    selectedTabIndex: Int,
    modifier: Modifier = Modifier,
    containerColor: Color = TabRowDefaults.primaryContainerColor,
    contentColor: Color = TabRowDefaults.primaryContentColor,
    indicator: @Composable TabIndicatorScope.() -> Unit = {
        TabRowDefaults.PrimaryIndicator(modifier = Modifier.tabIndicatorOffset(selectedTabIndex, matchContentSize = true), width = Dp.Unspecified)
    },
    divider: @Composable () -> Unit = { HorizontalDivider() },
    tabs: @Composable () -> Unit,
) {
    val glass = LocalLiquidGlass.current
    androidx.compose.material3.PrimaryTabRow(
        selectedTabIndex = selectedTabIndex,
        modifier = if (glass) modifier.padding(horizontal = 12.dp, vertical = 6.dp).clip(RoundedCornerShape(24.dp)) else modifier,
        containerColor = if (glass) MaterialTheme.colorScheme.surfaceBright else containerColor,
        contentColor = contentColor,
        indicator = if (glass) {
            {
                Box(
                    Modifier
                        .tabIndicatorOffset(selectedTabIndex, matchContentSize = false)
                        .fillMaxHeight()
                        .padding(4.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                        .zIndex(-1f),
                )
            }
        } else {
            indicator
        },
        divider = if (glass) {
            {}
        } else {
            divider
        },
        tabs = tabs,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveSecondaryTabRow(selectedTabIndex: Int, modifier: Modifier = Modifier, tabs: @Composable () -> Unit) {
    if (LocalLiquidGlass.current) {
        AdaptivePrimaryTabRow(selectedTabIndex, modifier, tabs = tabs)
    } else {
        androidx.compose.material3.SecondaryTabRow(selectedTabIndex, modifier, tabs = tabs)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptivePrimaryScrollableTabRow(selectedTabIndex: Int, modifier: Modifier = Modifier, tabs: @Composable () -> Unit) {
    if (!LocalLiquidGlass.current) {
        androidx.compose.material3.PrimaryScrollableTabRow(selectedTabIndex, modifier, tabs = tabs)
        return
    }
    androidx.compose.material3.PrimaryScrollableTabRow(
        selectedTabIndex = selectedTabIndex,
        modifier = modifier.padding(vertical = 6.dp),
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.primary,
        edgePadding = 12.dp,
        indicator = {
            Box(
                Modifier
                    .tabIndicatorOffset(selectedTabIndex, matchContentSize = false)
                    .fillMaxHeight()
                    .padding(4.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                    .zIndex(-1f),
            )
        },
        divider = {},
        tabs = tabs,
    )
}
