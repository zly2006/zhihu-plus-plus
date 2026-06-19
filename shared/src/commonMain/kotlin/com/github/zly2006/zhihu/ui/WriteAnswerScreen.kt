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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.zly2006.zhihu.editor.compileMdToZhihuHtml
import com.github.zly2006.zhihu.editor.rememberImagePickerLauncher
import com.github.zly2006.zhihu.editor.rememberZhihuAnswerPublisher
import com.github.zly2006.zhihu.markdown.zhihuHtmlToMarkdown
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.WriteAnswer
import com.github.zly2006.zhihu.shared.platform.rememberPlainTextClipboard
import com.github.zly2006.zhihu.shared.platform.rememberSettingsStore
import com.github.zly2006.zhihu.shared.platform.rememberSystemUrlOpener
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.shared.util.HttpStatusException
import com.github.zly2006.zhihu.ui.components.MarkdownShortcutToolbar
import com.github.zly2006.zhihu.ui.components.MyModalBottomSheet
import com.github.zly2006.zhihu.ui.components.SettingItemWithSwitch
import com.github.zly2006.zhihu.ui.components.WriteAnswerPreviewSheet
import com.github.zly2006.zhihu.ui.components.applyMarkdownShortcut
import com.github.zly2006.zhihu.ui.components.insertTextAtSelection
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

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
    val editorScrollState = androidx.compose.foundation.rememberScrollState()
    val openUrl = rememberSystemUrlOpener()
    val copyToClipboard = rememberPlainTextClipboard()
    val settings = rememberSettingsStore()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var content by remember { mutableStateOf(TextFieldValue("")) }
    var tocEnabled by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var existingAnswerId by remember { mutableStateOf<Long?>(null) }
    var isDetecting by remember { mutableStateOf(false) }
    var isLoadingExistingAnswer by remember { mutableStateOf(false) }
    var isUploadingImage by remember { mutableStateOf(false) }
    var errorDialogMessage by remember { mutableStateOf<String?>(null) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var showPreviewSheet by remember { mutableStateOf(false) }
    val settingsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val previewSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isPreviewLoading by remember { mutableStateOf(false) }
    var previewHtml by remember { mutableStateOf<String?>(null) }
    var previewMarkdown by remember { mutableStateOf<String?>(null) }
    var previewUseWebView by remember { mutableStateOf(false) }

    suspend fun ensureAnswerId(): Long? {
        val cached = existingAnswerId
        if (cached != null) return cached
        val answerId = publisher.findMyAnswerId(destination.questionId)
        existingAnswerId = answerId
        return answerId
    }

    suspend fun compileHtml(): String =
        compileMdToZhihuHtml(
            markdown = content.text,
            publisher = publisher,
        )

    fun submit(publish: Boolean) {
        if (!publisher.isSupported) return
        if (content.text.isBlank()) {
            userMessages.showShortMessage("内容为空")
            return
        }
        if (isSubmitting) return
        isSubmitting = true
        coroutineScope.launch {
            if (publish) {
                runCatching {
                    val html = compileHtml()
                    val answerId = ensureAnswerId()
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
                }.onSuccess { resultAnswerId ->
                    userMessages.showShortMessage("发布成功")
                    navigator.onNavigate(Article(type = ArticleType.Answer, id = resultAnswerId))
                }.onFailure { e ->
                    errorDialogMessage = buildErrorDialogMessage("发布失败", e)
                }
            } else {
                runCatching {
                    val html = compileHtml()
                    val answerId = ensureAnswerId()
                    publisher.patchDraft(
                        questionId = destination.questionId,
                        answerId = answerId,
                        html = html,
                        tocEnabled = tocEnabled,
                    )
                }.onSuccess {
                    userMessages.showShortMessage("已保存草稿")
                }.onFailure { e ->
                    errorDialogMessage = buildErrorDialogMessage("保存草稿失败", e)
                }
            }
            isSubmitting = false
        }
    }

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
                    if (uploaded.watermark == true) {
                        uploaded.watermarkUrl?.let { append(";wmsrc=").append(it) }
                    }
                }
                val alt = picked.fileName?.substringBeforeLast('.')?.takeIf { it.isNotBlank() } ?: "image"
                val snippet = "![$alt](${uploaded.url} \"$title\")"
                content = content.insertTextAtSelection(snippet)
                userMessages.showShortMessage("图片已插入")
            }.onFailure { e ->
                errorDialogMessage = buildErrorDialogMessage("插入图片失败", e)
            }
            isUploadingImage = false
        }
    }

    LaunchedEffect(destination.questionId, publisher.isSupported) {
        if (!publisher.isSupported) return@LaunchedEffect
        isDetecting = true
        existingAnswerId = runCatching {
            publisher.findMyAnswerId(destination.questionId)
        }.onFailure { e ->
            errorDialogMessage = buildErrorDialogMessage("检测已有回答失败", e)
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
            errorDialogMessage = buildErrorDialogMessage("加载已有回答失败", e)
        }
        isLoadingExistingAnswer = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text =
                            when {
                                isDetecting || isLoadingExistingAnswer -> "正在检测已有回答..."
                                existingAnswerId != null -> "编辑已有回答"
                                else -> "写回答"
                            },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = navigator.onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showSettingsSheet = true },
                        enabled = publisher.isSupported && !isSubmitting,
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "回答设置")
                    }
                    Button(
                        onClick = {
                            submit(publish = true)
                        },
                        enabled = publisher.isSupported && !isSubmitting,
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Text("发布")
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (publisher.isSupported) {
                val imageEnabled = launchImagePicker != null && !isSubmitting && !isUploadingImage
                val saveEnabled = !isSubmitting
                val previewEnabled = !isSubmitting && content.text.isNotBlank()
                Column(
                    modifier =
                        Modifier
                            .imePadding()
                            .padding(bottom = 72.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.End,
                ) {
                    FloatingActionButton(
                        onClick = {
                            if (!previewEnabled) return@FloatingActionButton
                            val useWebView = settings.getBoolean(ARTICLE_USE_WEBVIEW_PREFERENCE_KEY, false)
                            val markdownSnapshot = content.text
                            coroutineScope.launch {
                                focusManager.clearFocus(force = true)
                                keyboardController?.hide()
                                yield()
                                previewUseWebView = useWebView
                                previewMarkdown = markdownSnapshot
                                previewHtml = null
                                showPreviewSheet = true
                                if (!useWebView) {
                                    isPreviewLoading = false
                                    return@launch
                                }
                                isPreviewLoading = true
                                runCatching {
                                    compileMdToZhihuHtml(
                                        markdown = markdownSnapshot,
                                        publisher = publisher,
                                    )
                                }.onSuccess { html ->
                                    previewHtml = html
                                }.onFailure { e ->
                                    errorDialogMessage = buildErrorDialogMessage("生成预览失败", e)
                                    showPreviewSheet = false
                                }
                                isPreviewLoading = false
                            }
                        },
                        containerColor =
                            if (previewEnabled) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                        contentColor =
                            if (previewEnabled) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        modifier = Modifier.testTag("WriteAnswerFabPreview"),
                    ) {
                        Icon(Icons.Filled.Visibility, contentDescription = "预览")
                    }
                    if (launchImagePicker != null) {
                        FloatingActionButton(
                            onClick = { if (imageEnabled) launchImagePicker() },
                            containerColor =
                                if (imageEnabled) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                },
                            contentColor =
                                if (imageEnabled) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            modifier = Modifier.testTag("WriteAnswerFabImage"),
                        ) {
                            Icon(Icons.Filled.Image, contentDescription = "插入图片")
                        }
                    }
                    FloatingActionButton(
                        onClick = { if (saveEnabled) submit(publish = false) },
                        containerColor =
                            if (saveEnabled) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                        contentColor =
                            if (saveEnabled) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        modifier = Modifier.testTag("WriteAnswerFabSave"),
                    ) {
                        Icon(Icons.Filled.Save, contentDescription = "保存草稿")
                    }
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .imePadding()
                .padding(horizontal = 16.dp),
        ) {
            if (!publisher.isSupported) {
                Text(
                    text = "当前平台暂不支持发布/编辑回答",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp),
                )
            } else {
                BasicTextField(
                    value = content,
                    onValueChange = { newValue -> content = newValue },
                    modifier =
                        Modifier
                            .fillMaxSize()
                            .testTag(WRITE_ANSWER_CONTENT_TAG),
                    enabled = !isSubmitting,
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
                                    .padding(top = 16.dp, bottom = 160.dp),
                        ) {
                            if (content.text.isEmpty()) {
                                Text(
                                    text = "请输入图文回答内容……",
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
                        content = content.applyMarkdownShortcut(shortcut)
                    },
                    modifier =
                        Modifier
                            .align(Alignment.BottomCenter)
                            .offset(y = 10.dp),
                    enabled = !isSubmitting,
                )
            }
        }
    }

    if (showSettingsSheet) {
        MyModalBottomSheet(
            onDismissRequest = { showSettingsSheet = false },
            sheetState = settingsSheetState,
            contentWindowInsets = { WindowInsets(0, 0, 0, 0) },
        ) {
            Text(
                text = "回答设置",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )
            Text(
                text = "这些选项仅影响 Markdown 转换为知乎编辑器 HTML 的方式。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp),
            )
            Spacer(Modifier.height(16.dp))
            SettingItemWithSwitch(
                title = { Text("生成目录") },
                description = {
                    Text("适合长回答，知乎会根据标题生成目录结构。")
                },
                checked = tocEnabled,
                onCheckedChange = { tocEnabled = it },
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            Spacer(Modifier.height(8.dp))
            FilledTonalButton(
                onClick = { openUrl(ZHIHU_MARKDOWN_SYNTAX_DOC_URL) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
            ) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "打开语法文档")
                Spacer(Modifier.width(8.dp))
                Text("查看语法文档")
            }
            Spacer(Modifier.height(20.dp))
        }
    }

    if (showPreviewSheet) {
        WriteAnswerPreviewSheet(
            visible = showPreviewSheet,
            sheetState = previewSheetState,
            useWebView = previewUseWebView,
            isLoading = isPreviewLoading,
            html = previewHtml,
            markdown = previewMarkdown,
            onDismissRequest = {
                showPreviewSheet = false
                isPreviewLoading = false
            },
        )
    }

    val dialogMessage = errorDialogMessage
    if (dialogMessage != null) {
        AlertDialog(
            onDismissRequest = { errorDialogMessage = null },
            title = { Text("操作失败") },
            text = {
                Text(
                    text = dialogMessage,
                    style = MaterialTheme.typography.bodySmall,
                )
            },
            confirmButton = {
                Button(onClick = { errorDialogMessage = null }) {
                    Text("确定")
                }
            },
            dismissButton = {
                FilledTonalButton(
                    onClick = {
                        copyToClipboard("write-answer-error", dialogMessage)
                        userMessages.showShortMessage("已复制错误信息")
                    },
                ) {
                    Text("复制")
                }
            },
        )
    }
}

