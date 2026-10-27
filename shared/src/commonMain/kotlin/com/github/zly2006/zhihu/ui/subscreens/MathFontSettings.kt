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

package com.github.zly2006.zhihu.ui.subscreens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.markdown.rememberCustomMathFonts
import com.github.zly2006.zhihu.markdown.rememberSelectedMathFont
import com.github.zly2006.zhihu.platform.rememberUserMessageSink
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
internal expect fun rememberMathFontPicker(onPicked: (String, ByteArray) -> Unit): (() -> Unit)?

@Composable
internal fun MathFontSettings() {
    val fonts = rememberCustomMathFonts()
    val selected = rememberSelectedMathFont()
    val messages = rememberUserMessageSink()
    val scope = rememberCoroutineScope()
    var busy by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    val picker = rememberMathFontPicker { name, bytes ->
        scope.launch {
            busy = true
            try {
                withContext(Dispatchers.Default) { fonts.import(name, bytes) }
                messages.showShortMessage("公式字体已导入")
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                messages.showLongMessage(error.message ?: "字体导入失败")
            } finally {
                busy = false
            }
        }
    }
    Column {
        Text("用于 Compose 正文中的公式；请选择含 MATH 表的 OTF 或 TTF 数学字体（不超过 20 MB）。")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
            OutlinedButton(onClick = { expanded = true }, enabled = !busy, modifier = Modifier.weight(1f).testTag("mathFont.select")) {
                Text(selected?.name ?: "默认字体", modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(text = { Text("默认字体") }, onClick = {
                        fonts.select(null)
                        expanded = false
                    })
                    fonts.list().forEach { font ->
                        DropdownMenuItem(text = { Text(font.name) }, onClick = {
                            fonts.select(font)
                            expanded = false
                        })
                    }
                }
            }
            OutlinedButton(onClick = { picker?.invoke() }, enabled = picker != null && !busy, modifier = Modifier.testTag("mathFont.import")) {
                Text(if (busy) "正在导入…" else "导入字体")
            }
        }
        if (picker == null) Text("当前平台暂不支持导入字体。")
    }
}
