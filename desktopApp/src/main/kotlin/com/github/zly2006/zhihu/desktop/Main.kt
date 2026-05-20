package com.github.zly2006.zhihu.desktop

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Zhihu++",
    ) {
        DesktopQrLoginScreen()
    }
}
