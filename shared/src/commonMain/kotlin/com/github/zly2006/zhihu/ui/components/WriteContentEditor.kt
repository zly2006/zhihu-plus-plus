/*
 * Zhihu++ - Free & Ad-Free Zhihu client for all platforms.
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

package com.github.zly2006.zhihu.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun WriteContentMarkdownEditor(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    placeholder: String,
    contentTag: String,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    topPadding: Dp = 16.dp,
    bottomPadding: Dp = 160.dp,
) {
    val editorScrollState = rememberScrollState()

    Box(modifier = modifier) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier =
                Modifier
                    .fillMaxSize()
                    .testTag(contentTag),
            enabled = enabled,
            textStyle =
                TextStyle(
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 17.sp,
                    lineHeight = 26.sp,
                ),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { innerTextField ->
                Box(
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(editorScrollState)
                            .padding(top = topPadding, bottom = bottomPadding),
                ) {
                    if (value.text.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    innerTextField()
                }
            },
        )
        MarkdownShortcutToolbar(
            onApplyShortcut = { shortcut ->
                onValueChange(value.applyMarkdownShortcut(shortcut))
            },
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .offset(y = 10.dp),
            enabled = enabled,
        )
    }
}

@Composable
fun WriteContentFabColumn(
    previewEnabled: Boolean,
    imageEnabled: Boolean,
    saveEnabled: Boolean,
    showImageButton: Boolean,
    isUploadingImage: Boolean,
    previewTag: String,
    imageTag: String,
    saveTag: String,
    onPreview: () -> Unit,
    onImage: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(true) }
    val opacityFraction = fabOpacityPercent.intValue / 100f
    val enabledContainer = MaterialTheme.colorScheme.primaryContainer.copy(alpha = opacityFraction)
    val disabledContainer = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = opacityFraction)
    val enabledContent = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = opacityFraction)
    val disabledContent = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = opacityFraction)
    val fabElevation = if (opacityFraction < 1f) {
        FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp)
    } else {
        FloatingActionButtonDefaults.elevation()
    }
    Column(
        modifier =
            modifier
                .imePadding()
                .padding(bottom = 72.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.End,
    ) {
        val enterTransition = expandVertically(expandFrom = Alignment.Bottom) + fadeIn()
        val exitTransition = shrinkVertically(shrinkTowards = Alignment.Bottom) + fadeOut()
        AnimatedVisibility(visible = expanded, enter = enterTransition, exit = exitTransition) {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.End,
            ) {
                ExtendedFloatingActionButton(
                    onClick = { if (previewEnabled) onPreview() },
                    containerColor = if (previewEnabled) enabledContainer else disabledContainer,
                    contentColor = if (previewEnabled) enabledContent else disabledContent,
                    elevation = fabElevation,
                    modifier = Modifier.testTag(previewTag),
                    icon = {
                        Icon(Icons.Filled.Visibility, contentDescription = "预览")
                    },
                    text = {
                        Text("预览")
                    },
                )
                if (showImageButton) {
                    ExtendedFloatingActionButton(
                        onClick = { if (imageEnabled) onImage() },
                        containerColor = if (imageEnabled) enabledContainer else disabledContainer,
                        contentColor = if (imageEnabled) enabledContent else disabledContent,
                        elevation = fabElevation,
                        modifier = Modifier.testTag(imageTag),
                        icon = {
                            if (isUploadingImage) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Icon(Icons.Filled.Image, contentDescription = "插入图片")
                            }
                        },
                        text = {
                            Text("图片")
                        },
                    )
                }
                ExtendedFloatingActionButton(
                    onClick = { if (saveEnabled) onSave() },
                    containerColor = if (saveEnabled) enabledContainer else disabledContainer,
                    contentColor = if (saveEnabled) enabledContent else disabledContent,
                    elevation = fabElevation,
                    modifier = Modifier.testTag(saveTag),
                    icon = {
                        Icon(Icons.Filled.Save, contentDescription = "保存草稿")
                    },
                    text = {
                        Text("草稿")
                    },
                )
            }
        }
        SmallFloatingActionButton(
            onClick = { expanded = !expanded },
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = opacityFraction),
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = opacityFraction),
            elevation = fabElevation,
        ) {
            Icon(
                imageVector = if (expanded) Icons.Filled.Close else Icons.Filled.Add,
                contentDescription = if (expanded) "收起" else "展开",
            )
        }
    }
}
