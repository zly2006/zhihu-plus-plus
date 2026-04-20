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
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import coil3.compose.AsyncImage
import com.github.zly2006.zhihu.data.AccountData
import com.github.zly2006.zhihu.navigation.LocalNavigator
import com.github.zly2006.zhihu.navigation.resolveContent
import com.github.zly2006.zhihu.ui.PREFERENCE_NAME
import com.github.zly2006.zhihu.ui.components.OpenImageDislog
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
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    val httpClient = AccountData.httpClient(context)

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
                            val dialog = OpenImageDislog(context, httpClient, data.url)
                            dialog.show()
                        },
                        onLongPress = { offset ->
                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            pressOffset = with(density) {
                                DpOffset(offset.x.toDp(), offset.y.toDp() - 20.dp)
                            }
                            expanded = true
                        },
                    )
                },
        )

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            offset = pressOffset,
        ) {
            DropdownMenuItem(
                text = { Text("查看图片") },
                onClick = {
                    expanded = false
                    val dialog = OpenImageDislog(context, httpClient, data.url)
                    dialog.show()
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

@Composable
fun RenderMarkdown(
    html: String,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    selectable: Boolean = false,
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
    val theme = MarkdownTheme.auto().copy(
        bodyStyle = MarkdownTheme.auto().bodyStyle.copy(
            fontSize = 16.sp * fontSize / 100,
            lineHeight = 16.sp * fontSize / 100 * lineHeight / 100,
        ),
        mathFontSize = 18f * fontSize / 100,
    )

    if (selectable) {
        SelectionContainer(modifier = modifier) {
            Markdown(
                document = document,
                imageContent = ::RenderImage,
                scrollState = scrollState,
                enableScroll = enableScroll,
                onLinkClick = {
                    resolveContent(it.toUri())?.let(navigator.onNavigate)
                        ?: luoTianYiUrlLauncher(context, it.toUri())
                },
                theme = theme,
                header = header,
                footer = footer,
            )
        }
    } else {
        Markdown(
            document = document,
            modifier = modifier,
            imageContent = ::RenderImage,
            scrollState = scrollState,
            enableScroll = enableScroll,
            onLinkClick = {
                resolveContent(it.toUri())?.let(navigator.onNavigate)
                    ?: luoTianYiUrlLauncher(context, it.toUri())
            },
            theme = theme,
            header = header,
            footer = footer,
        )
    }
}
