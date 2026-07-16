/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation (version 3 only).
 */

package com.github.zly2006.zhihu.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.reading.ReadingPlaybackStatus
import com.github.zly2006.zhihu.reading.ReadingPlayerState
import com.github.zly2006.zhihu.reading.ReadingQueueItem
import com.github.zly2006.zhihu.shared.platform.exportTestTagsForUiAutomation
import kotlin.math.abs
import kotlin.math.roundToInt

const val READING_PLAYER_BAR_TAG = "reading_player_bar"
const val READING_PLAYER_PREVIOUS_TAG = "reading_player_previous"
const val READING_PLAYER_TOGGLE_TAG = "reading_player_toggle"
const val READING_PLAYER_NEXT_TAG = "reading_player_next"
const val READING_PLAYER_SETTINGS_TAG = "reading_player_settings"
const val READING_PLAYER_QUEUE_TAG = "reading_player_queue"
const val READING_PLAYER_SPEED_TAG = "reading_player_speed"
const val READING_PLAYER_SPEED_MENU_TAG = "reading_player_speed_menu"
const val READING_QUEUE_SHEET_TAG = "reading_queue_sheet"
const val READING_QUEUE_STOP_TAG = "reading_queue_stop"

fun readingQueueItemTag(index: Int): String = "reading_queue_item_$index"

fun readingPlayerSpeedOptionTag(speed: Float): String = "reading_player_speed_${(speed * 100).roundToInt()}"

private val readingPlaybackSpeedOptions = listOf(
    0.75f to "0.75×",
    1f to "1.0×",
    1.25f to "1.25×",
    1.5f to "1.5×",
    2f to "2.0×",
)

private fun playbackSpeedLabel(speed: Float): String = readingPlaybackSpeedOptions
    .firstOrNull { (option, _) -> abs(option - speed) < 0.001f }
    ?.second
    ?: "$speed×"

@Composable
fun ReadingPlayerBar(
    state: ReadingPlayerState,
    onPrevious: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onNext: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenQueue: () -> Unit,
    onPlaybackSpeedChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val item = state.currentItem ?: return
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(READING_PLAYER_BAR_TAG),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            tonalElevation = 3.dp,
            shadowElevation = 4.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 4.dp, end = 8.dp),
                    ) {
                        Text(
                            text = item.displayTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = state.errorMessage?.let { "朗读失败：$it" } ?: buildString {
                                append(state.currentIndex + 1)
                                append(" / ")
                                append(state.queue.size)
                                append(" · ")
                                append(item.contentType.displayName)
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    ReadingPlaybackSpeedMenu(
                        speed = state.playbackSpeed,
                        onSpeedChange = onPlaybackSpeedChange,
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    IconButton(
                        onClick = onPrevious,
                        enabled = state.canPlayPrevious,
                        modifier = Modifier.testTag(READING_PLAYER_PREVIOUS_TAG),
                    ) {
                        Icon(Icons.Default.SkipPrevious, contentDescription = "上一条")
                    }
                    FilledTonalIconButton(
                        onClick = onTogglePlayPause,
                        modifier = Modifier
                            .testTag(READING_PLAYER_TOGGLE_TAG)
                            .semantics {
                                contentDescription = if (state.isActivelyPlaying) "暂停朗读" else "继续朗读"
                            },
                    ) {
                        when (state.status) {
                            ReadingPlaybackStatus.Initializing,
                            ReadingPlaybackStatus.Loading,
                            -> CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                strokeWidth = 2.dp,
                            )
                            ReadingPlaybackStatus.Playing -> Icon(Icons.Default.Pause, contentDescription = null)
                            else -> Icon(Icons.Default.PlayArrow, contentDescription = null)
                        }
                    }
                    IconButton(
                        onClick = onNext,
                        enabled = state.canPlayNext,
                        modifier = Modifier.testTag(READING_PLAYER_NEXT_TAG),
                    ) {
                        Icon(Icons.Default.SkipNext, contentDescription = "下一条")
                    }
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.testTag(READING_PLAYER_SETTINGS_TAG),
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "朗读设置")
                    }
                    IconButton(
                        onClick = onOpenQueue,
                        modifier = Modifier.testTag(READING_PLAYER_QUEUE_TAG),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = "播放列表")
                    }
                }
            }
        }
    }
}

@Composable
private fun ReadingPlaybackSpeedMenu(
    speed: Float,
    onSpeedChange: (Float) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val currentLabel = playbackSpeedLabel(speed)
    Box {
        Surface(
            onClick = { expanded = true },
            modifier = Modifier
                .testTag(READING_PLAYER_SPEED_TAG)
                .semantics {
                    contentDescription = "播放速度"
                    stateDescription = "当前 ${currentLabel.replace("×", " 倍")}"
                },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Default.Speed,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = currentLabel,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            offset = DpOffset(x = (-16).dp, y = 0.dp),
            modifier = Modifier
                .testTag(READING_PLAYER_SPEED_MENU_TAG)
                .exportTestTagsForUiAutomation(),
        ) {
            readingPlaybackSpeedOptions.forEach { (option, label) ->
                val isSelected = abs(option - speed) < 0.001f
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        expanded = false
                        onSpeedChange(option)
                    },
                    trailingIcon = if (isSelected) {
                        {
                            Icon(
                                Icons.Default.Check,
                                contentDescription = "当前速度",
                            )
                        }
                    } else {
                        null
                    },
                    modifier = Modifier
                        .testTag(readingPlayerSpeedOptionTag(option))
                        .semantics {
                            selected = isSelected
                            role = Role.RadioButton
                        },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingQueueSheet(
    state: ReadingPlayerState,
    onDismissRequest: () -> Unit,
    onItemClick: (index: Int, item: ReadingQueueItem) -> Unit,
    onStop: () -> Unit,
) {
    if (!state.hasSession) return
    MyModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        modifier = Modifier.testTag(READING_QUEUE_SHEET_TAG),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = "播放列表",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "${state.currentIndex + 1} / ${state.queue.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(
                onClick = onStop,
                modifier = Modifier.testTag(READING_QUEUE_STOP_TAG),
            ) {
                Icon(Icons.Default.Stop, contentDescription = null)
                Text("停止朗读")
            }
        }
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 560.dp),
        ) {
            itemsIndexed(
                items = state.queue,
                key = { _, item -> item.key },
            ) { index, item ->
                val isCurrent = index == state.currentIndex
                ListItem(
                    headlineContent = {
                        Text(
                            text = item.displayTitle,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                        )
                    },
                    supportingContent = {
                        Text(
                            text = listOf(item.author, item.contentType.displayName)
                                .filter(String::isNotBlank)
                                .joinToString(" · "),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    leadingContent = {
                        if (isCurrent) {
                            if (state.status == ReadingPlaybackStatus.Paused) {
                                Icon(Icons.Default.PauseCircle, contentDescription = "当前已暂停")
                            } else {
                                Icon(
                                    Icons.Default.GraphicEq,
                                    contentDescription = if (state.isActivelyPlaying) "正在朗读" else "当前条目",
                                )
                            }
                        } else {
                            Text("${index + 1}")
                        }
                    },
                    colors = ListItemDefaults.colors(
                        containerColor = if (isCurrent) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceContainerLow
                        },
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag(readingQueueItemTag(index))
                        .clickable { onItemClick(index, item) },
                )
            }
        }
    }
}
