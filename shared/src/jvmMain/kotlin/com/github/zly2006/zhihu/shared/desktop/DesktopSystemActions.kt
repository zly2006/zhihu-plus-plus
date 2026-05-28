package com.github.zly2006.zhihu.shared.desktop

import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.net.URI

internal fun openDesktopExternalUrl(url: String): Boolean = runCatching {
    if (!Desktop.isDesktopSupported()) {
        return@runCatching false
    }
    Desktop.getDesktop().browse(URI(url))
    true
}.getOrDefault(false)

internal fun copyDesktopPlainText(text: String) {
    Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
}
