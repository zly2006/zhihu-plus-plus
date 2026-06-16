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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fleeksoft.ksoup.Ksoup
import com.github.zly2006.zhihu.editor.compileMdToZhihuHtml
import com.github.zly2006.zhihu.editor.rememberImagePickerLauncher
import com.github.zly2006.zhihu.editor.rememberZhihuAnswerPublisher
import com.github.zly2006.zhihu.editor.zhihuHtmlToMarkdown
import com.github.zly2006.zhihu.navigation.Article
import com.github.zly2006.zhihu.navigation.ArticleType
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.WriteAnswer
import com.github.zly2006.zhihu.shared.platform.rememberPlainTextClipboard
import com.github.zly2006.zhihu.shared.platform.rememberSettingsStore
import com.github.zly2006.zhihu.shared.platform.rememberSystemUrlOpener
import com.github.zly2006.zhihu.shared.platform.rememberUserMessageSink
import com.github.zly2006.zhihu.shared.util.HttpStatusException
import com.github.zly2006.zhihu.ui.components.MyModalBottomSheet
import com.github.zly2006.zhihu.ui.components.SettingItemWithSwitch
import kotlinx.coroutines.launch

const val WRITE_ANSWER_CONTENT_TAG = "WriteAnswerContent"
private const val ZHIHU_MARKDOWN_SYNTAX_DOC_URL = "https://zhihu.melonhu.cn/docs/syntax"
private const val USE_ZHIHU_HEADINGS_KEY = "use_zhihu_headings"

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
    val copyToClipboard = rememberPlainTextClipboard()
    val settings = rememberSettingsStore()

    var content by remember { mutableStateOf(TextFieldValue("")) }
    var tocEnabled by remember { mutableStateOf(false) }
    var useZhihuHeadings by remember {
        mutableStateOf(settings.getBoolean(USE_ZHIHU_HEADINGS_KEY, true))
    }
    var isSubmitting by remember { mutableStateOf(false) }
    var existingAnswerId by remember { mutableStateOf<Long?>(null) }
    var isDetecting by remember { mutableStateOf(false) }
    var isLoadingExistingAnswer by remember { mutableStateOf(false) }
    var isUploadingImage by remember { mutableStateOf(false) }
    var errorDialogMessage by remember { mutableStateOf<String?>(null) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var isQuestionDetailExpanded by rememberSaveable(destination.questionId) { mutableStateOf(false) }
    val settingsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val questionTitle = destination.questionTitle.ifBlank { "写回答" }
    val questionDetail = remember(destination.questionDetail) {
        destination.questionDetail.toQuestionDetailPlainText()
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
                content = insertTextAtSelection(content, snippet)
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
                    Text("写回答")
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
                            if (!publisher.isSupported) return@Button
                            if (content.text.isBlank()) {
                                userMessages.showShortMessage("内容为空")
                                return@Button
                            }
                            if (isSubmitting) return@Button
                            isSubmitting = true
                            coroutineScope.launch {
                                runCatching {
                                    val html = compileMdToZhihuHtml(
                                        markdown = content.text,
                                        publisher = publisher,
                                        useZhihuHeadings = useZhihuHeadings,
                                    )
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
                                    errorDialogMessage = buildErrorDialogMessage("发布失败", e)
                                }
                                isSubmitting = false
                            }
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
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 16.dp)
                .imePadding()
                .verticalScroll(scrollState),
        ) {
            Spacer(Modifier.height(12.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                shape = MaterialTheme.shapes.large,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = questionTitle,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    if (questionDetail.isNotBlank()) {
                        FilledTonalButton(
                            onClick = { isQuestionDetailExpanded = !isQuestionDetailExpanded },
                            enabled = !isSubmitting,
                        ) {
                            Text(if (isQuestionDetailExpanded) "收起详情" else "展开详情")
                        }
                        if (isQuestionDetailExpanded) {
                            Text(
                                text = questionDetail,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            if (!publisher.isSupported) {
                Text(
                    text = "当前平台暂不支持发布/编辑回答",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    shape = MaterialTheme.shapes.large,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isDetecting || isLoadingExistingAnswer) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(
                                text = when {
                                    isDetecting -> "正在检测已发布回答…"
                                    isLoadingExistingAnswer -> "正在加载内容…"
                                    existingAnswerId != null -> "编辑已有回答"
                                    else -> "发布新回答"
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (launchImagePicker != null) {
                                FilledTonalButton(
                                    onClick = launchImagePicker,
                                    enabled = publisher.isSupported && !isSubmitting && !isUploadingImage,
                                ) {
                                    Icon(Icons.Filled.Image, contentDescription = "插入图片")
                                    Spacer(Modifier.width(6.dp))
                                    Text(if (isUploadingImage) "上传中…" else "图片")
                                }
                            }
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
                                            val html = compileMdToZhihuHtml(
                                                markdown = content.text,
                                                publisher = publisher,
                                                useZhihuHeadings = useZhihuHeadings,
                                            )
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
                                            errorDialogMessage = buildErrorDialogMessage("保存草稿失败", e)
                                        }
                                        isSubmitting = false
                                    }
                                },
                                enabled = publisher.isSupported && !isSubmitting,
                            ) {
                                Icon(Icons.Default.Save, contentDescription = "保存草稿")
                                Spacer(Modifier.width(6.dp))
                                Text("保存")
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = "支持 Markdown，发布前会自动转换为知乎编辑器 HTML。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))
            BasicTextField(
                value = content,
                onValueChange = { newValue -> content = newValue },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 420.dp)
                    .testTag(WRITE_ANSWER_CONTENT_TAG),
                enabled = !isSubmitting,
                textStyle = TextStyle(
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 17.sp,
                    lineHeight = 26.sp,
                ),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 32.dp),
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
            Spacer(Modifier.height(24.dp))
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
                title = { Text("使用知乎特色标题") },
                description = { Text("把 #、## 渲染为知乎对应的标题层级，更高层级会降级为加粗段落。") },
                checked = useZhihuHeadings,
                onCheckedChange = {
                    useZhihuHeadings = it
                    settings.putBoolean(USE_ZHIHU_HEADINGS_KEY, it)
                },
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            Spacer(Modifier.height(8.dp))
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

private fun String.toQuestionDetailPlainText(): String =
    Ksoup
        .parse(this)
        .text()
        .replace('\u00A0', ' ')
        .replace(Regex("\\n{3,}"), "\n\n")
        .trim()