private fun buildErrorDialogMessage(
    title: String,
    throwable: Throwable,
): String {
    val chain = buildList {
        var current: Throwable? = throwable
        while (current != null) {
            add("${current::class.qualifiedName}: ${current.message.orEmpty()}")
            current = current.cause
        }
    }
    val httpStatusException = throwable.findCause<HttpStatusException>()
    return buildString {
        append(title).append('\n')
        append('\n')
        chain.forEachIndexed { index, item ->
            append("#")
                .append(index + 1)
                .append(' ')
                .append(item)
                .append('\n')
        }
        if (httpStatusException != null) {
            append('\n')
            append("HTTP 状态: ")
                .append(httpStatusException.status.value)
                .append(' ')
                .append(httpStatusException.status.description)
                .append('\n')
            append("请求地址: ")
                .append(httpStatusException.requestUrl)
                .append('\n')
            if (httpStatusException.bodyText.isNotBlank()) {
                append('\n')
                append("响应体:\n")
                append(httpStatusException.bodyText.trim())
                    .append('\n')
            }
            httpStatusException.dumpedCurlRequest
                ?.takeIf { it.isNotBlank() }
                ?.let { curl ->
                    append('\n')
                    append("请求复现:\n")
                    append(curl.trim())
                        .append('\n')
                }
        }
    }.trim()
}

private inline fun <reified T : Throwable> Throwable.findCause(): T? {
    var current: Throwable? = this
    while (current != null) {
        if (current is T) return current
        current = current.cause
    }
    return null
}
