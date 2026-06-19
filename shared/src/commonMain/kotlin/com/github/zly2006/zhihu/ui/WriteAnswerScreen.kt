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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.github.zly2006.zhihu.editor.UnknownImageFormatException
import com.github.zly2006.zhihu.editor.UploadedZhihuImage
import com.github.zly2006.zhihu.editor.ZhihuImageUploadSource
import com.github.zly2006.zhihu.editor.compileMdToZhihuHtml
import com.github.zly2006.zhihu.editor.rememberImagePickerLauncher
import com.github.zly2006.zhihu.editor.rememberZhihuAnswerPublisher
import com.github.zly2006.zhihu.markdown.rememberMarkdownImageModel
import com.github.zly2006.zhihu.markdown.zhihuHtmlToMarkdown
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.Pin
import com.github.zly2006.zhihu.navigation.WriteAnswer
import com.github.zly2006.zhihu.shared.platform.rememberPlainTextClipboard
import com.github.zly2006.zhihu.shared.platform.rememberSettingsStore
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.shared.util.HttpStatusException
import com.github.zly2006.zhihu.ui.components.MarkdownShortcutToolbar
import com.github.zly2006.zhihu.ui.components.MyModalBottomSheet
import com.github.zly2006.zhihu.ui.components.SettingItemWithSwitch
import com.github.zly2006.zhihu.ui.components.WriteAnswerPreviewSheet
import com.github.zly2006.zhihu.ui.components.applyMarkdownShortcut
import com.github.zly2006.zhihu.ui.components.replaceSelection
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

const val WRITE_ANSWER_CONTENT_TAG = "WriteAnswerContent"
const val WRITE_PIN_TITLE_TAG = "WritePinTitle"
const val WRITE_PIN_CONTENT_TAG = "WritePinContent"
private const val PIN_IMAGE_LIMIT = 9

private sealed interface WriteEditorTarget {
    val contentTag: String

    data class Answer(
        val destination: WriteAnswer,
    ) : WriteEditorTarget {
        override val contentTag: String = WRITE_ANSWER_CONTENT_TAG
    }

    data object Pin : WriteEditorTarget {
        override val contentTag: String = WRITE_PIN_CONTENT_TAG
    }
}

@Composable
fun WriteAnswerScreen(
    destination: WriteAnswer,
) {
    WriteZhihuContentScreen(WriteEditorTarget.Answer(destination))
}

