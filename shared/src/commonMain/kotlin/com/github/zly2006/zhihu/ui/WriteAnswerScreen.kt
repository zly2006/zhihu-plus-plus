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

package com.github.zly2006.zhihu.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.editor.UnknownImageFormatException
import com.github.zly2006.zhihu.editor.ZhihuImageUploadSource
import com.github.zly2006.zhihu.editor.compileMdToZhihuHtml
import com.github.zly2006.zhihu.editor.rememberImagePickerLauncher
import com.github.zly2006.zhihu.editor.rememberZhihuAnswerPublisher
import com.github.zly2006.zhihu.editor.uploadZhihuImage
import com.github.zly2006.zhihu.markdown.zhihuHtmlToMarkdown
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.WriteAnswer
import com.github.zly2006.zhihu.shared.platform.rememberPlainTextClipboard
import com.github.zly2006.zhihu.shared.platform.rememberSettingsStore
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.ui.components.MyModalBottomSheet
import com.github.zly2006.zhihu.ui.components.SettingItemWithSwitch
import com.github.zly2006.zhihu.ui.components.WriteContentFabColumn
import com.github.zly2006.zhihu.ui.components.WriteContentMarkdownEditor
import com.github.zly2006.zhihu.ui.components.WriteContentPreviewSheet
import com.github.zly2006.zhihu.ui.components.replaceSelection
import com.github.zly2006.zhihu.viewmodel.rememberPaginationEnvironment
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

const val WRITE_ANSWER_CONTENT_TAG = "WriteAnswerContent"
const val WRITE_ANSWER_FAB_PREVIEW_TAG = "WriteAnswerFabPreview"
const val WRITE_ANSWER_FAB_IMAGE_TAG = "WriteAnswerFabImage"
const val WRITE_ANSWER_FAB_SAVE_TAG = "WriteAnswerFabSave"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WriteAnswerScreen(
    destination: WriteAnswer,
) {
    val navigator = LocalNavigator.current
    val userMessages = rememberUserMessageSink()
    val publisher = rememberZhihuAnswerPublisher()
    val coroutineScope = rememberCoroutineScope()
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

    fun showPreview() {
        if (isSubmitting || content.text.isBlank()) return
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
                compileMdToZhihuHtml(markdown = markdownSnapshot)
            }.onSuccess { html ->
                previewHtml = html
            }.onFailure { e ->
                errorDialogMessage = buildWriteOperationErrorMessage("生成预览失败", e)
                showPreviewSheet = false
            }
            isPreviewLoading = false
        }
    }

    fun submitAnswer(publish: Boolean) {
        if (content.text.isBlank()) {
            userMessages.showShortMessage("内容为空")
            return
        }
        if (isSubmitting) return
        isSubmitting = true
        coroutineScope.launch {
            runCatching {
                val html = compileMdToZhihuHtml(markdown = content.text)
                val answerId = ensureAnswerId()
                publisher.patchDraft(
                    questionId = destination.questionId,
                    answerId = answerId,
                    html = html,
                    tocEnabled = tocEnabled,
                )
                if (publish) {
                    publisher.publishAnswer(
                        questionId = destination.questionId,
                        answerId = answerId,
                        html = html,
                        tocEnabled = tocEnabled,
                    )
                } else {
                    null
                }
            }.onSuccess { answerId ->
                if (publish) {
                    userMessages.showShortMessage("发布成功")
                    navigator.onNavigate(Article(type = ArticleType.Answer, id = answerId ?: return@onSuccess))
                } else {
                    userMessages.showShortMessage("已保存草稿")
                }
            }.onFailure { e ->
                errorDialogMessage = buildWriteOperationErrorMessage(
                    title = if (publish) "发布失败" else "保存草稿失败",
                    throwable = e,
                )
            }
            isSubmitting = false
        }
    }

    val environment = rememberPaginationEnvironment(false)
    val launchImagePicker = rememberImagePickerLauncher { picked ->
        if (isSubmitting || isUploadingImage) return@rememberImagePickerLauncher
        isUploadingImage = true
        coroutineScope.launch {
            runCatching {
                uploadZhihuImage(
                    environment,
                    picked.bytes,
                    picked.mimeType,
                    picked.fileName,
                    ZhihuImageUploadSource.Article,
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
                val alt = picked.fileName
                    ?.substringBeforeLast('.')
                    ?.takeIf { it.isNotBlank() }
                    .orEmpty()
                val snippet = "![$alt](${uploaded.url} \"$title\")"
                content = content.replaceSelection(snippet, cursorOffsetInInsert = snippet.length)
                userMessages.showShortMessage("图片已插入")
            }.onFailure { e ->
                if (e is UnknownImageFormatException) {
                    userMessages.showShortMessage(e.message ?: "无法识别图片格式，已取消上传")
                } else {
                    errorDialogMessage = buildWriteOperationErrorMessage("插入图片失败", e)
                }
            }
            isUploadingImage = false
        }
    }

    LaunchedEffect(destination.questionId) {
        isDetecting = true
        existingAnswerId = runCatching {
            publisher.findMyAnswerId(destination.questionId)
        }.onFailure { e ->
            errorDialogMessage = buildWriteOperationErrorMessage("检测已有回答失败", e)
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
            errorDialogMessage = buildWriteOperationErrorMessage("加载已有回答失败", e)
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
                        enabled = !isSubmitting,
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "回答设置")
                    }
                    Button(
                        onClick = { submitAnswer(publish = true) },
                        enabled = !isSubmitting && !isUploadingImage,
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
            WriteContentFabColumn(
                previewEnabled = !isSubmitting && content.text.isNotBlank(),
                imageEnabled = launchImagePicker != null && !isSubmitting && !isUploadingImage,
                saveEnabled = !isSubmitting,
                showImageButton = launchImagePicker != null,
                isUploadingImage = isUploadingImage,
                previewTag = WRITE_ANSWER_FAB_PREVIEW_TAG,
                imageTag = WRITE_ANSWER_FAB_IMAGE_TAG,
                saveTag = WRITE_ANSWER_FAB_SAVE_TAG,
                onPreview = ::showPreview,
                onImage = { launchImagePicker?.invoke() },
                onSave = { submitAnswer(publish = false) },
            )
        },
    ) { innerPadding ->
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
                    .imePadding()
                    .padding(horizontal = 16.dp),
        ) {
            WriteContentMarkdownEditor(
                value = content,
                onValueChange = { newValue -> content = newValue },
                placeholder = "请输入图文回答内容……",
                contentTag = WRITE_ANSWER_CONTENT_TAG,
                enabled = !isSubmitting,
                modifier = Modifier.fillMaxSize(),
            )
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
            Spacer(Modifier.height(20.dp))
        }
    }

    if (showPreviewSheet) {
        WriteContentPreviewSheet(
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

    WriteOperationErrorDialog(
        message = errorDialogMessage,
        onDismissRequest = { errorDialogMessage = null },
        onCopy = { message ->
            copyToClipboard("write-answer-error", message)
            userMessages.showShortMessage("已复制错误信息")
        },
    )
}
