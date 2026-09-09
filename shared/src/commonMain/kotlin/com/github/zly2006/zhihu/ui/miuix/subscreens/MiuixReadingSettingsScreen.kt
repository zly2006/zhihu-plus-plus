/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * Licensed under AGPL-3.0-only.
 */

package com.github.zly2006.zhihu.ui.miuix.subscreens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.platform.rememberSettingBoolean
import com.github.zly2006.zhihu.platform.rememberSettingsStore
import com.github.zly2006.zhihu.reading.ReadingCommentOrder
import com.github.zly2006.zhihu.reading.ReadingPreferences
import com.github.zly2006.zhihu.reading.ReadingPublishedTimeMode
import com.github.zly2006.zhihu.reading.ReadingRelativeTimePrecision
import com.github.zly2006.zhihu.reading.ReadingTemplateField
import com.github.zly2006.zhihu.reading.buildReadingTemplatePreview
import com.github.zly2006.zhihu.reading.loadReadingPreferences
import com.github.zly2006.zhihu.reading.saveReadingPreferences
import com.github.zly2006.zhihu.theme.getMiuixAppBarColor
import com.github.zly2006.zhihu.theme.installerMiuixBlurEffect
import com.github.zly2006.zhihu.theme.rememberMiuixBlurBackdrop
import com.github.zly2006.zhihu.ui.miuix.components.MiuixExpandableArrowPreference
import com.github.zly2006.zhihu.ui.miuix.components.MiuixIconsEmbedded
import com.github.zly2006.zhihu.ui.miuix.components.MiuixSliderRow
import com.github.zly2006.zhihu.ui.subscreens.READING_SETTINGS_COMMENT_AUTHOR_TAG
import com.github.zly2006.zhihu.ui.subscreens.READING_SETTINGS_COMMENT_COUNT_TAG
import com.github.zly2006.zhihu.ui.subscreens.READING_SETTINGS_FIELD_MOVE_DOWN_TAG_PREFIX
import com.github.zly2006.zhihu.ui.subscreens.READING_SETTINGS_FIELD_MOVE_UP_TAG_PREFIX
import com.github.zly2006.zhihu.ui.subscreens.READING_SETTINGS_FIELD_TAG_PREFIX
import com.github.zly2006.zhihu.ui.subscreens.READING_SETTINGS_QUEUE_LIMIT_TAG_PREFIX
import com.github.zly2006.zhihu.ui.subscreens.READING_SETTINGS_SCROLL_TAG
import com.github.zly2006.zhihu.ui.subscreens.READING_SETTINGS_TEMPLATE_PREVIEW_TAG
import com.github.zly2006.zhihu.ui.subscreens.READING_SETTINGS_TRANSITION_TEXT_TAG
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextField
import top.yukonga.miuix.kmp.basic.TopAppBar
import top.yukonga.miuix.kmp.blur.layerBackdrop
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.preference.WindowSpinnerPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import kotlin.math.roundToInt

/**
 * 朗读与播放设置的 miuix 版本，对标 M3 [com.github.zly2006.zhihu.ui.subscreens.ReadingSettingsScreen]。
 *
 * 与 M3 版共用 [ReadingPreferences]：改动立即写回同一份偏好，播放器新建会话时读取，
 * 不会因为换皮肤产生第二套配置。M3 版用数字输入框填评论条数和队列上限，这里换成 miuix 滑块，
 * 取值范围与 `ReadingPreferences.normalized()` 的上下限一致。
 */
