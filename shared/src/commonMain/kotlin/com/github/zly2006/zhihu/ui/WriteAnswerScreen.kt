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

package com.github.zly2006.zhihu.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.editor.compileMarkdownToZhihuAnswerHtml
import com.github.zly2006.zhihu.editor.rememberImagePickerLauncher
import com.github.zly2006.zhihu.editor.rememberZhihuAnswerPublisher
import com.github.zly2006.zhihu.editor.zhihuHtmlToMarkdown
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.WriteAnswer
import com.github.zly2006.zhihu.shared.platform.rememberSystemUrlOpener
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink
import kotlinx.coroutines.launch

const val WRITE_ANSWER_CONTENT_TAG = "WriteAnswerContent"
private const val ZHIHU_MARKDOWN_SYNTAX_DOC_URL = "https://zhihu.melonhu.cn/docs/syntax"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WriteAnswerScreen(
    destination: WriteAnswer,
) {
    val navigator = LocalNavigator.current
    val userMessages = rememberUserMessageSink()
    val publisher = rememberZhihuAnswerPublisher()
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    val openUrl = rememberSystemUrlOpener()

    var content by remember { mutableStateOf(TextFieldValue("")) }
    var tocEnabled by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var existingAnswerId by remember { mutableStateOf<Long?>(null) }
    var isDetecting by remember { mutableStateOf(false) }
    var isLoadingExistingAnswer by remember { mutableStateOf(false) }
    var isUploadingImage by remember { mutableStateOf(false) }

    val launchImagePicker = rememberImagePickerLauncher { picked ->
        if (!publisher.isSupported) {
            userMessages.showShortMessage("当前平台不支持插入图片")
            return@rememberImagePickerLauncher
        }
        if (isSubmitting || isUploadingImage) return@rememberImagePickerLauncher
        isUploadingImage = true
        coroutineScope.launch {
            runCatching {
                publisher.uploadImage(
                    bytes = picked.bytes,
                    mimeType = picked.mimeType,
                    fileName = picked.fileName,
                )
            }.onSuccess { uploaded ->
                val title = buildString {
                    append("zhimg:w=").append(uploaded.rawWidth)
                    append(";h=").append(uploaded.rawHeight)
                    uploaded.watermark?.let { append(";wm=").append(if (it) 1 else 0) }
                    uploaded.watermarkUrl?.let { append(";wmsrc=").append(it) }
                }
                val alt = picked.fileName?.substringBeforeLast('.')?.takeIf { it.isNotBlank() } ?: "image"
                val snippet = "![$alt](${uploaded.url} \"$title\")"
                content = insertTextAtSelection(content, snippet)
                userMessages.showShortMessage("图片已插入")
            }.onFailure { e ->
                userMessages.showLongMessage("插入图片失败: ${e.message}")
            }
            isUploadingImage = false
        }
    }

    LaunchedEffect(destination.questionId, publisher.isSupported) {
        if (!publisher.isSupported) return@LaunchedEffect
        isDetecting = true
        existingAnswerId = runCatching {
            publisher.findMyAnswerId(destination.questionId)
        }.getOrNull()
        isDetecting = false

        val answerId = existingAnswerId ?: return@LaunchedEffect
        if (content.text.isNotBlank()) return@LaunchedEffect
        isLoadingExistingAnswer = true
        runCatching {
            publisher.fetchAnswerForEditing(answerId)
        }.onSuccess { editing ->
            if (editing != null && content.text.isBlank()) {
                tocEnabled = editing.tocEnabled
                content = TextFieldValue(zhihuHtmlToMarkdown(editing.html))
            }
        }.onFailure { e ->
            userMessages.showLongMessage("加载已有回答失败: ${e.message}")
        }
        isLoadingExistingAnswer = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = destination.questionTitle.ifBlank { "写回答" },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = navigator.onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState),
        ) {
            Spacer(Modifier.height(12.dp))

            if (!publisher.isSupported) {
                Text(
                    text = "当前平台暂不支持发布/编辑回答",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = tocEnabled,
                            onCheckedChange = { tocEnabled = it },
                            enabled = !isSubmitting,
                        )
                        Text("生成目录", style = MaterialTheme.typography.bodyMedium)
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isDetecting) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier.height(18.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            text = when {
                                isDetecting -> "正在检测是否已有回答…"
                                isLoadingExistingAnswer -> "正在加载已有回答…"
                                existingAnswerId != null -> "将更新已有回答"
                                else -> "将发布新回答"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = "支持输入 Markdown。发布前会转换为知乎编辑器可接受的 HTML。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FilledTonalButton(
                    onClick = { openUrl(ZHIHU_MARKDOWN_SYNTAX_DOC_URL) },
                    enabled = !isSubmitting,
                ) {
                    Icon(Icons.Filled.OpenInNew, contentDescription = "打开语法文档")
                    Spacer(Modifier.width(8.dp))
                    Text("语法文档")
                }

                if (launchImagePicker != null) {
                    FilledTonalButton(
                        onClick = launchImagePicker,
                        enabled = publisher.isSupported && !isSubmitting && !isUploadingImage,
                    ) {
                        Icon(Icons.Filled.Image, contentDescription = "插入图片")
                        Spacer(Modifier.width(8.dp))
                        Text(if (isUploadingImage) "上传中…" else "插入图片")
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(360.dp)
                    .testTag(WRITE_ANSWER_CONTENT_TAG),
                label = { Text("回答内容") },
                enabled = !isSubmitting,
            )

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                FilledTonalButton(
                    onClick = {
                        if (!publisher.isSupported) return@FilledTonalButton
                        if (content.text.isBlank()) {
                            userMessages.showShortMessage("内容为空")
                            return@FilledTonalButton
                        }
                        if (isSubmitting) return@FilledTonalButton
                        isSubmitting = true
                        coroutineScope.launch {
                            runCatching {
                                val html = compileMarkdownToZhihuAnswerHtml(content.text, publisher)
                                val answerId = existingAnswerId
                                    ?: publisher.findMyAnswerId(destination.questionId)
                                publisher.patchDraft(
                                    questionId = destination.questionId,
                                    answerId = answerId,
                                    html = html,
                                    tocEnabled = tocEnabled,
                                )
                            }.onSuccess {
                                userMessages.showShortMessage("已保存草稿")
                            }.onFailure { e ->
                                userMessages.showLongMessage("保存草稿失败: ${e.message}")
                            }
                            isSubmitting = false
                        }
                    },
                    enabled = publisher.isSupported && !isSubmitting,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("保存草稿")
                }

                Button(
                    onClick = {
                        if (!publisher.isSupported) return@Button
                        if (content.text.isBlank()) {
                            userMessages.showShortMessage("内容为空")
                            return@Button
                        }
                        if (isSubmitting) return@Button
                        isSubmitting = true
                        coroutineScope.launch {
                            runCatching {
                                val html = compileMarkdownToZhihuAnswerHtml(content.text, publisher)
                                val answerId = existingAnswerId
                                    ?: publisher.findMyAnswerId(destination.questionId)

                                publisher.patchDraft(
                                    questionId = destination.questionId,
                                    answerId = answerId,
                                    html = html,
                                    tocEnabled = tocEnabled,
                                )
                                publisher.publishAnswer(
                                    questionId = destination.questionId,
                                    answerId = answerId,
                                    html = html,
                                    tocEnabled = tocEnabled,
                                )
                            }.onSuccess { publishedAnswerId ->
                                userMessages.showShortMessage("发布成功")
                                navigator.onNavigate(Article(type = ArticleType.Answer, id = publishedAnswerId))
                            }.onFailure { e ->
                                userMessages.showLongMessage("发布失败: ${e.message}")
                            }
                            isSubmitting = false
                        }
                    },
                    enabled = publisher.isSupported && !isSubmitting,
                    modifier = Modifier.weight(1f),
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.height(18.dp),
                        )
                    } else {
                        Text("发布")
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

private fun insertTextAtSelection(
    current: TextFieldValue,
    insert: String,
): TextFieldValue {
    val start = current.selection.min
    val end = current.selection.max
    val newText = buildString {
        append(current.text.substring(0, start))
        append(insert)
        append(current.text.substring(end))
    }
    val newCursor = start + insert.length
    return TextFieldValue(
        text = newText,
        selection = TextRange(newCursor, newCursor),
    )
}
