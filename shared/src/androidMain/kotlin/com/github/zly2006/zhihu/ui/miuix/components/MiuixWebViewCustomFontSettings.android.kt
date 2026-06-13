/*
 * Zhihu++ - Free & Ad-Free Zhihu client for Android.
 * Copyright (C) 2024-2026, zly2006 <i@zly2006.me>
 * Licensed under AGPL-3.0-only.
 */

package com.github.zly2006.zhihu.ui.miuix.components

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.github.zly2006.zhihu.shared.platform.androidUserMessageSink
import top.yukonga.miuix.kmp.basic.Button
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import java.io.File

@Composable
actual fun MiuixWebViewCustomFontSettings(
    customFontName: String?,
    onCustomFontNameChange: (String?) -> Unit,
) {
    val context = LocalContext.current
    val userMessages = androidUserMessageSink(context)
    val fontFilePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        val name = uri.lastPathSegment?.substringAfterLast('/') ?: uri.toString()
        val destFile = File(context.filesDir, "custom_font")
        context.contentResolver.openInputStream(uri)?.use { input ->
            destFile.outputStream().use { output -> input.copyTo(output) }
        }
        onCustomFontNameChange(name)
        userMessages.showShortMessage("字体已设置，重新打开文章后生效")
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Button(
            onClick = {
                fontFilePicker.launch(arrayOf("font/ttf", "font/otf", "application/octet-stream"))
            },
            modifier = Modifier.weight(1f),
        ) {
            Icon(Icons.Default.FolderOpen, contentDescription = null)
            Text("选择", modifier = Modifier.padding(start = 4.dp))
        }
        if (customFontName != null) {
            Button(
                onClick = {
                    File(context.filesDir, "custom_font").delete()
                    onCustomFontNameChange(null)
                    userMessages.showShortMessage("已清除自定义字体")
                },
                modifier = Modifier.weight(1f),
            ) {
                Icon(Icons.Default.Clear, contentDescription = null)
                Text("清除", modifier = Modifier.padding(start = 4.dp))
            }
        }
    }
}