@Composable
fun MiuixReadingSettingsScreen() {
    val navigator = LocalNavigator.current
    val settings = rememberSettingsStore()
    val blurEnabled = rememberSettingBoolean("blurEnabled", true, settings)
    val backdrop = rememberMiuixBlurBackdrop(blurEnabled)
    val scrollBehavior = MiuixScrollBehavior()

    var preferences by remember { mutableStateOf(loadReadingPreferences(settings)) }
    var showCommentCountSlider by remember { mutableStateOf(false) }
    var showQueueLimitSlider by remember { mutableStateOf(false) }

    fun persist(next: ReadingPreferences) {
        val normalized = next.normalized()
        preferences = normalized
        saveReadingPreferences(settings, normalized)
    }

    fun moveField(field: ReadingTemplateField, offset: Int) {
        val fromIndex = preferences.fieldOrder.indexOf(field)
        val toIndex = fromIndex + offset
        if (fromIndex < 0 || toIndex !in preferences.fieldOrder.indices) return
        val reordered = preferences.fieldOrder.toMutableList()
        reordered.removeAt(fromIndex)
        reordered.add(toIndex, field)
        persist(preferences.copy(fieldOrder = reordered))
    }

    val publishedTimeEnabled = ReadingTemplateField.PublishedAt in preferences.enabledFields
    val commentsEnabled = ReadingTemplateField.Comments in preferences.enabledFields

    Scaffold(
        topBar = {
            TopAppBar(
                modifier = Modifier.installerMiuixBlurEffect(backdrop),
                color = backdrop.getMiuixAppBarColor(),
                title = "朗读与播放",
                navigationIcon = {
                    IconButton(onClick = { navigator.onNavigateBack() }) {
                        Icon(MiuixIconsEmbedded.Back, "返回", tint = MiuixTheme.colorScheme.onBackground)
                    }
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .then(if (backdrop != null) Modifier.layerBackdrop(backdrop) else Modifier)
                .imePadding()
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
                .testTag(READING_SETTINGS_SCROLL_TAG),
            contentPadding = innerPadding,
        ) {
            item { Spacer(Modifier.size(12.dp)) }
            item { SectionNote("设置会在下次开始朗读时生效；当前会话和已经生成的播放队列不会随之改变。") }

            // ── 朗读内容与顺序 ──
            item { SmallTitle(text = "朗读内容与顺序") }
            item {
                Card(Modifier.padding(horizontal = 12.dp)) {
                    preferences.fieldOrder.forEachIndexed { index, field ->
                        val enabled = field in preferences.enabledFields
                        SwitchPreference(
                            modifier = Modifier.testTag(READING_SETTINGS_FIELD_TAG_PREFIX + field.name),
                            checked = enabled,
                            onCheckedChange = { checked ->
                                // 至少保留一个字段，否则朗读内容为空。
                                if (!checked && preferences.enabledFields.size <= 1) return@SwitchPreference
                                persist(
                                    preferences.copy(
                                        enabledFields = if (checked) {
                                            preferences.enabledFields + field
                                        } else {
                                            preferences.enabledFields - field
                                        },
                                    ),
                                )
                            },
                            title = field.displayName,
                            summary = field.description,
                            endActions = {
                                IconButton(
                                    onClick = { moveField(field, -1) },
                                    enabled = index > 0,
                                    modifier = Modifier.testTag(READING_SETTINGS_FIELD_MOVE_UP_TAG_PREFIX + field.name),
                                ) {
                                    Icon(
                                        Icons.Default.KeyboardArrowUp,
                                        contentDescription = "上移${field.displayName}",
                                        tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                                    )
                                }
                                IconButton(
                                    onClick = { moveField(field, 1) },
                                    enabled = index < preferences.fieldOrder.lastIndex,
                                    modifier = Modifier.testTag(READING_SETTINGS_FIELD_MOVE_DOWN_TAG_PREFIX + field.name),
                                ) {
                                    Icon(
                                        Icons.Default.KeyboardArrowDown,
                                        contentDescription = "下移${field.displayName}",
                                        tint = MiuixTheme.colorScheme.onSurfaceVariantActions,
                                    )
                                }
                            },
                        )
                    }
                }
            }
            item { SectionNote("至少保留一个字段。可用箭头调整实际朗读顺序；缺失的标题、时间等字段会自动跳过。") }

            // ── 发布时间朗读 ──
            item { SmallTitle(text = "发布时间朗读") }
            item {
                Card(Modifier.padding(horizontal = 12.dp).padding(bottom = 12.dp)) {
                    WindowSpinnerPreference(
                        title = "时间形式",
                        summary = "绝对时间沿用当前发布时间；相对时间朗读当前时间到最后编辑时间的间隔",
                        items = ReadingPublishedTimeMode.entries.map { DropdownItem(title = it.displayName) },
                        selectedIndex = ReadingPublishedTimeMode.entries.indexOf(preferences.publishedTimeMode),
                        enabled = publishedTimeEnabled,
                        onSelectedIndexChange = {
                            persist(preferences.copy(publishedTimeMode = ReadingPublishedTimeMode.entries[it]))
                        },
                    )
                    WindowSpinnerPreference(
                        title = "相对时间精度",
                        summary = "保留到所选的最小时间单位，更细的部分会省略",
                        items = ReadingRelativeTimePrecision.entries.map { DropdownItem(title = it.displayName) },
                        selectedIndex = ReadingRelativeTimePrecision.entries.indexOf(preferences.relativeTimePrecision),
                        enabled = publishedTimeEnabled &&
                            preferences.publishedTimeMode == ReadingPublishedTimeMode.Relative,
                        onSelectedIndexChange = {
                            persist(preferences.copy(relativeTimePrecision = ReadingRelativeTimePrecision.entries[it]))
                        },
                    )
                }
            }

            // ── 评论朗读 ──
            item { SmallTitle(text = "评论朗读") }
            item {
                Card(Modifier.padding(horizontal = 12.dp)) {
                    MiuixExpandableArrowPreference(
                        title = "朗读评论数量",
                        summary = "每条内容最多朗读 ${preferences.commentCount} 条评论；0 表示不加载评论",
                        expanded = showCommentCountSlider,
                        onExpandedChange = { showCommentCountSlider = !showCommentCountSlider },
                    ) {
                        MiuixSliderRow(
                            value = preferences.commentCount.toFloat(),
                            range = 0f..50f,
                            steps = 49,
                            enabled = commentsEnabled,
                            modifier = Modifier.testTag(READING_SETTINGS_COMMENT_COUNT_TAG),
                        ) { persist(preferences.copy(commentCount = it.roundToInt())) }
                    }
                    SwitchPreference(
                        modifier = Modifier.testTag(READING_SETTINGS_COMMENT_AUTHOR_TAG),
                        checked = preferences.readCommentAuthor,
                        onCheckedChange = { persist(preferences.copy(readCommentAuthor = it)) },
                        title = "朗读评论作者",
                        summary = "关闭后，每条评论只朗读序号和正文",
                        enabled = commentsEnabled && preferences.commentCount > 0,
                    )
                    WindowSpinnerPreference(
                        title = "评论排序",
                        summary = "按当前选择的热度或发布时间顺序读取评论",
                        items = ReadingCommentOrder.entries.map { DropdownItem(title = it.displayName) },
                        selectedIndex = ReadingCommentOrder.entries.indexOf(preferences.commentOrder),
                        enabled = commentsEnabled && preferences.commentCount > 0,
                        onSelectedIndexChange = {
                            persist(preferences.copy(commentOrder = ReadingCommentOrder.entries[it]))
                        },
                    )
                }
            }
            item { SectionNote("只有启用「评论区」字段且数量大于 0 时才会请求评论数据。") }

            // ── 连续播放 ──
            item { SmallTitle(text = "连续播放") }
            item {
                Card(Modifier.padding(horizontal = 12.dp)) {
                    MiuixExpandableArrowPreference(
                        title = "单次队列上限",
                        summary = "${preferences.queueLimit} 条（含当前内容）",
                        expanded = showQueueLimitSlider,
                        onExpandedChange = { showQueueLimitSlider = !showQueueLimitSlider },
                    ) {
                        MiuixSliderRow(
                            value = preferences.queueLimit.toFloat(),
                            range = 1f..100f,
                            steps = 98,
                            modifier = Modifier.testTag(READING_SETTINGS_QUEUE_LIMIT_TAG_PREFIX + "slider"),
                        ) { persist(preferences.copy(queueLimit = it.roundToInt())) }
                    }
                }
            }
            item { SectionNote("数量包含当前内容。播放器到达队尾后会停止，不会无限加载后续页面。") }

            // ── 条目过渡 ──
            item { SmallTitle(text = "条目过渡") }
            item {
                Card(Modifier.padding(horizontal = 12.dp).padding(vertical = 12.dp)) {
                    TextField(
                        value = preferences.transitionText,
                        onValueChange = { persist(preferences.copy(transitionText = it)) },
                        label = "过渡文本",
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .testTag(READING_SETTINGS_TRANSITION_TEXT_TAG),
                    )
                }
            }
            item {
                SectionNote(
                    "仅在相邻条目之间朗读；留空即可关闭。可用占位符：{index} 下一条序号，{total} 队列总数，" +
                        "{contentType} 内容类型，{title} 标题，{author} 作者。",
                )
            }

            // ── 朗读模板预览 ──
            item { SmallTitle(text = "朗读模板预览") }
            item {
                Card(Modifier.padding(horizontal = 12.dp)) {
                    SelectionContainer {
                        Text(
                            text = buildReadingTemplatePreview(preferences),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                                .testTag(READING_SETTINGS_TEMPLATE_PREVIEW_TAG),
                            color = MiuixTheme.colorScheme.onSurface,
                            style = MiuixTheme.textStyles.body2,
                        )
                    }
                }
            }
            item { SectionNote("随上方设置实时更新；大括号表示朗读时替换的动态内容，不包含条目过渡文本。") }

            item { Spacer(Modifier.size(24.dp)) }
        }
    }
}

/** 分组说明文字。miuix 的 Card 没有 footer 槽位，说明只能作为独立一行放在卡片下方。 */
@Composable
private fun SectionNote(text: String) {
    Text(
        text = text,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        style = MiuixTheme.textStyles.footnote1,
    )
}
