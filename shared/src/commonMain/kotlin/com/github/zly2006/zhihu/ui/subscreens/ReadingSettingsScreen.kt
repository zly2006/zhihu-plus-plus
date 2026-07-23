/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 */

package com.github.zly2006.zhihu.ui.subscreens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.reading.ReadingCommentOrder
import com.github.zly2006.zhihu.reading.ReadingPreferences
import com.github.zly2006.zhihu.reading.ReadingPublishedTimeMode
import com.github.zly2006.zhihu.reading.ReadingRelativeTimePrecision
import com.github.zly2006.zhihu.reading.ReadingTemplateField
import com.github.zly2006.zhihu.reading.buildReadingTemplatePreview
import com.github.zly2006.zhihu.reading.loadReadingPreferences
import com.github.zly2006.zhihu.reading.saveReadingPreferences
import com.github.zly2006.zhihu.shared.platform.rememberSettingsStore
import com.github.zly2006.zhihu.ui.components.SettingItem
import com.github.zly2006.zhihu.ui.components.SettingItemGroup
import com.github.zly2006.zhihu.ui.components.SettingItemWithSwitch
import com.github.zly2006.zhihu.ui.components.SwitchWithIcon

const val READING_SETTINGS_SCROLL_TAG = "readingSettings.scroll"
const val READING_SETTINGS_FIELD_TAG_PREFIX = "readingSettings.field."
const val READING_SETTINGS_FIELD_MOVE_UP_TAG_PREFIX = "readingSettings.field.moveUp."
const val READING_SETTINGS_FIELD_MOVE_DOWN_TAG_PREFIX = "readingSettings.field.moveDown."
const val READING_SETTINGS_PUBLISHED_TIME_MODE_TAG_PREFIX = "readingSettings.publishedTimeMode."
const val READING_SETTINGS_RELATIVE_TIME_PRECISION_TAG_PREFIX = "readingSettings.relativeTimePrecision."
const val READING_SETTINGS_COMMENT_COUNT_TAG = "readingSettings.commentCount"
const val READING_SETTINGS_COMMENT_ORDER_TAG_PREFIX = "readingSettings.commentOrder."
const val READING_SETTINGS_COMMENT_AUTHOR_TAG = "readingSettings.commentAuthor"
const val READING_SETTINGS_QUEUE_LIMIT_TAG_PREFIX = "readingSettings.queueLimit."
const val READING_SETTINGS_CUSTOM_QUEUE_LIMIT_TAG = "readingSettings.queueLimit.custom"
const val READING_SETTINGS_TRANSITION_TEXT_TAG = "readingSettings.transitionText"
const val READING_SETTINGS_TEMPLATE_PREVIEW_TAG = "readingSettings.templatePreview"

private val queueLimitPresets = listOf(5, 10, 20)

