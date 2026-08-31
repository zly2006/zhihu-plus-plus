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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.data.DataHolder
import com.github.zly2006.zhihu.data.ZhihuJson
import com.github.zly2006.zhihu.editor.PatchDraftRequest
import com.github.zly2006.zhihu.editor.PatchDraftSettings
import com.github.zly2006.zhihu.editor.PublishAnswerData
import com.github.zly2006.zhihu.editor.PublishAnswerRequest
import com.github.zly2006.zhihu.editor.PublishContentsTables
import com.github.zly2006.zhihu.editor.PublishDraft
import com.github.zly2006.zhihu.editor.PublishExtraInfo
import com.github.zly2006.zhihu.editor.PublishHybrid
import com.github.zly2006.zhihu.editor.PublishTrace
import com.github.zly2006.zhihu.editor.UnknownImageFormatException
import com.github.zly2006.zhihu.editor.ZhihuImageUploadSource
import com.github.zly2006.zhihu.editor.buildPcBusinessParams
import com.github.zly2006.zhihu.editor.compileMdToZhihuHtml
import com.github.zly2006.zhihu.editor.isImagePickerSupported
import com.github.zly2006.zhihu.editor.newPublishTraceId
import com.github.zly2006.zhihu.editor.parsePublishContentId
import com.github.zly2006.zhihu.editor.rememberImagePickerLauncher
import com.github.zly2006.zhihu.editor.uploadZhihuImage
import com.github.zly2006.zhihu.markdown.zhihuHtmlToMarkdown
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.WriteAnswer
import com.github.zly2006.zhihu.platform.rememberPlainTextClipboard
import com.github.zly2006.zhihu.platform.rememberSettingsStore
import com.github.zly2006.zhihu.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.ui.components.MyModalBottomSheet
import com.github.zly2006.zhihu.ui.components.SettingItemWithSwitch
import com.github.zly2006.zhihu.ui.components.WriteContentFabColumn
import com.github.zly2006.zhihu.ui.components.WriteContentMarkdownEditor
import com.github.zly2006.zhihu.ui.components.WriteContentPreviewSheet
import com.github.zly2006.zhihu.ui.components.replaceSelection
import com.github.zly2006.zhihu.util.raiseForStatus
import com.github.zly2006.zhihu.viewmodel.fetchContentDetail
import com.github.zly2006.zhihu.viewmodel.postSigned
import com.github.zly2006.zhihu.viewmodel.rememberPaginationEnvironment
import io.ktor.client.call.body
import io.ktor.client.request.header
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import kotlinx.serialization.json.JsonElement

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
    val environment = rememberPaginationEnvironment(false)
    val coroutineScope = rememberCoroutineScope()
    val copyToClipboard = rememberPlainTextClipboard()
    val settings = rememberSettingsStore()
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var editorActionsVisible by remember { mutableStateOf(true) }
    val actionVisibilityThreshold = with(LocalDensity.current) { 12.dp.roundToPx() }
    var accumulatedEditorScroll by remember { mutableStateOf(0f) }

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
        if (environment.authenticatedCookies()["d_c0"].isNullOrBlank()) return null
        val element = runCatching {
            environment.fetchJson(
                "https://api.zhihu.com/questions/${destination.questionId}",
                "relationship,relationship.my_answer",
            )
        }.getOrNull() ?: return null
        val relationship = ZhihuJson.decodeJson(DataHolder.QuestionRelationshipApiResponse.serializer(), element)
        val answerId = relationship.relationship
            ?.myAnswer
            ?.takeUnless { it.isDeleted == true }
            ?.answerId
            ?.toLongOrNull()
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
                val xsrf = environment.authenticatedCookies()["_xsrf"]
                    ?: error("缺少 _xsrf Cookie，无法写入回答草稿；请先确保已登录。")
                environment
                    .postSigned("https://www.zhihu.com/api/v4/questions/${destination.questionId}/draft") {
                        contentType(ContentType.Application.Json)
                        header(
                            HttpHeaders.Referrer,
                            "https://www.zhihu.com/question/${destination.questionId}/answer/${answerId ?: ""}",
                        )
                        header("x-xsrftoken", xsrf)
                        setBody(
                            PatchDraftRequest(
                                content = html,
                                settings = PatchDraftSettings(tableOfContentsEnabled = tocEnabled),
                            ),
                        )
                    }.raiseForStatus(dumpRequest = true)
                if (publish) {
                    val responseElement = environment
                        .postSigned("https://www.zhihu.com/api/v4/content/publish") {
                            contentType(ContentType.Application.Json)
                            header("x-xsrftoken", xsrf)
                            setBody(
                                PublishAnswerRequest(
                                    data = PublishAnswerData(
                                        publish = PublishTrace(traceId = newPublishTraceId()),
                                        draft = PublishDraft(
                                            isPublished = answerId != null,
                                            contentId = answerId?.toString(),
                                        ),
                                        extraInfo = PublishExtraInfo(
                                            questionId = destination.questionId.toString(),
                                            pcBusinessParams = buildPcBusinessParams(tocEnabled),
                                        ),
                                        hybrid = PublishHybrid(html),
                                        contentsTables = PublishContentsTables(tocEnabled),
                                    ),
                                ),
                            )
                        }.raiseForStatus(dumpRequest = true)
                        .body<JsonElement>()
                    val response = ZhihuJson.decodeJson(
                        DataHolder.ContentPublishResponse.serializer(),
                        responseElement,
                    )
                    if (response.message != "success") {
                        if (response.code == 103003) {
                            error(response.message ?: "已回答过该问题，创建回答失败")
                        }
                        error("发布失败: ${response.message ?: "unknown"}\n$responseElement")
                    }
                    parsePublishContentId(response.data?.result ?: error("发布成功但返回缺少 data.result"))
                        ?: error("发布成功但无法解析 publish.id")
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

    val launchImagePicker = if (isImagePickerSupported) {
        rememberImagePickerLauncher { picked ->
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
    } else {
        null
    }

    LaunchedEffect(destination.questionId) {
        isDetecting = true
        existingAnswerId = runCatching {
            ensureAnswerId()
        }.onFailure { e ->
            errorDialogMessage = buildWriteOperationErrorMessage("检测已有回答失败", e)
        }.getOrNull()
        isDetecting = false

        val answerId = existingAnswerId ?: return@LaunchedEffect
        if (content.text.isNotBlank()) return@LaunchedEffect
        isLoadingExistingAnswer = true
        runCatching {
            environment.fetchContentDetail(
                Article(type = ArticleType.Answer, id = answerId),
            ) as? DataHolder.Answer
        }.onSuccess { answer ->
            if (answer != null && content.text.isBlank()) {
                tocEnabled = answer.settings?.tableOfContents?.enabled ?: false
                content = TextFieldValue(zhihuHtmlToMarkdown(answer.editableContent ?: answer.content))
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
            AnimatedVisibility(
                visible = editorActionsVisible,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
            ) {
                WriteContentFabColumn(
                    previewEnabled = !isSubmitting && content.text.isNotBlank(),
                    imageEnabled = isImagePickerSupported && !isSubmitting && !isUploadingImage,
                    saveEnabled = !isSubmitting,
                    showImageButton = isImagePickerSupported,
                    isUploadingImage = isUploadingImage,
                    previewTag = WRITE_ANSWER_FAB_PREVIEW_TAG,
                    imageTag = WRITE_ANSWER_FAB_IMAGE_TAG,
                    saveTag = WRITE_ANSWER_FAB_SAVE_TAG,
                    onPreview = ::showPreview,
                    onImage = { launchImagePicker?.launch() },
                    onSave = { submitAnswer(publish = false) },
                )
            }
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
                bottomPadding = 280.dp,
                onVerticalScroll = { delta ->
                    if (accumulatedEditorScroll != 0f && (accumulatedEditorScroll > 0f) != (delta > 0f)) {
                        accumulatedEditorScroll = delta
                    } else {
                        accumulatedEditorScroll += delta
                    }
                    when {
                        accumulatedEditorScroll <= -actionVisibilityThreshold -> {
                            editorActionsVisible = false
                            accumulatedEditorScroll = 0f
                        }
                        accumulatedEditorScroll >= actionVisibilityThreshold -> {
                            editorActionsVisible = true
                            accumulatedEditorScroll = 0f
                        }
                    }
                },
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
