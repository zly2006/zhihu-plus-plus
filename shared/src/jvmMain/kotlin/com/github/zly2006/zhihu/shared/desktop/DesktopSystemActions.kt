package com.github.zly2006.zhihu.shared.desktop

import java.awt.Desktop
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.net.URI

internal fun openDesktopExternalUrl(url: String): Boolean {
    if (!Desktop.isDesktopSupported()) {
        return false
    }
    Desktop.getDesktop().browse(URI(url))
    return true
}

internal fun copyDesktopPlainText(text: String) {
    Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
}