/**
 * 连续朗读的内容模板、评论、有限队列和条目过渡设置。
 *
 * 设置修改后立即写入 [com.github.zly2006.zhihu.reading.READING_PREFERENCES_KEY]，播放器创建新会话时读取同一份
 * [ReadingPreferences]，避免 UI 与后台播放各自维护一套配置。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingSettingsScreen() {
    val navigator = LocalNavigator.current
    val settings = rememberSettingsStore()
    var preferences by remember { mutableStateOf(loadReadingPreferences(settings)) }
    var commentCountText by remember { mutableStateOf(preferences.commentCount.toString()) }
    var customQueueLimitText by remember {
        mutableStateOf(
            preferences.queueLimit
                .takeUnless(queueLimitPresets::contains)
                ?.toString()
                .orEmpty(),
        )
    }

    fun persist(next: ReadingPreferences) {
        val normalized = next.normalized()
        preferences = normalized
        saveReadingPreferences(settings, normalized)
    }

    fun moveField(
        field: ReadingTemplateField,
        offset: Int,
    ) {
        val fromIndex = preferences.fieldOrder.indexOf(field)
        val toIndex = fromIndex + offset
        if (fromIndex < 0 || toIndex !in preferences.fieldOrder.indices) return
        val reordered = preferences.fieldOrder.toMutableList()
        reordered.removeAt(fromIndex)
        reordered.add(toIndex, field)
        persist(preferences.copy(fieldOrder = reordered))
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        topBar = {
            LargeTopAppBar(
                title = { Text("朗读与播放") },
                navigationIcon = {
                    IconButton(
                        onClick = navigator.onNavigateBack,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors().copy(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .testTag(READING_SETTINGS_SCROLL_TAG)
                .padding(vertical = 16.dp),
        ) {
            Text(
                text = "设置会在下次开始朗读时生效；当前会话和已经生成的播放队列不会随之改变。",
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            SettingItemGroup(
                title = "朗读内容与顺序",
                footer = { Text("至少保留一个字段。可使用箭头调整实际朗读顺序；缺失的标题、时间等字段会自动跳过。") },
            ) {
                preferences.fieldOrder.forEachIndexed { index, field ->
                    val enabled = field in preferences.enabledFields
                    SettingItem(
                        title = { Text(field.displayName) },
                        description = { Text(field.description) },
                        modifier = Modifier.testTag(READING_SETTINGS_FIELD_TAG_PREFIX + field.name),
                        onClick = {
                            if (!enabled || preferences.enabledFields.size > 1) {
                                persist(
                                    preferences.copy(
                                        enabledFields = if (enabled) {
                                            preferences.enabledFields - field
                                        } else {
                                            preferences.enabledFields + field
                                        },
                                    ),
                                )
                            }
                        },
                        endAction = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = { moveField(field, -1) },
                                    enabled = index > 0,
                                    modifier = Modifier.testTag(READING_SETTINGS_FIELD_MOVE_UP_TAG_PREFIX + field.name),
                                ) {
                                    Icon(Icons.Filled.KeyboardArrowUp, contentDescription = "上移${field.displayName}")
                                }
                                IconButton(
                                    onClick = { moveField(field, 1) },
                                    enabled = index < preferences.fieldOrder.lastIndex,
                                    modifier = Modifier.testTag(READING_SETTINGS_FIELD_MOVE_DOWN_TAG_PREFIX + field.name),
                                ) {
                                    Icon(Icons.Filled.KeyboardArrowDown, contentDescription = "下移${field.displayName}")
                                }
                                SwitchWithIcon(
                                    checked = enabled,
                                    onCheckedChange = null,
                                )
                            }
                        },
                    )
                }
            }

            val publishedTimeEnabled = ReadingTemplateField.PublishedAt in preferences.enabledFields
            SettingItemGroup(
                title = "发布时间朗读",
            ) {
                SettingItem(
                    title = { Text("时间形式") },
                    description = {
                        Text("绝对时间沿用当前发布时间；相对时间朗读当前时间到最后编辑时间的间隔。")
                    },
                    enabled = publishedTimeEnabled,
                    bottomAction = {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            ReadingPublishedTimeMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = preferences.publishedTimeMode == mode,
                                    onClick = { persist(preferences.copy(publishedTimeMode = mode)) },
                                    label = { Text(mode.displayName) },
                                    enabled = publishedTimeEnabled,
                                    modifier = Modifier.testTag(
                                        READING_SETTINGS_PUBLISHED_TIME_MODE_TAG_PREFIX + mode.name,
                                    ),
                                )
                            }
                        }
                    },
                )

                SettingItem(
                    title = { Text("相对时间精度") },
                    description = { Text("保留到所选的最小时间单位，更细的部分会省略。") },
                    enabled = publishedTimeEnabled &&
                        preferences.publishedTimeMode == ReadingPublishedTimeMode.Relative,
                    bottomAction = {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            ReadingRelativeTimePrecision.entries.forEach { precision ->
                                FilterChip(
                                    selected = preferences.relativeTimePrecision == precision,
                                    onClick = {
                                        persist(preferences.copy(relativeTimePrecision = precision))
                                    },
                                    label = { Text(precision.displayName) },
                                    enabled = publishedTimeEnabled &&
                                        preferences.publishedTimeMode == ReadingPublishedTimeMode.Relative,
                                    modifier = Modifier.testTag(
                                        READING_SETTINGS_RELATIVE_TIME_PRECISION_TAG_PREFIX + precision.name,
                                    ),
                                )
                            }
                        }
                    },
                )
            }

            val commentsEnabled = ReadingTemplateField.Comments in preferences.enabledFields
            SettingItemGroup(
                title = "评论朗读",
                footer = { Text("只有启用“评论区”字段且数量大于 0 时才会请求评论数据。") },
            ) {
                SettingItem(
                    title = { Text("朗读评论数量") },
                    description = { Text("设置每条内容最多朗读多少条评论，范围为 0-50；0 表示不加载评论。") },
                    enabled = commentsEnabled,
                    bottomAction = {
                        OutlinedTextField(
                            value = commentCountText,
                            onValueChange = { value ->
                                val filtered = value.filter(Char::isDigit).take(2)
                                if (filtered.isEmpty()) {
                                    commentCountText = ""
                                    persist(preferences.copy(commentCount = 0))
                                } else {
                                    val normalized = preferences.copy(commentCount = filtered.toInt()).normalized()
                                    commentCountText = normalized.commentCount.toString()
                                    persist(normalized)
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                                .testTag(READING_SETTINGS_COMMENT_COUNT_TAG),
                            enabled = commentsEnabled,
                            singleLine = true,
                            label = { Text("评论条数") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        )
                    },
                )

                SettingItemWithSwitch(
                    title = { Text("朗读评论作者") },
                    description = { Text("关闭后，每条评论只朗读序号和正文。") },
                    checked = preferences.readCommentAuthor,
                    onCheckedChange = {
                        persist(preferences.copy(readCommentAuthor = it))
                    },
                    enabled = commentsEnabled && preferences.commentCount > 0,
                    modifier = Modifier.testTag(READING_SETTINGS_COMMENT_AUTHOR_TAG),
                )

                SettingItem(
                    title = { Text("评论排序") },
                    description = { Text("按当前选择的热度或发布时间顺序读取评论。") },
                    enabled = commentsEnabled && preferences.commentCount > 0,
                    bottomAction = {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            ReadingCommentOrder.entries.forEach { order ->
                                FilterChip(
                                    selected = preferences.commentOrder == order,
                                    onClick = { persist(preferences.copy(commentOrder = order)) },
                                    label = { Text(order.displayName) },
                                    enabled = commentsEnabled && preferences.commentCount > 0,
                                    modifier = Modifier.testTag(READING_SETTINGS_COMMENT_ORDER_TAG_PREFIX + order.name),
                                )
                            }
                        }
                    },
                )
            }

            SettingItemGroup(
                title = "连续播放",
            ) {
                SettingItem(
                    title = { Text("单次队列上限") },
                    description = { Text("数量包含当前内容。播放器到达队尾后会停止，不会无限加载后续页面。") },
                    bottomAction = {
                        Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                queueLimitPresets.forEach { limit ->
                                    FilterChip(
                                        selected = preferences.queueLimit == limit,
                                        onClick = {
                                            customQueueLimitText = ""
                                            persist(preferences.copy(queueLimit = limit))
                                        },
                                        label = { Text("$limit 条") },
                                        modifier = Modifier.testTag(READING_SETTINGS_QUEUE_LIMIT_TAG_PREFIX + limit),
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = customQueueLimitText,
                                onValueChange = { value ->
                                    val filtered = value.filter(Char::isDigit).take(3)
                                    customQueueLimitText = filtered
                                    filtered.toIntOrNull()?.let { customLimit ->
                                        val normalized = preferences.copy(queueLimit = customLimit).normalized()
                                        customQueueLimitText = normalized.queueLimit.toString()
                                        persist(normalized)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag(READING_SETTINGS_CUSTOM_QUEUE_LIMIT_TAG),
                                singleLine = true,
                                label = { Text("自定义上限（1-100 条）") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            )
                        }
                    },
                )
            }

            SettingItemGroup(
                title = "条目过渡",
            ) {
                SettingItem(
                    title = { Text("内容之间的过渡文本") },
                    description = { Text("仅在相邻条目之间朗读；留空即可关闭。") },
                    bottomAction = {
                        Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                            OutlinedTextField(
                                value = preferences.transitionText,
                                onValueChange = { persist(preferences.copy(transitionText = it)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag(READING_SETTINGS_TRANSITION_TEXT_TAG),
                                minLines = 3,
                                label = { Text("过渡文本") },
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "可用占位符：{index} 下一条序号，{total} 队列总数，{contentType} 内容类型，{title} 标题，{author} 作者。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    },
                )
            }

            SettingItemGroup(
                title = "朗读模板预览",
            ) {
                SettingItem(
                    title = { Text("当前朗读模板") },
                    description = { Text("随上方设置实时更新；大括号表示朗读时替换的动态内容，不包含条目过渡文本。") },
                    bottomAction = {
                        SelectionContainer {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 12.dp)
                                    .testTag(READING_SETTINGS_TEMPLATE_PREVIEW_TAG),
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            ) {
                                Text(
                                    text = buildReadingTemplatePreview(preferences),
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    },
                )
            }
        }
    }
}
