/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 * Licensed under AGPL-3.0-only.
 */

package com.github.zly2006.zhihu.ui.miuix.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.viewmodel.PaginationEnvironment
import com.github.zly2006.zhihu.viewmodel.feed.SearchContentType
import com.github.zly2006.zhihu.viewmodel.feed.SearchSortOption
import com.github.zly2006.zhihu.viewmodel.feed.SearchTimeRange
import com.github.zly2006.zhihu.viewmodel.feed.SearchViewModel
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.window.WindowBottomSheet

/**
 * 搜索筛选底部弹层（排序 / 内容类型 / 时间范围），操作 [SearchViewModel] 的过滤逻辑。
 * 首页内联搜索与独立搜索页共用，避免重复。
 */
@Composable
fun MiuixSearchFilterSheet(
    show: Boolean,
    onDismiss: () -> Unit,
    viewModel: SearchViewModel,
    environment: PaginationEnvironment,
) {
    WindowBottomSheet(show = show, onDismissRequest = onDismiss, title = "筛选搜索结果") {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            FilterSection("排序", SearchSortOption.entries, { it.label }, viewModel.sortOption) {
                viewModel.updateSortOption(environment, it)
            }
            FilterSection("内容类型", SearchContentType.entries, { it.label }, viewModel.contentType) {
                viewModel.updateContentType(environment, it)
            }
            FilterSection("时间范围", SearchTimeRange.entries, { it.label }, viewModel.timeRange) {
                viewModel.updateTimeRange(environment, it)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

/** 当前筛选状态的简要文字，用于内联搜索结果上方的筛选入口。 */
fun searchFilterSummary(viewModel: SearchViewModel): String =
    listOf(viewModel.sortOption.label, viewModel.contentType.label, viewModel.timeRange.label).joinToString(" · ")

/** 筛选分组：标题 + 一组单选项，选中项右侧打勾。 */
@Composable
private fun <T> FilterSection(
    title: String,
    options: List<T>,
    label: (T) -> String,
    selected: T,
    onSelect: (T) -> Unit,
) {
    Text(
        title,
        style = MiuixTheme.textStyles.subtitle,
        color = MiuixTheme.colorScheme.onSurfaceSecondary,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
    )
    options.forEach { option ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onSelect(option) }
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label(option), color = MiuixTheme.colorScheme.onSurface)
            if (option == selected) {
                Icon(MiuixIconsEmbedded.Ok, "已选", tint = MiuixTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            }
        }
    }
}
