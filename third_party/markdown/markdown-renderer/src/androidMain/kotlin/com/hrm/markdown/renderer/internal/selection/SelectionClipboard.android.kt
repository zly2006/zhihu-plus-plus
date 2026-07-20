package com.hrm.markdown.renderer.internal.selection

import android.content.ClipData
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.ClipEntry

@OptIn(ExperimentalComposeUiApi::class)
internal actual fun plainTextClipEntry(text: String): ClipEntry? =
    ClipEntry(ClipData.newPlainText("markdown_selection", text))
