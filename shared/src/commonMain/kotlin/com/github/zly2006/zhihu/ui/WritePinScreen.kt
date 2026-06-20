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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.github.zly2006.zhihu.editor.UnknownImageFormatException
import com.github.zly2006.zhihu.editor.UploadedZhihuImage
import com.github.zly2006.zhihu.editor.ZhihuImageUploadSource
import com.github.zly2006.zhihu.editor.calculatePinHtmlTextLength
import com.github.zly2006.zhihu.editor.compileMdToZhihuHtml
import com.github.zly2006.zhihu.editor.rememberImagePickerLauncher
import com.github.zly2006.zhihu.editor.rememberZhihuPinPublisher
import com.github.zly2006.zhihu.editor.uploadZhihuImage
import com.github.zly2006.zhihu.markdown.rememberMarkdownImageModel
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.Pin
import com.github.zly2006.zhihu.shared.platform.rememberPlainTextClipboard
import com.github.zly2006.zhihu.shared.platform.rememberSettingsStore
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.ui.components.WriteContentFabColumn
import com.github.zly2006.zhihu.ui.components.WriteContentMarkdownEditor
import com.github.zly2006.zhihu.ui.components.WriteContentPreviewSheet
import com.github.zly2006.zhihu.viewmodel.rememberPaginationEnvironment
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

const val WRITE_PIN_TITLE_TAG = "WritePinTitle"
const val WRITE_PIN_CONTENT_TAG = "WritePinContent"
const val WRITE_PIN_FAB_PREVIEW_TAG = "WritePinFabPreview"
const val WRITE_PIN_FAB_IMAGE_TAG = "WritePinFabImage"
const val WRITE_PIN_FAB_SAVE_TAG = "WritePinFabSave"
const val WRITE_PIN_IMAGE_LIST_TAG = "WritePinImageList"

private const val PIN_IMAGE_LIMIT = 9

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WritePinScreen() {
    val navigator = LocalNavigator.current
    val userMessages = rememberUserMessageSink()
    val publisher = rememberZhihuPinPublisher()
    val coroutineScope = rememberCoroutineScope()
    val copyToClipboard = rememberPlainTextClipboard()
    val settings = rememberSettingsStore()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    var content by remember { mutableStateOf(TextFieldValue("")) }
    var title by remember { mutableStateOf(TextFieldValue("")) }
    var images by remember { mutableStateOf<List<UploadedZhihuImage>>(emptyList()) }
    var isSubmitting by remember { mutableStateOf(false) }
    var isUploadingImage by remember { mutableStateOf(false) }
    var errorDialogMessage by remember { mutableStateOf<String?>(null) }
    var showPreviewSheet by remember { mutableStateOf(false) }
    val previewSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var isPreviewLoading by remember { mutableStateOf(false) }
    var previewHtml by remember { mutableStateOf<String?>(null) }
    var previewMarkdown by remember { mutableStateOf<String?>(null) }
    var previewUseWebView by remember { mutableStateOf(false) }

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

    fun submitPin(publish: Boolean) {
        val markdownSnapshot = content.text
        val imagesSnapshot = images
        if (markdownSnapshot.isBlank() && imagesSnapshot.isEmpty()) {
            userMessages.showShortMessage("想法内容为空")
            return
        }
        if (isSubmitting) return
        isSubmitting = true
        coroutineScope.launch {
            runCatching {
                val html = compileMdToZhihuHtml(markdown = markdownSnapshot)
                val textLength = calculatePinHtmlTextLength(html)
                if (publish) {
                    publisher.publishPin(
                        title = title.text.trim(),
                        html = html,
                        textLength = textLength,
                        images = imagesSnapshot,
                    )
                } else {
                    publisher.savePinDraft(
                        title = title.text.trim(),
                        html = html,
                        textLength = textLength,
                        images = imagesSnapshot,
                    )
                    null
                }
            }.onSuccess { pinId ->
                if (publish) {
                    userMessages.showShortMessage("发布成功")
                    navigator.onNavigate(Pin(pinId ?: return@onSuccess))
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
        if (images.size >= PIN_IMAGE_LIMIT) {
            userMessages.showShortMessage("图片最多添加 $PIN_IMAGE_LIMIT 张")
            return@rememberImagePickerLauncher
        }
        isUploadingImage = true
        coroutineScope.launch {
            runCatching {
                uploadZhihuImage(
                    environment,
                    picked.bytes,
                    picked.mimeType,
                    picked.fileName,
                    ZhihuImageUploadSource.Pin,
                )
            }.onSuccess { uploaded ->
                images = images + uploaded
                userMessages.showShortMessage("图片已添加")
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("发想法")
                },
                navigationIcon = {
                    IconButton(onClick = navigator.onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    Button(
                        onClick = { submitPin(publish = true) },
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
                previewTag = WRITE_PIN_FAB_PREVIEW_TAG,
                imageTag = WRITE_PIN_FAB_IMAGE_TAG,
                saveTag = WRITE_PIN_FAB_SAVE_TAG,
                onPreview = ::showPreview,
                onImage = { launchImagePicker?.invoke() },
                onSave = { submitPin(publish = false) },
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
            Column(modifier = Modifier.fillMaxSize()) {
                BasicTextField(
                    value = title,
                    onValueChange = { newValue -> title = newValue },
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
                            if (title.text.isEmpty()) {
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
                if (images.isNotEmpty()) {
                    Column(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .testTag(WRITE_PIN_IMAGE_LIST_TAG),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "图片 ${images.size}/$PIN_IMAGE_LIMIT",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        images.forEachIndexed { index, image ->
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
                                        images = images.filterIndexed { itemIndex, _ -> itemIndex != index }
                                    },
                                    enabled = !isSubmitting,
                                ) {
                                    Text("删除")
                                }
                            }
                        }
                    }
                }
                WriteContentMarkdownEditor(
                    value = content,
                    onValueChange = { newValue -> content = newValue },
                    placeholder = "分享你此刻的想法...",
                    contentTag = WRITE_PIN_CONTENT_TAG,
                    enabled = !isSubmitting,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    topPadding = 4.dp,
                )
            }
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
            copyToClipboard("write-pin-error", message)
            userMessages.showShortMessage("已复制错误信息")
        },
    )
}
