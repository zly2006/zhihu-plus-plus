package com.hrm.markdown.renderer.internal.selection

import androidx.compose.ui.platform.ClipEntry

/** Creates a platform clipboard entry containing plain text. */
internal expect fun plainTextClipEntry(text: String): ClipEntry?
