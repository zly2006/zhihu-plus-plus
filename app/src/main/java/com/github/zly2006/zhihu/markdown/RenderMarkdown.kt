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

package com.github.zly2006.zhihu.markdown

import android.content.Context
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.latex.rememberLatexFonts
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.Video
import com.github.zly2006.zhihu.navigation.resolveContent
import com.github.zly2006.zhihu.theme.ThemeManager
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import com.github.zly2006.zhihu.ui.components.OpenImageDialog
import com.github.zly2006.zhihu.ui.subscreens.PREF_FONT_SIZE
import com.github.zly2006.zhihu.ui.subscreens.PREF_LINE_HEIGHT
import com.github.zly2006.zhihu.util.luoTianYiUrlLauncher
import com.github.zly2006.zhihu.util.saveImageToGallery
import com.github.zly2006.zhihu.util.shareImage
import com.hrm.markdown.renderer.Markdown
import com.hrm.markdown.renderer.MarkdownImageData
import com.hrm.markdown.renderer.MarkdownTheme
import kotlinx.coroutines.launch

@Composable
fun RenderImage(
    data: MarkdownImageData,
    modifier: Modifier,
) {
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    var pressOffset by remember { mutableStateOf(DpOffset.Zero) }
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val httpClient = AccountData.httpClient(context)
    val hapticFeedback = LocalHapticFeedback.current

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = data.url,
            contentDescription = data.altText,
            modifier = modifier
                .fillMaxWidth(0.8f)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = {
                            OpenImageDialog(context, httpClient, data.url).show()
                        },
                        onLongPress = { offset ->
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            pressOffset = with(density) {
                                DpOffset(offset.x.toDp(), offset.y.toDp() - 20.dp)
                            }
                            expanded = true
                        },
                    )
                },
        )

        // DropdownMenu 在独立的 Popup 窗口中渲染，但其 Text 会注册到父级 SelectionRegistrar。
        // 当文本选择上下文菜单触发 isEntireContainerSelected → sort 时，
        // 跨窗口比较坐标会抛出 IllegalArgumentException: layouts are not part of the same hierarchy。
        DisableSelection {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                offset = pressOffset,
            ) {
                DropdownMenuItem(
                    text = { Text("查看图片") },
                    onClick = {
                        expanded = false
                        OpenImageDialog(context, httpClient, data.url).show()
                    },
                )
                DropdownMenuItem(
                    text = { Text("在浏览器中打开") },
                    onClick = {
                        expanded = false
                        luoTianYiUrlLauncher(context, data.url.toUri())
                    },
                )
                DropdownMenuItem(
                    text = { Text("保存图片") },
                    onClick = {
                        expanded = false
                        coroutineScope.launch {
                            saveImageToGallery(context, httpClient, data.url)
                        }
                    },
                )
                DropdownMenuItem(
                    text = { Text("分享图片") },
                    onClick = {
                        expanded = false
                        coroutineScope.launch {
                            shareImage(context, httpClient, data.url)
                        }
                    },
                )
            }
        }
    }
}

@Composable
fun RenderVideoBox(
    videoId: Long,
    thumbnailUrl: String?,
    modifier: Modifier = Modifier,
) {
    val navigator = LocalNavigator.current
    val shape = RoundedCornerShape(8.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable {
                navigator.onNavigate(Video(videoId))
            },
    ) {
        if (thumbnailUrl != null) {
            AsyncImage(
                model = thumbnailUrl,
                contentDescription = "视频封面",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Surface(
            modifier = Modifier.align(Alignment.Center),
            shape = CircleShape,
            color = Color.Black.copy(alpha = 0.56f),
            onClick = {
                navigator.onNavigate(Video(videoId))
            },
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "播放视频",
                tint = Color.White,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Composable
fun RenderMarkdown(
    html: String,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    selectable: Boolean = true,
    enableScroll: Boolean = true,
    header: (@Composable () -> Unit)? = null,
    footer: (@Composable () -> Unit)? = null,
) {
    val document = remember(html) { htmlToMdAst(html) }
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val preferences = remember { context.getSharedPreferences(PREFERENCE_NAME, Context.MODE_PRIVATE) }
    val fontSize = preferences.getInt(PREF_FONT_SIZE, 100)
    val lineHeight = preferences.getInt(PREF_LINE_HEIGHT, 160)
    val defaultTheme = MarkdownTheme.auto(ThemeManager.isDarkTheme())

    val fontResult = rememberLatexFonts(context, AccountData.httpClient(context))
    val mathFont = fontResult.downloaded?.mathFont ?: defaultTheme.mathFont

    val theme = defaultTheme.copy(
        bodyStyle = defaultTheme.bodyStyle.copy(
            fontSize = 16.sp * fontSize / 100,
            lineHeight = 16.sp * fontSize / 100 * lineHeight / 100,
        ),
        mathFontSize = 18f * fontSize / 100,
        mathFont = mathFont,
    )
    Markdown(
        document = document,
        modifier = modifier,
        imageContent = ::RenderImage,
        scrollState = scrollState,
        enableScroll = enableScroll,
        enableSelection = selectable,
        onLinkClick = { url ->
            resolveContent(url)?.let { navigator.onNavigate(it) }
                ?: luoTianYiUrlLauncher(context, url.toUri())
        },
        header = header,
        footer = footer,
        theme = theme,
    )
}