@Composable
fun WritePinScreen() {
    WriteZhihuContentScreen(WriteEditorTarget.Pin)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WriteZhihuContentScreen(
    target: WriteEditorTarget,
) {
    val navigator = LocalNavigator.current
    val userMessages = rememberUserMessageSink()
    val publisher = rememberZhihuAnswerPublisher()
    val coroutineScope = rememberCoroutineScope()
    val editorScrollState = androidx.compose.foundation.rememberScrollState()
    val copyToClipboard = rememberPlainTextClipboard()
    val settings = rememberSettingsStore()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var content by remember { mutableStateOf(TextFieldValue("")) }
    var pinTitle by remember { mutableStateOf(TextFieldValue("")) }
    var pinImages by remember { mutableStateOf<List<UploadedZhihuImage>>(emptyList()) }
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

    suspend fun ensureAnswerId(destination: WriteAnswer): Long? {
        val cached = existingAnswerId
        if (cached != null) return cached
        val answerId = publisher.findMyAnswerId(destination.questionId)
        existingAnswerId = answerId
        return answerId
    }

    fun submit(publish: Boolean) {
        if (!publisher.isSupported) return
        val markdownSnapshot = content.text
        val pinImagesSnapshot = pinImages
        val isContentEmpty = when (target) {
            is WriteEditorTarget.Answer -> markdownSnapshot.isBlank()
            WriteEditorTarget.Pin -> markdownSnapshot.isBlank() && pinImagesSnapshot.isEmpty()
        }
        if (isContentEmpty) {
            userMessages.showShortMessage(
                when (target) {
                    is WriteEditorTarget.Answer -> "内容为空"
                    WriteEditorTarget.Pin -> "想法内容为空"
                },
            )
            return
        }
        if (isSubmitting) return
        isSubmitting = true
        coroutineScope.launch {
            runCatching {
                val html = compileMdToZhihuHtml(markdown = markdownSnapshot)
                when (val currentTarget = target) {
                    is WriteEditorTarget.Answer -> {
                        val answerId = ensureAnswerId(currentTarget.destination)
                        if (publish) {
                            publisher.patchDraft(
                                questionId = currentTarget.destination.questionId,
                                answerId = answerId,
                                html = html,
                                tocEnabled = tocEnabled,
                            )
                            publisher.publishAnswer(
                                questionId = currentTarget.destination.questionId,
                                answerId = answerId,
                                html = html,
                                tocEnabled = tocEnabled,
                            )
                        } else {
                            publisher.patchDraft(
                                questionId = currentTarget.destination.questionId,
                                answerId = answerId,
                                html = html,
                                tocEnabled = tocEnabled,
                            )
                            null
                        }
                    }
                    WriteEditorTarget.Pin -> {
                        val title = pinTitle.text.trim()
                        val textLength = html.replace(Regex("<.+?>"), "").length
                        if (publish) {
                            publisher.publishPin(
                                title = title,
                                html = html,
                                textLength = textLength,
                                images = pinImagesSnapshot,
                            )
                        } else {
                            publisher.savePinDraft(
                                title = title,
                                html = html,
                                textLength = textLength,
                                images = pinImagesSnapshot,
                            )
                            null
                        }
                    }
                }
            }.onSuccess { resultContentId ->
                if (publish) {
                    userMessages.showShortMessage("发布成功")
                    when (target) {
                        is WriteEditorTarget.Answer -> {
                            navigator.onNavigate(Article(type = ArticleType.Answer, id = resultContentId ?: return@onSuccess))
                        }
                        WriteEditorTarget.Pin -> {
                            navigator.onNavigate(Pin(resultContentId ?: return@onSuccess))
                        }
                    }
                } else {
                    userMessages.showShortMessage("已保存草稿")
                }
            }.onFailure { e ->
                errorDialogMessage = buildErrorDialogMessage(
                    title = if (publish) "发布失败" else "保存草稿失败",
                    throwable = e,
                )
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
        if (target is WriteEditorTarget.Pin && pinImages.size >= PIN_IMAGE_LIMIT) {
            userMessages.showShortMessage("图片最多添加 $PIN_IMAGE_LIMIT 张")
            return@rememberImagePickerLauncher
        }
        isUploadingImage = true
        coroutineScope.launch {
            runCatching {
                publisher.uploadImage(
                    bytes = picked.bytes,
                    mimeType = picked.mimeType,
                    fileName = picked.fileName,
                    source = when (target) {
                        is WriteEditorTarget.Answer -> ZhihuImageUploadSource.Article
                        WriteEditorTarget.Pin -> ZhihuImageUploadSource.Pin
                    },
                )
            }.onSuccess { uploaded ->
                when (target) {
                    is WriteEditorTarget.Answer -> {
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
                    }
                    WriteEditorTarget.Pin -> {
                        pinImages = pinImages + uploaded
                        userMessages.showShortMessage("图片已添加")
                    }
                }
            }.onFailure { e ->
                if (e is UnknownImageFormatException) {
                    userMessages.showShortMessage(e.message ?: "无法识别图片格式，已取消上传")
                } else {
                    errorDialogMessage = buildErrorDialogMessage("插入图片失败", e)
                }
            }
            isUploadingImage = false
        }
    }

    val answerTarget = target as? WriteEditorTarget.Answer
    LaunchedEffect(answerTarget?.destination?.questionId, publisher.isSupported) {
        val answerDestination = answerTarget?.destination ?: return@LaunchedEffect
        if (!publisher.isSupported) return@LaunchedEffect
        isDetecting = true
        existingAnswerId = runCatching {
            publisher.findMyAnswerId(answerDestination.questionId)
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
                            when (target) {
                                is WriteEditorTarget.Answer -> {
                                    when {
                                        isDetecting || isLoadingExistingAnswer -> "正在检测已有回答..."
                                        existingAnswerId != null -> "编辑已有回答"
                                        else -> "写回答"
                                    }
                                }
                                WriteEditorTarget.Pin -> "发想法"
                            },
                    )
                },
                navigationIcon = {
                    IconButton(onClick = navigator.onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (target is WriteEditorTarget.Answer) {
                        IconButton(
                            onClick = { showSettingsSheet = true },
                            enabled = publisher.isSupported && !isSubmitting,
                        ) {
                            Icon(Icons.Default.Settings, contentDescription = "回答设置")
                        }
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
                    ExtendedFloatingActionButton(
                        onClick = {
                            if (!previewEnabled) return@ExtendedFloatingActionButton
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
                        icon = {
                            Icon(Icons.Filled.Visibility, contentDescription = "预览")
                        },
                        text = {
                            Text("预览")
                        },
                    )
                    if (launchImagePicker != null) {
                        ExtendedFloatingActionButton(
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
                        icon = {
                            Icon(Icons.Filled.Save, contentDescription = "保存草稿")
                        },
                        text = {
                            Text("草稿")
                        },
                    )
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
                    text =
                        when (target) {
                            is WriteEditorTarget.Answer -> "当前平台暂不支持发布/编辑回答"
                            WriteEditorTarget.Pin -> "当前平台暂不支持发布想法"
                        },
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp),
                )
            } else {
                Column(
                    modifier =
                        Modifier
                            .fillMaxSize(),
                ) {
                    if (target is WriteEditorTarget.Pin) {
                        BasicTextField(
                            value = pinTitle,
                            onValueChange = { newValue -> pinTitle = newValue },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .testTag(WRITE_PIN_TITLE_TAG),
                            enabled = !isSubmitting,
                            textStyle =
                                MaterialTheme.typography.titleLarge.copy(
                                    color = MaterialTheme.colorScheme.onBackground,
                                ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            decorationBox = { innerTextField ->
                                Box(
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(top = 16.dp, bottom = 12.dp),
                                ) {
                                    if (pinTitle.text.isEmpty()) {
                                        Text(
                                            text = "标题",
                                            style = MaterialTheme.typography.titleLarge,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    innerTextField()
                                }
                            },
                        )
                    }
                    if (target is WriteEditorTarget.Pin && pinImages.isNotEmpty()) {
                        Column(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "图片 ${pinImages.size}/$PIN_IMAGE_LIMIT",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            pinImages.forEachIndexed { index, image ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    AsyncImage(
                                        model = rememberMarkdownImageModel(image.url),
                                        contentDescription = "想法图片 ${index + 1}",
                                        contentScale = ContentScale.Crop,
                                        modifier =
                                            Modifier
                                                .size(56.dp)
                                                .clip(MaterialTheme.shapes.small),
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "图片 ${index + 1}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onBackground,
                                        )
                                        Text(
                                            text = "${image.rawWidth} x ${image.rawHeight}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    TextButton(
                                        onClick = {
                                            pinImages = pinImages.filterIndexed { itemIndex, _ -> itemIndex != index }
                                        },
                                        enabled = !isSubmitting,
                                    ) {
                                        Text("删除")
                                    }
                                }
                            }
                        }
                    }
                    BasicTextField(
                        value = content,
                        onValueChange = { newValue -> content = newValue },
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .testTag(target.contentTag),
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
                                        .padding(
                                            top = if (target is WriteEditorTarget.Pin) 4.dp else 16.dp,
                                            bottom = 160.dp,
                                        ),
                            ) {
                                if (content.text.isEmpty()) {
                                    Text(
                                        text =
                                            when (target) {
                                                is WriteEditorTarget.Answer -> "请输入图文回答内容……"
                                                WriteEditorTarget.Pin -> "分享你此刻的想法..."
                                            },
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                innerTextField()
                            }
                        },
                    )
                }
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

    if (showSettingsSheet && target is WriteEditorTarget.Answer) {
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
        WriteAnswerPreviewSheet(
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
    var httpStatusException: HttpStatusException? = null
    val chain = buildList {
        var current: Throwable? = throwable
        while (current != null) {
            if (httpStatusException == null && current is HttpStatusException) {
                httpStatusException = current
            }
            add("${current::class.qualifiedName}: ${current.message.orEmpty()}")
            current = current.cause
        }
    }
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
